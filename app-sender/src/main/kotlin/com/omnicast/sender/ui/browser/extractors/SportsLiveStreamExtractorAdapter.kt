package com.omnicast.sender.ui.browser.extractors

import com.omnicast.core.domain.models.MediaStream
import com.omnicast.core.domain.models.StreamCandidate
import com.omnicast.core.domain.ports.IMediaStreamExtractorPort
import com.omnicast.core.domain.usecases.StreamCandidateValidator
import com.omnicast.sender.ui.browser.UA_MOBILE
import java.net.URI

/**
 * Adaptador modular ("poner y sacar") para transmisiones deportivas y proxies HLS (/playlist/...).
 * Prioridad 24 para evaluar antes de extractores universales genéricos y forzar reproductor Chromium Anti-CORS.
 */
class SportsLiveStreamExtractorAdapter : IMediaStreamExtractorPort {

    override val priority: Int = 24

    override fun canHandle(candidate: StreamCandidate): Boolean {
        return StreamCandidateValidator.isPlaylistProxyStream(candidate.url)
    }

    override fun extract(candidate: StreamCandidate): MediaStream {
        val hdrs = candidate.headers.toMutableMap()

        if (hdrs["Referer"].isNullOrBlank() && candidate.pageUrl.isNotBlank()) {
            hdrs["Referer"] = candidate.pageUrl
        }

        if (hdrs["Origin"].isNullOrBlank()) {
            val referer = hdrs["Referer"]
            if (!referer.isNullOrBlank()) {
                val origin = try {
                    val uri = URI(referer)
                    val scheme = uri.scheme ?: "https"
                    val host = uri.host
                    if (!host.isNullOrBlank()) {
                        val portStr = if (uri.port != -1 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
                        "$scheme://$host$portStr"
                    } else null
                } catch (_: Throwable) {
                    null
                }
                if (origin != null) {
                    hdrs["Origin"] = origin
                }
            }
        }

        if (hdrs["User-Agent"].isNullOrBlank()) {
            hdrs["User-Agent"] = candidate.userAgent.ifBlank { UA_MOBILE }
        }

        hdrs["X-Target-Player"] = "CHROMIUM_ANTI_CORS"

        return MediaStream(
            url = candidate.url,
            format = "HLS",
            title = candidate.pageTitle ?: "🔴 Transmisión En Vivo",
            isMaster = true,
            qualityLabel = "En Vivo",
            headers = hdrs
        )
    }
}
