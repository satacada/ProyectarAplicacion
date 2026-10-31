package com.omnicast.sender.ui.browser

import android.webkit.CookieManager

/**
 * Utilidades para detección y normalización de URLs multimedia y cabeceras estilo Web Video Caster.
 */
object BrowserUrlHelper {

    private val YOUTUBE_REGEX = Regex("(?:watch\\?v=|shorts/|youtu\\.be/|embed/|live/)([a-zA-Z0-9_-]{8,32})")
    private val YOUTUBE_CHANNEL_REGEX = Regex("(?:channel=|channel/)([a-zA-Z0-9_-]+)")
    private val DAILYMOTION_REGEX = Regex("(?:video/|dai\\.ly/)([a-zA-Z0-9]+)")

    fun extractYouTubeId(url: String): String? {
        if (url.startsWith("live_stream?channel=")) return url
        if (!url.contains("youtube.com") && !url.contains("youtu.be")) return null
        if (url.contains("channel=") || url.contains("channel/")) {
            val chId = YOUTUBE_CHANNEL_REGEX.find(url)?.groupValues?.get(1)
            if (!chId.isNullOrBlank()) return "live_stream?channel=$chId"
        }
        return YOUTUBE_REGEX.find(url)?.groupValues?.get(1)
    }

    fun extractDailymotionId(url: String): String? {
        if (!url.contains("dailymotion.com") && !url.contains("dai.ly")) return null
        return DAILYMOTION_REGEX.find(url)?.groupValues?.get(1)
    }

    fun createYouTubeQualityOptions(videoId: String, title: String? = null): List<StreamQualityOption> {
        val isLiveCh = videoId.startsWith("live_stream?channel=")
        val ytUrl = if (isLiveCh) "https://www.youtube.com/embed/$videoId" else "https://www.youtube.com/watch?v=$videoId"
        val prefix = if (!title.isNullOrBlank() && title != "Transmisión") "[${title.take(20)}] " else ""
        val ytHeaders = mapOf("X-YouTube-Id" to videoId, "User-Agent" to UA_MOBILE)
        if (isLiveCh) {
            return listOf(
                StreamQualityOption("${prefix}▶ Stream en Vivo (YouTube)", "LIVE", "En Directo", ytUrl, isMaster = true, isMovie = true, isSuggested = true, isLive = true, healedHeaders = ytHeaders, groupKey = ytUrl)
            )
        }
        return listOf(
            StreamQualityOption("${prefix}1080p Full HD", "1080p", "Original", ytUrl, isMaster = true, isMovie = true, isSuggested = true, healedHeaders = ytHeaders, groupKey = ytUrl),
            StreamQualityOption("${prefix}720p HD", "720p", "Estándar HD", ytUrl, isMovie = true, healedHeaders = ytHeaders, groupKey = ytUrl),
            StreamQualityOption("${prefix}480p SD", "480p", "SD", ytUrl, isMovie = true, healedHeaders = ytHeaders, groupKey = ytUrl),
            StreamQualityOption("${prefix}360p", "360p", "Bajo Consumo", ytUrl, isMovie = true, healedHeaders = ytHeaders, groupKey = ytUrl)
        )
    }

