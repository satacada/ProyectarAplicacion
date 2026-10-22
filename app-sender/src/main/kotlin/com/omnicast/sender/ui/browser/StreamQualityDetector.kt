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

        var currentMediaUrl = BrowserUrlHelper.sanitizeMediaUrl(mediaUrl)
        var safeHeaders = BrowserUrlHelper.buildSafeHeaders(currentMediaUrl, headers["Referer"] ?: pageUrl ?: "", UA_MOBILE, headers)
        if (currentMediaUrl.contains("fastream.to", ignoreCase = true) && !currentMediaUrl.contains(".m3u8", ignoreCase = true)) {
            val resolved = MediaPlatformResolver.resolveFastreamSync(currentMediaUrl, headers["Referer"] ?: pageUrl)
            if (resolved != null) {
                currentMediaUrl = resolved.first
                safeHeaders = BrowserUrlHelper.buildSafeHeaders(currentMediaUrl, resolved.third["Referer"] ?: "", UA_MOBILE, resolved.third)
            }
        }

        val isSigned = StreamManifestProbe.hasSignedSecurityToken(currentMediaUrl)
        if (isSigned) {
            if (currentMediaUrl.contains("master.m3u8", ignoreCase = true)) {
                val uIframe = currentMediaUrl.replace("master.m3u8", "iframes-v1-a1.m3u8", ignoreCase = true)
                val uIndex = currentMediaUrl.replace("master.m3u8", "index-v1-a1.m3u8", ignoreCase = true)
                return@withContext listOf(
                    StreamQualityOption("${prefix}Película Adaptativa (Auto)", "Auto", "Adaptativa", currentMediaUrl, isMaster = true, isMovie = true, isSuggested = true, healedHeaders = safeHeaders, fileName = "master", formatDetail = "m3u8 (adaptive)"),
                    StreamQualityOption("${prefix}I-Frames (1920x816)", "1920x816", "I-Frame", uIframe, isMaster = false, isMovie = true, isSuggested = false, healedHeaders = safeHeaders, fileName = "iframes-v1-a1", formatDetail = "m3u8 (1920x816)"),
                    StreamQualityOption("${prefix}1080p Full HD", "1080p", "Full HD", uIndex, isMaster = false, isMovie = true, isSuggested = false, healedHeaders = safeHeaders, fileName = "index-v1-a1", formatDetail = "m3u8 (1920x816)")
                )
            }
            val okQuality = if (currentMediaUrl.contains("mycdn.me", true) || currentMediaUrl.contains("okcdn.ru", true)) {
                when {
                    currentMediaUrl.contains("/type/5") || currentMediaUrl.contains("type=5") -> "1080p FHD"
                    currentMediaUrl.contains("/type/4") || currentMediaUrl.contains("type=4") -> "720p HD"
                    currentMediaUrl.contains("/type/3") || currentMediaUrl.contains("type=3") -> "480p SD"
                    currentMediaUrl.contains("/type/2") || currentMediaUrl.contains("type=2") -> "360p"
                    currentMediaUrl.contains("/type/1") || currentMediaUrl.contains("type=1") -> "240p"
                    currentMediaUrl.contains("/type/0") || currentMediaUrl.contains("type=0") -> "144p Móvil"
                    currentMediaUrl.contains(".m3u8", true) || currentMediaUrl.contains("/hls", true) -> "Auto (Adaptativo)"
                    else -> null
                }
            } else null
            if (okQuality != null) {
                val isMaster = okQuality.contains("Auto")
                return@withContext listOf(
                    StreamQualityOption(
                        label = "${prefix}$okQuality",
                        resolution = if (isMaster) "Auto" else okQuality,
                        bitrate = if (isMaster) "Adaptativa" else "Directo",
                        streamUrl = currentMediaUrl,
                        isMaster = isMaster,
                        isMovie = true,
                        isSuggested = isMaster,
                        healedHeaders = safeHeaders,
                        fileName = okQuality,
                        formatDetail = if (isMaster) "m3u8 (adaptive)" else "mp4 (Directo)"
                    )
                )
            }
            val defName = extractFileName(currentMediaUrl, "master")
            val defFmt = if (currentMediaUrl.contains(".mp4", true)) "mp4 (Directo)" else "m3u8 (adaptive)"
            val label = if (currentMediaUrl.contains(".mp4", ignoreCase = true)) "${prefix}Video MP4 Directo" else "${prefix}Película Directa"
            return@withContext listOf(StreamQualityOption(label, "Auto", "Original", currentMediaUrl, isMovie = true, isSuggested = true, healedHeaders = safeHeaders, fileName = defName, formatDetail = defFmt))
        }

        // Sonda Pre-Cast + Auto-Reparador de Cabeceras (4 permutaciones deterministas)
        val probe = StreamManifestProbe.probeAndHeal(currentMediaUrl, safeHeaders, pageUrl)
        val rawManifest = if (probe.isPlayable && probe.manifestContent.isNotBlank()) {
            probe.manifestContent
        } else {
            com.omnicast.sender.media.LocalStreamProxyServer.fetchManifestContent(currentMediaUrl, safeHeaders) ?: probe.manifestContent
        }

        if (!probe.isPlayable && rawManifest.isBlank()) {
            android.util.Log.w("StreamQualityDetector", "Flujo advertido por sonda (HTTP ${probe.httpCode}): $currentMediaUrl")
            if (BrowserUrlHelper.isRealMedia(currentMediaUrl) && probe.httpCode != 404 && probe.httpCode != 410) {
                val label = if (currentMediaUrl.contains(".mp4", ignoreCase = true)) "${prefix}Video MP4 Directo" else "${prefix}Flujo Directo"
                val defName = extractFileName(currentMediaUrl, "master")
                val defFmt = if (currentMediaUrl.contains(".mp4", true)) "mp4 (Directo)" else "m3u8 (adaptive)"
                val warnFmt = if (probe.httpCode > 0) "$defFmt [HTTP ${probe.httpCode}]" else defFmt
                return@withContext listOf(StreamQualityOption(label, "Auto", if (probe.httpCode > 0) "HTTP ${probe.httpCode}" else "No verificado", currentMediaUrl, isMovie = false, isSuggested = false, healedHeaders = safeHeaders, fileName = defName, formatDetail = warnFmt))
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
        val isMovie = isMasterPl || probe.durationSeconds >= 300f || probe.isLiveStream
        val mainBadge = when {
            probe.isLiveStream -> "${prefix}Stream en Vivo"
            probe.durationSeconds in 1f..180f -> "${prefix}Tráiler / Clip (${probe.durationSeconds.toInt()}s)"
            else -> "${prefix}Película Adaptativa (Auto)"
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
                            formatDetail = "m3u8 ($res)"
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
                                formatDetail = "m3u8 ($res)"
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

    fun calculateMovieProbability(opt: StreamQualityOption, pageUrl: String?): Int {
        val url = opt.streamUrl
        val isM = opt.isMaster || url.contains("master", true) || url.contains("playlist", true) || url.contains(".urlset", true)
        val hasToken = StreamManifestProbe.hasSignedSecurityToken(url)
        val isCdn = url.contains("tnmr.org", true) || url.contains("dropcdn", true) || url.contains("fastream", true) ||
                url.contains("streamwish", true) || url.contains("vidhide", true) || url.contains("mycdn.me", true) ||
                url.contains("okcdn.ru", true) || url.contains("streamtape", true) || url.contains("waaw", true) ||
                url.contains("futureofgaming", true) || url.contains("/hls", true)
        val pageHost = pageUrl?.let { runCatching { URI(it).host }.getOrNull() }
        val streamHost = runCatching { URI(url).host }.getOrNull()
        val isSameHost = pageHost != null && streamHost != null && (pageHost.equals(streamHost, true) || pageHost.endsWith(".$streamHost", true) || streamHost.endsWith(".$pageHost", true))
        val isAdvertisedError = opt.bitrate.contains("HTTP 40", true) || opt.formatDetail.contains("HTTP 40", true)
        return when {
            isAdvertisedError -> 5
            isSameHost && !isM && !hasToken -> 15
            isM && (hasToken || isCdn) -> 100
            isM -> 90
            hasToken || isCdn -> 85
            url.contains(".m3u8", true) || url.contains(".txt", true) -> 75
            opt.isMovie && !isSameHost -> 65
            !isSameHost -> 50
            else -> 30
        }
    }
}
