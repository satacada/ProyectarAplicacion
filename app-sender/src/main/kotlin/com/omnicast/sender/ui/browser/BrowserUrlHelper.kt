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
            val cookie = CookieManager.getInstance().getCookie(streamUrl)
                ?: CookieManager.getInstance().getCookie(currentUrl)
                ?: (if (streamUrl.contains("dailymotion.com") || streamUrl.contains("dmcdn.net")) CookieManager.getInstance().getCookie("https://www.dailymotion.com") else null) ?: ""
            if (cookie.isNotBlank() && !headers.containsKey("Cookie")) headers["Cookie"] = cookie

            if (streamUrl.contains("mycdn.me", ignoreCase = true) || streamUrl.contains("okcdn.ru", ignoreCase = true) || streamUrl.contains("ok.ru", ignoreCase = true)) {
                headers["Referer"] = "https://ok.ru/"
            } else if (streamUrl.contains("dailymotion.com", ignoreCase = true) || streamUrl.contains("dmcdn.net", ignoreCase = true)) {
                headers["Referer"] = "https://www.dailymotion.com/"
                headers["Origin"] = "https://www.dailymotion.com"
            } else if (streamUrl.contains("tnmr.org", ignoreCase = true) || streamUrl.contains("luluvdo.com", ignoreCase = true) || streamUrl.contains("lulustream.com", ignoreCase = true)) {
                headers["Origin"] = "https://luluvdo.com"
                headers["Referer"] = "https://luluvdo.com/"
            } else if (streamUrl.contains("mail.ru", ignoreCase = true)) {
                headers.remove("Referer")
            } else if (streamUrl.contains("fastream.to", ignoreCase = true)) {
                val fastreamId = Regex("(?:/hls\\d*/\\d+/\\d+/|/embed-|/)([a-zA-Z0-9]{10,12})(?:[_,.]|$)").find(streamUrl)?.groupValues?.get(1)
                headers["Referer"] = if (!fastreamId.isNullOrBlank()) "https://fastream.to/embed-$fastreamId.html" else "https://fastream.to/embed-video.html"
                headers["Origin"] = "https://fastream.to"
            } else if (streamUrl.contains("pelisserieshoy.com", ignoreCase = true) || streamUrl.contains("p.php?url=", ignoreCase = true)) {
                headers["Referer"] = "https://player.pelisserieshoy.com/"
                headers["Origin"] = "https://player.pelisserieshoy.com"
            } else if (streamUrl.contains("morencius.com", ignoreCase = true) && !headers.containsKey("Referer")) {
                headers["Referer"] = "https://embed69.org/"
            } else if (!headers.containsKey("Referer") && currentUrl.isNotBlank()) {
                headers["Referer"] = currentUrl
            }
            if (!headers.containsKey("User-Agent")) headers["User-Agent"] = userAgent
        } catch (_: Throwable) {}
        return headers
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
