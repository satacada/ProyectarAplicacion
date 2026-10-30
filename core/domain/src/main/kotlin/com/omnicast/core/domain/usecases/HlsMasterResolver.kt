package com.omnicast.core.domain.usecases

/**
 * Resolvedor universal de listas maestras HLS (RFC 8216) y re-ascenso de sub-variantes.
 * 100% agnóstico a dominios: opera basado en la jerarquía estándar de carpetas de streaming.
 */
object HlsMasterResolver {

    /**
     * Re-asciende universalmente sub-variantes de solo video hacia el Master Playlist (.m3u8).
     * Garantiza que la TV Box reciba el manifiesto con las pistas de audio desmultiplexadas.
     */
    fun deriveMasterPlaylistUrl(url: String): String? {
        if (url.isBlank()) return null
        val cleanUrl = url.substringBefore('?')
        val query = if (url.contains('?')) "?" + url.substringAfter('?') else ""

        // Si ya es un master o playlist raíz, no requiere re-ascenso
        if (cleanUrl.endsWith("/master.m3u8", ignoreCase = true) ||
            cleanUrl.endsWith("/playlist.m3u8", ignoreCase = true)) {
            return null
        }

        // Intocabilidad de DropCDN (ADR-013): URLs puras sin reescritura
        val lower = cleanUrl.lowercase()
        if (lower.contains("dropcdn.io") || lower.contains("dr0pstream.com")) return null

        // 0. Patrón Wowza/Nimble .urlset (Fastream y servidores multiservicio)
        val urlsetRegex = Regex("/([a-zA-Z0-9]{8,16})_[a-zA-Z0-9]+/(?:index|tracks)-[^?]+\\.m3u8", RegexOption.IGNORE_CASE)
        val urlsetMatch = urlsetRegex.find(cleanUrl)
        if (urlsetMatch != null) {
            val id = urlsetMatch.groupValues[1]
            return cleanUrl.replace(urlsetRegex, "/${id}_,l,n,.urlset/master.m3u8") + query
        }

        // 1. Patrón Carpeta de Calidad (Sky News: .../stream04/playlist.m3u8 -> .../master.m3u8)
        val folderRegex = Regex("/(?:stream\\d+|tracks-\\d+|chunklist[^/]*)/[^/?#]+\\.m3u8", RegexOption.IGNORE_CASE)
        if (folderRegex.containsMatchIn(cleanUrl)) {
            return cleanUrl.replace(folderRegex, "/master.m3u8") + query
        }

        // 2. Patrón Sufijo de Resolución en Archivo (Das Erste: master270p700.m3u8 -> master.m3u8)
        val suffixRegex = Regex("master\\d+p\\d+\\.m3u8", RegexOption.IGNORE_CASE)
        if (suffixRegex.containsMatchIn(cleanUrl)) {
            return cleanUrl.replace(suffixRegex, "master.m3u8") + query
        }

        // 3. Patrón Índice de Sub-variante (ej. index_2.m3u8 -> master.m3u8)
        val indexRegex = Regex("/index_\\d+\\.m3u8", RegexOption.IGNORE_CASE)
        if (indexRegex.containsMatchIn(cleanUrl)) {
            return cleanUrl.replace(indexRegex, "/master.m3u8") + query
        }

        return null
    }

    /**
     * Detección Infalible RFC 8216: Un Master DEBE contener #EXT-X-STREAM-INF o #EXT-X-MEDIA.
     */
    fun isMasterContent(manifestContent: String): Boolean {
        return manifestContent.contains("#EXT-X-STREAM-INF:") ||
               manifestContent.contains("#EXT-X-MEDIA:TYPE=AUDIO") ||
               manifestContent.contains("#EXT-X-MEDIA:TYPE=VIDEO")
    }

    /**
     * Identifica si una URL tiene estructura evidente de sub-variante de video y no de Master.
     */
    fun isLikelySubVariantUrl(url: String): Boolean {
        val lower = url.lowercase().substringBefore('?')
        return lower.contains("chunklist") ||
               Regex("/stream\\d+/").containsMatchIn(lower) ||
               Regex("master\\d+p\\d+").containsMatchIn(lower) ||
               Regex("index_\\d+").containsMatchIn(lower) ||
               Regex("_\\d+p\\.").containsMatchIn(lower)
    }
}
