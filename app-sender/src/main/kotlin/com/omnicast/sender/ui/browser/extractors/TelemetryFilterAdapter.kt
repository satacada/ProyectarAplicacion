package com.omnicast.sender.ui.browser.extractors

import com.omnicast.core.domain.ports.IStreamTelemetryFilterPort
import com.omnicast.sender.ui.browser.AdBlocker
import com.omnicast.sender.ui.browser.AdBlockRuleSyncManager

/**
 * Adaptador de salida para filtrado temprano de balizas de telemetría, estadísticas,
 * anuncios y recursos estáticos que no son medios reproducibles.
 */
class TelemetryFilterAdapter : IStreamTelemetryFilterPort {

    private val staticExtensions = listOf(
        ".html", ".htm", ".css", ".js", ".json", ".xml",
        ".jpg", ".jpeg", ".png", ".webp", ".gif", ".svg", ".ico",
        ".woff", ".woff2", ".ttf", ".eot", ".otf",
        ".vtt", ".srt", ".sub", ".ass", ".key"
    )

    private val baseTelemetryKeywords = listOf(
        "videostat", "statnew", "telemetry",
        "analytics", "beacon", "heartbeat", "logger", "tracking",
        "/ping", "/event", "report", "collect", "metric", "counter",
        "geo.dailymotion.com"
    )

    private val baseAdSegmentKeywords = listOf(
        "preroll", "midroll", "commercial", "/vast", "vast.",
        "stub.mp4", "dummy.mp4", "blank.mp4", "ad.mp4",
        "ad-delivery", "imasdk", "cat-bolt", "spotx", "doubleclick",
        "/ads/", "/ad/", "popunder", "sponsor",
        "dmxleo.dailymotion.com", "/vmap/"
    )

    override fun isTelemetryOrGarbage(url: String): Boolean {
        if (url.isBlank() || url.startsWith("blob:") || url.startsWith("data:") || url.startsWith("about:")) return true
        // 0. Evaluación nativa C++20 de latencia ultra-baja (<0.1 ms)
        if (com.omnicast.sender.nativeclassifier.NativeStreamClassifier.shouldDiscard(url)) return true
        val lower = url.lowercase()

        // 1. Descartar anuncios identificados por el motor de AdBlock
        if (AdBlocker.isAd(url)) return true

        // 2. Descartar extensiones estáticas comunes
        val pathOnly = lower.substringBefore('?')
        if (staticExtensions.any { pathOnly.endsWith(it) }) return true

        // 3. Descartar segmentos y placeholders de publicidad de video (base + dinámicas)
        val dynamicAds = AdBlockRuleSyncManager.getAdKeywords()
        if (baseAdSegmentKeywords.any { lower.contains(it) } || dynamicAds.any { lower.contains(it) }) return true

        // 4. Descartar pings y llamadas de analíticas/telemetría (base + dinámicas)
        val dynamicTelemetry = AdBlockRuleSyncManager.getTelemetryKeywords()
        if (baseTelemetryKeywords.any { lower.contains(it) } || dynamicTelemetry.any { lower.contains(it) }) return true

        // 5. Descartar fragmentos elementales de streaming que no son listas maestras (evitar spam de chunks)
        if (pathOnly.endsWith(".ts") || pathOnly.endsWith(".m4s") || lower.contains(".ts?") || lower.contains(".m4s?") ||
            lower.contains("init.mp4") || lower.contains("/init-") || lower.contains("-init.")) return true

        // 6. Descartar audio aislado si se busca el flujo de video compuesto
        if (lower.contains("/audio") || lower.contains("_audio.m3u8") || lower.contains("-audio.m3u8")) return true

        return false
    }
}
