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
    val subtitleTracksSummary: String = "",
    val groupKey: String = ""
)

object StreamQualityDetector {

    suspend fun detectQualities(
        mediaUrl: String,
        headers: Map<String, String> = emptyMap(),
        title: String? = null,
        pageUrl: String? = null
    ): List<StreamQualityOption> = withContext(Dispatchers.IO) {
        val prefix = if (!title.isNullOrBlank() && title != "Transmisión") "[${title.take(20)}] " else ""

        var currentMediaUrl = BrowserUrlHelper.sanitizeMediaUrl(mediaUrl)
        val isFastreamCdn = currentMediaUrl.contains("fastream.to", true) || currentMediaUrl.contains("dropcdn.io", true) || currentMediaUrl.contains("dr0pstream.com", true)
        val defaultUa = if (isFastreamCdn) MediaPlatformResolver.UA_FASTREAM else UA_MOBILE
        var safeHeaders = BrowserUrlHelper.buildSafeHeaders(currentMediaUrl, headers["Referer"] ?: pageUrl ?: "", defaultUa, headers)
        if (currentMediaUrl.contains("fastream.to", ignoreCase = true) && !currentMediaUrl.contains(".m3u8", ignoreCase = true)) {
            val resolved = MediaPlatformResolver.resolveFastreamSync(currentMediaUrl, headers["Referer"] ?: pageUrl)
            if (resolved != null) {
                currentMediaUrl = resolved.first
                safeHeaders = BrowserUrlHelper.buildSafeHeaders(currentMediaUrl, resolved.third["Referer"] ?: "", MediaPlatformResolver.UA_FASTREAM, resolved.third)
            }
        }

        val lower = currentMediaUrl.lowercase()
        val isRussianCdn = (lower.contains("okcdn.ru") || lower.contains("mycdn.me") || lower.contains("vkuser.net") || lower.contains("vk-cdn")) &&
                           (lower.contains("type=") || lower.contains("/type/"))
        if (isRussianCdn) {
            val okHeaders = safeHeaders.toMutableMap().apply {
                if (!containsKey("Referer")) put("Referer", "https://ok.ru/")
                if (!containsKey("User-Agent")) put("User-Agent", UA_MOBILE)
            }
            fun withType(t: Int): String = if (currentMediaUrl.contains("/type/")) {
                currentMediaUrl.replace(Regex("/type/\\d+"), "/type/$t")
            } else currentMediaUrl.replace(Regex("type=\\d+"), "type=$t")
            val u1080 = withType(5); val u720 = withType(4); val u480 = withType(3); val u360 = withType(2)
            return@withContext listOf(
                StreamQualityOption("${prefix}Película Adaptativa", "Adaptive", "Adaptativa", currentMediaUrl, isMaster = true, isMovie = true, isSuggested = true, healedHeaders = okHeaders, fileName = "master", formatDetail = "mp4 (Adaptive)", groupKey = mediaUrl),
                StreamQualityOption("${prefix}1080p Full HD", "1080p", "Full HD", u1080, isMaster = false, isMovie = true, isSuggested = false, healedHeaders = okHeaders, fileName = "video-1080p", formatDetail = "mp4 (1080p)", groupKey = mediaUrl),
                StreamQualityOption("${prefix}720p HD", "720p", "HD", u720, isMaster = false, isMovie = true, isSuggested = false, healedHeaders = okHeaders, fileName = "video-720p", formatDetail = "mp4 (720p)", groupKey = mediaUrl),
                StreamQualityOption("${prefix}480p SD", "480p", "SD", u480, isMaster = false, isMovie = true, isSuggested = false, healedHeaders = okHeaders, fileName = "video-480p", formatDetail = "mp4 (480p)", groupKey = mediaUrl),
                StreamQualityOption("${prefix}360p", "360p", "Baja", u360, isMaster = false, isMovie = true, isSuggested = false, healedHeaders = okHeaders, fileName = "video-360p", formatDetail = "mp4 (360p)", groupKey = mediaUrl)
            )
        }

        val isSigned = StreamManifestProbe.hasSignedSecurityToken(currentMediaUrl)
        if (isSigned) {
            val isUrlset = currentMediaUrl.contains(".urlset/", ignoreCase = true)
            if (currentMediaUrl.contains("master.m3u8", ignoreCase = true) && !isUrlset) {
                val uIframe = currentMediaUrl.replace("master.m3u8", "iframes-v1-a1.m3u8", ignoreCase = true)
                val uIndex = currentMediaUrl.replace("master.m3u8", "index-v1-a1.m3u8", ignoreCase = true)
                return@withContext listOf(
                    StreamQualityOption("${prefix}Película Adaptativa", "Adaptive", "Adaptativa", currentMediaUrl, isMaster = true, isMovie = true, isSuggested = true, healedHeaders = safeHeaders, fileName = "master", formatDetail = "m3u8 (adaptive)", groupKey = mediaUrl),
                    StreamQualityOption("${prefix}I-Frames (1920x816)", "1920x816", "I-Frame", uIframe, isMaster = false, isMovie = true, isSuggested = false, healedHeaders = safeHeaders, fileName = "iframes-v1-a1", formatDetail = "m3u8 (1920x816)", groupKey = mediaUrl),
                    StreamQualityOption("${prefix}1080p Full HD", "1080p", "Full HD", uIndex, isMaster = false, isMovie = true, isSuggested = false, healedHeaders = safeHeaders, fileName = "index-v1-a1", formatDetail = "m3u8 (1920x816)", groupKey = mediaUrl)
                )
            }
            val defName = extractFileName(currentMediaUrl, "master")
            val defFmt = if (currentMediaUrl.contains(".mp4", true)) "mp4 (Directo)" else "m3u8 (adaptive)"
            val label = if (currentMediaUrl.contains(".mp4", ignoreCase = true)) "${prefix}Video MP4 Directo" else "${prefix}Película Adaptativa"
            return@withContext listOf(StreamQualityOption(label, "Adaptive", "Adaptativa", currentMediaUrl, isMaster = true, isMovie = true, isSuggested = true, healedHeaders = safeHeaders, fileName = defName, formatDetail = defFmt, groupKey = mediaUrl))
        }

        // Sonda Pre-Cast + Auto-Reparador de Cabeceras (4 permutaciones deterministas)
        val probe = StreamManifestProbe.probeAndHeal(currentMediaUrl, safeHeaders, pageUrl)
        val rawManifest = probe.manifestContent

        if (!probe.isPlayable && rawManifest.isBlank()) {
            android.util.Log.w("StreamQualityDetector", "Flujo advertido por sonda (HTTP ${probe.httpCode}): $currentMediaUrl")
            if (BrowserUrlHelper.isRealMedia(currentMediaUrl) && probe.httpCode != 404 && probe.httpCode != 410) {
                val label = if (currentMediaUrl.contains(".mp4", ignoreCase = true)) "${prefix}Video MP4 Directo" else "${prefix}Flujo Directo"
                val defName = extractFileName(currentMediaUrl, "master")
                val defFmt = if (currentMediaUrl.contains(".mp4", true)) "mp4 (Directo)" else "m3u8 (adaptive)"
                return@withContext listOf(StreamQualityOption(label, "Adaptive", "Original", currentMediaUrl, isMovie = false, isSuggested = false, healedHeaders = safeHeaders, fileName = defName, formatDetail = defFmt, groupKey = mediaUrl))
            }
            return@withContext emptyList()
        }

        val healed = if (probe.healedHeaders.isNotEmpty()) probe.healedHeaders else safeHeaders
        val isMasterPl = probe.isMasterPlaylist || rawManifest.contains("#EXT-X-STREAM-INF:")
        val defaultMasterName = extractFileName(currentMediaUrl, if (probe.isLiveStream) "stream" else "master")
        val masterFormat = when {
            probe.isLiveStream -> "m3u8 (en vivo)"
            currentMediaUrl.contains(".mpd", true) -> "mpd (adaptive)"
            currentMediaUrl.contains(".mp4", true) -> "mp4 (Directo)"
            else -> "m3u8 (adaptive)"
        }
        if (rawManifest.isBlank()) {
            val label = if (currentMediaUrl.contains(".mp4", ignoreCase = true)) "${prefix}Video MP4 Directo" else "${prefix}Flujo Directo"
            return@withContext listOf(
                StreamQualityOption(
                    label = label,
                    resolution = "Adaptive",
                    bitrate = "Original",
                    streamUrl = currentMediaUrl,
                    isMovie = true,
                    isSuggested = true,
                    healedHeaders = healed,
                    fileName = defaultMasterName,
                    formatDetail = masterFormat,
                    groupKey = mediaUrl
                )
            )
        }

        val options = mutableListOf<StreamQualityOption>()
        val isMovie = isMasterPl || probe.durationSeconds >= 300f || probe.isLiveStream
        val mainBadge = when {
            probe.isLiveStream -> "${prefix}Stream en Vivo"
            probe.durationSeconds in 1f..180f -> "${prefix}Tráiler / Clip (${probe.durationSeconds.toInt()}s)"
            else -> "${prefix}Película Adaptativa"
        }

        val audioList = mutableListOf<String>()
        val subList = mutableListOf<String>()

        try {
            val baseUri = URI(if (probe.resolvedUrl.isNotBlank()) probe.resolvedUrl else currentMediaUrl)
            var pendingInf: String? = null
            for (rawLine in rawManifest.lineSequence()) {
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
                        height >= 1080 -> "${prefix}1080p Full HD ($bwStr)"
                        height >= 720 -> "${prefix}720p HD ($bwStr)"
                        height >= 480 -> "${prefix}480p SD ($bwStr)"
                        height >= 360 -> "${prefix}360p ($bwStr)"
                        else -> "${prefix}$res ($bwStr)"
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
                            formatDetail = "m3u8 ($res)",
                            groupKey = mediaUrl
                        )
                    )
                    pendingInf = null
                } else if (trimmed.startsWith("#EXT-X-I-FRAME-STREAM-INF:")) {
                    val uri = extractAttr(trimmed, "URI")
                    if (!uri.isNullOrBlank()) {
                        val resolvedUrl = try { baseUri.resolve(uri).toString() } catch (_: Exception) { uri }
                        val res = extractAttr(trimmed, "RESOLUTION") ?: "1920x816"
                        val vName = extractFileName(resolvedUrl, "iframes-v1-a1")
                        options.add(
                            StreamQualityOption(
                                label = "${prefix}I-Frames ($res)",
                                resolution = res,
                                bitrate = "I-Frame",
                                streamUrl = resolvedUrl,
                                isMaster = false,
                                durationSeconds = probe.durationSeconds,
                                isMovie = isMovie,
                                isSuggested = false,
                                healedHeaders = healed,
                                isLive = probe.isLiveStream,
                                fileName = vName,
                                formatDetail = "m3u8 ($res)",
                                groupKey = mediaUrl
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
                resolution = if (probe.isLiveStream) "LIVE" else "Adaptive",
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
                subtitleTracksSummary = subList.joinToString(", "),
                groupKey = mediaUrl
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
