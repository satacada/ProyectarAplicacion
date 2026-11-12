package com.omnicast.sender.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.os.Message
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.omnicast.sender.log.SenderLogger

/**
 * Interceptor de popups y ventanas emergentes para navegación web protegida.
 * Redirige captchas y verificaciones al WebView padre y bloquea ventanas publicitarias.
 */
class BrowserPopupInterceptor(private val context: Context) {
    private var interceptorView: WebView? = null

    @Suppress("UNUSED_PARAMETER")
    @SuppressLint("SetJavaScriptEnabled")
    fun handleCreateWindow(parentView: WebView?, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        if (resultMsg == null) return false
        val curUrl = parentView?.url ?: ""
        val curHost = try { java.net.URI(curUrl).host?.lowercase() } catch (_: Throwable) { null }

        val tempView = interceptorView ?: WebView(context).apply {
            settings.javaScriptEnabled = false
            settings.setSupportMultipleWindows(false)
            interceptorView = this
        }

        tempView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val targetUrl = request?.url?.toString() ?: return true
                val targetHost = try { request.url.host?.lowercase() } catch (_: Throwable) { null }

                if (AdBlocker.isVerificationOrCaptcha(targetUrl)) {
                    parentView?.loadUrl(targetUrl)
                    return true
                }
                val isNotAd = targetHost != null && !AdBlocker.isAd(targetUrl) && !AdBlockDatabase.isAdDomain(targetHost)
                val isSameHost = curHost != null && targetHost != null && isSameMainHost(curHost, targetHost)
                val isFromSearch = curHost != null && AdBlocker.isSearchEngine(curHost)

                if (isNotAd && (isSameHost || isFromSearch)) {
                    parentView?.loadUrl(targetUrl)
                    return true
                }
                SenderLogger.w("POPUP_BLOCKED", "🛡️ Popup de iframe/ventana bloqueado: $targetHost (desde $curHost)")
                targetHost?.let { AdBlocker.learnAndBlockAdHost(it, curHost) }
                view?.stopLoading()
                return true
            }
        }

        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        transport.webView = tempView
        resultMsg.sendToTarget()
        return true
    }

    private fun isSameMainHost(h1: String, h2: String): Boolean {
        if (h1 == h2 || h1.endsWith(".$h2") || h2.endsWith(".$h1")) return true
        val b1 = extractBrand(h1)
        val b2 = extractBrand(h2)
        return b1.length >= 4 && b1 == b2
    }

    private fun extractBrand(host: String): String {
        val clean = host.removePrefix("www.").substringBefore(':')
        val parts = clean.split('.')
        return if (parts.size >= 2) parts[parts.size - 2] else clean
    }

    fun destroy() {
        try {
            interceptorView?.destroy()
            interceptorView = null
        } catch (_: Throwable) {}
    }
}
