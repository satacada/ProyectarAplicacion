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
import kotlinx.coroutines.delay
import java.io.ByteArrayInputStream

/**
 * Reproductor de YouTube para Android TV Box usando la API oficial IFrame Player.
 * Patrón verificado con AirScreen: cero error 152-4, controles completos y pantalla completa.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TvYouTubeScreen(videoId: String, title: String, timestamp: Long = 0L) {
    var showBanner by remember(videoId, timestamp) { mutableStateOf(true) }
    var customView by remember(videoId, timestamp) { mutableStateOf<View?>(null) }

    LaunchedEffect(videoId, timestamp) {
        delay(4000)
        showBanner = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (customView != null) {
            AndroidView(factory = { customView!! }, modifier = Modifier.fillMaxSize())
        } else {
            key(videoId, timestamp) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
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
                                    src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=1&fs=1&rel=0&iv_load_policy=3&modestbranding=1&cc_load_policy=0"
                                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen"
                                    allowfullscreen>
                                  </iframe>
                                </body>
                                </html>
                            """.trimIndent()
                            loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "UTF-8", null)
                        }
                    },
                    onRelease = { webView ->
                        try {
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
