package com.omnicast.receiver.directstream

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Motor de Grabación PVR Asíncrono para TV en Vivo y Descarga VOD (Fase 4).
 * Ring Buffer de 16 MB con volcado en bloques alineados de 128 KB,
 * selección de resolución, punto de inicio exacto y soporte de audio demuxed.
 */
object PvrStreamRecorder {

    private val _progress = MutableStateFlow(PvrRecordProgress())
    val progress: StateFlow<PvrRecordProgress> = _progress.asStateFlow()

    @Volatile
    private var recordJob: Job? = null
    @Volatile
    private var tickerJob: Job? = null
    @Volatile
    private var currentOutFile: File? = null
    @Volatile
    private var isPausedInternal = false

    fun isRecordingActive(): Boolean = _progress.value.isRecording
    fun isPaused(): Boolean = _progress.value.isPaused

    fun pause() {
        if (_progress.value.isRecording && !_progress.value.isPaused) {
            isPausedInternal = true; _progress.value = _progress.value.copy(isPaused = true, statusNotice = "⏸ Grabación en Pausa")
        }
    }

    fun resume() {
        if (_progress.value.isRecording && _progress.value.isPaused) {
            isPausedInternal = false; _progress.value = _progress.value.copy(isPaused = false, statusNotice = "🔴 Grabando...")
        }
    }

    val recorderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startRecording(
        context: Context,
        title: String,
        url: String,
        headers: Map<String, String>,
        useUsb: Boolean,
        customFolder: String? = null,
        targetHeight: Int = 1080,
        timerMinutes: Int = 0,
        onStopped: (String) -> Unit
    ) {
        if (_progress.value.isRecording) return

        recorderScope.launch {
            val targetDir = TvStorageSafetyManager.getTargetDirectory(context, useUsb, customFolder, "PVR_Grabaciones")
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val safeTitle = title.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim().replace(' ', '_').ifBlank { "Canal_PVR" }
            val outFile = File(targetDir, "${safeTitle}_${targetHeight}p_$timeStamp.mp4")
            currentOutFile = outFile
            isPausedInternal = false

            _progress.value = PvrRecordProgress(
                isRecording = true, isPaused = false, isMovieDownload = false,
                filePath = outFile.absolutePath, channelTitle = title,
                targetFolder = targetDir.absolutePath, statusNotice = "🔴 Grabando..."
            )

            TvStorageSafetyManager.startWatchdog(targetDir.path, useUsb, recorderScope) { alert ->
                stopRecording(alert); recorderScope.launch(Dispatchers.Main) { onStopped(alert) }
            }

            if (timerMinutes > 0) {
                TvRecordTimerManager.startTimer(timerMinutes, recorderScope) {
                    stopRecording("⏰ Temporizador completado. Grabación guardada."); recorderScope.launch(Dispatchers.Main) { onStopped("TIMER_FINISHED") }
                }
            }

            tickerJob = recorderScope.launch(Dispatchers.Default) {
                while (isActive && _progress.value.isRecording) {
                    delay(1000L)
                    if (!isPausedInternal && _progress.value.isRecording) {
                        _progress.value = _progress.value.copy(durationSeconds = _progress.value.durationSeconds + 1L)
                    }
                }
            }

            recordJob = recorderScope.launch(Dispatchers.IO) {
                var fileOutput: FileOutputStream? = null
                val ringBuffer = PvrRingBuffer()
                try {
                    fileOutput = FileOutputStream(outFile, true)
                    ringBuffer.startFlusher(fileOutput, this)

                    val isHls = url.lowercase().run { contains(".m3u8") || contains("urlset") || contains(".txt") }
                    if (isHls) {
                        recordLiveHlsLoop(url, headers, ringBuffer, targetHeight)
                    } else {
                        recordDirectStream(url, headers, ringBuffer)
                    }
                } catch (e: Throwable) {
                    if (isActive) _progress.value = _progress.value.copy(errorMsg = "Error en grabación: ${e.message}")
                } finally {
                    fileOutput?.let { ringBuffer.flushAndClose(it) }
                    if (outFile.exists() && outFile.length() == 0L) runCatching { outFile.delete() }
                }
            }
        }
    }

