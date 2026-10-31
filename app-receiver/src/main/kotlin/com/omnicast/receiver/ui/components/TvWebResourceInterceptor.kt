package com.omnicast.receiver.ui.components

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Interceptor nativo anti-CORS para el reproductor web de la TV Box.
 * Intercepta peticiones HLS/VOD (.m3u8, .ts, .vtt, .mp4) y asegura la cabecera Access-Control-Allow-Origin: *
 * respondiendo peticiones OPTIONS en 0ms y descargando fragmentos sin restricciones de navegador.
 */
object TvWebResourceInterceptor {

    private const val UA_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    fun shouldIntercept(request: WebResourceRequest?, defaultHeaders: Map<String, String>): WebResourceResponse? {
        val req = request ?: return null
        val urlStr = req.url?.toString() ?: return null
        val method = req.method ?: "GET"

        val reqOrigin = req.requestHeaders?.get("Origin")?.takeIf { it.isNotBlank() } ?: defaultHeaders["Origin"] ?: "*"
        // 1. Responder preflight OPTIONS de inmediato con autorización total (CORS OK en 0ms)
        if (method.equals("OPTIONS", ignoreCase = true)) {
            val preflightHeaders = mapOf(
                "Access-Control-Allow-Origin" to reqOrigin,
                "Access-Control-Allow-Methods" to "GET, POST, OPTIONS, HEAD",
                "Access-Control-Allow-Headers" to "*",
                "Access-Control-Allow-Credentials" to "true",
                "Access-Control-Max-Age" to "86400"
            )
            return WebResourceResponse("text/plain", "UTF-8", 200, "OK", preflightHeaders, ByteArrayInputStream(ByteArray(0)))
        }

        val lower = urlStr.lowercase()
        val isMedia = lower.contains(".m3u8") || lower.contains(".ts") || lower.contains(".m4s") ||
                lower.contains(".mp4") || lower.contains(".vtt") || lower.contains("/hls")
        if (!isMedia) return null

        // 2. Interceptar y descargar inyectando Access-Control-Allow-Origin: *
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 15000
                instanceFollowRedirects = true
                requestMethod = method

                // User-Agent: usar Desktop UA si viene de Fastream/DropCDN o si defaultHeaders lo especifica
                val ua = defaultHeaders["User-Agent"]?.takeIf { !it.contains("Android", true) } ?: UA_DESKTOP
                setRequestProperty("User-Agent", ua)

                // Referer: obligatorio para autorizar la descarga (Fastream, OK.ru, DropCDN)
                val ref = defaultHeaders["Referer"]
                if (!ref.isNullOrBlank()) {
                    setRequestProperty("Referer", ref)
                }

                // Origin: coherente con el Referer si aplica
                val origin = defaultHeaders["Origin"] ?: ref?.substringBeforeLast('/')
                if (!origin.isNullOrBlank()) {
                    setRequestProperty("Origin", origin)
                }

                // Cookie de sesión si está presente
                val cookie = defaultHeaders["Cookie"]
                if (!cookie.isNullOrBlank()) {
                    setRequestProperty("Cookie", cookie)
                }

                setRequestProperty("Accept", "*/*")
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                com.omnicast.receiver.log.ReceiverLogger.log("TvWebResourceInterceptor", "⚠️ Error HTTP $code en: $urlStr")
                conn.disconnect()
                return null
            }

            val inStream: InputStream = conn.inputStream
            val mime = when {
                lower.contains(".m3u8") -> "application/vnd.apple.mpegurl"
                lower.contains(".ts") -> "video/mp2t"
                lower.contains(".vtt") -> "text/vtt"
                lower.contains(".mp4") -> "video/mp4"
                else -> conn.contentType?.substringBefore(';') ?: "application/octet-stream"
            }
            val encoding = if (lower.contains(".m3u8") || lower.contains(".vtt")) "UTF-8" else null

            val respHeaders = mutableMapOf(
                "Access-Control-Allow-Origin" to reqOrigin,
                "Access-Control-Allow-Methods" to "GET, POST, OPTIONS, HEAD",
                "Access-Control-Allow-Headers" to "*",
                "Access-Control-Allow-Credentials" to "true"
            )
            conn.headerFields.forEach { (k, list) ->
                if (k != null && !respHeaders.containsKey(k) && !k.equals("Content-Encoding", true) && !k.equals("Access-Control-Allow-Origin", true) && !k.equals("Access-Control-Allow-Credentials", true)) {
                    respHeaders[k] = list.joinToString(", ")
                }
            }

            WebResourceResponse(mime, encoding, 200, "OK", respHeaders, inStream)
        } catch (e: Throwable) {
            com.omnicast.receiver.log.ReceiverLogger.log("TvWebResourceInterceptor", "Excepción en recurso web: ${e.message}")
            null
        }
    }
}
