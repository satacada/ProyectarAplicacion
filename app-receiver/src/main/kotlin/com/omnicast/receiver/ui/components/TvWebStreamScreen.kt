package com.omnicast.receiver.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.omnicast.core.protocol.SignalingMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Puente JavaScript para capturar eventos de reproducción del reproductor HTML5 en TV Box.
 */
class WebStreamBridge(
    private val onStatus: ((posMs: Long, durMs: Long, isPlaying: Boolean) -> Unit)?,
    private val onFailed: ((String) -> Unit)? = null,
    private val onToastMsg: ((String) -> Unit)? = null
) {
    @JavascriptInterface
    fun onPlaybackStatus(curSec: Float, durSec: Float, isPlaying: Boolean) {
        val curMs = (curSec * 1000).toLong()
        val durMs = if (durSec.isNaN() || durSec.isInfinite() || durSec < 0f) 0L else (durSec * 1000).toLong()
        onStatus?.invoke(curMs, durMs, isPlaying)
    }
    @JavascriptInterface
    fun onPlaybackFailed(reason: String) {
        com.omnicast.receiver.log.ReceiverLogger.log("WebStreamBridge", "❌ Error en reproductor web: $reason")
        onFailed?.invoke(reason)
    }
    @JavascriptInterface
    fun onToast(msg: String) {
        onToastMsg?.invoke(msg)
    }
}

private object WebPlayerAssetHelper {
    private var cachedHlsJs: String? = null
    fun getHlsJs(ctx: Context): String = cachedHlsJs ?: try { ctx.assets.open("hls.light.min.js").bufferedReader().use { it.readText() }.also { cachedHlsJs = it } } catch (_: Throwable) { "" }
}

/**
 * Reproductor Web HTML5 / Chromium para TV Box.
 * Soporta control de reproducción remoto y ajuste de proporción / Zoom TV.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TvWebStreamScreen(
    streamUrl: String,
    title: String,
    headers: Map<String, String> = emptyMap(),
    startPositionMs: Long = 0L,
    subtitles: List<SignalingMessage.SubtitleTrack> = emptyList(),
    timestamp: Long = 0L,
    commandFlow: Flow<SignalingMessage.PlayControl>? = null,
    displayMode: SignalingMessage.SetDisplayMode? = null,
    onPlaybackStatus: ((posMs: Long, durMs: Long, isPlaying: Boolean) -> Unit)? = null,
    onPlaybackFailed: ((String) -> Unit)? = null,
    onCloseClick: () -> Unit = {}
) {
    var showBanner by remember(streamUrl, timestamp) { mutableStateOf(true) }
    var zoomBadgeText by remember { mutableStateOf<String?>(null) }
    var showZoomBadge by remember { mutableStateOf(false) }
    var customView by remember(streamUrl, timestamp) { mutableStateOf<View?>(null) }
    var webViewRef by remember(streamUrl, timestamp) { mutableStateOf<WebView?>(null) }
    val scope = rememberCoroutineScope()
    var toastJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(streamUrl, timestamp) { delay(4000); showBanner = false }

    LaunchedEffect(displayMode, webViewRef) {
        val dm = displayMode ?: return@LaunchedEffect
        val wv = webViewRef ?: return@LaunchedEffect
        wv.evaluateJavascript("if (typeof setDisplayMode === 'function') setDisplayMode('${dm.mode}', ${dm.zoom}, ${dm.panY});", null)
        zoomBadgeText = when {
            dm.mode == "FILL" || dm.mode == "EXTEND_16_9" -> "📺 Escala: 16:9 (Llenar Pantalla)"
            dm.mode == "FIT" -> "📱 Escala: Original Fit (100%)"
            else -> "🔍 Zoom TV: ${(dm.zoom * 100).toInt()}%"
        }
        showZoomBadge = true
        delay(2200)
        showZoomBadge = false
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
                "CYCLE_ZOOM" -> wv.evaluateJavascript("cycleZoom();", null)
                "TOGGLE_SUBTITLES" -> wv.evaluateJavascript("toggleSubtitles();", null)
                "CYCLE_QUALITY" -> wv.evaluateJavascript("cycleQuality();", null)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 24.dp).size(38.dp)
                .background(Color(0x990F172A), RoundedCornerShape(10.dp))
        ) { ModernExitIcon() }

        if (customView != null) {
            AndroidView(factory = { customView!! }, modifier = Modifier.fillMaxSize())
        } else {
            key(streamUrl, timestamp) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewRef = this
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true; settings.allowFileAccess = true; settings.allowContentAccess = true
                            settings.cacheMode = WebSettings.LOAD_NO_CACHE
                            settings.allowFileAccessFromFileURLs = true; settings.allowUniversalAccessFromFileURLs = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.userAgentString = headers["User-Agent"] ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                            val streamOrigin = try { val uri = Uri.parse(streamUrl); if (!uri.scheme.isNullOrBlank() && !uri.host.isNullOrBlank()) "${uri.scheme}://${uri.host}/" else null } catch (_: Throwable) { null }
                            val ref = headers["Referer"]
                            val baseUrl = ref ?: (streamOrigin ?: "https://omnicast.tv/")
                            val cm = CookieManager.getInstance(); cm.setAcceptCookie(true); cm.setAcceptThirdPartyCookies(this, true)
                            headers["Cookie"]?.let { ck -> runCatching { ck.split("; ").map { it.trim() }.filter { it.isNotBlank() }.forEach { p -> cm.setCookie(streamUrl, p); streamOrigin?.let { cm.setCookie(it, p) }; headers["Referer"]?.takeIf { it.isNotBlank() }?.let { cm.setCookie(it, p) } }; cm.flush() } }
                            addJavascriptInterface(
                                WebStreamBridge(
                                    onPlaybackStatus,
                                    onPlaybackFailed,
                                    onToastMsg = { msg ->
                                        toastJob?.cancel()
                                        toastJob = scope.launch {
                                            zoomBadgeText = msg
                                            showZoomBadge = true
                                            delay(2500)
                                            showZoomBadge = false
                                        }
                                    }
                                ),
                                "AndroidBridge"
                            )
                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(v: WebView?, r: WebResourceRequest?): WebResourceResponse? =
                                    TvWebResourceInterceptor.shouldIntercept(r, headers) ?: super.shouldInterceptRequest(v, r)
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onShowCustomView(v: View?, c: CustomViewCallback?) { customView = v }
                                override fun onHideCustomView() { customView = null }
                                override fun onConsoleMessage(cm: ConsoleMessage?): Boolean { cm?.let { com.omnicast.receiver.log.ReceiverLogger.log("TvWebStreamJS", "[${it.messageLevel()}] ${it.message()}") }; return super.onConsoleMessage(cm) }
                            }

                            val hlsCode = WebPlayerAssetHelper.getHlsJs(ctx)
                            val html = TvWebPlayerHtmlBuilder.build(hlsCode, streamUrl, startPositionMs, subtitles)
                            loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
                        }
                    },
                    onRelease = { webView ->
                        try {
                            if (webViewRef == webView) webViewRef = null
                            webView.stopLoading(); webView.evaluateJavascript("stopStream();", null)
                            webView.loadUrl("about:blank"); webView.clearHistory(); webView.removeAllViews(); webView.destroy()
                        } catch (_: Throwable) {}
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AnimatedVisibility(visible = showBanner, exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xDD0F172A)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "OmniCast Web Player", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        AnimatedVisibility(visible = showZoomBadge && zoomBadgeText != null, exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xEE0F172A)), shape = RoundedCornerShape(10.dp)) {
                Text(text = zoomBadgeText ?: "", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
        }
    }
}
