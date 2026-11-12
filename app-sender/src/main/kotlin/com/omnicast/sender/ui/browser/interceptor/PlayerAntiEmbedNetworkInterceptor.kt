package com.omnicast.sender.ui.browser.interceptor

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL

object PlayerAntiEmbedNetworkInterceptor {
    fun shouldIntercept(request: WebResourceRequest?, refererUrl: String?): WebResourceResponse? {
        if (request == null || request.isForMainFrame) return null
        val url = request.url?.toString() ?: return null
        val dest = request.requestHeaders?.get("Sec-Fetch-Dest")?.lowercase()
        val accept = request.requestHeaders?.get("Accept")?.lowercase()
        val isSubframeDoc = dest == "iframe" || (accept != null && accept.contains("text/html"))
        if (!isSubframeDoc && !url.contains("/f/") && !url.contains("/embed/")) return null

        val cleanPath = url.substringBefore('?').lowercase()
        if (cleanPath.endsWith(".js") || cleanPath.endsWith(".css") || cleanPath.endsWith(".png") ||
            cleanPath.endsWith(".m3u8") || cleanPath.endsWith(".mp4") || cleanPath.endsWith(".ts")) {
            return null
        }

        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 10000
                instanceFollowRedirects = true
                request.requestHeaders?.forEach { (k, v) -> setRequestProperty(k, v) }
                if (!request.requestHeaders.containsKey("Referer") && !refererUrl.isNullOrBlank()) {
                    setRequestProperty("Referer", refererUrl)
                }
                val cookies = CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrBlank()) setRequestProperty("Cookie", cookies)
            }

            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) return null
            val contentType = conn.contentType?.lowercase() ?: ""
            if (!contentType.contains("text/html")) return null

            val rawBytes = conn.inputStream.use { it.readBytes() }
            val html = String(rawBytes, Charsets.UTF_8)

            if (html.contains("isAllowedHost") || html.contains("__embedBlocked")) {
                val sanitized = html
                    .replace("function isAllowedHost(h) {", "function isAllowedHost(h) { return true; ")
                    .replace("function isAllowedHost(h){", "function isAllowedHost(h){return true;")
                    .replace("window.stop && window.stop()", "/* stopped_noop */")
                    .replace("window.stop()", "/* stopped_noop */")
                val sanitizedBytes = sanitized.toByteArray(Charsets.UTF_8)
                WebResourceResponse("text/html", "UTF-8", 200, "OK", emptyMap(), ByteArrayInputStream(sanitizedBytes))
            } else {
                WebResourceResponse("text/html", "UTF-8", 200, "OK", emptyMap(), ByteArrayInputStream(rawBytes))
            }
        } catch (_: Throwable) {
            null
        }
    }
}
