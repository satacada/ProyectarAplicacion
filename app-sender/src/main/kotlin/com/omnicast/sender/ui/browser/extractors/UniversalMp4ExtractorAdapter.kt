package com.omnicast.sender.ui.browser.extractors

import com.omnicast.core.domain.models.MediaStream
import com.omnicast.core.domain.models.StreamCandidate
import com.omnicast.core.domain.ports.IMediaStreamExtractorPort

/**
 * Adaptador universal intocable para videos directos en contenedores estándar (MP4, WebM, MKV).
 */
class UniversalMp4ExtractorAdapter : IMediaStreamExtractorPort {

    override val priority: Int = 10

    override fun canHandle(candidate: StreamCandidate): Boolean {
        val lower = candidate.url.lowercase()
        val path = lower.substringBefore('?')
        return path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".mkv") ||
               (lower.contains(".mp4?") && !lower.contains("stub.mp4"))
    }

    override fun extract(candidate: StreamCandidate): MediaStream {
        val lower = candidate.url.lowercase()
        val format = when {
            lower.contains(".webm") -> "WEBM"
            lower.contains(".mkv") -> "MKV"
            else -> "MP4"
        }
        val title = candidate.pageTitle ?: "Video $format"

        return MediaStream(
            url = candidate.url,
            format = format,
            title = title,
            isMaster = false,
            qualityLabel = "Directo",
            headers = candidate.headers
        )
    }
}
