package com.omnicast.sender.ui.browser

import android.net.Uri
import android.util.Log

/**
 * Motor heurístico dinámico e inteligente para clasificación y auto-reparación de flujos multimedia.
 * Discrimina autónomamente entre flujos legítimos de películas/series y publicidad o trampas,
 * deduciendo dinámicamente orígenes y cabeceras de inserción (embed/referer) sin reglas rígidas.
 */
object StreamIntelligenceEngine {

    private const val TAG = "StreamIntelligence"

    // Firmas conocidas de CDNs de video y sus portales de inserción
    private val KNOWN_CDN_EMBED_MAP = mapOf(
        "tnmr.org" to "https://luluvdo.com",
        "luluvdo.com" to "https://luluvdo.com",
        "lulustream.com" to "https://luluvdo.com",
        "fastream.to" to "https://fastream.to",
        "ok.ru" to "https://ok.ru",
        "mycdn.me" to "https://ok.ru",
        "okcdn.ru" to "https://ok.ru",
        "dailymotion.com" to "https://www.dailymotion.com",
        "dmcdn.net" to "https://www.dailymotion.com",
        "pelisserieshoy.com" to "https://player.pelisserieshoy.com",
        "morencius.com" to "https://embed69.org"
    )

    fun isLikelyAdOrCommercial(url: String, duration: Float = 0f): Boolean {
        if (url.isBlank()) return true
        val recipe = StreamRuleSyncManager.findMatchingRecipe(url)
        val maxAdDuration = recipe?.skipAdsDuration ?: 60f
        if (duration in 0.1f..(maxAdDuration - 0.1f)) {
            Log.d(TAG, "🚫 Descarte heurístico: Video de duración publicitaria (${duration}s <= ${maxAdDuration}s)")
            return true
        }
        val lower = url.lowercase()
        val adKeywords = listOf(
            "preroll", "ad-delivery", "/ads/", "imasdk", "cat-bolt", "ad.mp4",
            "vast", "commercial", "spotx", "doubleclick", "popads", "adsterra",
            "syndication", "trafficjunky", "exoclick", "propellerads", "clickadu"
        )
        for (kw in adKeywords) {
            if (lower.contains(kw)) {
                Log.d(TAG, "🚫 Descarte heurístico por firma publicitaria [$kw]: $url")
                return true
            }
        }
        return false
    }

    fun scoreCandidate(url: String, duration: Float = 0f, isMaster: Boolean = false): Int {
        if (isLikelyAdOrCommercial(url, duration)) return -100
        var score = 10
        val lower = url.lowercase()

        if (isMaster || lower.contains("master.m3u8") || lower.contains("playlist.m3u8")) score += 40
        else if (lower.contains(".m3u8") || lower.contains("/hls")) score += 30
        else if (lower.contains(".mpd") || lower.contains("dash")) score += 25
        else if (lower.contains(".mp4")) score += 20

        if (duration >= 300f) score += 35
        else if (duration == 0f && lower.contains(".m3u8")) score += 20 // En vivo o adaptive sin metadata aún

        val host = try { Uri.parse(url).host?.lowercase() } catch (_: Throwable) { null }
        if (host != null) {
            for (cdn in KNOWN_CDN_EMBED_MAP.keys) {
                if (host.contains(cdn)) {
                    score += 25
                    break
                }
            }
        }
        return score
    }

    fun resolveDynamicHeaders(
        streamUrl: String,
        pageUrl: String,
        rawHeaders: Map<String, String>? = null
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        rawHeaders?.forEach { (k, v) ->
            if (v.isNotBlank()) {
                val keyLower = k.lowercase()
                if (keyLower == "referer" || keyLower == "origin" || keyLower == "user-agent" || keyLower == "cookie") {
                    result[k.replaceFirstChar { it.uppercase() }] = v
                }
            }
        }

        val streamHost = try { Uri.parse(streamUrl).host?.lowercase() } catch (_: Throwable) { null }
        var deducedOrigin: String? = null
        var deducedReferer: String? = null

        val recipe = StreamRuleSyncManager.findMatchingRecipe(streamUrl)
        if (recipe != null) {
            deducedOrigin = recipe.embedOrigin
            deducedReferer = recipe.referer
            result["Sec-Fetch-Mode"] = recipe.secFetchMode
            result["Sec-Fetch-Dest"] = recipe.secFetchDest
            result["Sec-Fetch-Site"] = recipe.secFetchSite
        }

        if (deducedOrigin == null && streamHost != null) {
            for ((cdn, embedOrigin) in KNOWN_CDN_EMBED_MAP) {
                if (streamHost.contains(cdn)) {
                    deducedOrigin = embedOrigin
                    break
                }
            }
        }

        if (deducedOrigin == null && pageUrl.startsWith("http")) {
            deducedOrigin = try {
                val pUri = Uri.parse(pageUrl)
                "${pUri.scheme}://${pUri.host}"
            } catch (_: Throwable) { null }
        }

        if (deducedOrigin != null) {
            if (!result.containsKey("Origin")) result["Origin"] = deducedOrigin
            if (!result.containsKey("Referer")) result["Referer"] = deducedReferer ?: "$deducedOrigin/"
        }

        result["Sec-Fetch-Mode"] = "cors"
        result["Sec-Fetch-Dest"] = "empty"
        result["Sec-Fetch-Site"] = "cross-site"
        if (!result.containsKey("User-Agent")) result["User-Agent"] = UA_MOBILE
        return result
    }
}
