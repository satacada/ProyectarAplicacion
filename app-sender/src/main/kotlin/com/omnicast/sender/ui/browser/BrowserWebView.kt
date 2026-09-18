package com.omnicast.sender.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Message
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.viewinterop.AndroidView

const val UA_MOBILE = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"
const val UA_DESKTOP = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36"

/**
 * Componente AndroidView para encapsular WebView con soporte de cookies de terceros y contenido mixto.
 * Permite la carga fluida de sitios complejos como ok.ru, VK, Dailymotion y reproductores de películas.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    isHome: Boolean,
    isAdBlockEnabled: Boolean,
    currentUrl: String,
    onProgress: (Int) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (title: String, url: String?) -> Unit,
    onHistoryUpdated: (url: String, title: String?) -> Unit,
    onStreamIntercepted: (streamUrl: String, headers: Map<String, String>, isMaster: Boolean) -> Unit,
    onSniffedVideo: (WvcDomSniffer.SniffedVideo) -> Unit,
    onOpenPopup: (WebView) -> Unit = {},
    onClosePopup: () -> Unit = {},
    onWebViewCreated: (WebView) -> Unit
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.allowFileAccess = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.mediaPlaybackRequiresUserGesture = false
                settings.setSupportMultipleWindows(true)
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.userAgentString = UA_MOBILE
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(v: WebView?, u: String?, f: Bitmap?) {
                        u?.let { if (!it.startsWith("about:") && !it.contains("favicon")) onPageStarted(it) }
                        if (isAdBlockEnabled) v?.evaluateJavascript(AdBlocker.getAdHidingScript(), null)
                    }
                    override fun onPageFinished(v: WebView?, u: String?) {
                        val title = v?.title ?: "Transmisión"
                        onPageFinished(title, u)
                        if (isAdBlockEnabled) v?.evaluateJavascript(AdBlocker.getAdHidingScript(), null)
                        v?.evaluateJavascript(WvcDomSniffer.getInjectionScript(ctx), null)
                    }
                    override fun doUpdateVisitedHistory(v: WebView?, u: String?, isR: Boolean) {
                        u?.let { onHistoryUpdated(it, v?.title) }
                    }
                    override fun shouldOverrideUrlLoading(v: WebView?, r: WebResourceRequest?): Boolean {
                        if (r?.isForMainFrame == false) return false
                        val reqUrl = r?.url?.toString() ?: return false
                        if (AdBlocker.isExternalRedirect(reqUrl)) return true
                        val liveUrl = v?.url ?: currentUrl
                        val curHost = try { java.net.URI(liveUrl).host?.lowercase() } catch (_: Throwable) { null }
                        val tgtHost = try { r.url.host?.lowercase() } catch (_: Throwable) { null }
                        if (curHost != null && tgtHost != null && curHost != tgtHost && !tgtHost.endsWith(curHost) && !curHost.endsWith(tgtHost)) {
                            if (!AdBlocker.isKnownMediaHost(tgtHost)) {
                                if (isAdBlockEnabled || AdBlocker.isAd(reqUrl)) {
                                    android.util.Log.w("BrowserWebView", "🛑 Redirección publicitaria bloqueada silenciosamente: $tgtHost")
                                    return true
                                }
                                val popup = WebView(ctx).apply {
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    settings.javaScriptEnabled = true; settings.domStorageEnabled = true; settings.userAgentString = UA_MOBILE
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView?, req: WebResourceRequest?): Boolean =
                                            req?.url?.toString()?.let { AdBlocker.isExternalRedirect(it) || AdBlocker.isAd(it) } ?: false
                                    }
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onCloseWindow(window: WebView?) { onClosePopup() }
                                    }
                                    loadUrl(reqUrl)
                                }
                                onOpenPopup(popup)
                                return true
                            }
                        }
                        return false
                    }
                    override fun onReceivedError(v: WebView?, req: WebResourceRequest?, err: WebResourceError?) {
                        if (req?.isForMainFrame != true) return
                        val u = req.url.toString()
                        if (AdBlocker.isAd(u) || AdBlocker.isExternalRedirect(u)) return
                        super.onReceivedError(v, req, err)
                    }
                    override fun shouldInterceptRequest(v: WebView?, r: WebResourceRequest?): WebResourceResponse? {
                        val reqUrl = r?.url?.toString() ?: return super.shouldInterceptRequest(v, r)
                        if (reqUrl.contains("/ibjslib/wai2iku6/")) {
                            val assetName = if (reqUrl.contains("json2.js")) "json2.js" else "wvc_sniffer.js"
                            return try {
                                val stream = ctx.assets.open(assetName)
                                WebResourceResponse("application/javascript", "UTF-8", stream)
                            } catch (_: Throwable) { super.shouldInterceptRequest(v, r) }
                        }
                        if (isAdBlockEnabled && AdBlocker.isAd(reqUrl)) return AdBlocker.createEmptyResponse()
                        val isMedia = BrowserUrlHelper.isPlayableStream(reqUrl) && !AdBlocker.isAd(reqUrl)
                        if (isMedia) {
                            android.util.Log.d("BrowserWebView", "🎬 Flujo interceptado en red: $reqUrl")
                            val hdrs = BrowserUrlHelper.buildSafeHeaders(reqUrl, currentUrl, UA_MOBILE, r.requestHeaders)
                            val isMaster = reqUrl.contains("master", ignoreCase = true) || reqUrl.contains("manifest", ignoreCase = true)
                            post { onStreamIntercepted(reqUrl, hdrs, isMaster) }
                        }
                        return super.shouldInterceptRequest(v, r)
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(v: WebView?, p: Int) {
                        onProgress(p)
                        if (isAdBlockEnabled && (p == 30 || p == 70)) v?.evaluateJavascript(AdBlocker.getAdHidingScript(), null)
                        if (p == 70) v?.evaluateJavascript(WvcDomSniffer.getInjectionScript(ctx), null)
                    }
                    override fun onCreateWindow(v: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                        if (resultMsg == null) return false
                        if (isAdBlockEnabled) {
                            android.util.Log.d("BrowserWebView", "🛑 Popup publicitario bloqueado en onCreateWindow")
                            return false
                        }
                        val popup = WebView(ctx).apply {
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            settings.javaScriptEnabled = true; settings.domStorageEnabled = true; settings.userAgentString = UA_MOBILE
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, req: WebResourceRequest?): Boolean =
                                    req?.url?.toString()?.let { AdBlocker.isExternalRedirect(it) } ?: false
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onCloseWindow(window: WebView?) { onClosePopup() }
                            }
                        }
                        (resultMsg.obj as? WebView.WebViewTransport)?.webView = popup
                        resultMsg.sendToTarget()
                        onOpenPopup(popup)
                        return true
                    }
                    override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                        val msg = cm?.message() ?: return super.onConsoleMessage(cm)
                        val sniffedList = WvcDomSniffer.parseConsoleMessage(msg)
                        if (sniffedList.isNotEmpty()) {
                            post { sniffedList.forEach { onSniffedVideo(it) } }
                            return true
                        }
                        return super.onConsoleMessage(cm)
                    }
                }
                onWebViewCreated(this)
            }
        },
        modifier = Modifier.fillMaxSize().alpha(if (isHome) 0f else 1f)
    )
}
