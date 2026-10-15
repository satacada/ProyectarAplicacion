package com.omnicast.sender.ui.browser.extractors

import com.omnicast.core.domain.models.MediaStream
import com.omnicast.core.domain.models.StreamCandidate
import com.omnicast.core.domain.ports.IMediaStreamExtractorPort
import com.omnicast.sender.ui.browser.UA_MOBILE

/**
 * Adaptador modular ("poner y sacar") para reproductor Fastream.
 * Garantiza cabeceras legítimas y extracción de flujos de video protegidos.
 */
class FastreamExtractorAdapter : IMediaStreamExtractorPort {

    override val priority: Int = 25

    private val fastreamIdRegex = Regex("(?:/hls\\d*/\\d+/\\d+/|/embed-|/)([a-zA-Z0-9]{10,12})(?:[_,.]|$)")

    override fun canHandle(candidate: StreamCandidate): Boolean {
        val lower = candidate.url.lowercase()
        return lower.contains("fastream.to") || lower.contains("/hls2/")
    }

    override fun extract(candidate: StreamCandidate): MediaStream? {
        val url = candidate.url
        val lower = url.lowercase()
        val fastreamId = fastreamIdRegex.find(url)?.groupValues?.get(1)
        val isMaster = lower.contains("master.m3u8") || lower.contains("urlset") || lower.contains(".m3u8")
        val title = candidate.pageTitle ?: "Fastream Video"
        val embedUrl = if (!fastreamId.isNullOrBlank()) "https://fastream.to/embed-$fastreamId.html" else "https://fastream.to/embed-video.html"

        val headers = mutableMapOf(
            "User-Agent" to UA_MOBILE,
            "Referer" to embedUrl,
            "Origin" to "https://fastream.to",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Site" to "cross-site"
        )

        return MediaStream(
            url = url,
            format = "HLS",
            title = title,
            isMaster = isMaster,
            qualityLabel = if (isMaster) "Auto Adaptativo" else "HLS Fastream",
            headers = headers
        )
    }
}
