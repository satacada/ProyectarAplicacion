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
    val isMaster: Boolean = false
)

object StreamQualityDetector {

    suspend fun detectQualities(mediaUrl: String, headers: Map<String, String> = emptyMap(), title: String? = null): List<StreamQualityOption> = withContext(Dispatchers.IO) {
        val ytId = BrowserUrlHelper.extractYouTubeId(mediaUrl)
        val prefix = if (!title.isNullOrBlank() && title != "Transmisión") "[${title.take(20)}] " else ""
        if (ytId != null) {
            return@withContext BrowserUrlHelper.createYouTubeQualityOptions(ytId, title)
        }

        val options = mutableListOf<StreamQualityOption>()
        if (!mediaUrl.contains(".m3u8", ignoreCase = true)) {
            val label = if (mediaUrl.contains(".mp4", ignoreCase = true)) "🎬 ${prefix}Video MP4 Directo" else "▶ ${prefix}Flujo Directo"
            return@withContext listOf(StreamQualityOption(label, "Nativa", "Original", mediaUrl))
        }

        // Siempre incluimos la opción Adaptativa con el Master Playlist original
        options.add(StreamQualityOption("⚡ ${prefix}Adaptativa Automática", "Auto", "Adaptativa", mediaUrl, isMaster = true))

        try {
            val url = URL(mediaUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                setRequestProperty("User-Agent", headers["User-Agent"] ?: "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")
                headers.forEach { (k, v) ->
                    if (!k.equals("User-Agent", ignoreCase = true)) setRequestProperty(k, v)
                }
            }
            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                var line: String?
                var pendingInf: String? = null
                val baseUri = URI(mediaUrl)

                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line!!.trim()
                    if (trimmed.startsWith("#EXT-X-STREAM-INF:")) {
                        pendingInf = trimmed
                    } else if (pendingInf != null && trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                        val resolvedUrl = try { baseUri.resolve(trimmed).toString() } catch (e: Exception) { trimmed }
                        val res = extractAttr(pendingInf, "RESOLUTION") ?: "Video"
                        val bw = extractAttr(pendingInf, "BANDWIDTH")?.toLongOrNull()
                        val bwStr = if (bw != null) "${String.format("%.1f", bw / 1_000_000.0)} Mbps" else "Variable"
                        val height = res.substringAfter("x").toIntOrNull() ?: 0
                        val name = when {
                            height >= 1080 -> "🎬 ${prefix}1080p Full HD ($bwStr)"
                            height >= 720 -> "📺 ${prefix}720p HD ($bwStr)"
                            height >= 480 -> "📱 ${prefix}480p SD ($bwStr)"
                            height >= 360 -> "📱 ${prefix}360p ($bwStr)"
                            else -> "▶ ${prefix}$res ($bwStr)"
                        }
                        options.add(StreamQualityOption(name, res, bwStr, resolvedUrl))
                        pendingInf = null
                    }
                }
                reader.close()
            }
            conn.disconnect()
        } catch (e: Throwable) {
            android.util.Log.w("StreamQualityDetector", "No se pudo leer playlist HLS: ${e.message}")
        }
        return@withContext options.distinctBy { it.streamUrl }
    }

    private fun extractAttr(inf: String, key: String): String? {
        val idx = inf.indexOf("$key=")
        if (idx == -1) return null
        val sub = inf.substring(idx + key.length + 1)
        return if (sub.startsWith("\"")) sub.substring(1).substringBefore("\"")
        else sub.substringBefore(",").substringBefore("\n").trim()
    }
}
