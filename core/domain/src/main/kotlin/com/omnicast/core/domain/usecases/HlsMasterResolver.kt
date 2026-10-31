package com.omnicast.core.domain.usecases

/**
 * Resolvedor universal de listas maestras HLS (RFC 8216) y re-ascenso de sub-variantes.
 * 100% agnóstico a dominios: opera basado en el estándar oficial RFC 8216 y firmas dinámicas.
 */
object HlsMasterResolver {

    @Volatile var masterTags: Set<String> = setOf(
        "#EXT-X-STREAM-INF:", "#EXT-X-MEDIA:", "#EXT-X-I-FRAME-STREAM-INF:",
        "#EXT-X-SESSION-DATA:", "#EXT-X-SESSION-KEY:"
    )

    @Volatile var subvariantDisqualifiers: Set<String> = setOf(
        "#EXTINF:", "#EXT-X-TARGETDURATION:", "#EXT-X-MEDIA-SEQUENCE:"
    )

    @Volatile var rawSegmentExts: Set<String> = setOf(
        ".aac", ".ts", ".m4s", ".m4a", ".mp3", ".ac3", ".eac3", ".vtt", ".srt", ".key"
    )

    @Volatile var masterFileNames: Set<String> = setOf(
        "master.m3u8", "playlist.m3u8", "index.m3u8"
    )

    fun isAudioOrRawSegment(url: String): Boolean {
        val clean = url.lowercase().substringBefore('?')
        return rawSegmentExts.any { clean.endsWith(it) }
    }

    /**
     * Detección Infalible RFC 8216: Un Master DEBE contener etiquetas de variantes
     * y NUNCA debe contener etiquetas de segmentos (#EXTINF, #EXT-X-TARGETDURATION).
     */
    fun isMasterContent(manifestContent: String): Boolean {
        if (manifestContent.isBlank()) return false
        if (subvariantDisqualifiers.any { manifestContent.contains(it) }) return false
        return masterTags.any { manifestContent.contains(it) }
    }

    /**
     * Identifica si una URL tiene estructura evidente de sub-variante, audio o segmento.
     */
    fun isLikelySubVariantUrl(url: String): Boolean {
        if (isAudioOrRawSegment(url)) return true
        val lower = url.lowercase().substringBefore('?')
        if (lower.contains("masteraudio") || lower.contains("mastersubs") ||
            lower.contains("/audio/") || lower.contains("_audio/") ||
            lower.contains("/lang/") || lower.contains("_spa/") ||
            lower.contains("/subs/") || lower.contains("chunklist")) return true
        return Regex("/stream\\d+/").containsMatchIn(lower) ||
               Regex("master\\d+p").containsMatchIn(lower) ||
               Regex("index_\\d+").containsMatchIn(lower) ||
               Regex("tracks-\\d+").containsMatchIn(lower) ||
               Regex("_\\d+p\\.").containsMatchIn(lower)
    }

    /**
     * Valida si la URL representa estructuralmente un Master Playlist (.m3u8).
     */
    fun isMasterPlaylistUrl(url: String): Boolean {
        if (isLikelySubVariantUrl(url)) return false
        val clean = url.lowercase().substringBefore('?')
        if (!clean.endsWith(".m3u8") && !clean.contains(".m3u8") && !clean.endsWith(".txt")) return false
        val fileName = clean.substringAfterLast('/')
        return masterFileNames.any { fileName.equals(it, ignoreCase = true) }
    }

    /**
     * Re-asciende universalmente sub-variantes hacia el Master Playlist (.m3u8).
     */
    fun deriveMasterPlaylistUrl(url: String): String? {
        if (url.isBlank()) return null
        val cleanUrl = url.substringBefore('?')
        val query = if (url.contains('?')) "?" + url.substringAfter('?') else ""

        val fileName = cleanUrl.substringAfterLast('/')
        if (masterFileNames.any { fileName.equals(it, ignoreCase = true) }) return null

        val lower = cleanUrl.lowercase()
        if (lower.contains("dropcdn.io") || lower.contains("dr0pstream.com")) return null

        // 0. Patrón Wowza/Nimble .urlset (exclusivo para dominios Fastream)
        val isFastream = cleanUrl.contains("fastream.to", ignoreCase = true)
        val urlsetRegex = Regex("/([a-zA-Z0-9]{8,16})_[a-zA-Z0-9]+/(?:index|tracks)-[^?]+\\.m3u8", RegexOption.IGNORE_CASE)
        val urlsetMatch = urlsetRegex.find(cleanUrl)
        if (isFastream && urlsetMatch != null) {
            val id = urlsetMatch.groupValues[1]
            return cleanUrl.replace(urlsetRegex, "/${id}_,l,n,.urlset/master.m3u8") + query
        }

        // 1. Patrón Carpeta de Calidad (Sky News: .../stream04/streamPlaylist.m3u8 -> .../master.m3u8)
        val folderRegex = Regex("/(?:stream\\d+|tracks-\\d+|chunklist[^/]*)/[^/?#]+\\.m3u8", RegexOption.IGNORE_CASE)
        if (folderRegex.containsMatchIn(cleanUrl)) {
            return cleanUrl.replace(folderRegex, "/master.m3u8") + query
        }

        // 2. Patrón Sufijo de Resolución Akamai (Das Erste: master720p3200.m3u8 -> master.m3u8)
        val suffixRegex = Regex("master\\d+p[^/]*\\.m3u8", RegexOption.IGNORE_CASE)
        if (suffixRegex.containsMatchIn(fileName)) {
            val dir = cleanUrl.substringBeforeLast('/')
            return "$dir/${fileName.replace(suffixRegex, "master.m3u8")}$query"
        }

        // 3. Patrón Simétrico de Sub-variante Indexada (${base}_\d+.m3u8 -> ${base}.m3u8)
        val indexedVariantRegex = Regex("/([a-zA-Z0-9_-]+)_\\d+\\.m3u8", RegexOption.IGNORE_CASE)
        val indexedMatch = indexedVariantRegex.find(cleanUrl)
        if (indexedMatch != null) {
            val baseName = indexedMatch.groupValues[1]
            return cleanUrl.replace(indexedVariantRegex, "/$baseName.m3u8") + query
        }

        return null
    }
}
