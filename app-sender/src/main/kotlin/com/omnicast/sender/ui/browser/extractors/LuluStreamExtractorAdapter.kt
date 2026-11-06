package com.omnicast.sender.ui.browser.extractors

import com.omnicast.core.domain.models.MediaStream
import com.omnicast.core.domain.models.StreamCandidate
import com.omnicast.core.domain.ports.IMediaStreamExtractorPort
import com.omnicast.core.domain.usecases.StreamCandidateValidator
import com.omnicast.sender.ui.browser.UA_MOBILE
import java.net.URI

/**
 * Adaptador modular ("poner y sacar") para servidores LuluStream / LuluVdo / PonStream.
 * Extrae flujos HLS inyectando cabeceras legítimas y tokens de sesión.
 */
class LuluStreamExtractorAdapter : IMediaStreamExtractorPort {

    override val priority: Int = 23

    override fun canHandle(candidate: StreamCandidate): Boolean {
        val url = candidate.url.lowercase()
        val page = candidate.pageUrl.lowercase()
        val isDomainMatches = url.contains("lulustream.com") || url.contains("luluvdo.com") || url.contains("ponstream.com") ||
                              page.contains("lulustream.com") || page.contains("luluvdo.com") || page.contains("ponstream.com")
        return StreamCandidateValidator.isValidCdnStream(candidate.url, isDomainMatches)
    }

    override fun extract(candidate: StreamCandidate): MediaStream {
        val url = candidate.url
        val title = candidate.pageTitle ?: "Video LuluStream"
        val docUrl = candidate.pageUrl
        val isMaster = url.contains(".m3u8", ignoreCase = true) || url.contains("master", ignoreCase = true)

        val refererHost = try {
            val u = URI(docUrl)
            val h = u.host?.lowercase()
            if (!h.isNullOrBlank() && (h.contains("lulu") || h.contains("ponstream"))) {
                "${u.scheme ?: "https"}://$h/"
            } else {
                "https://luluvdo.com/"
            }
        } catch (_: Throwable) {
            "https://luluvdo.com/"
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
            qualityLabel = "LuluStream HD",
            headers = headers
        )
    }
}
