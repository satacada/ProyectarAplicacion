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
                if (curHost != null && AdBlocker.isSearchEngine(curHost)) {
                    if (targetHost != null && !AdBlocker.isAd(targetUrl) && !AdBlockDatabase.isAdDomain(targetHost)) {
                        parentView?.loadUrl(targetUrl)
                        return true
                    }
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

    fun destroy() {
        try {
            interceptorView?.destroy()
            interceptorView = null
        } catch (_: Throwable) {}
    }
}
