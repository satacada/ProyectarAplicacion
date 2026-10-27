package com.omnicast.sender.ui.browser.extractors

import com.omnicast.core.domain.models.MediaStream
import com.omnicast.core.domain.models.StreamCandidate
import com.omnicast.core.domain.ports.IMediaStreamExtractorPort

/**
 * Adaptador específico e independiente ("poner y sacar") para videos de OK.ru y su red CDN (mycdn.me).
 * Si este sitio cambia su estructura o se deshabilita, ningún otro extractor se ve afectado.
 */
class OkRuSiteExtractorAdapter : IMediaStreamExtractorPort {

    override val priority: Int = 20

    override fun canHandle(candidate: StreamCandidate): Boolean {
        val lower = candidate.url.lowercase()

        // Rechazar explícitamente estadísticas y beacons
        if (lower.contains("videostat") || lower.contains("statnew") || lower.contains("tracking")) {
            return false
        }

        // El video real de OK.ru se sirve a través de su CDN dedicada mycdn.me / okcdn.ru
        val isMyCdn = (lower.contains("mycdn.me") || lower.contains("okcdn.ru")) &&
                      (lower.contains("vd") || lower.contains("video") || lower.contains("cmd=video") ||
                       lower.contains("/cmd/") || lower.contains(".m3u8") || lower.contains(".mp4") || lower.contains(".mpd") ||
                       lower.contains("sig") || lower.contains("expires") || lower.contains("mid=")) &&
                      !lower.contains("i.mycdn.me") && !lower.contains("st.mycdn.me") && !lower.contains("static.mycdn.me")

        val isOkRuVideo = (lower.contains("ok.ru/") || lower.contains("odnoklassniki.ru/")) &&
                          (lower.contains("cmd=video") || lower.contains("hlsmanifest") || lower.contains(".m3u8") || lower.contains(".mpd") || lower.contains(".mp4"))

        return isMyCdn || isOkRuVideo
    }

    override fun extract(candidate: StreamCandidate): MediaStream {
        val lower = candidate.url.lowercase()
        val isOkHls = (lower.contains("mycdn.me") || lower.contains("okcdn.ru")) &&
                      (lower.contains("/type/") || lower.contains("type=") || lower.contains("/video/") || lower.contains(".m3u8"))
        val isHls = lower.contains(".m3u8") || lower.contains("/hls") || isOkHls
        val isMaster = isHls && (lower.contains("master") || lower.contains("manifest") || (lower.contains("video.m3u8") && !lower.contains("type=")))
        val format = if (isHls) "HLS" else "MP4"
        val title = candidate.pageTitle ?: "Video OK.ru"

        val quality = when {
            lower.contains("/type/5") || lower.contains("type=5") -> "1080p FHD"
            lower.contains("/type/4") || lower.contains("type=4") -> "720p HD"
            lower.contains("/type/3") || lower.contains("type=3") -> "480p"
            lower.contains("/type/2") || lower.contains("type=2") -> "360p"
            lower.contains("/type/1") || lower.contains("type=1") -> "240p"
            lower.contains("/type/0") || lower.contains("type=0") -> "144p Móvil"
            isMaster -> "Adaptativo HLS"
            isHls -> "HLS Stream"
            else -> "Calidad Original"
        }

        // OK.ru requiere Referer a su dominio para autorizar los bloques CDN
        val safeHeaders = candidate.headers.toMutableMap().apply {
            if (!containsKey("Referer")) put("Referer", "https://ok.ru/")
        }

        return MediaStream(
            url = candidate.url,
            format = format,
            title = title,
            isMaster = isMaster,
            qualityLabel = quality,
            headers = safeHeaders
        )
    }
}
