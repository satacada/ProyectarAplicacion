package com.omnicast.sender.ui.browser

import android.webkit.CookieManager

/**
 * Utilidades para detección y normalización de URLs multimedia y cabeceras estilo Web Video Caster.
 */
object BrowserUrlHelper {

    private val YOUTUBE_REGEX = Regex("(?:watch\\?v=|shorts/|youtu\\.be/|embed/)([a-zA-Z0-9_-]{11})")
    private val DAILYMOTION_REGEX = Regex("(?:video/|dai\\.ly/)([a-zA-Z0-9]+)")

    fun extractYouTubeId(url: String): String? {
        if (!url.contains("youtube.com") && !url.contains("youtu.be")) return null
        return YOUTUBE_REGEX.find(url)?.groupValues?.get(1)
    }

    fun extractDailymotionId(url: String): String? {
        if (!url.contains("dailymotion.com") && !url.contains("dai.ly")) return null
        return DAILYMOTION_REGEX.find(url)?.groupValues?.get(1)
    }

    fun createYouTubeQualityOptions(videoId: String, title: String? = null): List<StreamQualityOption> {
        val ytUrl = "https://www.youtube.com/watch?v=$videoId"
        val prefix = if (!title.isNullOrBlank() && title != "Transmisión") "[${title.take(20)}] " else ""
        return listOf(
            StreamQualityOption("🎬 ${prefix}1080p Full HD", "1080p", "Original", ytUrl, isMaster = true),
            StreamQualityOption("📺 ${prefix}720p HD", "720p", "Estándar HD", ytUrl),
            StreamQualityOption("📱 ${prefix}480p SD", "480p", "SD", ytUrl),
            StreamQualityOption("📱 ${prefix}360p", "360p", "Bajo Consumo", ytUrl)
        )
    }

    fun isPlayableStream(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase().substringBefore('?')
        val isPhpStream = lower.endsWith(".php") && (url.contains(".m3u8", true) || url.contains(".mp4", true) || url.contains("p.php?", true) || url.contains("url=", true) || url.contains("stream", true))
        if (lower.endsWith(".html") || lower.endsWith(".htm") || (lower.endsWith(".php") && !isPhpStream) || lower.endsWith(".jsp")) return false
        val fullLower = url.lowercase()
        if (fullLower.contains("dmxleo.dailymotion.com") || fullLower.contains("geo.dailymotion.com")) return false
        if (fullLower.contains("/embed/") || fullLower.contains("/embed69") || fullLower.contains("/widget/")) return false
        val isWebPage = (fullLower.contains("dailymotion.com/video/") || fullLower.contains("ok.ru/video/")) &&
                        !fullLower.contains(".m3u8") && !fullLower.contains(".mpd") && !fullLower.contains(".mp4") && !fullLower.contains("cmd=video")
        if (isWebPage) return false
        return com.omnicast.sender.ui.browser.extractors.StreamExtractorRegistry.useCase.isStreamCandidate(url)
    }

    fun isRealMedia(s: String, dur: Float = 0f): Boolean {
        if (s.isBlank() || s.contains("dmxleo.dailymotion.com") || AdBlocker.isAd(s)) return false
        if (dur in 0.1f..59.9f) return false
        val clean = s.substringBefore('?').lowercase()
        val isPhpStream = clean.endsWith(".php") && (s.contains(".m3u8", true) || s.contains(".mp4", true) || s.contains("p.php?", true) || s.contains("url=", true) || s.contains("stream", true) || s.contains("hls", true))
        if (clean.endsWith(".html") || clean.endsWith(".htm") || (clean.endsWith(".php") && !isPhpStream) || clean.endsWith(".jsp") || clean.endsWith(".js") || clean.endsWith(".css")) return false
        val isWeb = (s.contains("dailymotion.com/video/") || s.contains("ok.ru/video/")) &&
                    !s.contains(".m3u8") && !s.contains(".mpd") && !s.contains(".mp4") && !s.contains("cmd=video")
        return !isWeb && (s.contains(".m3u8", true) || s.contains(".mp4", true) || s.contains(".mpd", true) ||
                          s.contains("/hls", true) || s.contains("chunklist", true) || s.contains("p.php?", true) || extractYouTubeId(s) != null)
    }

    fun isMovieOrVideoPage(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val l = url.lowercase()
        if (l.startsWith("about:") || l.contains("google.") || l.contains("yandex.") || l.contains("bing.")) return false
        return l.contains("/pelicula/") || l.contains("/pelis/") || l.contains("/movie/") ||
               l.contains("/ver-") || l.contains("/ver/") || l.contains("/watch/") ||
               l.contains("/video/") || l.contains("/serie/") || l.contains("/episode/") ||
               l.contains("/ep-") || l.contains("/stream/") || l.contains("/play")
    }

    fun buildSafeHeaders(
        streamUrl: String,
        currentUrl: String,
        userAgent: String,
        rawHeaders: Map<String, String>? = null
    ): Map<String, String> {
        val headers = StreamIntelligenceEngine.resolveDynamicHeaders(streamUrl, currentUrl, rawHeaders).toMutableMap()
        try {
            val refererUrl = headers["Referer"] ?: currentUrl
            val cookie = CookieManager.getInstance().getCookie(streamUrl)
                ?: CookieManager.getInstance().getCookie(refererUrl)
                ?: CookieManager.getInstance().getCookie(currentUrl)
                ?: ""
            if (cookie.isNotBlank() && !headers.containsKey("Cookie")) headers["Cookie"] = cookie
            if (!headers.containsKey("Referer") && currentUrl.isNotBlank()) {
                headers["Referer"] = currentUrl
            }
            if (!headers.containsKey("User-Agent")) headers["User-Agent"] = userAgent
        } catch (_: Throwable) {}
        return headers
    }

    fun sanitizeMediaUrl(url: String): String {
        var u = url
        if (u.contains(".dropcdn.io", true) && u.contains("srv=")) {
            val srvMatch = Regex("[?&]srv=([a-zA-Z0-9]+)").find(u)
            if (srvMatch != null) {
                val srv = srvMatch.groupValues[1]
                u = u.replace(Regex("https?://r\\d*\\.dropcdn\\.io"), "https://$srv.dropcdn.io")
            }
        }
        return u
    }

    fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "https://www.google.com"
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        val isDomainLike = !trimmed.contains(" ") && (
            trimmed.contains(".") || trimmed.startsWith("localhost") || trimmed.matches(Regex("^[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}(/.*)?$"))
        )
        return if (isDomainLike) {
            "https://$trimmed"
        } else {
            "https://www.google.com/search?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
        }
    }
}
