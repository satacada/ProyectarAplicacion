package com.omnicast.receiver.ui.components

import android.annotation.SuppressLint
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
 * Puente JavaScript para capturar eventos de reproducción de YouTube IFrame API.
 */
class YouTubeBridge(
    private val onStatus: ((posMs: Long, durMs: Long, isPlaying: Boolean) -> Unit)?
) {
    @JavascriptInterface
    fun onPlaybackStatus(curSec: Float, durSec: Float, state: Int) {
        val isPlaying = (state == 1)
        onStatus?.invoke((curSec * 1000).toLong(), (durSec * 1000).toLong(), isPlaying)
    }
}

/**
 * Reproductor de YouTube para Android TV Box usando la API oficial IFrame Player.
 * Soporta control de reproducción remoto (Play/Pausa/Seek/Volumen) y reporte de tiempo.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TvYouTubeScreen(
    videoId: String,
    title: String,
    timestamp: Long = 0L,
    commandFlow: Flow<SignalingMessage.PlayControl>? = null,
    onPlaybackStatus: ((posMs: Long, durMs: Long, isPlaying: Boolean) -> Unit)? = null
) {
    var showBanner by remember(videoId, timestamp) { mutableStateOf(true) }
    var customView by remember(videoId, timestamp) { mutableStateOf<View?>(null) }
    var webViewRef by remember(videoId, timestamp) { mutableStateOf<WebView?>(null) }

    LaunchedEffect(videoId, timestamp) {
        delay(4000)
        showBanner = false
    }

    LaunchedEffect(commandFlow, webViewRef) {
        commandFlow?.collect { msg ->
            val wv = webViewRef ?: return@collect
            when (msg.action) {
                "PLAY" -> wv.evaluateJavascript("sendYtCommand('playVideo');", null)
                "PAUSE" -> wv.evaluateJavascript("sendYtCommand('pauseVideo');", null)
                "SEEK" -> {
                    val sec = msg.positionMs / 1000.0
                    wv.evaluateJavascript("sendYtCommand('seekTo', [$sec, true]);", null)
                }
                "VOLUME" -> {
                    val volPct = (msg.volume.coerceIn(0f, 1f) * 100).toInt()
                    wv.evaluateJavascript("sendYtCommand('setVolume', [$volPct]);", null)
                }
                "STOP" -> wv.evaluateJavascript("sendYtCommand('stopVideo');", null)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (customView != null) {
            AndroidView(factory = { customView!! }, modifier = Modifier.fillMaxSize())
        } else {
            key(videoId, timestamp) {
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
                            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            addJavascriptInterface(YouTubeBridge(onPlaybackStatus), "AndroidBridge")
                            webViewClient = WebViewClient()
                            webChromeClient = object : WebChromeClient() {
                                override fun onShowCustomView(v: View?, c: CustomViewCallback?) { customView = v }
                                override fun onHideCustomView() { customView = null }
                            }
                            val embedHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                <style>
                                  * { margin:0; padding:0; box-sizing:border-box; }
                                  html, body { width:100vw; height:100vh; overflow:hidden; background:#000; }
                                  #ytplayer { position:fixed; top:0; left:0; width:100vw !important; height:100vh !important; border:none; }
                                </style>
                                </head>
                                <body>
                                  <iframe id="ytplayer"
                                    src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=1&fs=1&rel=0&iv_load_policy=3&modestbranding=1&cc_load_policy=0&enablejsapi=1"
                                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen"
                                    allowfullscreen>
                                  </iframe>
                                  <script>
                                    window.addEventListener('message', function(event) {
                                      try {
                                        var data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
                                        if (data && data.event === 'infoDelivery' && data.info) {
                                          var cur = data.info.currentTime;
                                          var dur = data.info.duration;
                                          var st = data.info.playerState;
                                          if (window.AndroidBridge && cur !== undefined && dur !== undefined) {
                                            window.AndroidBridge.onPlaybackStatus(cur, dur, st !== undefined ? st : 1);
                                          }
                                        }
                                      } catch(e) {}
                                    });

                                    function sendYtCommand(func, args) {
                                      var iframe = document.getElementById('ytplayer');
                                      if (iframe && iframe.contentWindow) {
                                        iframe.contentWindow.postMessage(JSON.stringify({
                                          event: 'command',
                                          func: func,
                                          args: args || []
                                        }), '*');
                                      }
                                    }

                                    setInterval(function() {
                                      var iframe = document.getElementById('ytplayer');
                                      if (iframe && iframe.contentWindow) {
                                        iframe.contentWindow.postMessage(JSON.stringify({
                                          event: 'listening',
                                          id: 1
                                        }), '*');
                                      }
                                    }, 1000);
                                  </script>
                                </body>
                                </html>
                            """.trimIndent()
                            loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "UTF-8", null)
                        }
                    },
                    onRelease = { webView ->
                        try {
                            if (webViewRef == webView) webViewRef = null
                            webView.removeJavascriptInterface("AndroidBridge")
                            webView.stopLoading()
                            webView.loadUrl("about:blank")
                            webView.destroy()
                        } catch (_: Throwable) {}
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AnimatedVisibility(visible = showBanner, exit = fadeOut(), modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 24.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)), shape = RoundedCornerShape(8.dp)) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📺", fontSize = 14.sp)
                    Column {
                        Text(text = title.ifBlank { "YouTube Video" }, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("🟢 YouTube TV Box", color = Color(0xFF10B981), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