    fun isPlayableStream(url: String): Boolean {
        if (url.isBlank() || com.omnicast.core.domain.usecases.HlsMasterResolver.isAudioOrRawSegment(url)) return false
        val lower = url.lowercase().substringBefore('?')
        if (lower.endsWith("/iframe.m3u8") || lower.contains("iframes-") || lower.contains("/default_audio") || lower.contains("/default_video") ||
            lower.contains("/lang/") || lower.contains("/subs/") || lower.contains("_spa/") || lower.contains("mastersubs") || lower.contains("masteraudio")) return false
        if (lower.endsWith(".vtt") || lower.endsWith(".srt") || lower.endsWith(".key") ||
            lower.endsWith(".ts") || lower.endsWith(".m4s") || lower.endsWith(".html") ||
            lower.endsWith(".htm") || lower.endsWith(".jsp") || lower.endsWith(".js") ||
            lower.endsWith(".css") || lower.endsWith(".json") || lower.endsWith(".webmanifest")) return false
        val isPhpStream = lower.endsWith(".php") && (url.contains(".m3u8", true) || url.contains(".mp4", true) || url.contains("p.php?", true) || url.contains("url=", true) || url.contains("stream", true))
        if (lower.endsWith(".php") && !isPhpStream) return false
        val fullLower = url.lowercase()
        if (fullLower.contains("dmxleo.dailymotion.com") || fullLower.contains("geo.dailymotion.com")) return false
        if (fullLower.contains("/embed/") || fullLower.contains("/embed69") || fullLower.contains("/widget/")) return false
        val isWebPage = (fullLower.contains("dailymotion.com/video/") || fullLower.contains("ok.ru/video/")) &&
                        !fullLower.contains(".m3u8") && !fullLower.contains(".mpd") && !fullLower.contains(".mp4") && !fullLower.contains("cmd=video")
        if (isWebPage) return false
        return com.omnicast.sender.ui.browser.extractors.StreamExtractorRegistry.useCase.isStreamCandidate(url)
    }

