package com.omnicast.sender.ui.browser

import com.omnicast.core.domain.usecases.StreamManifestProbe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

data class StreamQualityOption(
    val label: String,
    val resolution: String,
    val bitrate: String,
    val streamUrl: String,
    val isMaster: Boolean = false,
    val durationSeconds: Float = 0f,
    val isMovie: Boolean = false,
    val isSuggested: Boolean = false,
    val healedHeaders: Map<String, String> = emptyMap(),
    val isLive: Boolean = false
)

object StreamQualityDetector {

    suspend fun detectQualities(
        mediaUrl: String,
        headers: Map<String, String> = emptyMap(),
        title: String? = null,
        pageUrl: String? = null
    ): List<StreamQualityOption> = withContext(Dispatchers.IO) {
        val prefix = if (!title.isNullOrBlank() && title != "Transmisión") "[${title.take(20)}] " else ""

        var currentMediaUrl = mediaUrl
        var safeHeaders = BrowserUrlHelper.buildSafeHeaders(currentMediaUrl, headers["Referer"] ?: pageUrl ?: "", UA_MOBILE, headers)
        if (currentMediaUrl.contains("fastream.to", ignoreCase = true) && !currentMediaUrl.contains(".m3u8", ignoreCase = true)) {
            val resolved = MediaPlatformResolver.resolveFastreamSync(currentMediaUrl, headers["Referer"] ?: pageUrl)
            if (resolved != null) {
                currentMediaUrl = resolved.first
                safeHeaders = BrowserUrlHelper.buildSafeHeaders(currentMediaUrl, resolved.third["Referer"] ?: "", UA_MOBILE, resolved.third)
            }
        }

        // Sonda Pre-Cast + Auto-Reparador de Cabeceras (4 permutaciones deterministas)
        val probe = StreamManifestProbe.probeAndHeal(currentMediaUrl, safeHeaders, pageUrl)
        if (!probe.isPlayable) {
            android.util.Log.w("StreamQualityDetector", "🛑 Flujo advertido por sonda (HTTP ${probe.httpCode}): $currentMediaUrl")
            if (BrowserUrlHelper.isRealMedia(currentMediaUrl) && probe.httpCode != 404 && probe.httpCode != 410) {
                val label = if (currentMediaUrl.contains(".mp4", ignoreCase = true)) "🎬 ${prefix}Video MP4 Directo" else "▶ ${prefix}Flujo Directo (Nativo)"
                return@withContext listOf(
                    StreamQualityOption(
                        label = label,
                        resolution = "Auto",
                        bitrate = "Original",
                        streamUrl = currentMediaUrl,
                        isMovie = true,
                        isSuggested = true,
                        healedHeaders = safeHeaders
                    )
                )
            }
            return@withContext emptyList()
        }

        val healed = probe.healedHeaders
        if (probe.manifestContent.isBlank()) {
            val label = if (currentMediaUrl.contains(".mp4", ignoreCase = true)) "🎬 ${prefix}Video MP4 Directo" else "▶ ${prefix}Flujo Directo"
            return@withContext listOf(
                StreamQualityOption(
                    label = label,
                    resolution = "Nativa",
                    bitrate = "Original",
                    streamUrl = currentMediaUrl,
                    isMovie = true,
                    isSuggested = true,
                    healedHeaders = healed
                )
            )
        }

        val options = mutableListOf<StreamQualityOption>()
        val isMovie = probe.isMasterPlaylist || probe.durationSeconds >= 300f || probe.isLiveStream
        val mainBadge = when {
            probe.isLiveStream -> "🔴 ${prefix}Stream en Vivo"
            probe.durationSeconds in 1f..180f -> "🎞️ ${prefix}Tráiler / Clip (${probe.durationSeconds.toInt()}s)"
            else -> "⚡ ${prefix}Película Adaptativa (Auto)"
        }
        options.add(
            StreamQualityOption(
                label = mainBadge,
                resolution = if (probe.isLiveStream) "LIVE" else "Auto",
                bitrate = "Adaptativa",
                streamUrl = currentMediaUrl,
                isMaster = true,
                durationSeconds = probe.durationSeconds,
                isMovie = isMovie,
                isSuggested = isMovie,
                healedHeaders = healed,
                isLive = probe.isLiveStream
            )
        )

        try {
            val baseUri = URI(probe.resolvedUrl)
            var pendingInf: String? = null
            for (rawLine in probe.manifestContent.lineSequence()) {
                val trimmed = rawLine.trim()
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
                    options.add(
                        StreamQualityOption(
                            label = name,
                            resolution = resBadge,
                            bitrate = bwStr,
                            streamUrl = resolvedUrl,
                            isMaster = false,
                            durationSeconds = probe.durationSeconds,
                            isMovie = isMovie,
                            isSuggested = false,
                            healedHeaders = healed,
                            isLive = probe.isLiveStream
                        )
                    )
                    pendingInf = null
                }
            }
        } catch (_: Throwable) {}

        return@withContext options.distinctBy { it.streamUrl }.sortedWith(
            compareByDescending<StreamQualityOption> { it.isSuggested }
                .thenByDescending { it.isMovie }
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

    fun probeStreamPlayable(urlStr: String, safeHeaders: Map<String, String>, pageUrl: String? = null): Boolean {
        return StreamManifestProbe.probeAndHeal(urlStr, safeHeaders, pageUrl).isPlayable
    }

    fun probeAndHealStream(urlStr: String, safeHeaders: Map<String, String>, pageUrl: String? = null): StreamManifestProbe.ProbeResult {
        return StreamManifestProbe.probeAndHeal(urlStr, safeHeaders, pageUrl)
    }
}
