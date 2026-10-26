package com.omnicast.sender.ui.browser

import com.omnicast.core.domain.usecases.StreamManifestProbe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

data class StreamQualityOption(
    val label: String, val resolution: String, val bitrate: String, val streamUrl: String,
    val isMaster: Boolean = false, val durationSeconds: Float = 0f, val isMovie: Boolean = false,
    val isSuggested: Boolean = false, val healedHeaders: Map<String, String> = emptyMap(),
    val isLive: Boolean = false, val fileName: String = "", val formatDetail: String = "",
    val audioTracksSummary: String = "", val subtitleTracksSummary: String = "",
    val reputationScore: Int = 50, val isTestedSuccess: Boolean = false, val isTestedFailed: Boolean = false,
    val subtitles: List<com.omnicast.core.protocol.SignalingMessage.SubtitleTrack> = emptyList(),
    val groupTitle: String = ""
)

object StreamQualityDetector {

    suspend fun detectQualities(
        mediaUrl: String,
        headers: Map<String, String> = emptyMap(),
        title: String? = null,
        pageUrl: String? = null
    ): List<StreamQualityOption> = withContext(Dispatchers.IO) {
        val grpTitle = title?.takeIf { it.isNotBlank() && it != "Cargando..." && it != "Transmisión" } ?: ""
        val prefix = if (grpTitle.isNotBlank()) "[${grpTitle.take(20)}] " else ""

        var currentMediaUrl = BrowserUrlHelper.sanitizeMediaUrl(mediaUrl)
        val isFastHost = currentMediaUrl.contains("fastream.to", ignoreCase = true)
        val initialUa = if (isFastHost) MediaPlatformResolver.UA_FASTREAM else UA_MOBILE
        var safeHeaders = BrowserUrlHelper.buildSafeHeaders(currentMediaUrl, headers["Referer"] ?: pageUrl ?: "", initialUa, headers)
        if (isFastHost && !currentMediaUrl.contains(".m3u8", ignoreCase = true)) {
            val resolved = MediaPlatformResolver.resolveFastreamSync(currentMediaUrl, headers["Referer"] ?: pageUrl)
            if (resolved != null) {
                currentMediaUrl = resolved.first
                safeHeaders = BrowserUrlHelper.buildSafeHeaders(currentMediaUrl, resolved.third["Referer"] ?: "", MediaPlatformResolver.UA_FASTREAM, resolved.third)
            }
        }

        val isSigned = StreamManifestProbe.hasSignedSecurityToken(currentMediaUrl)
        if (isSigned) {
            val isIndex = currentMediaUrl.contains("index-", ignoreCase = true)
            val isMaster = currentMediaUrl.contains("master", ignoreCase = true) || currentMediaUrl.contains("playlist", ignoreCase = true)
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
                val isOkMaster = okQuality.contains("Auto")
                return@withContext listOf(StreamQualityOption("${prefix}$okQuality", if (isOkMaster) "Auto" else okQuality, if (isOkMaster) "Adaptativa" else "Directo", currentMediaUrl, isMaster = isOkMaster, isMovie = true, isSuggested = isOkMaster, healedHeaders = safeHeaders, fileName = okQuality, formatDetail = if (isOkMaster) "m3u8 (adaptive)" else "mp4 (Directo)", groupTitle = grpTitle))
            }
            if (currentMediaUrl.contains("master.m3u8", ignoreCase = true)) {
                val uIframe = currentMediaUrl.replace("master.m3u8", "iframes-v1-a1.m3u8", ignoreCase = true)
                val uIndex = currentMediaUrl.replace("master.m3u8", "index-v1-a1.m3u8", ignoreCase = true)
                return@withContext listOf(
                    StreamQualityOption("${prefix}Película Adaptativa (Auto)", "Auto", "Adaptativa", currentMediaUrl, isMaster = true, isMovie = true, isSuggested = true, healedHeaders = safeHeaders, fileName = "master", formatDetail = "m3u8 (adaptive)", groupTitle = grpTitle),
                    StreamQualityOption("${prefix}I-Frames (1920x816)", "1920x816", "I-Frame", uIframe, isMaster = false, isMovie = true, isSuggested = false, healedHeaders = safeHeaders, fileName = "iframes-v1-a1", formatDetail = "m3u8 (1920x816)", groupTitle = grpTitle),
                    StreamQualityOption("${prefix}1080p Full HD", "1080p", "Full HD", uIndex, isMaster = false, isMovie = true, isSuggested = false, healedHeaders = safeHeaders, fileName = "index-v1-a1", formatDetail = "m3u8 (1920x816)", groupTitle = grpTitle)
                )
            }
            val defName = extractFileName(currentMediaUrl, if (isIndex) "index-v1-a1" else "master")
            val defFmt = if (currentMediaUrl.contains(".mp4", true)) "mp4 (Directo)" else if (isIndex) "m3u8 (1080p Full HD)" else "m3u8 (adaptive)"
            val label = if (currentMediaUrl.contains(".mp4", ignoreCase = true)) "${prefix}Video MP4 Directo" else if (isIndex) "${prefix}1080p Full HD (index-v1-a1)" else if (isMaster) "${prefix}Película Adaptativa (Auto)" else "${prefix}Película Directa"
            return@withContext listOf(StreamQualityOption(label, if (isIndex) "1080p" else "Auto", if (isIndex) "Full HD" else "Original", currentMediaUrl, isMaster = isMaster, isMovie = true, isSuggested = true, healedHeaders = safeHeaders, fileName = defName, formatDetail = defFmt, groupTitle = grpTitle))
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
            if (BrowserUrlHelper.isRealMedia(currentMediaUrl)) {
                val label = if (currentMediaUrl.contains(".mp4", ignoreCase = true)) "${prefix}Video MP4 Directo" else "${prefix}Flujo Directo"
                val defName = extractFileName(currentMediaUrl, "master")
                val isDead = probe.httpCode == 404 || probe.httpCode == 410 || probe.httpCode in 500..504
                val defFmt = if (currentMediaUrl.contains(".mp4", true)) "mp4 (Directo)" else "m3u8"
                val warnFmt = if (probe.httpCode > 0) "$defFmt [HTTP ${probe.httpCode}]" else "$defFmt [Inaccesible]"
                return@withContext listOf(StreamQualityOption(label, "Auto", if (probe.httpCode > 0) "HTTP ${probe.httpCode}" else "No verificado", currentMediaUrl, isMovie = false, isSuggested = false, healedHeaders = safeHeaders, fileName = defName, formatDetail = warnFmt, isTestedFailed = isDead, groupTitle = grpTitle))
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
                    formatDetail = masterFormat,
                    groupTitle = grpTitle
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
                    options.add(StreamQualityOption(name, resBadge, bwStr, resolvedUrl, isMaster = false, durationSeconds = probe.durationSeconds, isMovie = isMovie, isSuggested = false, healedHeaders = healed, isLive = probe.isLiveStream, fileName = vName, formatDetail = "m3u8 ($res)", groupTitle = grpTitle))
                    pendingInf = null
                } else if (trimmed.startsWith("#EXT-X-I-FRAME-STREAM-INF:")) {
                    val uri = extractAttr(trimmed, "URI")
                    if (!uri.isNullOrBlank()) {
                        val resolvedUrl = try { baseUri.resolve(uri).toString() } catch (_: Exception) { uri }
                        val res = extractAttr(trimmed, "RESOLUTION") ?: "1920x816"
                        val vName = extractFileName(resolvedUrl, "iframes-v1-a1")
                        options.add(StreamQualityOption("${prefix}I-Frames ($res)", res, "I-Frame", resolvedUrl, isMaster = false, durationSeconds = probe.durationSeconds, isMovie = isMovie, isSuggested = false, healedHeaders = healed, isLive = probe.isLiveStream, fileName = vName, formatDetail = "m3u8 ($res)", groupTitle = grpTitle))
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

        options.add(0, StreamQualityOption(mainBadge, if (probe.isLiveStream) "LIVE" else "Auto", "Adaptativa", currentMediaUrl, isMaster = true, durationSeconds = probe.durationSeconds, isMovie = isMovie, isSuggested = isMovie, healedHeaders = healed, isLive = probe.isLiveStream, fileName = defaultMasterName, formatDetail = masterFormat, audioTracksSummary = audioList.joinToString(", "), subtitleTracksSummary = subList.joinToString(", "), groupTitle = grpTitle))

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
        if (opt.isTestedFailed) return 0
        if (opt.isTestedSuccess) return 150
        val url = opt.streamUrl
        val isYt = BrowserUrlHelper.extractYouTubeId(url) != null
        if (isYt && pageUrl != null && BrowserUrlHelper.isMovieOrVideoPage(pageUrl)) return 10
        val isM = opt.isMaster || url.contains("master", true) || url.contains("playlist", true) || url.contains(".urlset", true)
        val hasToken = StreamManifestProbe.hasSignedSecurityToken(url)
        val isCdn = url.contains("tnmr.org", true) || url.contains("dropcdn", true) || url.contains("fastream", true) ||
                url.contains("streamwish", true) || url.contains("vidhide", true) || url.contains("mycdn.me", true) ||
                url.contains("okcdn.ru", true) || url.contains("streamtape", true) || url.contains("waaw", true) ||
                url.contains("futureofgaming", true) || url.contains("/hls", true)
        val pageHost = pageUrl?.let { runCatching { URI(it).host }.getOrNull() }
        val streamHost = runCatching { URI(url).host }.getOrNull()
        val isSameHost = pageHost != null && streamHost != null && (pageHost.equals(streamHost, true) || pageHost.endsWith(".$streamHost", true) || streamHost.endsWith(".$pageHost", true))
        val is404 = opt.bitrate.contains("HTTP 404", true) || opt.formatDetail.contains("HTTP 404", true) || opt.bitrate.contains("HTTP 410", true)
        val base = when {
            is404 -> 0
            (hasToken || isCdn) && isM -> 100
            hasToken || isCdn -> 95
            isM && !isSameHost -> 90
            url.contains(".m3u8", true) && !isSameHost -> 85
            url.contains(".txt", true) && !isSameHost -> 80
            isSameHost && !isM && !hasToken -> 15
            !isSameHost -> 70
            else -> 30
        }
        val repBonus = if (opt.reputationScore >= 75) 15 else if (opt.reputationScore <= 30) -25 else 0
        return if (base == 0) 0 else (base + repBonus).coerceIn(1, 100)
    }
}
