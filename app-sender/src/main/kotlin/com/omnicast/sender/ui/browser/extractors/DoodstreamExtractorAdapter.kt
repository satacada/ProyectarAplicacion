package com.omnicast.sender.ui.browser.extractors

import com.omnicast.core.domain.models.MediaStream
import com.omnicast.core.domain.models.StreamCandidate
import com.omnicast.core.domain.ports.IMediaStreamExtractorPort
import com.omnicast.sender.ui.browser.UA_MOBILE
import java.net.URI

/**
 * Adaptador modular ("poner y sacar") para reproductores Doodstream y Doopstream.
 * Extrae flujos MP4 directos de la red CDN inyectando la cabecera Referer requerida.
 */
class DoodstreamExtractorAdapter : IMediaStreamExtractorPort {

    override val priority: Int = 24

    override fun canHandle(candidate: StreamCandidate): Boolean {
        val lower = candidate.url.lowercase()
        if (lower.contains("/cdn-cgi/") || lower.contains("/e/") || lower.contains("/embed")) return false
        if (lower.endsWith(".html") || lower.endsWith(".htm") || lower.endsWith(".js") || lower.endsWith(".css")) return false
        if (lower.contains("/pass_md5/")) return false

        val host = try { URI(candidate.url).host?.lowercase() ?: "" } catch (_: Throwable) { "" }
        val isDoodHost = host.contains("doodcdn") || host.contains("doopstream") || host.contains("ds2play") ||
                         host.contains("doodstream") || host.contains("doo.la") || host.startsWith("dood.")
        val isDoodPath = lower.contains("/pass/") || lower.contains("video~")
        return isDoodHost || isDoodPath
    }

    override fun extract(candidate: StreamCandidate): MediaStream {
        val url = candidate.url
        val title = candidate.pageTitle ?: "Video Doodstream"
        val docUrl = candidate.pageUrl

        val refererHost = try {
            val u = URI(docUrl)
            if (!u.host.isNullOrBlank()) "${u.scheme ?: "https"}://${u.host}/" else "https://doodstream.com/"
        } catch (_: Throwable) {
            "https://doodstream.com/"
        }

        val headers = candidate.headers.toMutableMap()
        if (!headers.containsKey("User-Agent")) headers["User-Agent"] = UA_MOBILE
        if (!headers.containsKey("Referer")) headers["Referer"] = refererHost
        if (!headers.containsKey("Origin")) {
            val origin = refererHost.trimEnd('/')
            headers["Origin"] = origin
        }
        if (!headers.containsKey("Sec-Fetch-Mode")) headers["Sec-Fetch-Mode"] = "cors"
        if (!headers.containsKey("Sec-Fetch-Dest")) headers["Sec-Fetch-Dest"] = "video"
        if (!headers.containsKey("Sec-Fetch-Site")) headers["Sec-Fetch-Site"] = "cross-site"

        return MediaStream(
            url = url,
            format = "MP4",
            title = title,
            isMaster = false,
            qualityLabel = "DoodStream HD",
            headers = headers
        )
    }
}
