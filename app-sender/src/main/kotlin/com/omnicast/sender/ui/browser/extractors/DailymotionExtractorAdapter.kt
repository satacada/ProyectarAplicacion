package com.omnicast.sender.ui.browser.extractors

import com.omnicast.core.domain.models.MediaStream
import com.omnicast.core.domain.models.StreamCandidate
import com.omnicast.core.domain.ports.IMediaStreamExtractorPort

/**
 * Adaptador específico e independiente ("poner y sacar") para videos de Dailymotion.
 */
class DailymotionExtractorAdapter : IMediaStreamExtractorPort {

    override val priority: Int = 20

    private val dmRegex = Regex("(?:video/|dai\\.ly/)([a-zA-Z0-9]+)")

    override fun canHandle(candidate: StreamCandidate): Boolean {
        val url = candidate.url
        if (!url.contains("dailymotion.com") && !url.contains("dai.ly")) return false
        return dmRegex.containsMatchIn(url)
    }

    override fun extract(candidate: StreamCandidate): MediaStream? {
        val videoId = dmRegex.find(candidate.url)?.groupValues?.get(1) ?: return null
        val cleanUrl = "https://www.dailymotion.com/video/$videoId"
        val title = candidate.pageTitle ?: "Dailymotion Video"

        return MediaStream(
            url = cleanUrl,
            format = "DAILYMOTION",
            title = title,
            isMaster = true,
            qualityLabel = "HD Adaptativo",
            headers = emptyMap()
        )
    }
}
