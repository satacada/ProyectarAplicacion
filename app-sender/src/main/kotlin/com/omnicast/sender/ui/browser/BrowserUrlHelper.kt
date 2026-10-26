package com.omnicast.sender.ui.browser

import android.net.Uri
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

    fun createYouTubeQualityOptions(videoId: String, title: String? = null, isTrailer: Boolean = false): List<StreamQualityOption> {
        val ytUrl = "https://www.youtube.com/watch?v=$videoId"
        val prefix = if (!title.isNullOrBlank() && title != "Transmisión") "[${title.take(20)}] " else ""
        val tag = if (isTrailer) "Tráiler YouTube" else "Full HD"
        return listOf(
            StreamQualityOption("${prefix}1080p $tag", "1080p", if (isTrailer) "Tráiler" else "Original", ytUrl, isMaster = !isTrailer, isMovie = !isTrailer),
            StreamQualityOption("${prefix}720p HD", "720p", "Estándar HD", ytUrl, isMaster = false, isMovie = !isTrailer),
            StreamQualityOption("${prefix}480p SD", "480p", "SD", ytUrl, isMaster = false, isMovie = !isTrailer),
            StreamQualityOption("${prefix}360p", "360p", "Bajo Consumo", ytUrl, isMaster = false, isMovie = !isTrailer)
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
        if (fullLower.contains("/lang/") || fullLower.contains("/subtitles/") || fullLower.contains("/subs/") ||
            fullLower.contains("_sub.") || fullLower.contains("_spa/index") || fullLower.contains("_eng/index")) return false
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
        val lowerFull = s.lowercase()
        if (lowerFull.contains("/lang/") || lowerFull.contains("/subtitles/") || lowerFull.contains("/subs/") ||
            lowerFull.contains("_sub.") || lowerFull.contains("_spa/index") || lowerFull.contains("_eng/index") ||
            lowerFull.contains("iframes-") || lowerFull.contains("/iframe-") || lowerFull.contains("iframes.") ||
            lowerFull.contains("keyframes")) return false
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
                        (s.contains("vd", true) || s.contains("cmd=video", true) || s.contains("/cmd/", true) || s.contains(".m3u8", true)) &&
                        (s.contains("sig=", true) || s.contains("expires=", true) || s.contains(".m3u8", true)) &&
                        !s.contains("i.mycdn.me", true) && !s.contains("st.mycdn.me", true) &&
                        !s.contains("i.okcdn.ru", true) && !s.contains("st.okcdn.ru", true) &&
                        !s.contains("videopreview", true) && !s.contains("preview", true) && !s.contains("/i?", true)
        return !isWeb && (s.contains(".m3u8", true) || s.contains(".mp4", true) || s.contains(".mpd", true) || isTxtHls ||
                          s.contains("/hls", true) || s.contains("chunklist", true) || s.contains("p.php?", true) || isOkRuCdn || extractYouTubeId(s) != null)
    }

    fun isMovieOrVideoPage(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val l = url.lowercase()
        if (l.startsWith("about:") || l.contains("google.") || l.contains("yandex.") || l.contains("bing.")) return false
        return l.contains("pelicula") || l.contains("pelis") || l.contains("movie") ||
               l.contains("/ver-") || l.contains("/ver/") || l.contains("/watch/") ||
               l.contains("/video/") || l.contains("serie") || l.contains("episode") ||
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
            // Por qué: Servidores CDN (Nginx en DropCDN/Fastream) rechazan con 403 solicitudes con
            // cabeceras 'Cookie' sobredimensionadas o con tokens/Cloudflare de dominios ajenos.
            val streamHost = runCatching { Uri.parse(streamUrl).host?.lowercase() }.getOrNull()
            val refererHost = runCatching { Uri.parse(refererUrl).host?.lowercase() }.getOrNull()
            val isSameSite = streamHost != null && refererHost != null &&
                (streamHost == refererHost || streamHost.endsWith(".$refererHost") || refererHost.endsWith(".$streamHost"))

            val cm = CookieManager.getInstance()
            val streamCookie = cm?.getCookie(streamUrl)
            val refererCookie = if (isSameSite && refererUrl.isNotBlank()) cm?.getCookie(refererUrl) else null
            val rawCookie = rawHeaders?.get("Cookie") ?: rawHeaders?.get("cookie")

            val cleanCookie = when {
                !streamCookie.isNullOrBlank() -> streamCookie
                !refererCookie.isNullOrBlank() -> refererCookie
                !rawCookie.isNullOrBlank() -> rawCookie
                else -> null
            }
            if (!cleanCookie.isNullOrBlank()) {
                headers["Cookie"] = cleanCookie
            } else {
                headers.remove("Cookie")
            }

            val rawRef = rawHeaders?.get("Referer") ?: rawHeaders?.get("referer")
            val id = MediaPlatformResolver.extractFastreamId(streamUrl)
            val luluId = MediaPlatformResolver.extractLuluStreamId(streamUrl)
            val isDrop = streamUrl.contains("dropcdn.io", true) || streamUrl.contains("dr0pstream.com", true)
            val isFast = streamUrl.contains("fastream.to", true)
            val isLulu = streamUrl.contains("tnmr.org", true) || streamUrl.contains("luluvdo.com", true) || streamUrl.contains("lulustream.com", true)
            if (isDrop && id != null) {
                headers["Referer"] = if (!rawRef.isNullOrBlank()) rawRef else "https://dr0pstream.com/e/$id"
                headers.remove("Origin")
                headers["User-Agent"] = UA_MOBILE
                headers["Sec-Fetch-Mode"] = "cors"; headers["Sec-Fetch-Site"] = "cross-site"; headers["Sec-Fetch-Dest"] = "empty"
            } else if (isFast && id != null) {
                headers["Referer"] = "https://fastream.to/embed-$id.html"
                headers.remove("Origin")
                headers["User-Agent"] = MediaPlatformResolver.UA_FASTREAM
                headers["Sec-Fetch-Mode"] = "cors"; headers["Sec-Fetch-Site"] = "cross-site"; headers["Sec-Fetch-Dest"] = "empty"
            } else if (isLulu && luluId != null) {
                headers["Referer"] = "https://luluvdo.com/e/$luluId"
                headers["Origin"] = "https://luluvdo.com"
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

    fun isSameLogicalStream(url1: String, url2: String): Boolean {
        if (url1 == url2) return true
        val y1 = extractYouTubeId(url1); val y2 = extractYouTubeId(url2)
        if (y1 != null && y2 != null) return y1 == y2
        val isOk1 = url1.contains("mycdn.me", true) || url1.contains("okcdn.ru", true)
        val isOk2 = url2.contains("mycdn.me", true) || url2.contains("okcdn.ru", true)
        if (isOk1 && isOk2) {
            val isM1 = url1.contains("video.m3u8", true) || url1.contains(".m3u8", true)
            val isM2 = url2.contains("video.m3u8", true) || url2.contains(".m3u8", true)
            if (isM1 && isM2) return true
            val t1 = Regex("[?&]type=([0-9]+)").find(url1)?.groupValues?.get(1)
            val t2 = Regex("[?&]type=([0-9]+)").find(url2)?.groupValues?.get(1)
            val id1 = Regex("[?&]id=([0-9]+)").find(url1)?.groupValues?.get(1)
            val id2 = Regex("[?&]id=([0-9]+)").find(url2)?.groupValues?.get(1)
            if (t1 != null && t2 != null && t1 == t2) {
                return id1 == null || id2 == null || id1 == id2
            }
        }
        val c1 = url1.substringBefore('?').lowercase()
        val c2 = url2.substringBefore('?').lowercase()
        if (c1 == c2 && c1.isNotBlank()) return true
        val p1 = c1.substringBeforeLast('/'); val p2 = c2.substringBeforeLast('/')
        return p1.isNotBlank() && p1 == p2 && c1.endsWith(".m3u8") && c2.endsWith(".m3u8")
    }

    fun hasSignedSecurityToken(url: String): Boolean = com.omnicast.core.domain.usecases.StreamManifestProbe.hasSignedSecurityToken(url)
}
