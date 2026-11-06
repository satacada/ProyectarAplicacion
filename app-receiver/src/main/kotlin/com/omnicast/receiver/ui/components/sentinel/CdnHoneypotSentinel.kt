package com.omnicast.receiver.ui.components.sentinel

import com.omnicast.receiver.log.ReceiverLogger
import java.net.HttpURLConnection

/**
 * Centinela 3: Satélite modular desacoplado de detección de respuestas falsas (Honeypot HTML) en TV Box.
 * Principio "Poner y Sacar" (AGENTS.md):
 * Intercepta y descarta en 0 ms respuestas de CDN que devuelven HTML en vez de streams multimedia (.m3u8, .ts, .mp4, .mpd),
 * protegiendo a Hls.js y ExoPlayer de colapsar por errores de parseo (manifestParsingError code=200).
 */
object CdnHoneypotSentinel {

    private val HTML_SIGNATURES = listOf("<html", "<!doctype", "parking", "challenge-platform")

    fun isHoneypotHtml(conn: HttpURLConnection, urlStr: String): Boolean {
        val lower = urlStr.lowercase().substringBefore('?')
        val isMedia = lower.contains(".m3u8") || lower.contains(".ts") ||
                      lower.contains(".mp4") || lower.contains(".mpd")
        if (!isMedia) return false

        val cType = conn.contentType?.lowercase() ?: ""
        val sample = peekSample(conn, cType)
        val lowerSample = sample.lowercase()

        val isHtmlContent = HTML_SIGNATURES.any { lowerSample.contains(it) }
        if (cType.contains("text/html") || isHtmlContent) {
            com.omnicast.core.domain.usecases.StreamCandidateValidator.markCdnHostOffline(urlStr)
            ReceiverLogger.log(
                "CDN_HONEYPOT_HTML",
                "🛑 CDN respondió HTML en vez de stream: $urlStr | Muestra: $sample",
                details = mapOf("url" to urlStr, "content_type" to cType, "sample" to sample)
            )
            return true
        }

        return false
    }

    private fun peekSample(conn: HttpURLConnection, cType: String): String = try {
        val stream = conn.inputStream
        if (stream.markSupported()) {
            stream.mark(1024)
            val buf = ByteArray(512)
            val read = stream.read(buf, 0, buf.size)
            stream.reset()
            if (read > 0) String(buf, 0, read) else ""
        } else if (cType.contains("text/html")) {
            val buf = ByteArray(512)
            val read = stream.read(buf, 0, buf.size)
            if (read > 0) String(buf, 0, read) else ""
        } else {
            ""
        }
    } catch (_: Throwable) { "" }
}
