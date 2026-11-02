package com.omnicast.receiver.ui.components

import android.annotation.SuppressLint
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
import kotlinx.coroutines.flow.Flow

/**
 * Reproductor de YouTube para Android TV Box usando la API oficial IFrame Player.
 * Equipado con superposición OSD D-Pad nativa (TvWebUnifiedOverlay y TvWebSettingsModal).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TvYouTubeScreen(
    videoId: String,
    title: String,
    timestamp: Long = 0L,
    commandFlow: Flow<SignalingMessage.PlayControl>? = null,
    onPlaybackStatus: ((posMs: Long, durMs: Long, isPlaying: Boolean) -> Unit)? = null,
    onCloseClick: () -> Unit = {}
) {
    var playerState by remember(videoId, timestamp) {
        mutableStateOf(
            TvWebPlayerState(
                isControlsVisible = true,
                availableQualities = listOf("Auto", "1080p", "720p", "480p", "360p"),
                availableSubtitles = listOf("Desactivados", "Español", "Inglés")
            )
        )
    }
    var customView by remember(videoId, timestamp) { mutableStateOf<View?>(null) }
    var webViewRef by remember(videoId, timestamp) { mutableStateOf<WebView?>(null) }

    LaunchedEffect(commandFlow, webViewRef) {
        commandFlow?.collect { msg ->
            val wv = webViewRef ?: return@collect
            when (msg.action) {
                "PLAY" -> wv.evaluateJavascript("sendYtCommand('playVideo');", null)
                "PAUSE" -> wv.evaluateJavascript("sendYtCommand('pauseVideo');", null)
                "TOGGLE_PLAY" -> wv.evaluateJavascript("toggleYtPlay();", null)
                "SEEK" -> wv.evaluateJavascript("sendYtCommand('seekTo', [${msg.positionMs / 1000.0}, true]);", null)
                "SEEK_REL" -> wv.evaluateJavascript("seekYtRel(${msg.positionMs / 1000.0});", null)
                "VOLUME" -> wv.evaluateJavascript("sendYtCommand('setVolume', [${(msg.volume.coerceIn(0f, 1f) * 100).toInt()}]);", null)
                "STOP" -> wv.evaluateJavascript("sendYtCommand('stopVideo');", null)
                "SHOW_CONTROLS" -> playerState = playerState.copy(isControlsVisible = true)
                "TOGGLE_SUBTITLES", "SUBTITLE_TOGGLE" -> wv.evaluateJavascript("toggleYtCaptions();", null)
                "TOGGLE_MENU" -> playerState = playerState.copy(isSettingsOpen = !playerState.isSettingsOpen)
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
                            isFocusable = false; isFocusableInTouchMode = false; clearFocus()
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            settings.javaScriptEnabled = true; settings.domStorageEnabled = true; settings.databaseEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false; settings.useWideViewPort = true; settings.loadWithOverviewMode = true
                            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                            CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun onPlaybackStatus(curSec: Float, durSec: Float, state: Int) {
                                    val isPlaying = (state == 1)
                                    val posMs = (curSec * 1000).toLong()
                                    val durMs = (durSec * 1000).toLong()
                                    playerState = playerState.copy(currentPositionMs = posMs, durationMs = durMs, isPlaying = isPlaying)
                                    onPlaybackStatus?.invoke(posMs, durMs, isPlaying)
                                }
                            }, "AndroidBridge")
                            webViewClient = WebViewClient()
                            webChromeClient = object : WebChromeClient() {
                                override fun onShowCustomView(v: View?, c: CustomViewCallback?) { customView = v }
                                override fun onHideCustomView() { customView = null }
                            }
                            val sep = if (videoId.contains("?")) "&" else "?"
                            val embedSrc = "https://www.youtube-nocookie.com/embed/$videoId${sep}autoplay=1&controls=0&fs=1&rel=0&iv_load_policy=3&modestbranding=1&enablejsapi=1"
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
                                  <iframe id="ytplayer" src="$embedSrc" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen" allowfullscreen></iframe>
                                  <script>
                                    var captionsOn = false; var lastState = 1; var curTime = 0; var totalDur = 0;
                                    function toggleYtPlay() { if (lastState === 1) sendYtCommand('pauseVideo'); else sendYtCommand('playVideo'); }
                                    function seekYtRel(deltaSec) { var target = Math.max(0, curTime + deltaSec); sendYtCommand('seekTo', [target, true]); }
                                    function setYtQuality(q) {
                                      var qMap = { '1080p':'hd1080', '720p':'hd720', '480p':'large', '360p':'medium', 'Auto':'auto' };
                                      var val = qMap[q] || 'auto';
                                      sendYtCommand('setPlaybackQuality', [val]);
                                      sendYtCommand('setPlaybackQualityRange', [val]);
                                    }
                                    function toggleYtCaptions() {
                                      captionsOn = !captionsOn;
                                      if (captionsOn) sendYtCommand('loadModule', ['captions']); else sendYtCommand('unloadModule', ['captions']);
                                    }
                                    window.addEventListener('message', function(event) {
                                      try {
                                        var data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
                                        if (data && data.event === 'infoDelivery' && data.info) {
                                          if (data.info.currentTime !== undefined) curTime = data.info.currentTime;
                                          if (data.info.duration !== undefined) totalDur = data.info.duration;
                                          if (data.info.playerState !== undefined) lastState = data.info.playerState;
                                          if (window.AndroidBridge && curTime !== undefined && totalDur > 0) {
                                            window.AndroidBridge.onPlaybackStatus(curTime, totalDur, lastState !== undefined ? lastState : 1);
                                          }
                                        }
                                      } catch(e) {}
                                    });
                                    function sendYtCommand(func, args) {
                                      var iframe = document.getElementById('ytplayer');
                                      if (iframe && iframe.contentWindow) {
                                        iframe.contentWindow.postMessage(JSON.stringify({ event: 'command', func: func, args: args || [] }), '*');
                                      }
                                    }
                                    setInterval(function() {
                                      var iframe = document.getElementById('ytplayer');
                                      if (iframe && iframe.contentWindow) iframe.contentWindow.postMessage(JSON.stringify({ event: 'listening', id: 1 }), '*');
                                      if (window.AndroidBridge && curTime !== undefined && totalDur > 0) window.AndroidBridge.onPlaybackStatus(curTime, totalDur, lastState !== undefined ? lastState : 1);
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
                            webView.stopLoading(); webView.loadUrl("about:blank"); webView.destroy()
                        } catch (_: Throwable) {}
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Superposición nativa de controles D-Pad
        TvWebUnifiedOverlay(
            title = title,
            badgeText = "📺 YouTube TV Box",
            state = playerState,
            onTogglePlay = { webViewRef?.evaluateJavascript("toggleYtPlay();", null) },
            onSeekRelative = { deltaMs -> webViewRef?.evaluateJavascript("seekYtRel(${deltaMs / 1000.0});", null) },
            onToggleSettings = { playerState = playerState.copy(isSettingsOpen = !playerState.isSettingsOpen) },
            onShowControls = { playerState = playerState.copy(isControlsVisible = true) },
            onHideControls = { playerState = playerState.copy(isControlsVisible = false) }
        )

        // Modal de configuración nativo D-Pad
        TvWebSettingsModal(
            state = playerState,
            onSelectQuality = { q ->
                playerState = playerState.copy(currentQuality = q)
                webViewRef?.evaluateJavascript("setYtQuality('$q');", null)
            },
            onSelectSubtitle = { s ->
                playerState = playerState.copy(currentSubtitle = s)
                webViewRef?.evaluateJavascript("toggleYtCaptions();", null)
            },
            onToggleAspect = {},
            onClose = { playerState = playerState.copy(isSettingsOpen = false) }
        )

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
    }
}
