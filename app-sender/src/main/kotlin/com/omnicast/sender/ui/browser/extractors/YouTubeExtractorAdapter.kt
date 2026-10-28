package com.omnicast.sender.ui.browser.extractors

import com.omnicast.core.domain.models.MediaStream
import com.omnicast.core.domain.models.StreamCandidate
import com.omnicast.core.domain.ports.IMediaStreamExtractorPort

/**
 * Adaptador específico e independiente ("poner y sacar") para videos y Shorts de YouTube.
 */
class YouTubeExtractorAdapter : IMediaStreamExtractorPort {

    override val priority: Int = 20

    private val ytRegex = Regex("(?:watch\\?v=|shorts/|youtu\\.be/|embed/)([a-zA-Z0-9_-]{11})")

    override fun canHandle(candidate: StreamCandidate): Boolean {
        val url = candidate.url
        if (!url.contains("youtube.com") && !url.contains("youtu.be")) return false
        if (url.contains("googlevideo.com") || url.contains("videoplayback")) return false
        val page = candidate.pageUrl.lowercase()
        if (page.isNotBlank() && !page.contains("youtube.com") && !page.contains("youtu.be") && !page.startsWith("about:")) return false
        return ytRegex.containsMatchIn(url)
    }

    override fun extract(candidate: StreamCandidate): MediaStream? {
        val videoId = ytRegex.find(candidate.url)?.groupValues?.get(1) ?: return null
        val cleanUrl = "https://www.youtube.com/watch?v=$videoId"
        val title = candidate.pageTitle ?: "YouTube Video"

        return MediaStream(
            url = cleanUrl,
            format = "YOUTUBE",
            title = title,
            isMaster = true,
            qualityLabel = "Full HD / Auto",
            headers = emptyMap()
        )
    }
}
