package com.omnicast.core.domain.usecases

/**
 * Validador centralizado de dominio para candidatos de flujos multimedia.
 * Define el contrato estándar para evitar que cada nuevo extractor ("poner y sacar")
 * reinvente la rueda o cometa errores manuales al distinguir entre streams binarios
 * y páginas web, iframes, endpoints de control, anuncios o telemetría.
 *
 * Arquitectura: Hexagonal pura (sin dependencias de Android, solo Kotlin puro en :core:domain).
 * Estándar obligatorio de proyecto: Todo nuevo extractor debe delegar su validación aquí.
 * Susceptible a mejora continua conforme evolucionen las CDNs de la industria.
 */
object StreamCandidateValidator {

    private val WEB_DOCUMENT_EXTENSIONS = setOf(
        ".html", ".htm", ".xhtml", ".js", ".css", ".json", ".xml",
        ".txt", ".svg", ".png", ".jpg", ".jpeg", ".gif", ".webp",
        ".ico", ".woff", ".woff2", ".ttf", ".eot", ".otf"
    )

    private val NON_MEDIA_ROUTES = listOf(
        "/cdn-cgi/",
        "/pass_md5/",
        "op=watch",
        "op=download",
        "/dood?",
        "/watch?",
        "/e/",
        "/embed/",
        "/embed-",
        "/iframe/",
        "/player/",
        "videostat",
        "beacon",
        "statnew",
        "tracking",
        "overroll_",
        "gasparin",
        "/public/video.mp4"
    )

    /**
     * Determina si la URL corresponde a un documento web, script, asset o ruta de reproductor/iframe
     * que NO es un flujo de video directo.
     */
    fun isWebDocumentOrPlayerRoute(url: String): Boolean {
        if (url.isBlank()) return true
        val lower = url.lowercase()
        val cleanPath = lower.substringBefore('?').substringBefore('#')

        // 1. Detección de scripts y librerías web (incluyendo versiones npm ej: .js@1.6.16 y CDNs públicas)
        if (cleanPath.endsWith(".js") || cleanPath.contains(".js@") || cleanPath.contains(".min.js") ||
            cleanPath.endsWith(".mjs") || lower.contains(".js?") ||
            lower.contains("cdn.jsdelivr.net") || lower.contains("cdnjs.cloudflare.com") || lower.contains("unpkg.com")
        ) return true

        // 2. Extensiones de documentos web y assets estáticos
        for (ext in WEB_DOCUMENT_EXTENSIONS) {
            if (cleanPath.endsWith(ext)) {
                if (ext == ".txt" && (cleanPath.contains("master") || cleanPath.contains("index") || lower.contains("hls") || lower.contains(".txt?hls") || lower.contains("master.txt"))) {
                    continue
                }
                return true
            }
        }

        // 3. Rutas de páginas web de reproductor, iframes, endpoints de control y falsos prerolls
        for (route in NON_MEDIA_ROUTES) {
            if (lower.contains(route)) return true
        }

        return false
    }

    /**
     * Valida si una URL presenta una firma o contrato de entrega de stream binario real
     * (HLS, DASH, MP4, WebM, MKV o firmas de entrega certificadas de CDN como /pass/ o video~).
     */
    fun isBinaryMediaStream(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()
        val cleanPath = lower.substringBefore('?').substringBefore('#')

        // Extensiones directas estándar
        if (cleanPath.endsWith(".m3u8") || cleanPath.endsWith(".mp4") ||
            cleanPath.endsWith(".mpd") || cleanPath.endsWith(".webm") ||
            cleanPath.endsWith(".mkv")
        ) return true

        // Extensiones directas con parámetros query (ej. video.mp4?token=...)
        if (lower.contains(".m3u8?") || lower.contains(".mp4?") ||
            lower.contains(".mpd?") || lower.contains(".webm?") ||
            lower.contains(".mkv?")
        ) return true

        // Firmas reconocidas de rutas de entrega de segmentos o streaming continuo
        if (lower.contains("/hls/") || lower.contains("/hls2/") ||
            lower.contains("/pass/") || lower.contains("video~") ||
            (lower.contains("~") && lower.contains("token=") && lower.contains("expiry=")) ||
            lower.contains("chunklist") || lower.contains("format=dash") ||
            lower.contains("master.m3u8") || lower.contains("manifest.mpd")
        ) return true

        // Rutas de manifiesto dinámicas y proxies de streaming (.txt?hls, master.txt, .php?url=)
        if (lower.contains(".txt?hls") || lower.contains("master.txt") ||
            lower.contains(".php?url=") || lower.contains("p.php?url=") ||
            (cleanPath.endsWith(".txt") && lower.contains("hls"))
        ) return true

        return false
    }

