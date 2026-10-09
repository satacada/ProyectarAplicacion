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

    fun isPlayableStream(url: String): Boolean =
        com.omnicast.sender.ui.browser.extractors.StreamExtractorRegistry.useCase.isStreamCandidate(url)

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

            if (streamUrl.contains("mycdn.me", ignoreCase = true) || streamUrl.contains("okcdn.ru", ignoreCase = true) || streamUrl.contains("ok.ru", ignoreCase = true)) {
                headers["Referer"] = "https://ok.ru/"
            } else if (streamUrl.contains("dailymotion.com", ignoreCase = true) || streamUrl.contains("dmcdn.net", ignoreCase = true)) {
                headers["Referer"] = "https://www.dailymotion.com/"
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
