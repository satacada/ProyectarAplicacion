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
    private val onStatus: ((posMs: Long, durMs: Long, isPlaying: Boolean) -> Unit)?,
    private val onFailed: ((String) -> Unit)? = null
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
                            settings.allowFileAccessFromFileURLs = true; settings.allowUniversalAccessFromFileURLs = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.userAgentString = headers["User-Agent"] ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                            CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            headers["Cookie"]?.let { ck -> runCatching { CookieManager.getInstance().setCookie(headers["Referer"] ?: streamUrl, ck) } }
                            addJavascriptInterface(WebStreamBridge(onPlaybackStatus, onPlaybackFailed), "AndroidBridge")
                            webViewClient = WebViewClient()
                            webChromeClient = object : WebChromeClient() {
                                override fun onShowCustomView(v: View?, c: CustomViewCallback?) { customView = v }
                                override fun onHideCustomView() { customView = null }
                                override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                                    cm?.let { com.omnicast.receiver.log.ReceiverLogger.log("TvWebStreamJS", "[${it.messageLevel()}] ${it.message()}") }
                                    return super.onConsoleMessage(cm)
                                }
                            }

                            val hlsCode = WebPlayerAssetHelper.getHlsJs(ctx)
                            val safeUrl = streamUrl.replace("\\", "\\\\").replace("\"", "\\\"")
                            val startSec = (startPositionMs / 1000.0).coerceAtLeast(0.0)
                            val baseUrl = headers["Referer"]?.takeIf { it.isNotBlank() } ?: try {
                                val uri = Uri.parse(streamUrl); "${uri.scheme}://${uri.host}/"
                            } catch (_: Throwable) { "https://omnicast.tv/" }

                            val html = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                <style>
                                  * { margin:0; padding:0; box-sizing:border-box; }
                                  html, body { width:100vw; height:100vh; overflow:hidden; background:#000; position:relative; }
                                  video { width:100vw; height:100vh; object-fit:contain; background:#000; border:none; outline:none; transition:transform 0.2s ease, object-fit 0.2s ease; }
                                  video::-webkit-media-controls-overlay-play-button { display:none !important; opacity:0 !important; pointer-events:none !important; width:0 !important; height:0 !important; }
                                  video::-webkit-media-controls-start-playback-button { display:none !important; opacity:0 !important; pointer-events:none !important; width:0 !important; height:0 !important; }
                                  video::-webkit-media-controls-overlay-enclosure { display:none !important; }
                                  .spinner { position:absolute; top:50%; left:50%; width:60px; height:60px; margin:-30px 0 0 -30px; border:4px solid rgba(255,255,255,0.15); border-top-color:#38bdf8; border-radius:50%; animation:spin 0.8s linear infinite; pointer-events:none; z-index:10; opacity:1; transition:opacity 0.3s ease; }
                                  @keyframes spin { 0% { transform:rotate(0deg); } 100% { transform:rotate(360deg); } }
                                </style>
                                <script>$hlsCode</script>
                                </head>
                                <body>
                                  <div id="black-curtain" style="position:fixed;top:0;left:0;width:100vw;height:100vh;background:#000;z-index:5;transition:opacity 0.3s ease;"></div>
                                  <video id="vplayer" autoplay playsinline controls poster="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"></video>
                                  <div id="loader" class="spinner"></div>
                                  <script>
                                    var vid = document.getElementById('vplayer'), spinner = document.getElementById('loader'), curtain = document.getElementById('black-curtain');
                                    var streamUrl = "$safeUrl", startSec = $startSec, hasResumed = false;
                                    function hideSpin() { if (spinner) spinner.style.opacity = '0'; }
                                    function showSpin() { if (spinner) spinner.style.opacity = '1'; }
                                    function hideCurtain() { if (curtain) { curtain.style.opacity = '0'; curtain.style.pointerEvents = 'none'; } }
                                    function tryPlay() {
                                      if (!vid) return;
                                      var p = vid.play();
                                      if (p !== undefined) {
                                        p.then(function() { hideSpin(); hideCurtain(); }).catch(function() {
                                          vid.muted = true;
                                          vid.play().then(function() { setTimeout(function() { if (vid) vid.muted = false; hideSpin(); hideCurtain(); }, 250); }).catch(function(){});
                                        });
                                      }
                                    }
                                    function reportStatus() {
                                      if (window.AndroidBridge && vid) {
                                        window.AndroidBridge.onPlaybackStatus(vid.currentTime || 0, vid.duration || 0, !vid.paused && !vid.ended && vid.readyState > 2);
                                      }
                                    }
                                    vid.addEventListener('timeupdate', reportStatus); vid.addEventListener('play', function() { hideSpin(); hideCurtain(); reportStatus(); });
                                    vid.addEventListener('playing', function() { hideSpin(); hideCurtain(); }); vid.addEventListener('waiting', showSpin); vid.addEventListener('seeking', showSpin); vid.addEventListener('seeked', hideSpin);
                                    vid.addEventListener('pause', reportStatus); vid.addEventListener('ended', reportStatus);
                                    function seekStart() { if (startSec > 0 && !hasResumed && vid) { hasResumed = true; vid.currentTime = startSec; } }
                                    vid.addEventListener('loadedmetadata', seekStart); vid.addEventListener('canplay', function() { seekStart(); tryPlay(); });
                                    var hasTriedHls = false;
                                    vid.addEventListener('error', function() {
                                      if (!hasTriedHls && window.Hls && Hls.isSupported() && streamUrl.indexOf('.mp4') === -1) {
                                        initHls(); return;
                                      }
                                      var c = vid.error ? vid.error.code : 0;
                                      if (window.AndroidBridge && window.AndroidBridge.onPlaybackFailed) window.AndroidBridge.onPlaybackFailed('VID_ERR_' + c);
                                    });

                                    function initHls() {
                                      hasTriedHls = true;
                                      if (window.Hls && Hls.isSupported()) {
                                        var hls = new Hls({ enableWorker: true, lowLatencyMode: false, backBufferLength: 90 });
                                        hls.loadSource(streamUrl);
                                        hls.attachMedia(vid);
                                        hls.on(Hls.Events.MANIFEST_PARSED, function() { tryPlay(); });
                                        hls.on(Hls.Events.ERROR, function(ev, data) {
                                          if (data.fatal) {
                                            if (data.type === Hls.ErrorTypes.NETWORK_ERROR) hls.startLoad();
                                            else if (data.type === Hls.ErrorTypes.MEDIA_ERROR) hls.recoverMediaError();
                                            else { try { hls.destroy(); } catch(_) {} vid.src = streamUrl; tryPlay(); }
                                          }
                                        });
                                      } else {
                                        vid.src = streamUrl; tryPlay();
                                      }
                                    }

                                    var isOkMp4 = (streamUrl.indexOf('mycdn.me') !== -1 || streamUrl.indexOf('okcdn.ru') !== -1) && streamUrl.indexOf('/hls') === -1 && streamUrl.indexOf('m3u8') === -1;
                                    var isDirectVideo = streamUrl.indexOf('.mp4') !== -1 || streamUrl.indexOf('.webm') !== -1 || isOkMp4;
                                    if (!isDirectVideo && window.Hls && Hls.isSupported()) initHls(); else { vid.src = streamUrl; vid.play().catch(function(){}); }

                                    function playStream() { if (vid) vid.play().catch(function(){}); }
                                    function pauseStream() { if (vid) vid.pause(); }
                                    function togglePlay() { if (vid) { if (vid.paused) vid.play().catch(function(){}); else vid.pause(); } }
                                    function seekTo(s) { if (vid) vid.currentTime = Math.max(0, s); }; function seekRel(d) { if (vid) vid.currentTime = Math.max(0, (vid.currentTime || 0) + d); }
                                    function setVolume(v) { if (vid) vid.volume = Math.max(0, Math.min(1, v)); }; function stopStream() { if (vid) { vid.pause(); vid.removeAttribute('src'); vid.load(); } }

                                    function setDisplayMode(mode, zoom, panY) {
                                      if (!vid) return;
                                      if (mode === 'FILL' || mode === 'EXTEND_16_9') { vid.style.objectFit = 'fill'; vid.style.transform = 'scale(1.0)'; }
                                      else if (mode === 'FIT') { vid.style.objectFit = 'contain'; vid.style.transform = 'scale(1.0)'; }
                                      else { vid.style.objectFit = 'contain'; vid.style.transform = 'scale(' + (zoom || 1.0) + ') translateY(' + (panY || 0) + 'px)'; }
                                    }

                                    var zoomLevels = ['FIT', '1.15', '1.30', '0.85', '0.75', 'FILL'];
                                    var zoomIdx = 0;
                                    function cycleZoom() {
                                      zoomIdx = (zoomIdx + 1) % zoomLevels.length;
                                      var z = zoomLevels[zoomIdx];
                                      if (z === 'FIT') setDisplayMode('FIT', 1.0, 0);
                                      else if (z === 'FILL') setDisplayMode('FILL', 1.0, 0);
                                      else setDisplayMode('ZOOM', parseFloat(z), 0);
                                    }

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