    private val offlineCdnHosts = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /**
     * Registra un host o URL de CDN comprobado como caído, estacionado (Honeypot) o con error fatal.
     * Complejidad O(1) en memoria RAM L1 (cero acceso a disco).
     */
    fun markCdnHostOffline(hostOrUrl: String) {
        val host = extractHost(hostOrUrl)
        if (host.isNotBlank()) {
            offlineCdnHosts.add(host)
        }
    }

    /**
     * Determina en < 0.001 ms si la URL pertenece a un host de CDN en cuarentena/offline.
     */
    fun isCdnHostOffline(url: String): Boolean {
        if (offlineCdnHosts.isEmpty()) return false
        val host = extractHost(url)
        if (host.isBlank()) return false
        if (offlineCdnHosts.contains(host)) return true
        for (dead in offlineCdnHosts) {
            if (host.endsWith(".$dead")) return true
        }
        return false
    }

    fun clearOfflineCdnHosts() {
        offlineCdnHosts.clear()
    }

    fun extractHost(urlOrHost: String): String {
        val s = urlOrHost.trim().lowercase()
        val noProto = when {
            s.startsWith("https://") -> s.substring(8)
            s.startsWith("http://") -> s.substring(7)
            else -> s
        }
        val host = noProto.substringBefore('/').substringBefore(':').substringBefore('?')
        return if (host.startsWith("www.")) host.substring(4) else host
    }

    /**
     * Contrato universal para extractores basados en red CDN:
     * El candidato debe coincidir con el criterio del host/dominio del extractor
     * Y debe ser un stream binario real Y NO una ruta de reproductor web, documento o CDN caída.
     */
    fun isValidCdnStream(url: String, domainMatches: Boolean): Boolean {
        if (!domainMatches) return false
        if (isCdnHostOffline(url)) return false
        if (isWebDocumentOrPlayerRoute(url)) return false
        return isBinaryMediaStream(url)
    }

    /**
     * Verifica si el host de url o pageUrl pertenece a la familia de un proveedor.
     * Evalúa si contiene el providerFamilyTag (en minúsculas) o coincide con alguno de fallbackDomains.
     */
    fun matchesProviderFamily(
        url: String,
        pageUrl: String?,
        providerFamilyTag: String,
        fallbackDomains: List<String>
    ): Boolean {
        val tag = providerFamilyTag.trim().lowercase()
        val urlHost = extractHost(url)
        val pageHost = pageUrl?.takeIf { it.isNotBlank() }?.let { extractHost(it) }.orEmpty()

        if (tag.isNotEmpty()) {
            if (urlHost.contains(tag) || pageHost.contains(tag)) return true
        }

        for (domain in fallbackDomains) {
            val d = domain.trim().lowercase()
            if (d.isEmpty()) continue
            val cleanDomain = if (d.startsWith("www.")) d.substring(4) else d
            if (urlHost == cleanDomain || urlHost.endsWith(".$cleanDomain") || urlHost.contains(cleanDomain) ||
                pageHost == cleanDomain || pageHost.endsWith(".$cleanDomain") || pageHost.contains(cleanDomain)
            ) {
                return true
            }
        }
        return false
    }
}
