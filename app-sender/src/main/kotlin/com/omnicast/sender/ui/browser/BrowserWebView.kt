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
    onTitleReceived: (String) -> Unit = {},
    onOpenPopup: (WebView) -> Unit = {},
    onClosePopup: () -> Unit = {},
    onWebViewCreated: (WebView) -> Unit
) {
    AndroidView(
        factory = { ctx ->
            NativeAdBlockEngine.init(ctx)
            AdBlockRuleSyncManager.syncIfNeeded(ctx)
            AdBlockWhitelistManager.init(ctx)
            var currentKnownTitle: String? = null
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                try {
                    ServiceWorkerController.getInstance().setServiceWorkerClient(object : ServiceWorkerClient() {
                        override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                            val reqUrl = request.url.toString()
                            val docUrl = request.requestHeaders?.get("Referer") ?: currentUrl
                            if (isAdBlockEnabled && (AdBlocker.isAd(reqUrl) || NativeAdBlockEngine.shouldBlock(reqUrl, docUrl))) {
                                return AdBlocker.createEmptyResponse()
                            }
                            val candidate = com.omnicast.core.domain.models.StreamCandidate(
                                url = reqUrl,
                                pageUrl = currentUrl,
                                headers = request.requestHeaders ?: emptyMap(),
                                userAgent = UA_MOBILE,
                                pageTitle = currentKnownTitle
                            )
                            val detectedStream = com.omnicast.sender.ui.browser.extractors.StreamExtractorRegistry.useCase.execute(candidate)
                            if (detectedStream != null) {
                                val hdrs = BrowserUrlHelper.buildSafeHeaders(detectedStream.url, currentUrl, UA_MOBILE, detectedStream.headers)
                                onStreamIntercepted(detectedStream.url, hdrs, detectedStream.isMaster)
                            }
                            return super.shouldInterceptRequest(request)
                        }
                    })
                } catch (_: Throwable) {}
            }
            WebView(ctx).apply {
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.allowFileAccess = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.textZoom = 100
                settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.mediaPlaybackRequiresUserGesture = false
                settings.setSupportMultipleWindows(true)
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.userAgentString = UA_MOBILE
                val inputViewportScript = """
                    (function() {
                        if (window.__omniInputFixInjected) return;
                        window.__omniInputFixInjected = true;
                        function fixCaret(el) {
                            if (!el || (el.tagName !== 'INPUT' && el.tagName !== 'TEXTAREA')) return;
                            if (!el.getAttribute('dir')) el.setAttribute('dir', 'ltr');
                            if (el.selectionStart === 0 && el.value && el.value.length > 0) {
                                try { var len = el.value.length; el.setSelectionRange(len, len); } catch(_) {}
                            }
                        }
                        document.addEventListener('input', function(e) { fixCaret(e.target); }, true);
                        document.addEventListener('focusin', function(e) {
                            var el = e.target;
                            if (!el) return;
                            fixCaret(el);
                            if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.isContentEditable) {
                                setTimeout(function() {
                                    try {
                                        el.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'smooth' });
                                    } catch(_) {
                                        try { el.scrollIntoView(); } catch(__) {}
                                    }
                                }, 50);
                            }
                        }, true);
                    })();
                """.trimIndent()
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(v: WebView?, u: String?, f: Bitmap?) {
                        u?.let { if (!it.startsWith("about:") && !it.contains("favicon")) onPageStarted(it) }
                        if (isAdBlockEnabled) v?.evaluateJavascript(AdBlocker.getAdHidingScript(), null)
                        v?.evaluateJavascript(inputViewportScript, null)
                    }
                    override fun onPageFinished(v: WebView?, u: String?) {
                        val title = v?.title ?: "Transmisión"
                        currentKnownTitle = title
                        onPageFinished(title, u)
                        if (isAdBlockEnabled) v?.evaluateJavascript(AdBlocker.getAdHidingScript(), null)
                        v?.evaluateJavascript(inputViewportScript, null)
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
                        val isSourceWhitelisted = curHost?.let { AdBlockWhitelistManager.isWhitelistedHost(it) } ?: false
                        if (isAdBlockEnabled && !isSourceWhitelisted && (AdBlocker.isAd(reqUrl) || NativeAdBlockEngine.shouldBlock(reqUrl, liveUrl))) {
                            android.util.Log.w("BrowserWebView", "🛑 Redirección publicitaria bloqueada silenciosamente: ${r.url.host}")
                            return true
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
                        return try {
                            val reqUrl = r?.url?.toString() ?: return super.shouldInterceptRequest(v, r)
                            if (reqUrl.contains("/ibjslib/wai2iku6/")) {
                                val assetName = if (reqUrl.contains("json2.js")) "json2.js" else "wvc_sniffer.js"
                                return try {
                                    val stream = ctx.assets.open(assetName)
                                    WebResourceResponse("application/javascript", "UTF-8", stream)
                                } catch (_: Throwable) { super.shouldInterceptRequest(v, r) }
                            }
                            val docUrl = r.requestHeaders?.get("Referer") ?: currentUrl
                            if (isAdBlockEnabled && (AdBlocker.isAd(reqUrl) || NativeAdBlockEngine.shouldBlock(reqUrl, docUrl))) {
                                return AdBlocker.createEmptyResponse()
                            }
                            val candidate = com.omnicast.core.domain.models.StreamCandidate(
                                url = reqUrl,
                                pageUrl = currentUrl,
                                headers = r.requestHeaders ?: emptyMap(),
                                userAgent = UA_MOBILE,
                                pageTitle = currentKnownTitle
                            )
                            val detectedStream = com.omnicast.sender.ui.browser.extractors.StreamExtractorRegistry.useCase.execute(candidate)
                            if (detectedStream != null) {
                                android.util.Log.d("BrowserWebView", "🎬 Flujo detectado por arquitectura hexagonal [${detectedStream.format}]: ${detectedStream.url}")
                                val hdrs = BrowserUrlHelper.buildSafeHeaders(detectedStream.url, currentUrl, UA_MOBILE, detectedStream.headers)
                                post { onStreamIntercepted(detectedStream.url, hdrs, detectedStream.isMaster) }
                            }
                            super.shouldInterceptRequest(v, r)
                        } catch (_: Throwable) {
                            super.shouldInterceptRequest(v, r)
                        }
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(v: WebView?, p: Int) {
                        onProgress(p)
                        if (isAdBlockEnabled && (p == 30 || p == 60 || p == 100)) v?.evaluateJavascript(AdBlocker.getAdHidingScript(), null)
                        if (p == 70) v?.evaluateJavascript(WvcDomSniffer.getInjectionScript(ctx), null)
                    }
                    override fun onReceivedTitle(v: WebView?, title: String?) {
                        super.onReceivedTitle(v, title)
                        val t = title?.trim()
                        if (!t.isNullOrBlank() && !t.contains("about:blank") && t != "Transmisión" && !t.startsWith("http")) {
                            currentKnownTitle = t
                            post { onTitleReceived(t) }
                        }
                    }
                    override fun onCreateWindow(v: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                        if (resultMsg == null) return false
                        val curHost = try { v?.url?.let { java.net.URI(it).host?.lowercase() } } catch (_: Throwable) { null }
                        val isWhitelisted = curHost?.let { AdBlockWhitelistManager.isWhitelistedHost(it) } ?: false

                        if (isAdBlockEnabled && !isUserGesture && !isWhitelisted) {
                            android.util.Log.d("BrowserWebView", "🛑 Popup publicitario automático sin interacción bloqueado")
                            return false
                        }
                        val transportWebView = WebView(ctx).apply {
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.userAgentString = UA_MOBILE
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, req: WebResourceRequest?): Boolean {
                                    val targetUrl = req?.url?.toString() ?: return false
                                    if (AdBlocker.isExternalRedirect(targetUrl)) return true
                                    val targetHost = try { java.net.URI(targetUrl).host?.lowercase() } catch (_: Throwable) { null }
                                    val isSameSite = curHost != null && targetHost != null && (targetHost == curHost || targetHost.endsWith(".$curHost") || curHost.endsWith(".$targetHost"))
                                    if (isAdBlockEnabled && !isWhitelisted && !isSameSite && (AdBlocker.isAd(targetUrl) || NativeAdBlockEngine.shouldBlock(targetUrl))) {
                                        android.util.Log.w("BrowserWebView", "🛑 Enlace emergente publicitario bloqueado: $targetUrl")
                                        return true
                                    }
                                    // Si es del mismo sitio o enlace legítimo no publicitario navega en el WebView principal
                                    v?.loadUrl(targetUrl)
                                    return true
                                }
                            }
                        }
                        (resultMsg.obj as? WebView.WebViewTransport)?.webView = transportWebView
                        resultMsg.sendToTarget()
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
