package com.omnicast.receiver.directstream

import java.net.URI

data class HlsVariant(
    val url: String,
    val bandwidth: Long = 0L,
    val resolution: String? = null,
    val audioGroupId: String? = null
) {
    val height: Int
        get() = resolution?.substringAfter('x')?.toIntOrNull() ?: 0
}

data class HlsMasterInfo(
    val variants: List<HlsVariant>,
    val audioTracks: Map<String, String> // GroupID -> AudioPlaylistUrl
)

/**
 * Analizador desacoplado de especificación RFC 8216 HLS para PVR.
 * Extrae pistas de video y audio desmultiplexado y selecciona resoluciones óptimas.
 */
object PvrHlsMasterParser {

    fun parseMaster(masterText: String, baseUrl: String): HlsMasterInfo {
        val lines = masterText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val audioTracks = mutableMapOf<String, String>()
        val variants = mutableListOf<HlsVariant>()

        // 1. Extraer pistas de audio independientes (#EXT-X-MEDIA:TYPE=AUDIO)
        lines.filter { it.startsWith("#EXT-X-MEDIA:") && it.contains("TYPE=AUDIO", ignoreCase = true) }.forEach { line ->
            val groupId = extractTagValue(line, "GROUP-ID")
            val uri = extractTagValue(line, "URI")
            if (!groupId.isNullOrBlank() && !uri.isNullOrBlank()) {
                audioTracks[groupId] = resolveRelativeUrl(baseUrl, uri)
            }
        }

        // 2. Extraer variantes de video (#EXT-X-STREAM-INF)
        var pendingStreamInf: String? = null
        for (line in lines) {
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                pendingStreamInf = line
            } else if (!line.startsWith("#") && pendingStreamInf != null) {
                val bw = extractTagValue(pendingStreamInf, "BANDWIDTH")?.toLongOrNull() ?: 0L
                val res = extractTagValue(pendingStreamInf, "RESOLUTION")
                val audioGrp = extractTagValue(pendingStreamInf, "AUDIO")
                val variantUrl = resolveRelativeUrl(baseUrl, line)
                variants.add(HlsVariant(url = variantUrl, bandwidth = bw, resolution = res, audioGroupId = audioGrp))
                pendingStreamInf = null
            }
        }

        return HlsMasterInfo(variants = variants, audioTracks = audioTracks)
    }

    fun selectVariant(variants: List<HlsVariant>, targetHeight: Int): HlsVariant? {
        if (variants.isEmpty()) return null
        if (targetHeight <= 0) {
            // Máxima calidad disponible
            return variants.maxByOrNull { it.height.takeIf { h -> h > 0 } ?: it.bandwidth.toInt() }
        }
        // Buscar la variante más cercana a targetHeight
        return variants.minByOrNull { kotlin.math.abs(it.height - targetHeight) }
            ?: variants.maxByOrNull { it.bandwidth }
    }

    fun parseSegments(manifestText: String, baseUrl: String): List<String> {
        return manifestText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { resolveRelativeUrl(baseUrl, it) }
    }

    fun resolveRelativeUrl(baseUrl: String, rel: String): String {
        if (rel.startsWith("http://") || rel.startsWith("https://")) return rel
        return try {
            val baseUri = URI(baseUrl)
            baseUri.resolve(rel).toString()
        } catch (_: Throwable) {
            val prefix = baseUrl.substringBeforeLast('/')
            "$prefix/$rel"
        }
    }

    private fun extractTagValue(tagLine: String, key: String): String? {
        val pattern = Regex("""\b$key=(?:"([^"]*)"|([^,\s]+))""")
        val match = pattern.find(tagLine) ?: return null
        return match.groupValues[1].ifEmpty { match.groupValues[2] }
    }

    suspend fun probeResolutions(manifestUrl: String, headers: Map<String, String>): List<Int> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val lower = manifestUrl.lowercase()
        val isHls = lower.contains(".m3u8") || lower.contains("urlset") || lower.contains(".txt")
        if (!isHls) return@withContext emptyList()
        try {
            val conn = (java.net.URL(manifestUrl).openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val master = parseMaster(text, manifestUrl)
            val resList = master.variants.map { it.height }.filter { it > 0 }.distinct().sortedDescending()
            if (resList.isNotEmpty()) resList else emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
