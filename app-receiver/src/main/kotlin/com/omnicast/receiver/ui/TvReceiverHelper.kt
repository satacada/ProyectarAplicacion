package com.omnicast.receiver.ui

import android.content.Context
import android.media.AudioManager
import com.omnicast.receiver.directstream.ExoPlayerTvManager
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Utilidades auxiliares para TvReceiverActivity desacopladas para mantener el archivo <= 300 líneas.
 */
object TvReceiverHelper {

    fun getLocalIpAddress(): String = try {
        NetworkInterface.getNetworkInterfaces()?.toList()?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address && it.hostAddress != "127.0.0.1" }?.hostAddress ?: "127.0.0.1"
    } catch (_: Throwable) { "127.0.0.1" }

    private val YOUTUBE_REGEX = Regex("(?:watch\\?v=|shorts/|youtu\\.be/|embed/|live/)([a-zA-Z0-9_-]{8,32})")
    private val YOUTUBE_CHANNEL_REGEX = Regex("(?:channel=|channel/)([a-zA-Z0-9_-]+)")

    fun extractYouTubeId(url: String): String? {
        if (url.startsWith("live_stream?channel=")) return url
        if (!url.contains("youtube.com") && !url.contains("youtu.be")) return null
        if (url.contains("channel=") || url.contains("channel/")) {
            val chId = YOUTUBE_CHANNEL_REGEX.find(url)?.groupValues?.get(1)
            if (!chId.isNullOrBlank()) return "live_stream?channel=$chId"
        }
        return YOUTUBE_REGEX.find(url)?.groupValues?.get(1)
    }

    fun applySystemAndPlayerVolume(context: Context, exoPlayerManager: ExoPlayerTvManager, vol: Float) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = (vol.coerceIn(0f, 1f) * maxVol).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
            }
        } catch (_: Throwable) {}
        exoPlayerManager.setVolume(vol)
    }

    fun shouldUseWebPlayer(url: String, headers: Map<String, String>): Boolean {
        val target = headers["X-Target-Player"]?.uppercase()
        if (target == "WEBVIEW" || target == "CHROMIUM_PURE" || target == "CHROMIUM_ANTI_CORS") return true
        if (target == "EXOPLAYER") return false

        val lowerUrl = url.lowercase()
        val isDash = lowerUrl.contains(".mpd") || lowerUrl.contains("format=dash")
        if (isDash) return false

        val seed = com.omnicast.core.domain.usecases.StreamManifestProbe.lookupSeedProfile(url)
            ?: com.omnicast.core.domain.usecases.StreamManifestProbe.lookupSeedProfile(headers["Referer"] ?: "")
        if (seed != null && seed.first == "WEBVIEW") return true

        val hasCfClearance = headers["Cookie"]?.contains("cf_clearance", ignoreCase = true) == true
        val hasSignedToken = com.omnicast.core.domain.usecases.StreamManifestProbe.hasSignedSecurityToken(url)

        return hasSignedToken || hasCfClearance
    }

    fun shouldUseAntiCorsInterceptor(url: String, headers: Map<String, String>): Boolean {
        val target = headers["X-Target-Player"]?.uppercase()
        if (target == "CHROMIUM_PURE") return false
        return true
    }

    fun createWebClient(streamUrl: String, headers: Map<String, String>): android.webkit.WebViewClient {
        return object : android.webkit.WebViewClient() {
            override fun shouldInterceptRequest(v: android.webkit.WebView?, r: android.webkit.WebResourceRequest?): android.webkit.WebResourceResponse? {
                if (shouldUseAntiCorsInterceptor(streamUrl, headers)) {
                    val resp = com.omnicast.receiver.ui.components.TvWebResourceInterceptor.shouldIntercept(r, headers)
                    if (resp != null) return resp
                }
                return super.shouldInterceptRequest(v, r)
            }

            override fun onReceivedSslError(view: android.webkit.WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                handler?.proceed()
            }
        }
    }

    fun resolveBaseUrl(streamUrl: String, headers: Map<String, String>): String {
        val streamHostOrigin = try {
            val uri = android.net.Uri.parse(streamUrl)
            "${uri.scheme}://${uri.host}/"
        } catch (_: Throwable) { null }
        if (headers["X-Base-Url-Policy"] == "STREAM_HOST" || headers["X-Target-Player"] == "CHROMIUM_ANTI_CORS") {
            if (streamHostOrigin != null) return streamHostOrigin
        }
        return headers["Referer"]?.takeIf { it.isNotBlank() } ?: streamHostOrigin ?: "https://omnicast.tv"
    }

    suspend fun buildRecordTelemetry(
        context: Context,
        receiverState: androidx.compose.runtime.MutableState<ReceiverUiState>,
        exoPlayerManager: ExoPlayerTvManager
    ): com.omnicast.core.protocol.SignalingMessage.RecordTelemetry {
        val units = com.omnicast.receiver.directstream.TvStorageSafetyManager.detectAvailableStorageUnits(context)
        val internalUnit = units.firstOrNull { !it.isUsb }
        val usbUnit = units.firstOrNull { it.isUsb }
        val pvrProgress = com.omnicast.receiver.directstream.PvrStreamRecorder.progress.value

        val (url, title, hdrs, isLive) = when (val cur = receiverState.value) {
            is ReceiverUiState.DirectStreamPlaying -> {
                val live = exoPlayerManager.durationMs <= 0L || cur.title.contains("news|sky|live|directo".toRegex(RegexOption.IGNORE_CASE))
                listOf(cur.streamUrl, cur.title, exoPlayerManager.currentHeaders, live)
            }
            is ReceiverUiState.WebStreamPlaying -> {
                listOf(cur.streamUrl, cur.title, cur.headers, cur.title.contains("news|sky|live|directo".toRegex(RegexOption.IGNORE_CASE)))
            }
            else -> listOf("", "", emptyMap<String, String>(), false)
        }
        val streamUrl = url as String
        val streamTitle = title as String
        @Suppress("UNCHECKED_CAST")
        val streamHeaders = hdrs as Map<String, String>
        val liveMode = isLive as Boolean

        val resolutions = if (streamUrl.isNotBlank()) {
            com.omnicast.receiver.directstream.PvrHlsMasterParser.probeResolutions(streamUrl, streamHeaders).ifEmpty { listOf(1080, 720, 480) }
        } else listOf(1080, 720, 480)

        val intPath = com.omnicast.receiver.directstream.TvStorageSafetyManager.getPvrRecordingsDir(context, false).absolutePath
        val usbPath = if (usbUnit != null) com.omnicast.receiver.directstream.TvStorageSafetyManager.getPvrRecordingsDir(context, true).absolutePath else ""

        return com.omnicast.core.protocol.SignalingMessage.RecordTelemetry(
            isRecording = pvrProgress.isRecording,
            isMovieDownload = pvrProgress.isMovieDownload,
            canRecord = internalUnit?.canRecord == true || usbUnit?.canRecord == true,
            warningMessage = internalUnit?.warningMessage ?: usbUnit?.warningMessage,
            freeBytes = internalUnit?.freeBytes ?: 0L,
            usableBytes = internalUnit?.usableBytes ?: 0L,
            isUsbAvailable = usbUnit != null,
            usbFreeBytes = usbUnit?.freeBytes ?: 0L,
            usbUsableBytes = usbUnit?.usableBytes ?: 0L,
            internalPath = intPath,
            usbPath = usbPath,
            availableResolutions = resolutions,
            currentTitle = streamTitle,
            isLive = liveMode,
            progressPct = pvrProgress.progressPct,
            recordedDurationSec = pvrProgress.durationSeconds,
            targetFolder = pvrProgress.targetFolder.ifBlank { intPath },
            bytesDownloaded = pvrProgress.recordedBytes,
            totalSizeBytes = pvrProgress.totalExpectedBytes,
            isResumable = pvrProgress.isResumable,
            errorNotice = pvrProgress.errorMsg ?: pvrProgress.statusNotice
        )
    }

    fun handleRemoteRecord(
        context: Context,
        scope: kotlinx.coroutines.CoroutineScope,
        receiverState: androidx.compose.runtime.MutableState<ReceiverUiState>,
        exoPlayerManager: ExoPlayerTvManager,
        action: String = "RECORD",
        param: String? = null,
        sendSignaling: ((com.omnicast.core.protocol.SignalingMessage) -> Unit)? = null
    ) {
        if (action == "REQUEST_RECORD_INFO") {
            scope.launch {
                val telemetry = buildRecordTelemetry(context, receiverState, exoPlayerManager)
                sendSignaling?.invoke(telemetry)
            }
            return
        }

        if (action == "STOP_RECORD") {
            if (param == "DISCARD") {
                com.omnicast.receiver.directstream.PvrStreamRecorder.stopAndDiscardRecording("🗑️ Grabación descartada desde control remoto")
                android.widget.Toast.makeText(context, "🗑️ Grabación descartada y eliminada", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                com.omnicast.receiver.directstream.PvrStreamRecorder.stopRecording("⏹ Grabación detenida desde control remoto")
                android.widget.Toast.makeText(context, "⏹ Grabación finalizada y guardada", android.widget.Toast.LENGTH_SHORT).show()
            }
            scope.launch { sendSignaling?.invoke(buildRecordTelemetry(context, receiverState, exoPlayerManager)) }
            return
        }

        if (action == "START_RECORD" || action == "RECORD") {
            if (com.omnicast.receiver.directstream.PvrStreamRecorder.isRecordingActive()) {
                com.omnicast.receiver.directstream.PvrStreamRecorder.stopRecording("⏹ Grabación detenida desde control remoto")
                android.widget.Toast.makeText(context, "⏹ Grabación finalizada y guardada", android.widget.Toast.LENGTH_SHORT).show()
                scope.launch { sendSignaling?.invoke(buildRecordTelemetry(context, receiverState, exoPlayerManager)) }
                return
            }

            val currentState = receiverState.value
            val (url, title, hdrs, durationMs) = when (currentState) {
                is ReceiverUiState.DirectStreamPlaying -> listOf(currentState.streamUrl, currentState.title ?: "Canal en Vivo", exoPlayerManager.currentHeaders, exoPlayerManager.durationMs)
                is ReceiverUiState.WebStreamPlaying -> listOf(
                    currentState.streamUrl,
                    currentState.title,
                    currentState.headers,
                    com.omnicast.receiver.directstream.TvWebPvrBridge.activeDurationMs.takeIf { it > 0L } ?: 5_400_000L
                )
                is ReceiverUiState.YouTubePlaying -> {
                    android.widget.Toast.makeText(context, "⚠️ YouTube no admite grabación PVR directa (reproductor protegido por Google)", android.widget.Toast.LENGTH_LONG).show()
                    return
                }
                else -> return
            }

            val sUrl = url as String
            val sTitle = title as String
            @Suppress("UNCHECKED_CAST")
            val sHdrs = hdrs as Map<String, String>
            val sDur = durationMs as Long
            val isLive = sDur <= 0L || sTitle.contains("news|sky|live|directo".toRegex(RegexOption.IGNORE_CASE))

            val parts = param?.split(",")?.associate { it.substringBefore('=') to it.substringAfter('=') } ?: emptyMap()
            val useUsb = parts["useUsb"]?.toBooleanStrictOrNull() ?: false
            val targetRes = parts["res"]?.toIntOrNull() ?: 1080
            val timerMin = parts["timer"]?.toIntOrNull() ?: if (isLive) 60 else 0
            val customFolder = parts["folder"]?.takeIf { it.isNotBlank() }

            if (isLive) {
                com.omnicast.receiver.directstream.PvrStreamRecorder.startRecording(
                    context = context, title = sTitle, url = sUrl, headers = sHdrs,
                    useUsb = useUsb, customFolder = customFolder, targetHeight = targetRes,
                    timerMinutes = timerMin,
                    onStopped = { reason ->
                        android.widget.Toast.makeText(context, reason, android.widget.Toast.LENGTH_LONG).show()
                        if (reason == "TIMER_FINISHED") {
                            exoPlayerManager.stop("Timer PVR Finalizado")
                            receiverState.value = ReceiverUiState.Standby
                        }
                    }
                )
                android.widget.Toast.makeText(context, "🔴 Grabación PVR Iniciada (${if (timerMin > 0) "$timerMin min" else "Continuo"})", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                com.omnicast.receiver.directstream.PvrStreamRecorder.startMovieDownload(
                    context = context, title = sTitle, url = sUrl, headers = sHdrs,
                    useUsb = useUsb, customFolder = customFolder, targetHeight = targetRes,
                    expectedBytes = sDur.takeIf { it > 0 }?.let { (it / 1000L) * 165_000L } ?: 800_000_000L,
                    onStopped = { reason -> android.widget.Toast.makeText(context, reason, android.widget.Toast.LENGTH_LONG).show() }
                )
                android.widget.Toast.makeText(context, "📥 Descarga de Película Iniciada", android.widget.Toast.LENGTH_SHORT).show()
            }
            scope.launch { sendSignaling?.invoke(buildRecordTelemetry(context, receiverState, exoPlayerManager)) }
        }
    }
}
