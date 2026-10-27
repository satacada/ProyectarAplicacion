package com.omnicast.sender.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

const val UA_MOBILE = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
const val UA_DESKTOP = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36"

private fun showBlockedToast(@Suppress("UNUSED_PARAMETER") ctx: android.content.Context) {
    // Silencioso estilo Web Video Caster: bloquear sin interrumpir al usuario con Toasts
}

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    ServiceWorkerController.getInstance().setServiceWorkerClient(object : ServiceWorkerClient() {
                        override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                            val reqUrl = request.url.toString()
                            val docUrl = request.requestHeaders?.get("Referer") ?: currentUrl
                            if (isAdBlockEnabled && (AdBlocker.isAd(reqUrl) || NativeAdBlockEngine.shouldBlock(reqUrl, docUrl))) {
                                return AdBlocker.createEmptyResponse()
                            }
                            val candidate = com.omnicast.core.domain.models.StreamCandidate(
                                url = reqUrl, pageUrl = currentUrl,
                                headers = request.requestHeaders ?: emptyMap(),
                                userAgent = UA_MOBILE, pageTitle = currentKnownTitle
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

            val webView = WebView(ctx)
            val swipeRefresh = SwipeRefreshLayout(ctx).apply {
                setColorSchemeColors(0xFF38BDF8.toInt())
                setOnRefreshListener {
                    webView.reload()
                }
                setOnChildScrollUpCallback { _, _ ->
                    webView.canScrollVertically(-1)
                }
            }

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
                                try { el.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'smooth' }); }
                                catch(_) { try { el.scrollIntoView(); } catch(__) {} }
                            }, 50);
                        }
                    }, true);
                })();
            """.trimIndent()

            webView.apply {
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
                settings.setSupportMultipleWindows(false)
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.userAgentString = UA_MOBILE

                webViewClient = object : WebViewClient() {
                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        android.util.Log.e("BrowserWebView", "🛡️ Chromium Render Process terminó de forma imprevista. Evitando cierre de la app.")
                        swipeRefresh.isRefreshing = false
                        return true
                    }

                    override fun onPageStarted(v: WebView?, u: String?, f: Bitmap?) {
                        CookieManager.getInstance().flush()
                        u?.let { if (!it.startsWith("about:") && !it.contains("favicon")) onPageStarted(it) }
                        if (isAdBlockEnabled) {
                            v?.evaluateJavascript(AdBlocker.getAdHidingScript(), null)
                            if (WvcShieldSecurityEngine.isEnabled) v?.evaluateJavascript(WvcShieldSecurityEngine.getProtectionScript(), null)
                        }
                        v?.evaluateJavascript(inputViewportScript, null)
                    }

                    override fun onPageFinished(v: WebView?, u: String?) {
                        swipeRefresh.isRefreshing = false
                        CookieManager.getInstance().flush()
                        val title = v?.title ?: "Transmisión"
                        currentKnownTitle = title
                        onPageFinished(title, u)
                        if (isAdBlockEnabled) {
                            v?.evaluateJavascript(AdBlocker.getAdHidingScript(), null)
                            if (WvcShieldSecurityEngine.isEnabled) v?.evaluateJavascript(WvcShieldSecurityEngine.getProtectionScript(), null)
                        }
                        v?.evaluateJavascript(inputViewportScript, null)
                        v?.evaluateJavascript(WvcDomSniffer.getInjectionScript(ctx), null)
                    }

                    override fun doUpdateVisitedHistory(v: WebView?, u: String?, isR: Boolean) {
                        u?.let { onHistoryUpdated(it, v?.title) }
                    }

                    override fun shouldOverrideUrlLoading(v: WebView?, r: WebResourceRequest?): Boolean {
                        val reqUrl = r?.url?.toString() ?: return false
                        if (AdBlocker.isExternalRedirect(reqUrl)) return true
                        val liveUrl = v?.url ?: currentUrl
                        if (isAdBlockEnabled && WvcShieldSecurityEngine.isEnabled && WvcShieldSecurityEngine.shouldBlockDelayedRedirect(r.hasGesture(), reqUrl, liveUrl)) return true
                        val curHost = try { java.net.URI(liveUrl).host?.lowercase() } catch (_: Throwable) { null }
                        val targetHost = try { r.url.host?.lowercase() } catch (_: Throwable) { null }
                        val isSameSite = curHost != null && targetHost != null && (targetHost == curHost || targetHost.endsWith(".$curHost") || curHost.endsWith(".$targetHost"))
                        val isSourceWhitelisted = curHost?.let { AdBlockWhitelistManager.isWhitelistedHost(it) } ?: false
                        val isSourceSearchEngine = curHost?.let { AdBlocker.isSearchEngine(it) } ?: false

                        if (isAdBlockEnabled && !isSourceWhitelisted) {
                            if (AdBlocker.isAd(reqUrl) || NativeAdBlockEngine.shouldBlock(reqUrl, liveUrl)) {
                                android.util.Log.w("BrowserWebView", "🛑 Redirección publicitaria bloqueada: $targetHost")
                                targetHost?.let { AdBlocker.learnAndBlockAdHost(it, curHost) }
                                showBlockedToast(ctx)
                                return true
                            }
                            if (!isSameSite && !isSourceSearchEngine && targetHost != null) {
                                val isAllowedExternal = AdBlocker.isSearchEngine(targetHost) ||
                                    AdBlocker.isKnownMediaHost(targetHost) ||
                                    AdBlocker.isVerificationOrCaptcha(targetHost) ||
                                    AdBlockWhitelistManager.isWhitelistedHost(targetHost)
                                if (!isAllowedExternal) {
                                    android.util.Log.w("BrowserWebView", "🛑 Secuestro publicitario bloqueado hacia: $targetHost (aprendiendo dominio)")
                                    AdBlocker.learnAndBlockAdHost(targetHost, curHost)
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
                            val docUrl = r.requestHeaders?.get("Referer") ?: currentUrl
                            if (isAdBlockEnabled && (AdBlocker.isAd(reqUrl) || AdBlocker.isVideoAd(reqUrl) || NativeAdBlockEngine.shouldBlock(reqUrl, docUrl))) {
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
                        return false
                    }

                    override fun onProgressChanged(v: WebView?, p: Int) {
                        onProgress(p)
                        if (p >= 80) swipeRefresh.isRefreshing = false
                        if (isAdBlockEnabled && (p == 30 || p == 60 || p == 100)) v?.evaluateJavascript(AdBlocker.getAdHidingScript(), null)
                        if (p == 100) v?.evaluateJavascript(WvcDomSniffer.getInjectionScript(ctx), null)
                    }

                    override fun onReceivedTitle(v: WebView?, title: String?) {
                        super.onReceivedTitle(v, title)
                        val t = title?.trim()
                        if (!t.isNullOrBlank() && !t.contains("about:blank") && t != "Transmisión" && !t.startsWith("http")) {
                            currentKnownTitle = t
                            post { onTitleReceived(t) }
                        }
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

            swipeRefresh.addView(webView)
            swipeRefresh
        },
        modifier = Modifier.fillMaxSize().alpha(if (isHome) 0f else 1f)
    )
}
