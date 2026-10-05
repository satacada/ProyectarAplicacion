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
               lower.contains("swdyu.com") || lower.contains("cloudemb.com") || lower.contains("morencius.com")
    }

    override fun extract(candidate: StreamCandidate): MediaStream {
        val lower = candidate.url.lowercase()
        val isMaster = lower.contains("master") || lower.contains("manifest") || lower.contains("playlist")
        val title = candidate.pageTitle ?: "Transmisión HLS"
        val quality = if (isMaster) "Auto Adaptativo" else "HLS Estándar"

        return MediaStream(
            url = candidate.url,
            format = "HLS",
            title = title,
            isMaster = isMaster,
            qualityLabel = quality,
            headers = candidate.headers
        )
    }
}
