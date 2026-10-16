package com.omnicast.sender.ui.browser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class StreamQualityOption(
    val label: String,
    val resolution: String,
    val bitrate: String,
    val streamUrl: String,
    val isMaster: Boolean = false,
    val durationSeconds: Float = 0f,
    val isMovie: Boolean = false,
    val isSuggested: Boolean = false
)

object StreamQualityDetector {

    suspend fun detectQualities(mediaUrl: String, headers: Map<String, String> = emptyMap(), title: String? = null): List<StreamQualityOption> = withContext(Dispatchers.IO) {
        val ytId = BrowserUrlHelper.extractYouTubeId(mediaUrl)
        val prefix = if (!title.isNullOrBlank() && title != "Transmisión") "[${title.take(20)}] " else ""
        if (ytId != null) {
            return@withContext BrowserUrlHelper.createYouTubeQualityOptions(ytId, title)
        }

        val options = mutableListOf<StreamQualityOption>()
        var currentMediaUrl = mediaUrl
        var safeHeaders = BrowserUrlHelper.buildSafeHeaders(currentMediaUrl, headers["Referer"] ?: "", UA_MOBILE, headers)
        if (currentMediaUrl.contains("fastream.to", ignoreCase = true) || currentMediaUrl.contains("dropcdn.io", ignoreCase = true)) {
            val resolved = MediaPlatformResolver.resolveFastreamSync(currentMediaUrl, headers["Referer"])
            if (resolved != null) {
                currentMediaUrl = resolved.first
                safeHeaders = BrowserUrlHelper.buildSafeHeaders(currentMediaUrl, headers["Referer"] ?: "", UA_MOBILE, resolved.third)
            }
        }

        if (!currentMediaUrl.contains(".m3u8", ignoreCase = true)) {
            val isPlayable = probeStreamPlayable(currentMediaUrl, safeHeaders)
            if (!isPlayable) {
                android.util.Log.w("StreamQualityDetector", "🛑 Flujo MP4/Directo corrupto o bloqueado descartado: $currentMediaUrl")
                return@withContext emptyList()
            }
            val label = if (currentMediaUrl.contains(".mp4", ignoreCase = true)) "🎬 ${prefix}Video MP4 Directo" else "▶ ${prefix}Flujo Directo"
            return@withContext listOf(StreamQualityOption(label, "Nativa", "Original", currentMediaUrl))
        }

        try {
            var currentUrlStr = currentMediaUrl
            var conn: HttpURLConnection? = null
            for (redirect in 0..3) {
                val url = URL(currentUrlStr)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3500
                    readTimeout = 3500
                    instanceFollowRedirects = false
                    safeHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
                }
                val code = conn.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    val loc = conn.getHeaderField("Location") ?: break
                    currentUrlStr = try { URI(currentUrlStr).resolve(loc).toString() } catch (_: Throwable) { loc }
                    conn.disconnect()
                } else {
                    break
                }
            }
            val respCode = conn?.responseCode ?: -1
            val cType = conn?.contentType?.lowercase() ?: ""
            if (respCode in 200..299 && !cType.contains("text/html")) {
                options.add(StreamQualityOption("⚡ ${prefix}Adaptativa Automática", "Auto", "Adaptativa", currentMediaUrl, isMaster = true, isSuggested = true))
                val reader = BufferedReader(InputStreamReader(conn!!.inputStream))
                var line: String?
                var pendingInf: String? = null
                val baseUri = URI(currentUrlStr)

                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line!!.trim()
                    if (trimmed.startsWith("#EXT-X-STREAM-INF:")) {
                        pendingInf = trimmed
                    } else if (pendingInf != null && trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                        val resolvedUrl = try { baseUri.resolve(trimmed).toString() } catch (_: Exception) { trimmed }
                        val res = extractAttr(pendingInf, "RESOLUTION") ?: "Video"
                        val bw = extractAttr(pendingInf, "BANDWIDTH")?.toLongOrNull()
                        val bwStr = if (bw != null) "${String.format(java.util.Locale.US, "%.1f", bw / 1_000_000.0)} Mbps" else "Variable"
                        val height = res.substringAfter("x").toIntOrNull() ?: 0
                        val resBadge = if (height > 0) "${height}p" else res
                        val name = when {
                            height >= 1080 -> "🎬 ${prefix}1080p Full HD ($bwStr)"
                            height >= 720 -> "📺 ${prefix}720p HD ($bwStr)"
                            height >= 480 -> "📱 ${prefix}480p SD ($bwStr)"
                            height >= 360 -> "📱 ${prefix}360p ($bwStr)"
                            else -> "▶ ${prefix}$res ($bwStr)"
                        }
                        options.add(StreamQualityOption(name, resBadge, bwStr, resolvedUrl, isMaster = false, isSuggested = false))
                        pendingInf = null
                    }
                }
                reader.close()
                conn.disconnect()
            } else {
                android.util.Log.w("StreamQualityDetector", "⚠️ Respuesta HTTP $respCode para $mediaUrl, preservando contingencia HLS")
                options.add(StreamQualityOption("⚡ ${prefix}Reproducción Directa HLS", "Auto", "Máxima Calidad", currentMediaUrl, isMaster = true, isSuggested = true))
            }
        } catch (e: Throwable) {
            android.util.Log.w("StreamQualityDetector", "⚠️ Error leyendo playlist HLS (${e.message}), preservando contingencia")
            options.add(StreamQualityOption("⚡ ${prefix}Reproducción Directa HLS", "Auto", "Máxima Calidad", currentMediaUrl, isMaster = true, isSuggested = true))
        }
        return@withContext options.distinctBy { it.streamUrl }.sortedWith(
            compareByDescending<StreamQualityOption> { it.isSuggested }
                .thenByDescending { it.resolution.removeSuffix("p").toIntOrNull() ?: 0 }
        )
    }

    private fun extractAttr(inf: String, key: String): String? {
        val idx = inf.indexOf("$key=")
        if (idx == -1) return null
        val sub = inf.substring(idx + key.length + 1)
        return if (sub.startsWith("\"")) sub.substring(1).substringBefore("\"")
        else sub.substringBefore(",").substringBefore("\n").trim()
    }

    fun probeStreamPlayable(urlStr: String, safeHeaders: Map<String, String>): Boolean {
        val lower = urlStr.lowercase()
        if (lower.contains("dropcdn.io") || lower.contains("fastream.to") || lower.contains(".cyou") || lower.contains("/hls") || lower.contains(".m3u8")) {
            return true
        }
        return try {
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "HEAD"
                instanceFollowRedirects = true
                safeHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            val code = conn.responseCode
            val isHtml = conn.contentType?.lowercase()?.contains("text/html") == true
            conn.disconnect()
            code in 200..299 && !isHtml
        } catch (_: Throwable) {
            false
        }
    }
}
