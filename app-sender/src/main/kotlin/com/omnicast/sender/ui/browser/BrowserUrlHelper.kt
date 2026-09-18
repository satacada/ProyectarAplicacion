package com.omnicast.sender.ui.browser

import android.webkit.CookieManager

/**
 * Utilidades para detección y normalización de URLs multimedia y cabeceras estilo Web Video Caster.
 */
object BrowserUrlHelper {

    private val YOUTUBE_REGEX = Regex("(?:watch\\?v=|shorts/|youtu\\.be/|embed/)([a-zA-Z0-9_-]{11})")

    fun extractYouTubeId(url: String): String? {
        if (!url.contains("youtube.com") && !url.contains("youtu.be")) return null
        return YOUTUBE_REGEX.find(url)?.groupValues?.get(1)
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
        val lower = url.lowercase()
        if (AdBlocker.isAd(url)) return false
        if (lower.contains("googlevideo.com") || lower.contains("youtube.com/videoplayback")) return false
        if (lower.contains("preroll") || lower.contains("midroll") || lower.contains("commercial") || lower.contains("/vast") || lower.contains("vast.")) return false
        if (lower.contains("stub.mp4") || lower.contains("dummy.mp4") || lower.contains("blank.mp4") || lower.contains("ad.mp4")) return false
        if (lower.contains(".ts?") || lower.endsWith(".ts") || lower.contains(".m4s")) return false
        if (lower.contains("/audio") || lower.contains("_audio.m3u8") || lower.contains("-audio.m3u8")) return false
        if (lower.endsWith(".html") || lower.endsWith(".htm") || lower.contains(".html?") || lower.contains(".htm?")) return false
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".svg") || lower.endsWith(".ico") || lower.contains(".jpg?") || lower.contains(".jpeg?") || lower.contains(".png?") || lower.contains(".webp?")) return false
        if (lower.endsWith(".css") || lower.endsWith(".js") || lower.endsWith(".woff") || lower.endsWith(".woff2") || lower.endsWith(".ttf") || lower.contains(".css?") || lower.contains(".js?")) return false
        if (lower.contains("i.mycdn.me") || lower.contains("st.mycdn.me") || lower.contains("static.mycdn.me")) return false
        if (lower.contains(".m3u8") || lower.contains(".mpd") || lower.contains(".mp4") || lower.contains(".webm") || lower.contains(".mkv")) return true
        if (lower.contains("/hls/") || lower.contains("/stream/") || lower.contains("/manifest") || lower.contains("chunklist")) return true
        if (lower.contains("morencius.com") || lower.contains("swdyu.com") || lower.contains("cloudemb.com")) return true
        if (lower.contains("vd.mycdn.me") || lower.contains("vd2.mycdn.me") || lower.contains("vd3.mycdn.me") || lower.contains("vd4.mycdn.me") || (lower.contains("mycdn.me") && (lower.contains("/video/") || lower.contains("cmd=video") || lower.contains("sig=")))) return true
        if (lower.contains("ok.ru") && (lower.contains("videometa") || lower.contains("api/video") || lower.contains("cmd=video"))) return true
        return false
    }

    fun buildSafeHeaders(
        streamUrl: String,
        currentUrl: String,
        userAgent: String,
        rawHeaders: Map<String, String>? = null
    ): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        try {
            rawHeaders?.forEach { (k, v) ->
                if (k.equals("referer", ignoreCase = true) && v.isNotBlank()) headers["Referer"] = v
                if (k.equals("origin", ignoreCase = true) && v.isNotBlank()) headers["Origin"] = v
                if (k.equals("user-agent", ignoreCase = true) && v.isNotBlank()) headers["User-Agent"] = v
                if (k.equals("cookie", ignoreCase = true) && v.isNotBlank()) headers["Cookie"] = v
            }
            val cookie = CookieManager.getInstance().getCookie(streamUrl)
                ?: CookieManager.getInstance().getCookie(currentUrl) ?: ""
            if (cookie.isNotBlank() && !headers.containsKey("Cookie")) headers["Cookie"] = cookie

            if (streamUrl.contains("mycdn.me", ignoreCase = true) || streamUrl.contains("ok.ru", ignoreCase = true)) {
                headers["Referer"] = "https://ok.ru/"
            } else if (streamUrl.contains("mail.ru", ignoreCase = true)) {
                headers.remove("Referer")
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
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed
        else "https://www.google.com/search?q=${trimmed.replace(" ", "+")}"
    }
}
