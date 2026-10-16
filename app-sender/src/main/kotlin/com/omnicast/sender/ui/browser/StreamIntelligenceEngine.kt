package com.omnicast.sender.ui.browser

import android.net.Uri
import android.util.Log

/**
 * Clasificador determinista y constructor de cabeceras reales de flujo multimedia.
 * Cero dominios en duro: respeta estrictamente las cabeceras entregadas por Chromium
 * y el origen real del iframe reproductor que solicitó el recurso.
 */
object StreamIntelligenceEngine {

    private const val TAG = "StreamIntelligence"
    private const val DEFAULT_AD_MAX_DURATION_SEC = 60f

    fun isLikelyAdOrCommercial(url: String, duration: Float = 0f): Boolean {
        if (url.isBlank()) return true
        if (duration in 0.1f..(DEFAULT_AD_MAX_DURATION_SEC - 0.1f)) {
            Log.d(TAG, "🚫 Descarte: Video de duración publicitaria (${duration}s < ${DEFAULT_AD_MAX_DURATION_SEC}s)")
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
                Log.d(TAG, "🚫 Descarte por firma publicitaria [$kw]: $url")
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

        if (duration >= 1200f) score += 50
        else if (duration >= 300f) score += 35
        else if (duration == 0f && lower.contains(".m3u8")) score += 20

        return score
    }

    fun resolveDynamicHeaders(
        streamUrl: String,
        sourceFrameOrPageUrl: String,
        rawHeaders: Map<String, String>? = null
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        rawHeaders?.forEach { (k, v) ->
            if (v.isNotBlank()) {
                when (k.lowercase()) {
                    "referer" -> result["Referer"] = v
                    "origin" -> result["Origin"] = v
                    "user-agent" -> result["User-Agent"] = v
                    "cookie" -> result["Cookie"] = v
                    "authorization" -> result["Authorization"] = v
                    "sec-fetch-mode" -> result["Sec-Fetch-Mode"] = v
                    "sec-fetch-dest" -> result["Sec-Fetch-Dest"] = v
                    "sec-fetch-site" -> result["Sec-Fetch-Site"] = v
                }
            }
        }

        // Si sourceFrameOrPageUrl es una URL de embed o más específica que el Referer truncado por Chromium, priorizarla
        if (sourceFrameOrPageUrl.startsWith("http") && (result["Referer"].isNullOrBlank() || sourceFrameOrPageUrl.contains("/e/") || sourceFrameOrPageUrl.contains("embed") || sourceFrameOrPageUrl.length > (result["Referer"]?.length ?: 0))) {
            result["Referer"] = sourceFrameOrPageUrl
        }

        val existingReferer = result["Referer"]
        if (!existingReferer.isNullOrBlank() && !result.containsKey("Origin")) {
            try {
                val rUri = Uri.parse(existingReferer)
                if (!rUri.scheme.isNullOrBlank() && !rUri.host.isNullOrBlank()) {
                    result["Origin"] = "${rUri.scheme}://${rUri.host}"
                }
            } catch (_: Throwable) {}
        }

        if (!result.containsKey("Sec-Fetch-Mode")) result["Sec-Fetch-Mode"] = "cors"
        if (!result.containsKey("Sec-Fetch-Dest")) result["Sec-Fetch-Dest"] = "empty"
        if (!result.containsKey("Sec-Fetch-Site")) result["Sec-Fetch-Site"] = "cross-site"
        if (!result.containsKey("User-Agent")) result["User-Agent"] = UA_MOBILE
        return result
    }
}
