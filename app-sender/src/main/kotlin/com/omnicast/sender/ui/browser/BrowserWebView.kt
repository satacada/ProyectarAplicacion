package com.omnicast.sender.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

const val UA_MOBILE = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
const val UA_DESKTOP = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36"

fun getAdaptiveMobileUserAgent(ctx: android.content.Context): String =
    try { WebSettings.getDefaultUserAgent(ctx).replace("; wv", "").replace(Regex("Version/\\d+\\.\\d+\\s*"), "") } catch (_: Throwable) { UA_MOBILE }

private fun showBlockedToast(@Suppress("UNUSED_PARAMETER") ctx: android.content.Context) {}

/**
 * Componente AndroidView con SwipeRefreshLayout y WebView blindado contra cierres por fallos de renderizado.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    isHome: Boolean,
    isAdBlockEnabled: Boolean,
    currentUrl: String,
    hasStreams: Boolean = false,
    onProgress: (Int) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (title: String, url: String?) -> Unit,
    onHistoryUpdated: (url: String, title: String?) -> Unit,
    onStreamIntercepted: (streamUrl: String, headers: Map<String, String>, isMaster: Boolean) -> Unit,
    onSniffedVideo: (WvcDomSniffer.SniffedVideo) -> Unit,
    onTitleReceived: (String) -> Unit = {},
    onClearFocus: () -> Unit = {},
    onOpenPopup: (WebView) -> Unit = {},
    onClosePopup: () -> Unit = {},
    onWebViewCreated: (WebView) -> Unit
) {
    AndroidView(
        factory = { ctx ->
            AdBlocker.init(ctx)
            NativeAdBlockEngine.init(ctx)
            AdBlockRuleSyncManager.syncIfNeeded(ctx)
            AdBlockWhitelistManager.init(ctx)
            var currentKnownTitle: String? = null
            var isChallenge = false
            var livePageUrl = currentUrl

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    ServiceWorkerController.getInstance().setServiceWorkerClient(object : ServiceWorkerClient() {
                        override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                            val reqUrl = request.url.toString()
                            val docUrl = request.requestHeaders?.get("Referer") ?: livePageUrl
                            if (isAdBlockEnabled && (AdBlocker.isAd(reqUrl) || NativeAdBlockEngine.shouldBlock(reqUrl, docUrl))) {
                                return AdBlocker.createEmptyResponse()
                            }
                            val candidate = com.omnicast.core.domain.models.StreamCandidate(
                                url = reqUrl, pageUrl = docUrl,
                                headers = request.requestHeaders ?: emptyMap(),
                                userAgent = UA_MOBILE, pageTitle = currentKnownTitle
                            )
                            val detectedStream = com.omnicast.sender.ui.browser.extractors.StreamExtractorRegistry.useCase.execute(candidate)
                            if (detectedStream != null) {
                                val hdrs = BrowserUrlHelper.buildSafeHeaders(detectedStream.url, docUrl, UA_MOBILE, detectedStream.headers)
                                onStreamIntercepted(detectedStream.url, hdrs, detectedStream.isMaster)
                            }
                            return super.shouldInterceptRequest(request)
                        }
                    })
                } catch (_: Throwable) {}
            }

            val webView = WebView(ctx)
            val swipeRefresh = SwipeRefreshLayout(ctx).apply {
                isEnabled = false
                descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
                setColorSchemeColors(0xFF38BDF8.toInt())
                setOnRefreshListener { webView.reload() }
            }

            webView.apply {
                isFocusable = true; isFocusableInTouchMode = true; isClickable = true
                setOnTouchListener { _, ev ->
                    if (ev.action == android.view.MotionEvent.ACTION_DOWN) onClearFocus()
                    false
                }
                SmartSelectionAutoCompleter.attach(this, ctx)
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                settings.apply {
                    javaScriptEnabled = true; domStorageEnabled = true; databaseEnabled = true
                    allowFileAccess = true; allowContentAccess = true; useWideViewPort = true
                    loadWithOverviewMode = true; textZoom = 100
                    layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    mediaPlaybackRequiresUserGesture = false; setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = true
                    userAgentString = getAdaptiveMobileUserAgent(ctx)
                }

                webViewClient = object : WebViewClient() {
                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        android.util.Log.e("BrowserWebView", "🛡️ Chromium Render Process terminó de forma imprevista. Evitando cierre de la app.")
                        swipeRefresh.isRefreshing = false
                        return true
                    }

                    override fun onPageStarted(v: WebView?, u: String?, f: Bitmap?) {
                        CookieManager.getInstance().flush()
                        v?.evaluateJavascript("try{if(!window.chrome)window.chrome={runtime:{},app:{}};Object.defineProperty(navigator,'webdriver',{get:()=>false});}catch(_){}", null)
                        u?.let { if (!it.startsWith("about:") && !it.contains("favicon")) { livePageUrl = it; onPageStarted(it) } }
                        isChallenge = AdBlocker.isVerificationOrCaptcha(u)
                        if (!isChallenge) {
                            if (isAdBlockEnabled) v?.evaluateJavascript(AdBlocker.getAdHidingScript(), null)
                            v?.evaluateJavascript(SmartSelectionAutoCompleter.getCombinedInjectionScript(), null)
                        }
                    }

                    override fun onPageFinished(v: WebView?, u: String?) {
                        swipeRefresh.isRefreshing = false
                        CookieManager.getInstance().flush()
                        u?.let { if (!it.startsWith("about:")) livePageUrl = it }
                        val title = v?.title ?: "Transmisión"
                        currentKnownTitle = title
                        onPageFinished(title, u)
                        val isChl = isChallenge || AdBlocker.isVerificationOrCaptcha(u) || title.contains("Just a moment", ignoreCase = true)
                        if (!isChl) {
                            if (isAdBlockEnabled) v?.evaluateJavascript(AdBlocker.getAdHidingScript(), null)
                            v?.evaluateJavascript(SmartSelectionAutoCompleter.getCombinedInjectionScript(), null)
                            v?.evaluateJavascript(WvcDomSniffer.getInjectionScript(ctx), null)
                        }
                    }

                    override fun doUpdateVisitedHistory(v: WebView?, u: String?, isR: Boolean) {
                        u?.let { if (!it.startsWith("about:")) livePageUrl = it; onHistoryUpdated(it, v?.title) }
                    }

                    override fun shouldOverrideUrlLoading(v: WebView?, r: WebResourceRequest?): Boolean {
                        val reqUrl = r?.url?.toString() ?: return false
                        if (AdBlocker.isExternalRedirect(reqUrl)) return true
                        val liveUrl = v?.url ?: livePageUrl
                        if (isChallenge || AdBlocker.isVerificationOrCaptcha(reqUrl) || AdBlocker.isVerificationOrCaptcha(liveUrl) || (v?.title?.contains("Just a moment", ignoreCase = true) == true)) return false
                        val curHost = try { java.net.URI(liveUrl).host?.lowercase() } catch (_: Throwable) { null }
                        val targetHost = try { r.url.host?.lowercase() } catch (_: Throwable) { null }
                        val isSameSite = AdBlocker.isSameSite(curHost, targetHost)
                        val isSourceWhitelisted = curHost?.let { AdBlockWhitelistManager.isWhitelistedHost(it) } ?: false
                        val isSourceSearchEngine = curHost?.let { AdBlocker.isSearchEngine(it) } ?: false

                        if (isAdBlockEnabled && !isSourceWhitelisted) {
                            if (AdBlocker.isAd(reqUrl) || NativeAdBlockEngine.shouldBlock(reqUrl, liveUrl)) {
                                android.util.Log.w("BrowserWebView", "🛑 Redirección publicitaria bloqueada: $targetHost")
                                targetHost?.let { AdBlocker.learnAndBlockAdHost(it, curHost) }
                                showBlockedToast(ctx)
                                return true
                            }
                            if (!isSameSite && !isSourceSearchEngine && targetHost != null && !r.hasGesture()) {
                                val isAllowedExternal = AdBlocker.isSearchEngine(targetHost) ||
                                    AdBlocker.isKnownMediaHost(targetHost) ||
                                    AdBlocker.isVerificationOrCaptcha(targetHost) ||
                                    AdBlockWhitelistManager.isWhitelistedHost(targetHost)
                                if (!isAllowedExternal) {
                                    android.util.Log.w("BrowserWebView", "🛑 Redirección automática sin gesto bloqueada hacia: $targetHost")
                                    showBlockedToast(ctx)
                                    return true
                                }
                            }
                        }
                        return false
                    }

                    override fun onReceivedError(v: WebView?, req: WebResourceRequest?, err: WebResourceError?) {
                        swipeRefresh.isRefreshing = false
                        if (req?.isForMainFrame != true) return
                        val u = req.url.toString()
                        if (AdBlocker.isAd(u) || AdBlocker.isExternalRedirect(u)) return
                        super.onReceivedError(v, req, err)
                    }

                    override fun shouldInterceptRequest(v: WebView?, r: WebResourceRequest?): WebResourceResponse? {
                        return try {
                            val reqUrl = r?.url?.toString() ?: return super.shouldInterceptRequest(v, r)
                            if (reqUrl.contains("challenges.cloudflare.com") || reqUrl.contains("cdn-cgi/challenge-platform")) isChallenge = true
                            if (reqUrl.contains("/ibjslib/wai2iku6/")) {
                                val assetName = if (reqUrl.contains("json2.js")) "json2.js" else "wvc_sniffer.js"
                                return try {
                                    val stream = ctx.assets.open(assetName)
                                    WebResourceResponse("application/javascript", "UTF-8", stream)
                                } catch (_: Throwable) { super.shouldInterceptRequest(v, r) }
                            }
                            val docUrl = r.requestHeaders?.get("Referer") ?: livePageUrl
                            val isDocWhitelisted = AdBlockWhitelistManager.isWhitelisted(docUrl)
                            if (isAdBlockEnabled && !isDocWhitelisted && (AdBlocker.isAd(reqUrl) || AdBlocker.isVideoAd(reqUrl) || NativeAdBlockEngine.shouldBlock(reqUrl, docUrl))) {
                                return AdBlocker.createEmptyResponse()
                            }
                            if (com.omnicast.sender.nativeclassifier.NativeStreamClassifier.shouldDiscard(reqUrl) || AdBlocker.isVideoAd(reqUrl)) {
                                return super.shouldInterceptRequest(v, r)
                            }
                            val candidate = com.omnicast.core.domain.models.StreamCandidate(
                                url = reqUrl, pageUrl = docUrl,
                                headers = r.requestHeaders ?: emptyMap(),
                                userAgent = UA_MOBILE, pageTitle = currentKnownTitle
                            )
                            val detectedStream = com.omnicast.sender.ui.browser.extractors.StreamExtractorRegistry.useCase.execute(candidate)
                            if (detectedStream != null) {
                                val hdrs = BrowserUrlHelper.buildSafeHeaders(detectedStream.url, docUrl, UA_MOBILE, detectedStream.headers)
                                post { onStreamIntercepted(detectedStream.url, hdrs, detectedStream.isMaster) }
                            }
                            super.shouldInterceptRequest(v, r)
                        } catch (_: Throwable) {
                            super.shouldInterceptRequest(v, r)
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                        if (transport != null && view != null) {
                            val tempWebView = WebView(view.context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.userAgentString = view.settings.userAgentString
                            }
                            var handled = false
                            fun handlePopupUrl(v: WebView?, destUrl: String?) {
                                if (handled || destUrl.isNullOrBlank() || destUrl.startsWith("about:")) return
                                handled = true
                                val liveUrl = view.url ?: livePageUrl
                                val curHost = try { java.net.URI(liveUrl).host?.lowercase() } catch (_: Throwable) { null }
                                val targetHost = try { java.net.URI(destUrl).host?.lowercase() } catch (_: Throwable) { null }
                                val isSourceWhitelisted = curHost?.let { AdBlockWhitelistManager.isWhitelistedHost(it) } ?: false
                                val isBlockedAd = isAdBlockEnabled && !isSourceWhitelisted &&
                                    (AdBlocker.isAd(destUrl) || AdBlocker.isExternalRedirect(destUrl) || NativeAdBlockEngine.shouldBlock(destUrl, liveUrl))
                                if (!isBlockedAd) {
                                    view.post { view.loadUrl(destUrl) }
                                } else {
                                    targetHost?.let { AdBlocker.learnAndBlockAdHost(it, curHost) }
                                    showBlockedToast(ctx)
                                }
                                v?.post { try { v.destroy() } catch (_: Throwable) {} }
                            }
                            tempWebView.webViewClient = object : WebViewClient() {
                                override fun onPageStarted(v: WebView?, u: String?, f: Bitmap?) { handlePopupUrl(v, u) }
                                override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean { handlePopupUrl(v, req?.url?.toString()); return true }
                            }
                            tempWebView.postDelayed({ try { tempWebView.destroy() } catch (_: Throwable) {} }, 10000)
                            transport.webView = tempWebView
                            resultMsg.sendToTarget()
                            return true
                        }
                        return false
                    }

                    override fun onProgressChanged(v: WebView?, p: Int) {
                        onProgress(p)
                        if (p >= 80) swipeRefresh.isRefreshing = false
                        val isChl = isChallenge || AdBlocker.isVerificationOrCaptcha(v?.url) || (v?.title?.contains("Just a moment", ignoreCase = true) == true)
                        if (!isChl) {
                            if (isAdBlockEnabled && (p == 30 || p == 60 || p == 100)) v?.evaluateJavascript(AdBlocker.getAdHidingScript(), null)
                            if (p == 60 || p == 85 || p == 100) v?.evaluateJavascript(WvcDomSniffer.getInjectionScript(ctx), null)
                        }
                    }

                    override fun onReceivedTitle(v: WebView?, title: String?) {
                        super.onReceivedTitle(v, title)
                        val t = title?.trim()
                        if (!t.isNullOrBlank() && !t.contains("about:blank") && t != "Transmisión" && !t.startsWith("http")) {
                            currentKnownTitle = t
                            if (t.contains("Just a moment", ignoreCase = true) || t.contains("Cloudflare", ignoreCase = true)) isChallenge = true
                            post { onTitleReceived(t) }
                        }
                    }

                    override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                        val msg = cm?.message() ?: return super.onConsoleMessage(cm)
                        val sniffedList = WvcDomSniffer.parseConsoleMessage(msg)
                        if (sniffedList.isNotEmpty()) { post { sniffedList.forEach { onSniffedVideo(it) } }; return true }
                        return super.onConsoleMessage(cm)
                    }
                }
                onWebViewCreated(this)
            }
            swipeRefresh.addView(webView)
            swipeRefresh
        },
        update = { it.visibility = if (isHome) android.view.View.GONE else android.view.View.VISIBLE },
        modifier = if (isHome) Modifier.size(0.dp) else Modifier.fillMaxSize()
    )
}
