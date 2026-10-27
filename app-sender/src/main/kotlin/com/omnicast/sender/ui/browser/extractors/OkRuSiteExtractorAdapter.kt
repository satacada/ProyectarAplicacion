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

        // Rechazar explícitamente estadísticas, beacons, trackers y páginas web de catálogo
        if (lower.contains("videostat") || lower.contains("statnew") || lower.contains("tracking") ||
            lower.contains("tracker") || lower.contains("top-fwz") || lower.contains("anonymvideo") ||
            lower.contains("showcase") || lower.contains("st.cmd") || lower.contains("/dk?") ||
            lower.contains("videopreview") || lower.contains("preview") || lower.contains("bytes=") ||
            lower.contains("ct=11") || lower.contains("ct=12") || lower.contains("/cdn-cgi/")) {
            return false
        }

        // El video real de OK.ru y VK se sirve a través de su CDN dedicada mycdn.me / okcdn.ru / vk
        val isMyCdn = (lower.contains("mycdn.me") || lower.contains("okcdn.ru") || lower.contains("vkuser.net") || lower.contains("vk-cdn")) &&
                      (lower.contains("vd") || lower.contains("video") || lower.contains("cmd=video") ||
                       lower.contains("/cmd/") || lower.contains(".m3u8") || lower.contains(".mp4") || lower.contains(".mpd") ||
                       lower.contains("sig") || lower.contains("expires") || lower.contains("mid=")) &&
                      !lower.contains("i.mycdn.me") && !lower.contains("st.mycdn.me") && !lower.contains("static.mycdn.me") &&
                      !lower.contains("i.okcdn.ru") && !lower.contains("st.okcdn.ru")

        // Manifiesto directo HLS servido por la propia plataforma (sin páginas HTML)
        val isPlatformMedia = (lower.contains("ok.ru/") || lower.contains("vk.com/")) &&
                              (lower.contains("hlsmanifest") || lower.contains("cmd=video")) &&
                              !lower.contains("showcase") && !lower.contains("anonym")

        return isMyCdn || isPlatformMedia
    }

    override fun extract(candidate: StreamCandidate): MediaStream {
        val lower = candidate.url.lowercase()
        val isOkHls = (lower.contains("mycdn.me") || lower.contains("okcdn.ru") || lower.contains("vk")) &&
                      (lower.contains("/type/") || lower.contains("type=") || lower.contains("/video/") || lower.contains(".m3u8"))
        val isHls = lower.contains(".m3u8") || lower.contains("/hls") || isOkHls
        val isMaster = isHls && (lower.contains("master") || lower.contains("manifest") || (lower.contains("video.m3u8") && !lower.contains("type=")))
        val format = if (isHls) "HLS" else "MP4"
        val title = candidate.pageTitle ?: when {
            lower.contains("vk.com") || lower.contains("vkvideo") -> "Video VK"
            lower.contains("mail.ru") -> "Video Mail.ru"
            else -> "Video OK.ru"
        }

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

        // Requiere Referer a su dominio para autorizar los bloques CDN
        val safeHeaders = candidate.headers.toMutableMap().apply {
            if (!containsKey("Referer")) {
                val ref = when {
                    lower.contains("vk.com") || lower.contains("vkvideo") -> "https://vk.com/"
                    lower.contains("mail.ru") -> "https://mail.ru/"
                    else -> "https://ok.ru/"
                }
                put("Referer", ref)
            }
            if (!containsKey("User-Agent")) {
                put("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
            }
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
