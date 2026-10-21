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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * Puente JavaScript para capturar eventos de reproducción del reproductor HTML5 en TV Box.
 */
class WebStreamBridge(
    private val onStatus: ((posMs: Long, durMs: Long, isPlaying: Boolean) -> Unit)?
) {
    @JavascriptInterface
    fun onPlaybackStatus(curSec: Float, durSec: Float, isPlaying: Boolean) {
        val curMs = (curSec * 1000).toLong()
        val durMs = if (durSec.isNaN() || durSec.isInfinite() || durSec < 0f) 0L else (durSec * 1000).toLong()
        onStatus?.invoke(curMs, durMs, isPlaying)
    }
}

private object WebPlayerAssetHelper {
    private var cachedHlsJs: String? = null
    fun getHlsJs(context: Context): String {
        cachedHlsJs?.let { return it }
        return try {
            context.assets.open("hls.light.min.js").bufferedReader().use { it.readText() }.also { cachedHlsJs = it }
        } catch (_: Throwable) { "" }
    }
}

/**
 * Reproductor Web HTML5 / Chromium para TV Box.
 * Ejecuta flujos web protegidos con tokens de seguridad (DropCDN, Fastream, HLS)
 * dentro del stack de red nativo de Chromium sin riesgo de HTTP 403.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TvWebStreamScreen(
    streamUrl: String,
    title: String,
    headers: Map<String, String> = emptyMap(),
    startPositionMs: Long = 0L,
    timestamp: Long = 0L,
    commandFlow: Flow<SignalingMessage.PlayControl>? = null,
    onPlaybackStatus: ((posMs: Long, durMs: Long, isPlaying: Boolean) -> Unit)? = null,
    onCloseClick: () -> Unit = {}
) {
    var showBanner by remember(streamUrl, timestamp) { mutableStateOf(true) }
    var customView by remember(streamUrl, timestamp) { mutableStateOf<View?>(null) }
    var webViewRef by remember(streamUrl, timestamp) { mutableStateOf<WebView?>(null) }

    LaunchedEffect(streamUrl, timestamp) {
        delay(4000)
        showBanner = false
    }

    LaunchedEffect(commandFlow, webViewRef) {
        commandFlow?.collect { msg ->
            val wv = webViewRef ?: return@collect
            when (msg.action) {
                "PLAY" -> wv.evaluateJavascript("playStream();", null)
                "PAUSE" -> wv.evaluateJavascript("pauseStream();", null)
                "TOGGLE_PLAY" -> wv.evaluateJavascript("togglePlay();", null)
                "SEEK" -> {
                    val sec = msg.positionMs / 1000.0
                    wv.evaluateJavascript("seekTo($sec);", null)
                }
                "SEEK_REL" -> {
                    val secDelta = msg.positionMs / 1000.0
                    wv.evaluateJavascript("seekRel($secDelta);", null)
                }
                "VOLUME" -> {
                    val vol = msg.volume.coerceIn(0f, 1f)
                    wv.evaluateJavascript("setVolume($vol);", null)
                }
                "STOP" -> wv.evaluateJavascript("stopStream();", null)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 24.dp)
                .size(38.dp)
                .background(Color(0x990F172A), RoundedCornerShape(10.dp))
        ) {
            ModernExitIcon()
        }

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
                            settings.loadWithOverviewMode = true
                            settings.userAgentString = headers["User-Agent"] ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            addJavascriptInterface(WebStreamBridge(onPlaybackStatus), "AndroidBridge")
                            webViewClient = WebViewClient()
                            webChromeClient = object : WebChromeClient() {
                                override fun onShowCustomView(v: View?, c: CustomViewCallback?) { customView = v }
                                override fun onHideCustomView() { customView = null }
                            }

                            val hlsCode = WebPlayerAssetHelper.getHlsJs(ctx)
                            val safeUrl = streamUrl.replace("\\", "\\\\").replace("\"", "\\\"")
                            val startSec = (startPositionMs / 1000.0).coerceAtLeast(0.0)
                            val baseUrl = headers["Referer"]?.takeIf { it.isNotBlank() } ?: try {
                                val uri = Uri.parse(streamUrl)
                                "${uri.scheme}://${uri.host}/"
                            } catch (_: Throwable) { "https://omnicast.tv" }

                            val html = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                <style>
                                  * { margin:0; padding:0; box-sizing:border-box; }
                                  html, body { width:100vw; height:100vh; overflow:hidden; background:#000; }
                                  video { width:100vw; height:100vh; object-fit:contain; background:#000; border:none; outline:none; }
                                </style>
                                <script>$hlsCode</script>
                                </head>
                                <body>
                                  <video id="vplayer" autoplay playsinline controls></video>
                                  <script>
                                    var vid = document.getElementById('vplayer');
                                    var streamUrl = "$safeUrl";
                                    var startSec = $startSec;
                                    var hasResumed = false;

                                    function reportStatus() {
                                      if (window.AndroidBridge && vid) {
                                        var cur = vid.currentTime || 0;
                                        var dur = vid.duration || 0;
                                        var playing = !vid.paused && !vid.ended && vid.readyState > 2;
                                        window.AndroidBridge.onPlaybackStatus(cur, dur, playing);
                                      }
                                    }

                                    vid.addEventListener('timeupdate', reportStatus);
                                    vid.addEventListener('play', reportStatus);
                                    vid.addEventListener('pause', reportStatus);
                                    vid.addEventListener('ended', reportStatus);

                                    function seekStart() {
                                      if (startSec > 0 && !hasResumed && vid) {
                                        hasResumed = true;
                                        vid.currentTime = startSec;
                                      }
                                    }
                                    vid.addEventListener('loadedmetadata', seekStart);
                                    vid.addEventListener('canplay', seekStart);

                                    function initHls() {
                                      if (window.Hls && Hls.isSupported()) {
                                        var hls = new Hls({ enableWorker: true, lowLatencyMode: false, backBufferLength: 90 });
                                        hls.loadSource(streamUrl);
                                        hls.attachMedia(vid);
                                        hls.on(Hls.Events.MANIFEST_PARSED, function() { vid.play().catch(function(){}); });
                                        hls.on(Hls.Events.ERROR, function(ev, data) {
                                          if (data.fatal) {
                                            if (data.type === Hls.ErrorTypes.NETWORK_ERROR) hls.startLoad();
                                            else if (data.type === Hls.ErrorTypes.MEDIA_ERROR) hls.recoverMediaError();
                                            else { vid.src = streamUrl; vid.play().catch(function(){}); }
                                          }
                                        });
                                      } else {
                                        vid.src = streamUrl;
                                        vid.play().catch(function(){});
                                      }
                                    }

                                    if (streamUrl.indexOf('.m3u8') !== -1) {
                                      initHls();
                                    } else {
                                      vid.src = streamUrl;
                                      vid.play().catch(function(){});
                                    }

                                    function playStream() { if (vid) vid.play().catch(function(){}); }
                                    function pauseStream() { if (vid) vid.pause(); }
                                    function togglePlay() { if (vid) { if (vid.paused) vid.play().catch(function(){}); else vid.pause(); } }
                                    function seekTo(s) { if (vid) vid.currentTime = Math.max(0, s); }
                                    function seekRel(d) { if (vid) vid.currentTime = Math.max(0, (vid.currentTime || 0) + d); }
                                    function setVolume(v) { if (vid) vid.volume = Math.max(0, Math.min(1, v)); }
                                    function stopStream() { if (vid) { vid.pause(); vid.removeAttribute('src'); vid.load(); } }

                                    setInterval(reportStatus, 1000);
                                  </script>
                                </body>
                                </html>
                            """.trimIndent()
                            loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
                        }
                    },
                    onRelease = { webView ->
                        try {
                            if (webViewRef == webView) webViewRef = null
                            webView.stopLoading()
                            webView.evaluateJavascript("stopStream();", null)
                            webView.loadUrl("about:blank")
                            webView.clearHistory()
                            webView.removeAllViews()
                            webView.destroy()
                        } catch (_: Throwable) {}
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AnimatedVisibility(
            visible = showBanner,
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xDD0F172A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "OmniCast Web Player", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
