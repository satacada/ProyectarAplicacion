package com.omnicast.receiver.ui.components

import android.annotation.SuppressLint
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.omnicast.core.protocol.SignalingMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * Reproductor Web HTML5 / Chromium para TV Box.
 * Soporta flujos web protegidos con tokens de seguridad (DropCDN, Fastream, HLS)
 * con controles OSD D-Pad 100% nativos (TvWebUnifiedOverlay y TvWebSettingsModal).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TvWebStreamScreen(
    streamUrl: String, title: String, headers: Map<String, String> = emptyMap(),
    startPositionMs: Long = 0L, subtitles: List<SignalingMessage.SubtitleTrack> = emptyList(), timestamp: Long = 0L,
    commandFlow: Flow<SignalingMessage.PlayControl>? = null, displayMode: SignalingMessage.SetDisplayMode? = null,
    onPlaybackStatus: ((posMs: Long, durMs: Long, isPlaying: Boolean) -> Unit)? = null,
    onPlaybackFailed: ((String) -> Unit)? = null, onCloseClick: () -> Unit = {}
) {
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val pvrProgress by com.omnicast.receiver.directstream.PvrStreamRecorder.progress.collectAsState()
    val timerState by com.omnicast.receiver.directstream.TvRecordTimerManager.timerState.collectAsState()
    var showRecordDialog by remember { mutableStateOf(false) }
    var showStopConfirm by remember { mutableStateOf(false) }
    var isVideoReady by remember(streamUrl, timestamp) { mutableStateOf(false) }
    var fatalErrorReason by remember(streamUrl, timestamp) { mutableStateOf<String?>(null) }
    var playerState by remember(streamUrl, timestamp) {
        mutableStateOf(
            TvWebPlayerState(
                currentPositionMs = startPositionMs,
                isControlsVisible = true
            )
        )
    }
    var customView by remember(streamUrl, timestamp) { mutableStateOf<View?>(null) }
    var webViewRef by remember(streamUrl, timestamp) { mutableStateOf<WebView?>(null) }

    LaunchedEffect(streamUrl, timestamp, isVideoReady, fatalErrorReason) {
        if (fatalErrorReason != null) {
            delay(3000L)
            onPlaybackFailed?.invoke(fatalErrorReason ?: "FATAL_ERROR")
            return@LaunchedEffect
        }
        if (!isVideoReady) {
            delay(22000L)
            if (!isVideoReady && fatalErrorReason == null) {
                fatalErrorReason = "WATCHDOG_TIMEOUT"
            }
        }
    }

    LaunchedEffect(displayMode, webViewRef) {
        val dm = displayMode ?: return@LaunchedEffect; val wv = webViewRef ?: return@LaunchedEffect
        wv.evaluateJavascript("if (typeof setDisplayMode === 'function') setDisplayMode('${dm.mode}', ${dm.zoom}, ${dm.panY}, ${dm.panX});", null)
    }

    LaunchedEffect(commandFlow, webViewRef) {
        commandFlow?.collect { msg ->
            val wv = webViewRef ?: return@collect
            when (msg.action) {
                "PLAY" -> wv.evaluateJavascript("playStream();", null)
                "PAUSE" -> wv.evaluateJavascript("pauseStream();", null)
                "TOGGLE_PLAY" -> wv.evaluateJavascript("togglePlay();", null)
                "SEEK" -> wv.evaluateJavascript("seekTo(${msg.positionMs / 1000.0});", null)
                "SEEK_REL" -> wv.evaluateJavascript("seekRel(${msg.positionMs / 1000.0});", null)
                "VOLUME" -> wv.evaluateJavascript("setVolume(${msg.volume.coerceIn(0f, 1f)});", null)
                "STOP" -> wv.evaluateJavascript("stopStream();", null)
                "SHOW_CONTROLS" -> playerState = playerState.copy(isControlsVisible = true)
                "CYCLE_QUALITY" -> wv.evaluateJavascript("cycleQuality();") { r ->
                    val q = r?.replace("\"", "")?.takeIf { it != "null" } ?: "Auto"
                    playerState = playerState.copy(currentQuality = q)
                    android.widget.Toast.makeText(wv.context, "📺 Calidad: $q", android.widget.Toast.LENGTH_SHORT).show()
                }
                "TOGGLE_SUBTITLES", "SUBTITLE_TOGGLE" -> wv.evaluateJavascript("toggleSubtitles();") { r ->
                    val s = r?.replace("\"", "")?.takeIf { it != "null" } ?: "Desactivados"
                    playerState = playerState.copy(currentSubtitle = s)
                    android.widget.Toast.makeText(wv.context, "💬 Subtítulos: $s", android.widget.Toast.LENGTH_SHORT).show()
                }
                "TOGGLE_MENU", "CYCLE_ZOOM" -> {
                    playerState = playerState.copy(isSettingsOpen = !playerState.isSettingsOpen)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (customView != null) {
            AndroidView(factory = { customView!! }, modifier = Modifier.fillMaxSize())
        } else {
            key(streamUrl, timestamp) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewRef = this
                            isFocusable = false; isFocusableInTouchMode = false; clearFocus(); setBackgroundColor(android.graphics.Color.BLACK)
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.userAgentString = headers["User-Agent"] ?: "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
                            val cm = CookieManager.getInstance(); cm.setAcceptCookie(true); cm.setAcceptThirdPartyCookies(this, true)
                            headers["Cookie"]?.let { ck ->
                                runCatching {
                                    val sOrigin = try { Uri.parse(streamUrl).let { "${it.scheme}://${it.host}/" } } catch (_: Throwable) { null }
                                    val rOrigin = try { headers["Referer"]?.let { Uri.parse(it) }?.let { "${it.scheme}://${it.host}/" } } catch (_: Throwable) { null }
                                    val targets = listOfNotNull(streamUrl, sOrigin, headers["Referer"], rOrigin).distinct()
                                    ck.split("; ").map { it.trim() }.filter { it.isNotBlank() }.forEach { p -> targets.forEach { cm.setCookie(it, p) } }
                                    cm.flush()
                                }
                            }
                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun onPlaybackStatus(c: Double, d: Double, p: Boolean) {
                                    val cMs = (c * 1000).toLong()
                                    val dMs = if (d.isFinite() && d > 0) (d * 1000).toLong() else 0L
                                    mainHandler.post {
                                        if (p && (cMs > 50L || dMs > 0L)) isVideoReady = true
                                        playerState = playerState.copy(
                                            currentPositionMs = cMs,
                                            durationMs = if (dMs > 0L) dMs else playerState.durationMs,
                                            isPlaying = p
                                        )
                                        onPlaybackStatus?.invoke(cMs, dMs, p)
                                    }
                                }
                                @JavascriptInterface
                                fun onFatalError(reason: String) {
                                    mainHandler.post {
                                        if (!isVideoReady && fatalErrorReason == null) fatalErrorReason = reason
                                    }
                                }
                                @JavascriptInterface
                                fun onMetadataLoaded(qStr: String, sStr: String) {
                                    val qList = qStr.split(",").filter { it.isNotBlank() }
                                    val sList = sStr.split(",").filter { it.isNotBlank() }
                                    mainHandler.post {
                                        playerState = playerState.copy(availableQualities = qList, availableSubtitles = sList)
                                    }
                                }
                            }, "AndroidBridge")
                            webViewClient = com.omnicast.receiver.ui.TvReceiverHelper.createWebClient(streamUrl, headers)
                            webChromeClient = object : WebChromeClient() {
                                override fun onShowCustomView(v: View?, c: CustomViewCallback?) { customView = v }
                                override fun onHideCustomView() { customView = null }
                                override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                                    cm?.let { com.omnicast.receiver.log.ReceiverLogger.log("TvWebStreamJS", "[${it.messageLevel()}] ${it.message()}") }
                                    return super.onConsoleMessage(cm)
                                }
                            }

                            val hlsCode = TvWebHtmlBuilder.getHlsJs(ctx)
                            val safeUrl = streamUrl.replace("\\", "\\\\").replace("\"", "\\\"")
                            val startSec = (startPositionMs / 1000.0).coerceAtLeast(0.0)
                            val baseUrl = com.omnicast.receiver.ui.TvReceiverHelper.resolveBaseUrl(streamUrl, headers)
                            val html = TvWebHtmlBuilder.buildPlayerHtml(safeUrl, startSec, hlsCode, headers)
                            loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
                        }
                    },
                    onRelease = { webView ->
                        try {
                            if (webViewRef == webView) webViewRef = null
                            webView.stopLoading(); webView.onPause(); webView.evaluateJavascript("stopStream();", null)
                            webView.loadUrl("about:blank"); webView.clearHistory(); webView.clearCache(true)
                            webView.removeAllViews(); webView.freeMemory(); webView.destroy()
                        } catch (_: Throwable) {}
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Cortina de carga moderna que oculta Chromium y su icono nativo de Play hasta recibir video
        if (!isVideoReady) {
            TvWebLoadingOverlay(title = title, errorMessage = fatalErrorReason)
        }

        // Superposición nativa de controles D-Pad
        TvWebUnifiedOverlay(
            title = title,
            badgeText = "🌐 OmniCast HTML5",
            state = playerState,
            isRecording = pvrProgress.isRecording,
            onRecordClick = {
                if (pvrProgress.isRecording) {
                    showStopConfirm = true
                } else {
                    showRecordDialog = true
                }
            },
            onTogglePlay = { webViewRef?.evaluateJavascript("togglePlay();", null) },
            onSeekRelative = { deltaMs -> webViewRef?.evaluateJavascript("seekRel(${deltaMs / 1000.0});", null) },
            onToggleSettings = { playerState = playerState.copy(isSettingsOpen = !playerState.isSettingsOpen) },
            onShowControls = { playerState = playerState.copy(isControlsVisible = true) },
            onHideControls = { playerState = playerState.copy(isControlsVisible = false) }
        )

        // Insignia flotante de grabación OSD
        TvRecFloatingBadge(
            isRecording = pvrProgress.isRecording,
            durationSeconds = pvrProgress.durationSeconds,
            remainingSeconds = if (timerState.isRunning && timerState.remainingSeconds > 0) timerState.remainingSeconds else -1L,
            isMovieDownload = pvrProgress.isMovieDownload,
            progressPct = pvrProgress.progressPct,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = 24.dp),
            onClick = { showStopConfirm = true }
        )

        // Modal de configuración nativo D-Pad
        TvWebSettingsModal(
            state = playerState,
            isRecording = pvrProgress.isRecording,
            onSelectQuality = { q ->
                playerState = playerState.copy(currentQuality = q)
                webViewRef?.evaluateJavascript("setQuality('$q');", null)
            },
            onSelectSubtitle = { s ->
                playerState = playerState.copy(currentSubtitle = s)
                webViewRef?.evaluateJavascript("setSubtitle('$s');", null)
            },
            onToggleAspect = {
                val nextAspect = when (playerState.aspectRatio) {
                    "FIT" -> "FILL"
                    "FILL" -> "EXTEND_16_9"
                    else -> "FIT"
                }
                playerState = playerState.copy(aspectRatio = nextAspect)
                webViewRef?.evaluateJavascript("setDisplayMode('$nextAspect', 1.0, 0, 0);", null)
            },
            onClose = { playerState = playerState.copy(isSettingsOpen = false) },
            onToggleRecord = {
                playerState = playerState.copy(isSettingsOpen = false)
                if (pvrProgress.isRecording) {
                    showStopConfirm = true
                } else {
                    showRecordDialog = true
                }
            }
        )

        if (showRecordDialog && streamUrl.isNotBlank()) {
            val isLive = playerState.durationMs <= 0L || listOf("news", "sky", "directo", "envivo", "en vivo", "live", "linear", "chunklist").any { title.contains(it, true) || streamUrl.contains(it, true) }

            TvRecordDialog(
                title = title.ifBlank { if (isLive) "Canal en Vivo" else "Película en reproducción" },
                streamUrl = streamUrl, headers = headers,
                durationMs = playerState.durationMs, isLive = isLive,
                onDismiss = { showRecordDialog = false },
                onRecordingStarted = {}
            )
        }

        TvConfirmStopRecordDialog(
            isOpen = showStopConfirm,
            recordedDurationSec = pvrProgress.durationSeconds,
            onKeep = {
                showStopConfirm = false
                com.omnicast.receiver.directstream.PvrStreamRecorder.stopRecording("⏹ Grabación guardada")
                android.widget.Toast.makeText(webViewRef?.context ?: return@TvConfirmStopRecordDialog, "💾 Grabación conservada exitosamente", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDiscard = {
                showStopConfirm = false
                com.omnicast.receiver.directstream.PvrStreamRecorder.stopAndDiscardRecording("🗑️ Grabación descartada y eliminada")
                android.widget.Toast.makeText(webViewRef?.context ?: return@TvConfirmStopRecordDialog, "🗑️ Grabación descartada y eliminada", android.widget.Toast.LENGTH_SHORT).show()
            },
            onContinue = {
                showStopConfirm = false
            }
        )
    }
}
