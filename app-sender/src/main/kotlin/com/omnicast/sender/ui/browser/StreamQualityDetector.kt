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
    val isLive: Boolean = false,
    val fileName: String = "",
    val formatDetail: String = "",
    val audioTracksSummary: String = "",
    val subtitleTracksSummary: String = ""
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
        val defaultMasterName = extractFileName(currentMediaUrl, if (probe.isLiveStream) "stream" else "master")
        val masterFormat = when {
            probe.isLiveStream -> "m3u8 (en vivo)"
            currentMediaUrl.contains(".mpd", true) -> "mpd (adaptive)"
            currentMediaUrl.contains(".mp4", true) -> "mp4 (Directo)"
            else -> "m3u8 (adaptive)"
        }
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
                    healedHeaders = healed,
                    fileName = defaultMasterName,
                    formatDetail = masterFormat
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

        val audioList = mutableListOf<String>()
        val subList = mutableListOf<String>()

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
                    val vName = extractFileName(resolvedUrl, "index-v1-a1")
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
                            isLive = probe.isLiveStream,
                            fileName = vName,
                            formatDetail = "m3u8 ($res)"
                        )
                    )
                    pendingInf = null
                } else if (trimmed.startsWith("#EXT-X-I-FRAME-STREAM-INF:")) {
                    val uriVal = extractAttr(trimmed, "URI")?.replace("\"", "")
                    val resVal = extractAttr(trimmed, "RESOLUTION") ?: "1920x800"
                    if (!uriVal.isNullOrBlank()) {
                        val resolvedIframe = try { baseUri.resolve(uriVal).toString() } catch (_: Throwable) { uriVal }
                        val ifName = extractFileName(resolvedIframe, "iframes-v1-a1")
                        options.add(
                            StreamQualityOption(
                                label = "🎞️ ${prefix}I-Frames ($resVal)",
                                resolution = resVal,
                                bitrate = "I-Frame",
                                streamUrl = resolvedIframe,
                                isMaster = false,
                                durationSeconds = probe.durationSeconds,
                                isMovie = isMovie,
                                isSuggested = false,
                                healedHeaders = healed,
                                isLive = probe.isLiveStream,
                                fileName = ifName,
                                formatDetail = "m3u8 ($resVal)"
                            )
                        )
                    }
                } else if (trimmed.startsWith("#EXT-X-MEDIA:TYPE=AUDIO")) {
                    val aName = extractAttr(trimmed, "NAME") ?: extractAttr(trimmed, "LANGUAGE")
                    if (!aName.isNullOrBlank() && !audioList.contains(aName)) audioList.add(aName)
                } else if (trimmed.startsWith("#EXT-X-MEDIA:TYPE=SUBTITLES")) {
                    val sName = extractAttr(trimmed, "NAME") ?: extractAttr(trimmed, "LANGUAGE")
                    if (!sName.isNullOrBlank() && !subList.contains(sName)) subList.add(sName)
                }
            }
        } catch (_: Throwable) {}

        options.add(
            0,
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
                isLive = probe.isLiveStream,
                fileName = defaultMasterName,
                formatDetail = masterFormat,
                audioTracksSummary = audioList.joinToString(", "),
                subtitleTracksSummary = subList.joinToString(", ")
            )
        )

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

    fun extractFileName(url: String, defaultName: String = "video"): String {
        return try {
            val clean = url.substringBefore('?').substringBefore('#')
            val lastSegment = clean.substringAfterLast('/')
            val nameWithoutExt = lastSegment.substringBeforeLast('.')
            if (nameWithoutExt.isNotBlank() && nameWithoutExt.length in 2..40) nameWithoutExt else defaultName
        } catch (_: Throwable) { defaultName }
    }

    fun probeStreamPlayable(urlStr: String, safeHeaders: Map<String, String>, pageUrl: String? = null): Boolean {
        return StreamManifestProbe.probeAndHeal(urlStr, safeHeaders, pageUrl).isPlayable
    }

    fun probeAndHealStream(urlStr: String, safeHeaders: Map<String, String>, pageUrl: String? = null): StreamManifestProbe.ProbeResult {
        return StreamManifestProbe.probeAndHeal(urlStr, safeHeaders, pageUrl)
    }
}
