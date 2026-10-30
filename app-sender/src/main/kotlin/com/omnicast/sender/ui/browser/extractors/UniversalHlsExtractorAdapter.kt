package com.omnicast.sender.ui.browser.extractors

import com.omnicast.core.domain.models.MediaStream
import com.omnicast.core.domain.models.StreamCandidate
import com.omnicast.core.domain.ports.IMediaStreamExtractorPort

/**
 * Adaptador universal intocable para transmisiones HLS (HTTP Live Streaming).
 * Soporta listas maestras (.m3u8), chunklists y fragmentos HLS estándar de la web abierta.
 */
class UniversalHlsExtractorAdapter : IMediaStreamExtractorPort {

    override val priority: Int = 10

    override fun canHandle(candidate: StreamCandidate): Boolean {
        if (com.omnicast.core.domain.usecases.HlsMasterResolver.isAudioOrRawSegment(candidate.url)) return false
        val cleanPath = candidate.url.substringBefore('?').lowercase()
        if (cleanPath.endsWith(".vtt") || cleanPath.endsWith(".srt") || cleanPath.endsWith(".key") ||
            cleanPath.endsWith(".ts") || cleanPath.endsWith(".m4s") || cleanPath.endsWith(".css") ||
            cleanPath.endsWith(".js") || cleanPath.endsWith(".json")) return false

        val lower = candidate.url.lowercase()
        if (lower.contains("masteraudio") || lower.contains("mastersubs")) return false
        val isSpecialHost = (lower.contains("swdyu.com") || lower.contains("cloudemb.com") || lower.contains("morencius.com")) &&
                            (lower.contains(".m3u8") || lower.contains("/hls") || lower.contains("master") || lower.contains("chunklist") || lower.contains(".mp4")) &&
                            !lower.contains("/embed") && !lower.contains("/e/") && !lower.contains("/cdn-cgi/")

        return lower.contains(".m3u8") ||
               lower.contains("chunklist") ||
               (lower.contains("/hls") && !cleanPath.endsWith(".ts")) ||
               ((cleanPath.endsWith(".txt") || lower.contains(".txt?")) && (lower.contains("master") || lower.contains("index") || lower.contains("/hls"))) ||
               lower.contains("/stream/") || lower.contains("/manifest") ||
               lower.contains("/live") || lower.contains("live.m3u8") ||
               isSpecialHost ||
               lower.contains("p.php?url=") || (lower.contains("/p.php") && lower.contains("sig=")) ||
               lower.contains("pelisserieshoy.com/p.php") || lower.contains("embed69.org/p.php")
    }

    override fun extract(candidate: StreamCandidate): MediaStream {
        val lower = candidate.url.lowercase()
        val isSubVar = com.omnicast.core.domain.usecases.HlsMasterResolver.isLikelySubVariantUrl(candidate.url)
        val isLive = lower.contains("live")
        val isMaster = !isSubVar && com.omnicast.core.domain.usecases.HlsMasterResolver.isMasterPlaylistUrl(candidate.url)
        val title = candidate.pageTitle ?: if (isLive) "🔴 Transmisión En Vivo" else "Transmisión HLS"
        val quality = if (isLive) "En Vivo" else if (isMaster) "Auto Adaptativo" else "HLS Estándar"
        val hdrs = candidate.headers.toMutableMap()
        if (lower.contains("pelisserieshoy.com") || lower.contains("p.php")) {
            hdrs["Referer"] = "https://player.pelisserieshoy.com/"
            hdrs["Origin"] = "https://player.pelisserieshoy.com"
        }

        return MediaStream(
            url = candidate.url,
            format = "HLS",
            title = title,
            isMaster = isMaster,
            qualityLabel = quality,
            headers = hdrs
        )
    }
}
