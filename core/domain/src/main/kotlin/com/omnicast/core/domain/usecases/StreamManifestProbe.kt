package com.omnicast.core.domain.usecases

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Sonda Pre-Cast y Auto-Reparador determinista de cabeceras (Costo $0, 100% Kotlin).
 * 1. Verifica que un enlace de video realmente responda HTTP 200..299 y no sea HTML ni esté roto (evita cierres de ExoPlayer).
 * 2. Si recibe HTTP 403/401, prueba automáticamente las 4 permutaciones lógicas reales (Iframe -> PageUrl -> Propio Host -> Sin Referer).
 * 3. Analiza manifiestos HLS (#EXTM3U) para distinguir entre Película VOD, Tráiler corto y Stream en Vivo (sin #EXT-X-ENDLIST).
 */
object StreamManifestProbe {

    private const val DEFAULT_UA = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    private const val TIMEOUT_MS = 3200
    private val learnedHostStrategy = ConcurrentHashMap<String, String>()

    data class ProbeResult(
        val isPlayable: Boolean, val resolvedUrl: String, val healedHeaders: Map<String, String>,
        val isLiveStream: Boolean = false, val isMasterPlaylist: Boolean = false,
        val durationSeconds: Float = 0f, val manifestContent: String = "", val httpCode: Int = -1
    )

    fun sanitizeMediaUrl(url: String): String {
        if (url.isBlank()) return url
        if (url.contains(".dropcdn.io", true) && url.contains("srv=")) {
            val srv = Regex("[?&]srv=([^&]+)").find(url)?.groupValues?.get(1)
            if (!srv.isNullOrBlank()) {
                return url.replace(Regex("https?://r\\d+\\.dropcdn\\.io"), "https://$srv.dropcdn.io")
                          .replace(Regex("https?://[^/]+\\.dropcdn\\.io(?=/hls)"), "https://$srv.dropcdn.io")
            }
        }
        return url
    }

    val DEFAULT_HOST_PROFILES: Map<String, Pair<String, Boolean>> = mapOf(
        "dropcdn.io" to ("WEBVIEW" to true), "dr0pstream.com" to ("WEBVIEW" to true), "pelisplus.hair" to ("WEBVIEW" to true),
        "fastream.to" to ("WEBVIEW" to true), "luluvdo.com" to ("WEBVIEW" to true), "lulustream.com" to ("WEBVIEW" to true),
        "pelisgo.online" to ("WEBVIEW" to true), "pelisgo" to ("WEBVIEW" to true), "tnmr.org" to ("EXOPLAYER" to true),
        "boltdns.net" to ("EXOPLAYER" to false), "canela.tv" to ("EXOPLAYER" to false), "okcdn.ru" to ("EXOPLAYER" to false),
        "mycdn.me" to ("EXOPLAYER" to false), "ok.ru" to ("EXOPLAYER" to false), "spark.9bg.net" to ("EXOPLAYER" to false),
        "dailymotion.com" to ("EXOPLAYER" to false), "dmcdn.net" to ("EXOPLAYER" to false),
        "cloudfront.net" to ("EXOPLAYER" to false)
    )

    fun lookupSeedProfile(urlOrReferer: String): Pair<String, Boolean>? {
        if (urlOrReferer.isBlank()) return null
        val lower = urlOrReferer.lowercase()
        return DEFAULT_HOST_PROFILES.entries.firstOrNull { lower.contains(it.key) }?.value
    }

    fun hasSignedSecurityToken(url: String): Boolean {
        val query = url.substringAfter('?', "")
        if (query.isBlank()) return false
        val params = query.split('&').map { it.substringBefore('=').lowercase() to it.substringAfter('=', "") }
        val hasToken = params.any { (k, v) -> k in setOf("t", "token", "st", "md5", "hmac", "auth_key") && v.length >= 8 }
        val hasTime = params.any { (k, v) -> k in setOf("s", "e", "t_expire", "sp") && v.isNotEmpty() && v.all { it.isDigit() } }
        return hasToken && hasTime
    }

    fun probeAndHeal(
        streamUrl: String,
        initialHeaders: Map<String, String> = emptyMap(),
        pageUrl: String? = null,
        isHealingAttempt: Boolean = false
    ): ProbeResult {
        if (streamUrl.isBlank()) return ProbeResult(false, streamUrl, initialHeaders)
        val cleanUrl = sanitizeMediaUrl(streamUrl)
        val lower = cleanUrl.lowercase()
        // Los enlaces crudos de página web de YouTube no son flujos reproducibles por ExoPlayer directo
        if ((lower.contains("youtube.com/watch") || lower.contains("youtu.be/")) && !lower.contains(".m3u8") && !lower.contains("googlevideo.com")) {
            return ProbeResult(false, cleanUrl, initialHeaders, httpCode = 415)
        }
        val isSingleUse = initialHeaders["X-Single-Use-Token"] == "true" ||
            (initialHeaders["X-Single-Use-Token"] != "false" && hasSignedSecurityToken(cleanUrl))
        if (isSingleUse) {
            val isLive = !cleanUrl.contains(".m3u8") || cleanUrl.contains("live", true)
            val isMaster = cleanUrl.contains("master", true) || cleanUrl.contains("playlist", true)
            return ProbeResult(isPlayable = true, resolvedUrl = cleanUrl, healedHeaders = initialHeaders, isLiveStream = isLive, isMasterPlaylist = isMaster, durationSeconds = 7200f)
        }

        val candidates = buildHeaderPermutations(cleanUrl, initialHeaders, pageUrl)
        var lastResult = ProbeResult(false, cleanUrl, initialHeaders)

        for ((strategyName, candidateHeaders) in candidates) {
            val res = executeSingleProbe(cleanUrl, candidateHeaders)
            lastResult = res
            if (res.isPlayable) {
                val host = extractHost(cleanUrl)
                if (host != null) learnedHostStrategy[host] = strategyName
                return res
            }
            if (res.httpCode == 404 || res.httpCode == 410 || res.httpCode == -1) {
                break
            }
        }
        return lastResult
    }

    fun buildHeaderPermutations(
        streamUrl: String,
        initialHeaders: Map<String, String>,
        pageUrl: String?
    ): List<Pair<String, Map<String, String>>> {
        val baseUa = initialHeaders["User-Agent"] ?: initialHeaders["user-agent"] ?: DEFAULT_UA
        val baseCookie = initialHeaders["Cookie"] ?: initialHeaders["cookie"] ?: ""
        val list = mutableListOf<Pair<String, Map<String, String>>>()

        // 1. Cabeceras originales reales (del iframe o de Chromium)
        val orig = mutableMapOf<String, String>("User-Agent" to baseUa)
        if (baseCookie.isNotBlank()) orig["Cookie"] = baseCookie
        initialHeaders.forEach { (k, v) ->
            if (v.isNotBlank()) {
                when (k.lowercase()) {
                    "referer" -> orig["Referer"] = v
                    "origin" -> orig["Origin"] = v
                    "authorization" -> orig["Authorization"] = v
                    "sec-fetch-mode" -> orig["Sec-Fetch-Mode"] = v
                    "sec-fetch-dest" -> orig["Sec-Fetch-Dest"] = v
                    "sec-fetch-site" -> orig["Sec-Fetch-Site"] = v
                    "accept" -> orig["Accept"] = v
                    "accept-language" -> orig["Accept-Language"] = v
                }
            }
        }
        orig["Referer"]?.let { ref ->
            if (!orig.containsKey("Origin")) extractOrigin(ref)?.let { orig["Origin"] = it }
        }
        list.add("ORIGINAL" to orig)

        // 1b. Permutación de host embed específico para CDN protegidas (DropCDN, Fastream)
        val lower = streamUrl.lowercase()
        if (lower.contains("dropcdn.io") || lower.contains("dr0pstream.com") || lower.contains("fastream.to")) {
            val id = Regex("(?:/hls\\d*/\\d+/\\d+/|/embed-|/e/|/d/)([a-zA-Z0-9]{8,16})(?:[_,.]|$)").find(streamUrl)?.groupValues?.get(1)
            if (id != null) {
                val isDrop = lower.contains("dropcdn.io") || lower.contains("dr0pstream.com")
                val embedUrl = if (isDrop) "https://dr0pstream.com/e/$id" else "https://fastream.to/embed-$id.html"
                val origin = if (isDrop) "https://dr0pstream.com" else "https://fastream.to"
                val embedMap = mutableMapOf(
                    "User-Agent" to baseUa,
                    "Referer" to embedUrl,
                    "Origin" to origin,
                    "Sec-Fetch-Mode" to "cors",
                    "Sec-Fetch-Dest" to "empty",
                    "Sec-Fetch-Site" to "cross-site"
                )
                if (baseCookie.isNotBlank()) embedMap["Cookie"] = baseCookie
                list.add(0, "EMBED_HOST" to embedMap)
            }
        }

        // 2. Cabeceras de la página principal (pageUrl), si es distinta al Referer original
        if (!pageUrl.isNullOrBlank() && pageUrl.startsWith("http") && pageUrl != orig["Referer"]) {
            val pageMap = mutableMapOf("User-Agent" to baseUa, "Referer" to pageUrl)
            if (baseCookie.isNotBlank()) pageMap["Cookie"] = baseCookie
            extractOrigin(pageUrl)?.let { pageMap["Origin"] = it }
            list.add("PAGE_URL" to pageMap)
        }

        // 3. Cabeceras del propio dominio del CDN (Self-Origin)
        val selfOrigin = extractOrigin(streamUrl)
        if (selfOrigin != null && selfOrigin != orig["Origin"]) {
            val selfMap = mutableMapOf("User-Agent" to baseUa, "Referer" to "$selfOrigin/", "Origin" to selfOrigin)
            if (baseCookie.isNotBlank()) selfMap["Cookie"] = baseCookie
            list.add("SELF_CDN" to selfMap)
        }

        // 4. Petición limpia (Sin Referer ni Origin)
        val cleanMap = mutableMapOf("User-Agent" to baseUa)
        if (baseCookie.isNotBlank()) cleanMap["Cookie"] = baseCookie
        list.add("CLEAN" to cleanMap)

        // Si ya aprendimos qué estrategia funciona para este host, ponerla de primera
        val host = extractHost(streamUrl)
        val preferred = if (host != null) learnedHostStrategy[host] else null
        if (preferred != null) {
            val idx = list.indexOfFirst { it.first == preferred }
            if (idx > 0) {
                val item = list.removeAt(idx)
                list.add(0, item)
            }
        }
        return list
    }

    private fun executeSingleProbe(streamUrl: String, headers: Map<String, String>): ProbeResult {
        var currentUrlStr = streamUrl
        var conn: HttpURLConnection? = null
        return try {
            for (redirect in 0..3) {
                val url = URL(currentUrlStr)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    requestMethod = "GET"
                    instanceFollowRedirects = false
                    headers.forEach { (k, v) -> setRequestProperty(k, v) }
                    if (!currentUrlStr.contains(".m3u8", true) && !currentUrlStr.contains("/hls", true)) {
                        setRequestProperty("Range", "bytes=0-4095")
                    }
                }
                val code = conn.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    val loc = conn.getHeaderField("Location") ?: break
                    currentUrlStr = try { URI(currentUrlStr).resolve(loc).toString() } catch (_: Throwable) { loc }
                    conn.disconnect()
                } else {
                    break
                }
            }
            val code = conn?.responseCode ?: -1
            val cType = conn?.contentType?.lowercase() ?: ""
            if (code !in 200..299 || cType.contains("text/html")) {
                conn?.disconnect()
                return ProbeResult(false, currentUrlStr, headers, httpCode = code)
            }

            val isHlsCandidate = currentUrlStr.contains(".m3u8", true) ||
                currentUrlStr.contains("/hls", true) ||
                cType.contains("mpegurl") ||
                cType.contains("vnd.apple.mpegurl")

            if (!isHlsCandidate) {
                conn?.disconnect()
                return ProbeResult(true, currentUrlStr, headers, httpCode = code)
            }

            // Leer hasta 250 líneas del manifiesto HLS para validar #EXTM3U, calcular duración y detectar vivo
            val reader = BufferedReader(InputStreamReader(conn!!.inputStream))
            val sb = StringBuilder()
            var lineCount = 0
            var line: String?
            var isValidM3u8 = false
            var isMaster = false
            var hasEndList = false
            var isVodType = false
            var totalDuration = 0f

            while (reader.readLine().also { line = it } != null && lineCount < 250) {
                val trimmed = line!!.trim()
                if (trimmed.isEmpty()) continue
                sb.append(trimmed).append('\n')
                lineCount++
                if (trimmed.startsWith("#EXTM3U")) isValidM3u8 = true
                else if (trimmed.startsWith("#EXT-X-STREAM-INF:")) isMaster = true
                else if (trimmed.startsWith("#EXT-X-ENDLIST")) hasEndList = true
                else if (trimmed.contains("#EXT-X-PLAYLIST-TYPE:VOD", true)) isVodType = true
                else if (trimmed.startsWith("#EXTINF:")) {
                    val durStr = trimmed.removePrefix("#EXTINF:").substringBefore(',').trim()
                    totalDuration += durStr.toFloatOrNull() ?: 0f
                }
            }
            reader.close()
            conn.disconnect()

            if (!isValidM3u8) {
                return ProbeResult(false, currentUrlStr, headers, httpCode = code)
            }

            val isLive = !isMaster && !hasEndList && !isVodType
            ProbeResult(
                isPlayable = true,
                resolvedUrl = currentUrlStr,
                healedHeaders = headers,
                isLiveStream = isLive,
                isMasterPlaylist = isMaster,
                durationSeconds = totalDuration,
                manifestContent = sb.toString(),
                httpCode = code
            )
        } catch (_: Throwable) {
            runCatching { conn?.disconnect() }
            ProbeResult(false, currentUrlStr, headers, httpCode = -1)
        }
    }

    private fun extractOrigin(url: String): String? = try {
        val u = URI(url)
        if (!u.scheme.isNullOrBlank() && !u.host.isNullOrBlank()) "${u.scheme}://${u.host}" else null
    } catch (_: Throwable) { null }

    private fun extractHost(url: String): String? = try {
        URI(url).host?.lowercase()
    } catch (_: Throwable) { null }
}
