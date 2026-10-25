package com.omnicast.sender.ui.browser

import android.content.Context
import android.os.Message
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.URI

/**
 * Interceptor de ventanas emergentes (target="_blank" y window.open).
 * Permite que los resultados de búsqueda (Yandex, Google) y enlaces legítimos
 * abran en el WebView principal con 0 MB de consumo extra de RAM, erradicando OOM.
 */
class BrowserPopupInterceptor(
    context: Context,
    private val mainWebView: WebView,
    private val isAdBlockEnabled: () -> Boolean,
    private val getLivePageUrl: () -> String,
    private val onBlocked: () -> Unit
) {
    private val interceptorView = WebView(context).apply {
        settings.javaScriptEnabled = false
    }

    init {
        interceptorView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                val destUrl = req?.url?.toString() ?: return true
                v?.stopLoading()
                handleNavigation(destUrl)
                return true
            }
        }
    }

    fun handleCreateWindow(view: WebView?, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        val liveUrl = view?.url ?: getLivePageUrl()
        val curHost = try { URI(liveUrl).host?.lowercase() } catch (_: Throwable) { null }
        val isSourceWhitelisted = curHost?.let { AdBlockWhitelistManager.isWhitelistedHost(it) } ?: false
        val isSourceSearchEngine = curHost?.let { AdBlocker.isSearchEngine(it) } ?: false

        // 1. Suprime popups automáticos de scripts sin gesto del usuario
        if (!isUserGesture && !isSourceWhitelisted && !isSourceSearchEngine) {
            return false
        }

        // 2. Si el usuario tocó un enlace directo con target="_blank" (Yandex, Google, etc.)
        val hitUrl = view?.hitTestResult?.extra
        if (!hitUrl.isNullOrBlank() && !hitUrl.startsWith("javascript:", ignoreCase = true)) {
            handleNavigation(hitUrl)
            return false
        }

        // 3. Si es un window.open dinámico disparado por click, capturar en interceptorView único
        val transport = resultMsg?.obj as? WebView.WebViewTransport
        if (transport != null) {
            transport.webView = interceptorView
            resultMsg.sendToTarget()
            return true
        }
        return false
    }

    private fun handleNavigation(destUrl: String) {
        if (destUrl.isBlank() || destUrl.startsWith("about:")) return
        val liveUrl = mainWebView.url ?: getLivePageUrl()
        val curHost = try { URI(liveUrl).host?.lowercase() } catch (_: Throwable) { null }
        val targetHost = try { URI(destUrl).host?.lowercase() } catch (_: Throwable) { null }
        val isSourceWhitelisted = curHost?.let { AdBlockWhitelistManager.isWhitelistedHost(it) } ?: false
        val isSourceSearchEngine = curHost?.let { AdBlocker.isSearchEngine(it) } ?: false

        val isAllowedTarget = isSourceSearchEngine || isSourceWhitelisted || AdBlocker.isSameSite(curHost, targetHost) ||
            (targetHost != null && (AdBlocker.isSearchEngine(targetHost) || AdBlocker.isKnownMediaHost(targetHost) ||
                                   AdBlocker.isVerificationOrCaptcha(targetHost) || AdBlockWhitelistManager.isWhitelistedHost(targetHost)))

        val isBlocked = isAdBlockEnabled() && !isSourceWhitelisted && !isSourceSearchEngine &&
            (!isAllowedTarget || AdBlocker.isAd(destUrl) || AdBlocker.isExternalRedirect(destUrl) || NativeAdBlockEngine.shouldBlock(destUrl, liveUrl))

        if (!isBlocked) {
            mainWebView.post { mainWebView.loadUrl(destUrl) }
        } else {
            targetHost?.let { AdBlocker.learnAndBlockAdHost(it, curHost) }
            onBlocked()
        }
    }

    fun destroy() {
        try { interceptorView.destroy() } catch (_: Throwable) {}
    }
}