    fun startMovieDownload(
        context: Context,
        title: String,
        url: String,
        headers: Map<String, String>,
        useUsb: Boolean,
        customFolder: String? = null,
        targetHeight: Int = 1080,
        expectedBytes: Long = 800_000_000L,
        onStopped: (String) -> Unit
    ) {
        if (_progress.value.isRecording) return

        recorderScope.launch {
            val targetDir = TvStorageSafetyManager.getTargetDirectory(context, useUsb, customFolder, "Pelis_descargas")
            val safeTitle = title.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim().replace(' ', '_').ifBlank { "pelicula_omnicast" }
            val lower = url.lowercase()
            val isHls = lower.contains(".m3u8") || lower.contains("urlset") || lower.contains(".txt")
            val isFmp4 = lower.contains("dailymotion.com") || lower.contains("dmcdn.net")
            val ext = if (isHls && !isFmp4) "ts" else "mp4"
            val outFile = File(targetDir, "${safeTitle}_${targetHeight}p.$ext")
            currentOutFile = outFile
            isPausedInternal = false

            _progress.value = PvrRecordProgress(
                isRecording = true, isPaused = false, isMovieDownload = true,
                totalExpectedBytes = expectedBytes,
                filePath = outFile.absolutePath, channelTitle = title,
                targetFolder = targetDir.absolutePath, statusNotice = "📥 Descargando película..."
            )

            TvStorageSafetyManager.startWatchdog(targetDir.path, useUsb, recorderScope) { alert ->
                stopRecording(alert); onStopped(alert)
            }

            tickerJob = recorderScope.launch(Dispatchers.Default) {
                while (isActive && _progress.value.isRecording) {
                    delay(1000L)
                    if (!isPausedInternal && _progress.value.isRecording) {
                        _progress.value = _progress.value.copy(durationSeconds = _progress.value.durationSeconds + 1L)
                    }
                }
            }

            recordJob = recorderScope.launch(Dispatchers.IO) {
                TvDownloadManager.downloadMovieInBridgeMode(
                    context = context, title = title, url = url, headers = headers,
                    expectedBytes = expectedBytes, useUsb = useUsb, targetDir = targetDir,
                    targetHeight = targetHeight,
                    onProgress = { p ->
                        if (!p.isDownloading && p.isCompleted) {
                            stopRecording("✅ Película guardada exitosamente"); recorderScope.launch(Dispatchers.Main) { onStopped("✅ Película guardada exitosamente") }
                        } else if (!p.isDownloading && p.errorMsg != null) {
                            stopRecording(p.errorMsg); recorderScope.launch(Dispatchers.Main) { onStopped(p.errorMsg) }
                        } else {
                            _progress.value = _progress.value.copy(progressPct = p.percent, recordedBytes = p.downloadedBytes, totalExpectedBytes = p.totalBytes)
                        }
                    }
                )
            }
        }
    }

