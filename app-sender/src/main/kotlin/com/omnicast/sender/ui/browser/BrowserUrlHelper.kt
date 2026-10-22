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
            StreamQualityOption("${prefix}1080p Full HD", "1080p", "Original", ytUrl, isMaster = true),
            StreamQualityOption("${prefix}720p HD", "720p", "Estándar HD", ytUrl),
            StreamQualityOption("${prefix}480p SD", "480p", "SD", ytUrl),
            StreamQualityOption("${prefix}360p", "360p", "Bajo Consumo", ytUrl)
        )
    }

    fun isPlayableStream(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase().substringBefore('?')
        if (lower.endsWith(".vtt") || lower.endsWith(".srt") || lower.endsWith(".key") ||
            lower.endsWith(".ts") || lower.endsWith(".m4s") || lower.endsWith(".html") ||
            lower.endsWith(".htm") || lower.endsWith(".jsp") || lower.endsWith(".js") ||
            lower.endsWith(".css") || lower.endsWith(".json")) return false
        val isPhpStream = lower.endsWith(".php") && (url.contains(".m3u8", true) || url.contains(".mp4", true) || url.contains("p.php?", true) || url.contains("url=", true) || url.contains("stream", true))
        if (lower.endsWith(".php") && !isPhpStream) return false
        val fullLower = url.lowercase()
        if (fullLower.contains("dmxleo.dailymotion.com") || fullLower.contains("geo.dailymotion.com")) return false
        if (fullLower.contains("/embed/") || fullLower.contains("/embed69") || fullLower.contains("/widget/")) return false
        if (url.contains("bytes=", true) || url.contains("&bytes", true) || url.contains("ct=11") || url.contains("ct=12")) return false
        val isWebPage = (fullLower.contains("dailymotion.com/video/") || fullLower.contains("ok.ru/video/")) &&
                        !fullLower.contains(".m3u8") && !fullLower.contains(".mpd") && !fullLower.contains(".mp4") && !fullLower.contains("cmd=video")
        if (isWebPage) return false
        return com.omnicast.sender.ui.browser.extractors.StreamExtractorRegistry.useCase.isStreamCandidate(url)
    }

    fun isRealMedia(s: String, dur: Float = 0f): Boolean {
        if (s.isBlank() || s.contains("dmxleo.dailymotion.com") || AdBlocker.isAd(s)) return false
        if (s.contains("bytes=", true) || s.contains("&bytes", true) || s.contains("ct=11") || s.contains("ct=12")) return false
        if (dur in 0.1f..59.9f) return false
        val clean = s.substringBefore('?').lowercase()
        if (clean.endsWith(".vtt") || clean.endsWith(".srt") || clean.endsWith(".key") ||
            clean.endsWith(".ts") || clean.endsWith(".m4s") || clean.endsWith(".html") ||
            clean.endsWith(".htm") || clean.endsWith(".jsp") || clean.endsWith(".js") ||
            clean.endsWith(".css") || clean.endsWith(".json")) return false
        val isPhpStream = clean.endsWith(".php") && (s.contains(".m3u8", true) || s.contains(".mp4", true) || s.contains("p.php?", true) || s.contains("url=", true) || s.contains("stream", true) || s.contains("hls", true))
        if (clean.endsWith(".php") && !isPhpStream) return false
        val isWeb = (s.contains("dailymotion.com/video/") || s.contains("ok.ru/video/")) &&
                    !s.contains(".m3u8") && !s.contains(".mpd") && !s.contains(".mp4") && !s.contains("cmd=video")
        val isTxtHls = (clean.endsWith(".txt") || s.contains(".txt?", true)) && (s.contains("/hls", true) || s.contains("master", true) || s.contains("index", true))
        val isOkRuCdn = (s.contains("mycdn.me", true) || s.contains("okcdn.ru", true)) &&
                        (s.contains("vd", true) || s.contains("video", true) || s.contains("cmd=video", true) ||
                         s.contains("/cmd/", true) || s.contains("sig", true) || s.contains("type=", true) || s.contains("/type/", true)) &&
                        !s.contains("i.mycdn.me", true) && !s.contains("st.mycdn.me", true) && !s.contains("static.mycdn.me", true)
        return !isWeb && (s.contains(".m3u8", true) || s.contains(".mp4", true) || s.contains(".mpd", true) || isTxtHls ||
                          s.contains("/hls", true) || s.contains("chunklist", true) || s.contains("p.php?", true) || isOkRuCdn || extractYouTubeId(s) != null)
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
            val cm = CookieManager.getInstance()
            val c1 = cm?.getCookie(streamUrl)
            val c2 = if (refererUrl.isNotBlank()) cm?.getCookie(refererUrl) else null
            val c3 = if (currentUrl.isNotBlank()) cm?.getCookie(currentUrl) else null
            val c4 = cm?.getCookie("https://dr0pstream.com")
            val c5 = cm?.getCookie("https://dropcdn.io")
            val c6 = cm?.getCookie("https://fastream.to")
            val mergedCookies = listOfNotNull(c1, c2, c3, c4, c5, c6, rawHeaders?.get("Cookie"), rawHeaders?.get("cookie"))
                .flatMap { it.split("; ") }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString("; ")
            if (mergedCookies.isNotBlank()) headers["Cookie"] = mergedCookies

            val id = MediaPlatformResolver.extractFastreamId(streamUrl)
            val isDrop = streamUrl.contains("dropcdn.io", true) || streamUrl.contains("dr0pstream.com", true)
            val isFast = streamUrl.contains("fastream.to", true)
            if (isDrop && id != null) {
                headers["Referer"] = "https://dr0pstream.com/e/$id"
                headers["Origin"] = "https://dr0pstream.com"
                headers["Sec-Fetch-Mode"] = "cors"; headers["Sec-Fetch-Site"] = "cross-site"; headers["Sec-Fetch-Dest"] = "empty"
            } else if (isFast && id != null) {
                headers["Referer"] = "https://fastream.to/embed-$id.html"
                headers["Origin"] = "https://fastream.to"
                headers["Sec-Fetch-Mode"] = "cors"; headers["Sec-Fetch-Site"] = "cross-site"; headers["Sec-Fetch-Dest"] = "empty"
            } else if (!headers.containsKey("Referer") && currentUrl.isNotBlank()) {
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

    fun hasSignedSecurityToken(url: String): Boolean = com.omnicast.core.domain.usecases.StreamManifestProbe.hasSignedSecurityToken(url)
}
