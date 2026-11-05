package com.omnicast.receiver.ui.components.sentinel

import com.omnicast.receiver.log.ReceiverLogger
import java.net.HttpURLConnection

/**
 * Centinela 3: Satélite modular desacoplado de detección de respuestas falsas (Honeypot HTML) en TV Box.
 * Principio "Poner y Sacar" (AGENTS.md):
 * Intercepta y descarta en 0 ms respuestas de CDN que devuelven HTML en vez de streams multimedia (.m3u8, .ts, .mp4),
 * protegiendo a Hls.js y ExoPlayer de colapsar por errores de parseo (manifestParsingError code=200).
 */
object CdnHoneypotSentinel {

    fun isHoneypotHtml(conn: HttpURLConnection, urlStr: String): Boolean {
        val lower = urlStr.lowercase()
        val isMedia = lower.contains(".m3u8") || lower.contains(".ts") || lower.contains(".mp4")
        if (!isMedia) return false

        val cType = conn.contentType?.lowercase() ?: ""
        if (cType.contains("text/html")) {
            val sample = try {
                conn.inputStream.bufferedReader().use { it.readText().take(200) }
            } catch (_: Throwable) { "" }

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
}
