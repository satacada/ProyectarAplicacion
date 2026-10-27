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
        // Rechazar explícitamente telemetría CDN, páginas web de iframe o embeds
        if (lower.contains("/cdn-cgi/") || lower.contains("/e/") || lower.contains("/embed")) return false

        val isDomain = lower.contains("fastream.to") || lower.contains("dropcdn.io") || lower.contains("dr0pstream.com") || lower.contains("/hls2/")
        val isMedia = lower.contains(".m3u8") || lower.contains("/hls") || lower.contains(".mp4")
        return isDomain && isMedia
    }

    override fun extract(candidate: StreamCandidate): MediaStream? {
        val url = candidate.url
        val lower = url.lowercase()
        val fastreamId = fastreamIdRegex.find(url)?.groupValues?.get(1)
        val isMaster = lower.contains("master.m3u8") || lower.contains("urlset") || lower.contains(".m3u8")
        val title = candidate.pageTitle ?: "Fastream Video"
        val isDrop = lower.contains("dropcdn.io") || lower.contains("dr0pstream.com")
        val baseHost = if (isDrop) "https://dr0pstream.com" else "https://fastream.to"
        val embedUrl = if (!fastreamId.isNullOrBlank()) "$baseHost/embed-$fastreamId.html" else "$baseHost/embed-video.html"

        val headers = candidate.headers.toMutableMap()
        if (!headers.containsKey("User-Agent")) headers["User-Agent"] = UA_MOBILE
        if (!headers.containsKey("Referer")) headers["Referer"] = embedUrl
        val ref = headers["Referer"]
        if (!headers.containsKey("Origin")) {
            val origin = if (!ref.isNullOrBlank()) {
                try {
                    val u = java.net.URI(ref)
                    if (!u.scheme.isNullOrBlank() && !u.host.isNullOrBlank()) "${u.scheme}://${u.host}" else baseHost
                } catch (_: Throwable) { baseHost }
            } else baseHost
            headers["Origin"] = origin
        }
        if (!headers.containsKey("Sec-Fetch-Mode")) headers["Sec-Fetch-Mode"] = "cors"
        if (!headers.containsKey("Sec-Fetch-Dest")) headers["Sec-Fetch-Dest"] = "empty"
        if (!headers.containsKey("Sec-Fetch-Site")) headers["Sec-Fetch-Site"] = "cross-site"

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
