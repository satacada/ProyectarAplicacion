package com.omnicast.receiver.ui.components

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Interceptor nativo anti-CORS para el reproductor web de la TV Box.
 * Intercepta peticiones HLS/VOD (.m3u8, .ts, .vtt) y asegura la cabecera Access-Control-Allow-Origin: *
 * respondiendo peticiones OPTIONS en 0ms y descargando fragmentos sin restricciones de navegador.
 */
object TvWebResourceInterceptor {

    fun shouldIntercept(request: WebResourceRequest?, defaultHeaders: Map<String, String>): WebResourceResponse? {
        val req = request ?: return null
        val urlStr = req.url?.toString() ?: return null
        val method = req.method ?: "GET"

        // Responder preflight OPTIONS de inmediato con autorización total
        if (method.equals("OPTIONS", ignoreCase = true)) {
            val preflightHeaders = mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Access-Control-Allow-Methods" to "GET, POST, OPTIONS, HEAD",
                "Access-Control-Allow-Headers" to "*",
                "Access-Control-Max-Age" to "86400"
            )
            return WebResourceResponse("text/plain", "UTF-8", 200, "OK", preflightHeaders, ByteArrayInputStream(ByteArray(0)))
        }

        val lower = urlStr.lowercase()
        val isMedia = lower.contains(".m3u8") || lower.contains(".ts") || lower.contains(".m4s") ||
                lower.contains(".mp4") || lower.contains(".vtt") || lower.contains("/hls")
        if (!isMedia) return null

        val isDropOrFast = lower.contains("dropcdn.io") || lower.contains("dr0pstream.com") || lower.contains("fastream.to")
        return try {
            var conn = openHttpConnection(urlStr, method, defaultHeaders, req.requestHeaders, isDropOrFast, useMobileUa = false)
            var code = conn.responseCode
            if (isDropOrFast && code == 403) {
                conn.disconnect()
                // Auto-reintento con identidad móvil por si el token fue firmado contra el WebView del móvil
                conn = openHttpConnection(urlStr, method, defaultHeaders, req.requestHeaders, isDropOrFast, useMobileUa = true)
                code = conn.responseCode
            }

            val isSuccess = code in 200..299
            if (!isSuccess) {
                val errBody = runCatching { conn.errorStream?.bufferedReader()?.use { it.readText() }?.take(150) }.getOrNull() ?: ""
                com.omnicast.receiver.log.ReceiverLogger.log("TvWebResourceInterceptor", "⚠️ Error HTTP $code en recurso web: $urlStr $errBody")
                conn.disconnect()
                return null
            }
            val inStream: InputStream = conn.inputStream
            val msg = conn.responseMessage ?: "OK"

            val mime = when {
                lower.contains(".m3u8") -> "application/vnd.apple.mpegurl"
                lower.contains(".ts") -> "video/mp2t"
                lower.contains(".vtt") -> "text/vtt"
                lower.contains(".mp4") -> "video/mp4"
                else -> conn.contentType?.substringBefore(';') ?: "application/octet-stream"
            }

            val respHeaders = mutableMapOf(
                "Access-Control-Allow-Origin" to "*",
                "Access-Control-Allow-Methods" to "GET, POST, OPTIONS, HEAD",
                "Access-Control-Allow-Headers" to "*"
            )
            conn.headerFields.forEach { (k, list) ->
                if (k != null && !respHeaders.containsKey(k)) {
                    respHeaders[k] = list.joinToString(", ")
                }
            }

            WebResourceResponse(mime, conn.contentEncoding ?: "UTF-8", code, msg, respHeaders, inStream)
        } catch (_: Throwable) {
            null
        }
    }

    private fun openHttpConnection(
        urlStr: String,
        method: String,
        defaultHeaders: Map<String, String>,
        requestHeaders: Map<String, String>?,
        isDropOrFast: Boolean,
        useMobileUa: Boolean
    ): HttpURLConnection {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 12000
            instanceFollowRedirects = true
            requestMethod = method
            if (isDropOrFast) {
                val ref = defaultHeaders["Referer"] ?: "https://dr0pstream.com/"
                val ua = if (useMobileUa) "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                         else (defaultHeaders["User-Agent"] ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                setRequestProperty("Referer", ref)
                setRequestProperty("User-Agent", ua)
                setRequestProperty("Accept", "*/*")
            } else {
                defaultHeaders.forEach { (k, v) -> if (!k.equals("Origin", true)) setRequestProperty(k, v) }
                requestHeaders?.forEach { (k, v) ->
                    if (!defaultHeaders.containsKey(k) && !k.equals("Origin", true)) {
                        setRequestProperty(k, v)
                    }
                }
            }
        }
        return conn
    }
}
