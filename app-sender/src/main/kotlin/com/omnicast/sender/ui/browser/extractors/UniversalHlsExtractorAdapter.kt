package com.omnicast.sender.ui.browser.extractors

import com.omnicast.core.domain.models.MediaStream
import com.omnicast.core.domain.models.StreamCandidate
import com.omnicast.core.domain.ports.IMediaStreamExtractorPort

/**
 * Adaptador universal intocable para transmisiones HLS (HTTP Live Streaming).
 * Soporta listas maestras (.m3u8), chunklists y fragmentos HLS estándar de la web abierta.
 */
class UniversalHlsExtractorAdapter : IMediaStreamExtractorPort {

    override val priority: Int = 10

    override fun canHandle(candidate: StreamCandidate): Boolean {
        val lower = candidate.url.lowercase()
        return lower.contains(".m3u8") ||
               lower.contains("chunklist") ||
               (lower.contains("/hls") && !lower.endsWith(".ts")) ||
               (lower.contains("master.txt") && lower.contains("/hls")) ||
               lower.contains("/stream/") || lower.contains("/manifest") ||
               lower.contains("/live") || lower.contains("live.m3u8") ||
               lower.contains("swdyu.com") || lower.contains("cloudemb.com") || lower.contains("morencius.com") ||
               lower.contains("p.php?url=") || (lower.contains("/p.php") && lower.contains("sig=")) ||
               lower.contains("pelisserieshoy.com/p.php") || lower.contains("embed69.org/p.php")
    }

    override fun extract(candidate: StreamCandidate): MediaStream {
        val lower = candidate.url.lowercase()
        val isLive = lower.contains("live") || lower.contains("chunklist")
        val isMaster = lower.contains("master") || lower.contains("manifest") || lower.contains("playlist") || isLive || lower.contains("p.php")
        val title = candidate.pageTitle ?: if (isLive) "🔴 Transmisión En Vivo" else "Transmisión HLS"
        val quality = if (isLive) "En Vivo" else if (isMaster) "Auto Adaptativo" else "HLS Estándar"
        val hdrs = candidate.headers.toMutableMap()
        if (lower.contains("pelisserieshoy.com") || lower.contains("p.php")) {
            hdrs["Referer"] = "https://player.pelisserieshoy.com/"
            hdrs["Origin"] = "https://player.pelisserieshoy.com"
        }

        return MediaStream(
            url = candidate.url,
            format = "HLS",
            title = title,
            isMaster = isMaster,
            qualityLabel = quality,
            headers = hdrs
        )
    }
}
