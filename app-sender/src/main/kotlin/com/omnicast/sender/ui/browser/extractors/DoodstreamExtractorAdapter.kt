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
        val pageHost = try { URI(candidate.pageUrl).host?.lowercase() ?: "" } catch (_: Throwable) { "" }

        val isDoodHost = host.contains("doodcdn") || host.contains("doopstream") || host.contains("ds2play") ||
                         host.contains("doodstream") || host.contains("doo.la") || host.startsWith("dood.") ||
                         host.contains("playmogo") || host.contains("d000d") || host.contains("myvidplay")

        val isDoodPage = pageHost.contains("doopstream") || pageHost.contains("doodstream") ||
                         pageHost.contains("playmogo") || pageHost.contains("ds2play") ||
                         pageHost.contains("doo.la") || pageHost.contains("d000d") || pageHost.startsWith("dood.")

        val isDoodPath = lower.contains("/pass/") || lower.contains("video~")
        return isDoodHost || isDoodPath || (isDoodPage && !lower.contains(".m3u8") && !lower.contains(".mpd"))
    }

    override fun extract(candidate: StreamCandidate): MediaStream {
        val url = candidate.url
        val title = candidate.pageTitle ?: "Video Doodstream"
        val docUrl = candidate.pageUrl
        val host = try { URI(candidate.url).host?.lowercase() ?: "" } catch (_: Throwable) { "" }

        val refererHost = try {
            val u = URI(docUrl)
            val h = u.host?.lowercase()
            if (!h.isNullOrBlank() && (h.contains("doop") || h.contains("dood") || h.contains("playmogo") || h.contains("ds2play") || h.contains("d000d"))) {
                "${u.scheme ?: "https"}://$h/"
            } else if (host.contains("playmogo")) {
                "https://playmogo.com/"
            } else if (host.contains("doopstream")) {
                "https://doopstream.com/"
            } else {
                "https://doodstream.com/"
            }
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
