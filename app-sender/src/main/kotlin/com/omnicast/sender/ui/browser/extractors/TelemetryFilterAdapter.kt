package com.omnicast.sender.ui.browser.extractors

import com.omnicast.core.domain.ports.IStreamTelemetryFilterPort
import com.omnicast.sender.ui.browser.AdBlocker

/**
 * Adaptador de salida para filtrado temprano de balizas de telemetría, estadísticas,
 * anuncios y recursos estáticos que no son medios reproducibles.
 */
class TelemetryFilterAdapter : IStreamTelemetryFilterPort {

    private val staticExtensions = listOf(
        ".html", ".htm", ".css", ".js", ".json", ".xml",
        ".jpg", ".jpeg", ".png", ".webp", ".gif", ".svg", ".ico",
        ".woff", ".woff2", ".ttf", ".eot", ".otf"
    )

    private val telemetryKeywords = listOf(
        "videostat", "statnew", "telemetry",
        "analytics", "beacon", "heartbeat", "logger", "tracking",
        "/ping", "/event", "report", "collect", "metric", "counter"
    )

    private val adSegmentKeywords = listOf(
        "preroll", "midroll", "commercial", "/vast", "vast.",
        "stub.mp4", "dummy.mp4", "blank.mp4", "ad.mp4"
    )

    override fun isTelemetryOrGarbage(url: String): Boolean {
        if (url.isBlank() || url.startsWith("blob:") || url.startsWith("data:") || url.startsWith("about:")) return true
        val lower = url.lowercase()

        // 1. Descartar anuncios identificados por el motor de AdBlock
        if (AdBlocker.isAd(url)) return true

        // 2. Descartar extensiones estáticas comunes
        val pathOnly = lower.substringBefore('?')
        if (staticExtensions.any { pathOnly.endsWith(it) }) return true

        // 3. Descartar segmentos y placeholders de publicidad de video
        if (adSegmentKeywords.any { lower.contains(it) }) return true

        // 4. Descartar pings y llamadas de analíticas/telemetría
        if (telemetryKeywords.any { lower.contains(it) }) return true

        // 5. Descartar fragmentos elementales de streaming que no son listas maestras (evitar spam de chunks)
        if (lower.contains(".ts?") || lower.endsWith(".ts") || lower.contains(".m4s") ||
            lower.contains("init.mp4") || lower.contains("/init-") || lower.contains("-init.")) return true

        // 6. Descartar audio aislado si se busca el flujo de video compuesto
        if (lower.contains("/audio") || lower.contains("_audio.m3u8") || lower.contains("-audio.m3u8")) return true

        return false
    }
}
