package com.omnicast.receiver.directstream

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PvrRecordProgress(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val recordedBytes: Long = 0L,
    val durationSeconds: Long = 0L,
    val filePath: String = "",
    val channelTitle: String = "",
    val errorMsg: String? = null,
    val statusNotice: String? = null
)

/**
 * Motor de Grabación PVR para TV en Vivo (LIVE_HLS) y Flujos Directos.
 * Captura continua en tiempo real (1x) sin recodificación, con soporte de pausa por escenas.
 */
object PvrStreamRecorder {

    private val _progress = MutableStateFlow(PvrRecordProgress())
    val progress: StateFlow<PvrRecordProgress> = _progress.asStateFlow()

    @Volatile
    private var recordJob: Job? = null
    @Volatile
    private var currentOutFile: File? = null
    @Volatile
    private var isPausedInternal = false

    fun isRecordingActive(): Boolean = _progress.value.isRecording

    fun isPaused(): Boolean = _progress.value.isPaused

    fun pause() {
        if (_progress.value.isRecording && !_progress.value.isPaused) {
            isPausedInternal = true
            _progress.value = _progress.value.copy(isPaused = true, statusNotice = "⏸ Grabación en Pausa")
        }
    }

    fun resume() {
        if (_progress.value.isRecording && _progress.value.isPaused) {
            isPausedInternal = false
            _progress.value = _progress.value.copy(isPaused = false, statusNotice = "🔴 Grabando...")
        }
    }

    suspend fun startRecording(
        context: Context,
        title: String,
        url: String,
        headers: Map<String, String>,
        useUsb: Boolean,
        scope: CoroutineScope,
        timerMinutes: Int = 0,
        onStopped: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (_progress.value.isRecording) {
            return@withContext
        }

        val targetDir = TvStorageSafetyManager.getPvrRecordingsDir(context, useUsb)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim().replace(' ', '_').ifBlank { "Canal_PVR" }
        val outFile = File(targetDir, "${safeTitle}_$timeStamp.mp4")
        currentOutFile = outFile
        isPausedInternal = false

        _progress.value = PvrRecordProgress(
            isRecording = true,
            isPaused = false,
            filePath = outFile.absolutePath,
            channelTitle = title,
            statusNotice = "🔴 Grabando..."
        )

        // 1. Iniciar Supervisor de Almacenamiento en Caliente (Watchdog cada 5s)
        TvStorageSafetyManager.startWatchdog(targetDir.path, useUsb, scope) { alertMsg ->
            stopRecording(alertMsg)
            onStopped(alertMsg)
        }

        // 2. Iniciar Temporizador si se especificó
        if (timerMinutes > 0) {
            TvRecordTimerManager.startTimer(timerMinutes, scope) {
                stopRecording("⏰ Temporizador completado. Grabación guardada.")
                onStopped("TIMER_FINISHED")
            }
        }

        recordJob = scope.launch(Dispatchers.IO) {
            var fileOutput: FileOutputStream? = null
            try {
                fileOutput = FileOutputStream(outFile, true)
                val isHls = url.lowercase().run { contains(".m3u8") || contains("urlset") || contains(".txt") }

                if (isHls) {
                    recordLiveHlsLoop(url, headers, fileOutput)
                } else {
                    recordDirectStream(url, headers, fileOutput)
                }
            } catch (e: Throwable) {
                if (isActive) {
                    _progress.value = _progress.value.copy(errorMsg = "Error en grabación: ${e.message ?: "Conexión perdida"}")
                }
            } finally {
                runCatching { fileOutput?.flush() }
                runCatching { fileOutput?.close() }
            }
        }
    }

    private suspend fun recordLiveHlsLoop(
        manifestUrl: String,
        headers: Map<String, String>,
        output: FileOutputStream
    ) = withContext(Dispatchers.IO) {
        val downloadedSegments = mutableSetOf<String>()
        var recordedBytes = 0L
        var startTs = System.currentTimeMillis()
        var accumulatedDurationSec = 0L

        while (isActive && _progress.value.isRecording) {
            if (isPausedInternal) {
                delay(1000L)
                startTs = System.currentTimeMillis() // no sumar duración mientras está en pausa
                continue
            }

            try {
                val text = fetchText(manifestUrl, headers)
                val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                val subUrl = lines.firstOrNull { !it.startsWith("#") && (it.contains(".m3u8") || it.contains(".txt")) }
                    ?.let { resolveRelativeUrl(manifestUrl, it) }
                val finalManifestUrl = subUrl ?: manifestUrl
                val finalLines = if (subUrl != null) fetchText(subUrl, headers).lines().map { it.trim() } else lines

                val segments = finalLines.filter { it.isNotEmpty() && !it.startsWith("#") }
                    .map { resolveRelativeUrl(finalManifestUrl, it) }

                for (segUrl in segments) {
                    if (!isActive || !_progress.value.isRecording || isPausedInternal) break
                    if (downloadedSegments.contains(segUrl)) continue

                    val conn = (URL(segUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 6000
                        readTimeout = 8000
                        headers.forEach { (k, v) -> setRequestProperty(k, v) }
                    }
                    val buf = ByteArray(32 * 1024)
                    conn.inputStream.use { input ->
                        while (isActive && _progress.value.isRecording && !isPausedInternal) {
                            val r = input.read(buf)
                            if (r == -1) break
                            output.write(buf, 0, r)
                            recordedBytes += r
                        }
                    }
                    output.flush()
                    downloadedSegments.add(segUrl)
                    if (downloadedSegments.size > 800) {
                        downloadedSegments.removeAll(downloadedSegments.take(200).toSet())
                    }
                }
            } catch (_: Throwable) {
                delay(2000L)
            }

            val now = System.currentTimeMillis()
            accumulatedDurationSec += ((now - startTs) / 1000L).coerceAtLeast(0L)
            startTs = now

            _progress.value = _progress.value.copy(
                recordedBytes = recordedBytes,
                durationSeconds = accumulatedDurationSec
            )

            delay(2500L)
        }
    }

    private suspend fun recordDirectStream(
        url: String,
        headers: Map<String, String>,
        output: FileOutputStream
    ) = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 12000
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        val buf = ByteArray(64 * 1024)
        var recordedBytes = 0L
        val startTs = System.currentTimeMillis()

        conn.inputStream.use { input ->
            while (isActive && _progress.value.isRecording) {
                if (isPausedInternal) {
                    delay(500L)
                    continue
                }
                val r = input.read(buf)
                if (r == -1) break
                output.write(buf, 0, r)
                recordedBytes += r
                val dur = (System.currentTimeMillis() - startTs) / 1000L
                _progress.value = _progress.value.copy(
                    recordedBytes = recordedBytes,
                    durationSeconds = dur
                )
            }
        }
    }

    fun stopRecording(reasonNotice: String? = null) {
        TvStorageSafetyManager.stopWatchdog()
        TvRecordTimerManager.cancelTimer()
        recordJob?.cancel()
        recordJob = null
        isPausedInternal = false

        val last = _progress.value
        _progress.value = last.copy(
            isRecording = false,
            isPaused = false,
            statusNotice = reasonNotice ?: "✅ Grabación guardada exitosamente"
        )
    }

    private fun fetchText(url: String, headers: Map<String, String>): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 6000
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun resolveRelativeUrl(baseUrl: String, relative: String): String {
        if (relative.startsWith("http://") || relative.startsWith("https://")) return relative
        return try { URI(baseUrl).resolve(relative).toString() } catch (_: Throwable) { relative }
    }
}
