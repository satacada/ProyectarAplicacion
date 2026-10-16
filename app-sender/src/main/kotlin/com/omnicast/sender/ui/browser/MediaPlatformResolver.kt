package com.omnicast.sender.ui.browser

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resuelve metadatos y flujos directos para plataformas de video web como Dailymotion.
 */
object MediaPlatformResolver {
    private const val TAG = "MediaPlatformResolver"
    private val scope = CoroutineScope(Dispatchers.IO)

    fun resolveDailymotion(
        url: String,
        onResolved: (streamUrl: String, title: String?, headers: Map<String, String>) -> Unit
    ) {
        val videoId = BrowserUrlHelper.extractDailymotionId(url) ?: return
        scope.launch {
            try {
                val apiUrl = "https://www.dailymotion.com/player/metadata/video/$videoId"
                val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", UA_MOBILE)
                    setRequestProperty("Referer", "https://www.dailymotion.com/")
                }
                if (conn.responseCode == 200) {
                    val cookieHeader = conn.headerFields["Set-Cookie"]?.joinToString("; ") { it.substringBefore(";") }
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val title = json.optString("title").takeIf { it.isNotBlank() }
                    val qualities = json.optJSONObject("qualities")
                    val autoArr = qualities?.optJSONArray("auto")
                    val streamUrl = autoArr?.optJSONObject(0)?.optString("url")
                    if (!streamUrl.isNullOrBlank()) {
                        val cookieMgr = runCatching { android.webkit.CookieManager.getInstance().getCookie("https://www.dailymotion.com") }.getOrNull()
                        val combinedCookie = listOfNotNull(cookieMgr, cookieHeader).filter { it.isNotBlank() }.joinToString("; ")
                        val hdrs = mutableMapOf(
                            "Referer" to "https://www.dailymotion.com/",
                            "Origin" to "https://www.dailymotion.com",
                            "User-Agent" to UA_MOBILE,
                            "Sec-Fetch-Mode" to "cors",
                            "Sec-Fetch-Dest" to "empty",
                            "Sec-Fetch-Site" to "cross-site"
                        )
                        if (combinedCookie.isNotBlank()) hdrs["Cookie"] = combinedCookie
                        var isReachable = true
                        try {
                            val testConn = (URL(streamUrl).openConnection() as HttpURLConnection).apply {
                                connectTimeout = 2500
                                readTimeout = 2500
                                requestMethod = "GET"
                                hdrs.forEach { (k, v) -> setRequestProperty(k, v) }
                            }
                            val rCode = testConn.responseCode
                            val isHtml = testConn.contentType?.lowercase()?.contains("text/html") == true
                            if (rCode !in 200..299 || isHtml) isReachable = false
                            testConn.disconnect()
                        } catch (_: Throwable) { isReachable = false }

                        if (isReachable) {
                            Log.i(TAG, "🎬 Dailymotion resuelto y verificado ($videoId): $title -> $streamUrl")
                            withContext(Dispatchers.Main) { onResolved(streamUrl, title, hdrs) }
                        } else {
                            Log.w(TAG, "⚠️ Dailymotion cdndirector protegido o bloqueado (403/HTML). Se usarán flujos directos CDN.")
                        }
                    }
                }
                conn.disconnect()
            } catch (e: Throwable) {
                com.omnicast.sender.log.SenderLogger.w(TAG, "No se pudo resolver Dailymotion vía API: ${e.message}", e)
            }
        }
    }

    fun checkPlatform(
        url: String?,
        title: String?,
        onYouTube: (ytUrl: String, cleanTitle: String?, headers: Map<String, String>) -> Unit,
        onDailymotion: (streamUrl: String, title: String?, headers: Map<String, String>) -> Unit
    ) {
        val u = url ?: return
        if (u.contains("youtube.com") || u.contains("youtu.be")) {
            val ytId = BrowserUrlHelper.extractYouTubeId(u) ?: return
            val cleanT = if (!title.isNullOrBlank() && title != "Transmisión") title.replace(" - YouTube", "") else null
            onYouTube("https://www.youtube.com/watch?v=$ytId", cleanT, mapOf("X-YouTube-Id" to ytId, "User-Agent" to UA_MOBILE))
        } else if (u.contains("dailymotion.com") || u.contains("dai.ly")) {
            resolveDailymotion(u, onDailymotion)
        }
    }

    fun extractFastreamId(url: String): String? {
        val lower = url.lowercase()
        if (!lower.contains("fastream.to") && !lower.contains("dropcdn.io") && !lower.contains("dr0pstream.com")) return null
        return Regex("(?:/hls\\d*/\\d+/\\d+/|/embed-|/)([a-zA-Z0-9]{10,12})(?:[_,.]|$)").find(url)?.groupValues?.get(1)
    }

    fun resolveFastreamSync(url: String, referer: String? = null): Triple<String, String?, Map<String, String>>? {
        val id = extractFastreamId(url) ?: return null
        return try {
            val isDrop = url.contains("dropcdn.io", true) || url.contains("dr0pstream.com", true)
            val embedUrl = if (isDrop) "https://dr0pstream.com/e/$id" else "https://fastream.to/embed-$id.html"
            val origin = if (isDrop) "https://dr0pstream.com" else "https://fastream.to"
            val conn = (URL(embedUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", UA_MOBILE)
                setRequestProperty("Referer", referer?.takeIf { it.isNotBlank() } ?: "https://reypelis.net/")
            }
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return null
            }
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val unpacked = unpackDeanEdwards(html) ?: html
            val streamUrlMatch = Regex("file:\\s*\"([^\"]+)\"").find(unpacked)
                ?: Regex("https?://[^\\s\"',]+\\.m3u8[^\\s\"',]*").find(unpacked)
            val streamUrl = streamUrlMatch?.groupValues?.get(1) ?: streamUrlMatch?.value ?: return null
            val cleanStreamUrl = BrowserUrlHelper.sanitizeMediaUrl(streamUrl)
            val hdrs = mapOf(
                "User-Agent" to UA_MOBILE,
                "Referer" to embedUrl,
                "Origin" to origin,
                "Sec-Fetch-Mode" to "cors",
                "Sec-Fetch-Dest" to "empty",
                "Sec-Fetch-Site" to "cross-site"
            )
            Log.i(TAG, "🎬 Fastream/DropCDN resuelto exitosamente: $id -> $cleanStreamUrl")
            Triple(cleanStreamUrl, null, hdrs)
        } catch (e: Throwable) {
            Log.w(TAG, "No se pudo resolver Fastream ($id): ${e.message}")
            null
        }
    }

    fun prepareStreamForPlayback(streamUrl: String, pageUrl: String, rawHeaders: Map<String, String>): Pair<String, Map<String, String>> {
        var finalUrl = BrowserUrlHelper.sanitizeMediaUrl(streamUrl)
        val hdrs = rawHeaders.toMutableMap()
        val lower = finalUrl.lowercase()
        val isDrop = lower.contains("dropcdn.io") || lower.contains("dr0pstream.com")
        val isFastream = lower.contains("fastream.to")
        if (isDrop || isFastream) {
            val id = extractFastreamId(finalUrl)
            if (id != null) {
                val embed = if (isDrop) "https://dr0pstream.com/e/$id" else "https://fastream.to/embed-$id.html"
                val origin = if (isDrop) "https://dr0pstream.com" else "https://fastream.to"
                hdrs["Referer"] = embed
                hdrs["Origin"] = origin
            }
        }
        val safeHeaders = BrowserUrlHelper.buildSafeHeaders(finalUrl, pageUrl, UA_MOBILE, hdrs)
        return Pair(finalUrl, safeHeaders)
    }

    fun unpackDeanEdwards(html: String): String? {
        val pos = html.indexOf(".split('|')")
        if (pos == -1) return null
        val start = html.lastIndexOf("eval(function(", pos)
        if (start == -1) return null
        val chunk = html.substring(start, pos + 20)
        val callPart = chunk.substringAfter("}(")
        val kEnd = callPart.lastIndexOf("'.split")
        if (kEnd == -1) return null
        val kStart = callPart.lastIndexOf('\'', kEnd - 1)
        if (kStart == -1) return null
        val kStr = callPart.substring(kStart + 1, kEnd)
        val numbersAndP = callPart.substring(0, kStart).trim().trimEnd(',')
        val matchNums = Regex(",\\s*(\\d+)\\s*,\\s*(\\d+)\\s*$").find(numbersAndP) ?: return null
        val a = matchNums.groupValues[1].toIntOrNull() ?: 36
        val c = matchNums.groupValues[2].toIntOrNull() ?: return null
        var p = numbersAndP.substring(0, matchNums.range.first).trim().trim('\'', '"', ' ')
        val kList = kStr.split('|')
        for (idx in c - 1 downTo 0) {
            if (idx < kList.size && kList[idx].isNotEmpty()) {
                val key = idx.toString(a)
                p = p.replace(Regex("\\b$key\\b")) { kList[idx] }
            }
        }
        return p
    }
}
