package com.omnicast.sender.ui.browser.extractors

import com.omnicast.core.domain.models.MediaStream
import com.omnicast.core.domain.models.StreamCandidate
import com.omnicast.core.domain.ports.IMediaStreamExtractorPort
import com.omnicast.core.domain.usecases.StreamCandidateValidator
import com.omnicast.sender.ui.browser.UA_MOBILE
import java.net.URI

/**
 * Adaptador modular ("poner y sacar") para reproductor Streamwish y sus variantes espejo.
 * Extrae flujos HLS e inyecta cabeceras Referer/Origin requeridas por sus servidores edge.
 */
class StreamwishExtractorAdapter : IMediaStreamExtractorPort {

    override val priority: Int = 22

    companion object {
        private val DOMAINS = listOf(
            "streamwish.to",
            "streamwish.com",
            "strwish.com",
            "swish.com",
            "flaswish.com"
        )
    }

    override fun canHandle(candidate: StreamCandidate): Boolean {
        val isDomainMatches = StreamCandidateValidator.matchesProviderFamily(
            candidate.url,
            candidate.pageUrl,
            "streamwish",
            DOMAINS
        )
        return StreamCandidateValidator.isValidCdnStream(candidate.url, isDomainMatches)
    }

    override fun extract(candidate: StreamCandidate): MediaStream {
        val url = candidate.url
        val title = candidate.pageTitle ?: "Video Streamwish"
        val docUrl = candidate.pageUrl
        val isMaster = url.contains(".m3u8", ignoreCase = true) || url.contains("master", ignoreCase = true)

        val refererHost = try {
            val u = URI(docUrl)
            val h = u.host?.lowercase()
            if (!h.isNullOrBlank() && (h.contains("streamwish") || h.contains("strwish") || h.contains("swish") || h.contains("flaswish"))) {
                "${u.scheme ?: "https"}://$h/"
            } else {
                "https://streamwish.to/"
            }
        } catch (_: Throwable) {
            "https://streamwish.to/"
        }

        val headers = candidate.headers.toMutableMap()
        if (!headers.containsKey("User-Agent")) headers["User-Agent"] = UA_MOBILE
        if (!headers.containsKey("Referer")) headers["Referer"] = refererHost
        if (!headers.containsKey("Origin")) headers["Origin"] = refererHost.trimEnd('/')
        if (!headers.containsKey("Sec-Fetch-Mode")) headers["Sec-Fetch-Mode"] = "cors"
        if (!headers.containsKey("Sec-Fetch-Dest")) headers["Sec-Fetch-Dest"] = "video"
        if (!headers.containsKey("Sec-Fetch-Site")) headers["Sec-Fetch-Site"] = "cross-site"

        return MediaStream(
            url = url,
            format = "HLS",
            title = title,
            isMaster = isMaster,
            qualityLabel = "Streamwish HD",
            headers = headers
        )
    }
}