    private suspend fun recordLiveHlsLoop(
        manifestUrl: String,
        headers: Map<String, String>,
        ringBuffer: PvrRingBuffer,
        targetHeight: Int
    ) = withContext(Dispatchers.IO) {
        val downloadedSegments = mutableSetOf<String>()
        var isFirstFetch = true

        while (isActive && _progress.value.isRecording) {
            if (isPausedInternal) { delay(1000L); continue }
            try {
                val masterText = fetchText(manifestUrl, headers)
                val master = PvrHlsMasterParser.parseMaster(masterText, manifestUrl)

                val chosenVariant = PvrHlsMasterParser.selectVariant(master.variants, targetHeight)
                val videoPlaylistUrl = chosenVariant?.url ?: manifestUrl
                val audioPlaylistUrl = chosenVariant?.audioGroupId?.let { master.audioTracks[it] }

                val videoText = if (chosenVariant != null) fetchText(videoPlaylistUrl, headers) else masterText
                val videoSegments = PvrHlsMasterParser.parseSegments(videoText, videoPlaylistUrl)

                val audioSegments = if (!audioPlaylistUrl.isNullOrBlank()) {
                    val aText = fetchText(audioPlaylistUrl, headers)
                    PvrHlsMasterParser.parseSegments(aText, audioPlaylistUrl)
                } else emptyList()

                if (isFirstFetch) {
                    isFirstFetch = false
                    val isVod = videoText.contains("#EXT-X-ENDLIST", ignoreCase = true) || videoText.contains("#EXT-X-PLAYLIST-TYPE:VOD", ignoreCase = true)
                    if (!isVod) {
                        if (videoSegments.size > 1) downloadedSegments.addAll(videoSegments.dropLast(1).filterNot { it.contains("init.mp4", true) })
                        if (audioSegments.size > 1) downloadedSegments.addAll(audioSegments.dropLast(1).filterNot { it.contains("init.mp4", true) })
                    }
                }

                for (i in videoSegments.indices) {
                    if (!isActive || !_progress.value.isRecording || isPausedInternal) break
                    val vSeg = videoSegments[i]
                    if (!downloadedSegments.contains(vSeg)) {
                        downloadSegmentToRing(vSeg, headers, ringBuffer)
                        downloadedSegments.add(vSeg)
                    }

                    val aSeg = audioSegments.getOrNull(i)
                    if (aSeg != null && !downloadedSegments.contains(aSeg)) {
                        downloadSegmentToRing(aSeg, headers, ringBuffer)
                        downloadedSegments.add(aSeg)
                    }
                }

                if (downloadedSegments.size > 800) {
                    downloadedSegments.removeAll(downloadedSegments.take(200).toSet())
                }
            } catch (_: Throwable) {
                delay(2000L)
            }
            _progress.value = _progress.value.copy(recordedBytes = ringBuffer.totalBytesWritten)
            delay(2500L)
        }
    }

    private fun downloadSegmentToRing(segUrl: String, headers: Map<String, String>, ringBuffer: PvrRingBuffer) {
        val conn = PvrNetworkHelper.openConnection(segUrl, headers, 6000, 8000)
        val buf = ByteArray(32 * 1024)
        conn.inputStream.use { input ->
            while (!isPausedInternal) {
                val r = input.read(buf)
                if (r == -1) break
                ringBuffer.write(buf, 0, r)
            }
        }
    }

    private suspend fun recordDirectStream(
        url: String,
        headers: Map<String, String>,
        ringBuffer: PvrRingBuffer
    ) = withContext(Dispatchers.IO) {
        val conn = PvrNetworkHelper.openConnection(url, headers, 8000, 12000)
        val buf = ByteArray(64 * 1024)
        conn.inputStream.use { input ->
            while (isActive && _progress.value.isRecording) {
                if (isPausedInternal) { delay(500L); continue }
                val r = input.read(buf)
                if (r == -1) break
                ringBuffer.write(buf, 0, r)
                _progress.value = _progress.value.copy(recordedBytes = ringBuffer.totalBytesWritten)
            }
        }
    }

    fun stopRecording(reasonNotice: String? = null) {
        TvStorageSafetyManager.stopWatchdog(); TvRecordTimerManager.cancelTimer()
        TvWebPvrBridge.abortActiveDownload()
        tickerJob?.cancel(); tickerJob = null; recordJob?.cancel(); recordJob = null; isPausedInternal = false
        currentOutFile?.let { if (it.exists() && it.length() == 0L) runCatching { it.delete() } }
        val last = _progress.value
        _progress.value = last.copy(isRecording = false, isPaused = false, statusNotice = reasonNotice ?: "✅ Grabación guardada exitosamente")
    }

    fun stopAndDiscardRecording(reasonNotice: String? = null) {
        val f = currentOutFile; stopRecording(reasonNotice ?: "🗑️ Grabación descartada y eliminada")
        try { f?.takeIf { it.exists() }?.delete() } catch (_: Throwable) {}
        currentOutFile = null
    }

    private fun fetchText(url: String, headers: Map<String, String>): String =
        PvrNetworkHelper.fetchText(url, headers, 5000, 6000)
}