    fun isRealMedia(s: String, dur: Float = 0f): Boolean {
        if (s.isBlank() || s.contains("dmxleo.dailymotion.com") || AdBlocker.isAd(s) || com.omnicast.core.domain.usecases.HlsMasterResolver.isAudioOrRawSegment(s)) return false
        if (dur in 0.1f..59.9f) return false
        val clean = s.substringBefore('?').lowercase()
        if (clean.endsWith("/iframe.m3u8") || clean.contains("iframes-") || clean.contains("keyframes") ||
            clean.contains("/default_audio") || clean.contains("/default_video") ||
            clean.contains("/lang/") || clean.contains("/subs/") || clean.contains("_spa/") ||
            clean.contains("mastersubs") || clean.contains("masteraudio")) return false
        if (clean.endsWith(".vtt") || clean.endsWith(".srt") || clean.endsWith(".key") ||
            clean.endsWith(".ts") || clean.endsWith(".m4s") || clean.endsWith(".html") ||
            clean.endsWith(".htm") || clean.endsWith(".jsp") || clean.endsWith(".js") ||
            clean.endsWith(".css") || clean.endsWith(".json") || clean.endsWith(".webmanifest")) return false
        val isPhpStream = clean.endsWith(".php") && (s.contains(".m3u8", true) || s.contains(".mp4", true) || s.contains("p.php?", true) || s.contains("url=", true) || s.contains("stream", true) || s.contains("hls", true))
        if (clean.endsWith(".php") && !isPhpStream) return false
        val isWeb = (s.contains("dailymotion.com/video/") || s.contains("ok.ru/video/")) &&
                    !s.contains(".m3u8") && !s.contains(".mpd") && !s.contains(".mp4") && !s.contains("cmd=video")
        val isTxtHls = (clean.endsWith(".txt") || s.contains(".txt?", true)) && (s.contains("/hls", true) || s.contains("master", true) || s.contains("index", true))
        return !isWeb && (s.contains(".m3u8", true) || s.contains(".mp4", true) || s.contains(".mpd", true) || isTxtHls ||
                          s.contains("/hls", true) || s.contains("chunklist", true) || s.contains("p.php?", true) ||
                          com.omnicast.sender.ui.browser.extractors.StreamExtractorRegistry.useCase.isStreamCandidate(s) ||
                          extractYouTubeId(s) != null)
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
            val mergedCookies = harvestUniversalCookies(streamUrl, refererUrl, currentUrl, rawHeaders)
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
            if (!headers.containsKey("X-Target-Player")) {
                headers["X-Target-Player"] = com.omnicast.sender.stream.StreamProfileDatabase.resolveTargetPlayer(streamUrl, headers)
            }
            if (!headers.containsKey("X-Single-Use-Token")) {
                headers["X-Single-Use-Token"] = com.omnicast.sender.stream.StreamProfileDatabase.isSingleUseTokenHost(streamUrl, headers).toString()
            }
            if (!headers.containsKey("X-With-Credentials")) {
                headers["X-With-Credentials"] = com.omnicast.sender.stream.StreamProfileDatabase.requiresCredentials(streamUrl, headers).toString()
            }
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

    fun extractMediaIdentityKey(url: String): String {
        if (url.isBlank()) return ""
        val lower = url.lowercase()
        extractYouTubeId(url)?.let { return "yt_$it" }
        if (lower.contains("okcdn.ru") || lower.contains("mycdn.me") || lower.contains("ok.ru")) {
            val idMatch = Regex("[?&]id=([0-9]+)").find(url)
            if (idMatch != null) return "okru_${idMatch.groupValues[1]}"
        }
        MediaPlatformResolver.extractFastreamId(url)?.let { return "fastream_$it" }
        val frMatch = Regex("[?&]fr=([a-zA-Z0-9_-]+)").find(url)
        if (frMatch != null) return "media_${frMatch.groupValues[1]}"
        val urlsetMatch = Regex(",([a-zA-Z0-9]{8,16})_").find(url) ?: Regex("/([a-zA-Z0-9]{8,16})_").find(url)
        if (urlsetMatch != null && url.contains(".urlset", true)) return "media_${urlsetMatch.groupValues[1]}"
        val clean = url.substringBefore('?')
        val parent = clean.substringBeforeLast('/')
        val parentFolder = if (parent.contains('_')) parent.substringBefore('_') else parent
        val lastSegment = clean.substringAfterLast('/')
        val filePrefix = lastSegment.substringBefore('_').substringBefore('.').take(12)
        return if (filePrefix.contains("master", true) || filePrefix.contains("index", true)) parentFolder else "$parentFolder/$filePrefix"
    }

    fun deriveMasterPlaylistUrl(url: String): String? =
        com.omnicast.core.domain.usecases.HlsMasterResolver.deriveMasterPlaylistUrl(url)

    fun harvestUniversalCookies(
        streamUrl: String,
        refererUrl: String,
        currentUrl: String,
        rawHeaders: Map<String, String>? = null
    ): String {
        val cm = CookieManager.getInstance() ?: return ""
        val targetUrls = mutableSetOf<String>()
        listOf(streamUrl, refererUrl, currentUrl).filter { it.isNotBlank() }.forEach { u ->
            targetUrls.add(u)
            try {
                val uri = android.net.Uri.parse(u)
                val host = uri.host?.lowercase()
                val scheme = uri.scheme ?: "https"
                if (host != null) {
                    targetUrls.add("$scheme://$host")
                    val parts = host.split(".")
                    if (parts.size >= 2) {
                        val rootDomain = parts.takeLast(2).joinToString(".")
                        targetUrls.add("$scheme://$rootDomain")
                        targetUrls.add("$scheme://.$rootDomain")
                    }
                }
            } catch (_: Throwable) {}
        }
        val collectedCookies = mutableListOf<String>()
        targetUrls.forEach { u ->
            cm.getCookie(u)?.takeIf { it.isNotBlank() }?.let { collectedCookies.add(it) }
        }
        rawHeaders?.entries?.firstOrNull { it.key.equals("Cookie", ignoreCase = true) }?.value?.let {
            if (it.isNotBlank()) collectedCookies.add(it)
        }
        return collectedCookies
            .flatMap { it.split(";") }
            .map { it.trim() }
            .filter { it.isNotBlank() && it.contains("=") }
            .distinctBy { it.substringBefore("=").trim() }
            .joinToString("; ")
    }
}

