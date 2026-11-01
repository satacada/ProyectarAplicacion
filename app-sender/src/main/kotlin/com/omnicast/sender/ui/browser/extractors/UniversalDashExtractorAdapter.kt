package com.omnicast.sender.ui.browser.extractors

import com.omnicast.core.domain.models.MediaStream
import com.omnicast.core.domain.models.StreamCandidate
import com.omnicast.core.domain.ports.IMediaStreamExtractorPort

/**
 * Adaptador universal intocable para transmisiones DASH (Dynamic Adaptive Streaming over HTTP).
 */
class UniversalDashExtractorAdapter : IMediaStreamExtractorPort {

    override val priority: Int = 10

    override fun canHandle(candidate: StreamCandidate): Boolean {
        val cleanPath = candidate.url.substringBefore('?').lowercase()
        if (cleanPath.contains(".m3u8") || cleanPath.contains("/hls")) return false
        return cleanPath.endsWith(".mpd") || candidate.url.contains("format=dash", ignoreCase = true)
    }

    override fun extract(candidate: StreamCandidate): MediaStream {
        val title = candidate.pageTitle ?: "Transmisión DASH"
        return MediaStream(
            url = candidate.url,
            format = "DASH",
            title = title,
            isMaster = true,
            qualityLabel = "Auto Adaptativo",
            headers = candidate.headers
        )
    }
}
