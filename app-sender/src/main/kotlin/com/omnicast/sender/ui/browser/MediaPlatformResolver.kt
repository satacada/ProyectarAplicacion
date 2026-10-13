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
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val title = json.optString("title").takeIf { it.isNotBlank() }
                    val qualities = json.optJSONObject("qualities")
                    val autoArr = qualities?.optJSONArray("auto")
                    val streamUrl = autoArr?.optJSONObject(0)?.optString("url")
                    if (!streamUrl.isNullOrBlank()) {
                        val hdrs = mapOf(
                            "Referer" to "https://www.dailymotion.com/",
                            "Origin" to "https://www.dailymotion.com",
                            "User-Agent" to UA_MOBILE,
                            "Sec-Fetch-Mode" to "cors",
                            "Sec-Fetch-Dest" to "empty",
                            "Sec-Fetch-Site" to "cross-site"
                        )
                        Log.i(TAG, "🎬 Dailymotion resuelto con éxito ($videoId): $title -> $streamUrl")
                        withContext(Dispatchers.Main) {
                            onResolved(streamUrl, title, hdrs)
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
}
