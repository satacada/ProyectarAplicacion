package com.omnicast.sender.ui.browser

import org.json.JSONObject

/**
 * Motor de análisis DOM avanzado estilo Web Video Caster.
 * Intercepta <video>, fetch, XHR, iframes y reproductores Pelismart/Vidhide/Embed69/ok.ru.
 */
object WvcDomSniffer {

    const val CONSOLE_PREFIX = "OMNICAST_STREAM:"
    private var cachedWvcScript: String? = null

    data class SniffedVideo(
        val src: String,
        val title: String,
        val poster: String,
        val isPlaying: Boolean = false,
        val label: String? = null,
        val duration: Float = 0f
    )

    fun getInjectionScript(context: android.content.Context): String =
        DynamicSnifferManager.getInjectionScript(context)

    fun parseConsoleMessage(message: String): List<SniffedVideo> {
        val list = mutableListOf<SniffedVideo>()
        try {
            val cleanMessage = if (message.startsWith(CONSOLE_PREFIX)) message.removePrefix(CONSOLE_PREFIX).trim() else message.trim()
            if (cleanMessage.contains("\"ibMessage\":") || cleanMessage.contains("\"type\":")) {
                val json = JSONObject(cleanMessage)
                when (json.optString("type")) {
                    "videoPlaying" -> {
                        val videoUrl = json.optString("video")
                        val dur = json.optDouble("duration", 0.0).toFloat()
                        if (dur in 0.1f..59.9f) return emptyList()
                        if (isValidMediaUrl(videoUrl)) {
                            list.add(
                                SniffedVideo(
                                    src = videoUrl,
                                    title = json.optString("title", "Video en Reproducción"),
                                    poster = json.optString("poster", ""),
                                    isPlaying = true,
                                    duration = dur
                                )
                            )
                        }
                    }
                    "videoList" -> {
                        val pageTitle = json.optString("title", "Transmisión Web")
                        val vList = json.optJSONArray("videoList")
                        if (vList != null) {
                            for (i in 0 until vList.length()) {
                                if (list.size >= 25) break
                                val item = vList.optJSONObject(i) ?: continue
                                val itemDur = item.optDouble("duration", 0.0).toFloat()
                                if (itemDur in 0.1f..59.9f) continue
                                val poster = item.optString("poster", "")
                                val itemTitle = item.optString("title").takeIf { it.isNotBlank() } ?: pageTitle
                                val sources = item.optJSONArray("sources")
                                if (sources != null) {
                                    for (j in 0 until sources.length()) {
                                        if (list.size >= 25) break
                                        val sObj = sources.optJSONObject(j) ?: continue
                                        val sDur = sObj.optDouble("duration", itemDur.toDouble()).toFloat()
                                        if (sDur in 0.1f..59.9f) continue
                                        val sUrl = sObj.optString("source")
                                        val label = sObj.optString("label", "Calidad Original")
                                        if (isValidMediaUrl(sUrl)) {
                                            list.add(SniffedVideo(sUrl, itemTitle, poster, false, label, sDur))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "video" -> {
                        val src = json.optString("src")
                        val dur = json.optDouble("duration", 0.0).toFloat()
                        if (dur in 0.1f..59.9f) return emptyList()
                        if (isValidMediaUrl(src)) {
                            list.add(SniffedVideo(src, json.optString("title", "Transmisión Web"), json.optString("poster", ""), duration = dur))
                        }
                    }
                }
            } else if (message.startsWith(CONSOLE_PREFIX)) {
                val jsonStr = message.removePrefix(CONSOLE_PREFIX).trim()
                val json = JSONObject(jsonStr)
                val src = json.optString("src")
                val dur = json.optDouble("duration", 0.0).toFloat()
                if (dur in 0.1f..59.9f) return emptyList()
                if (isValidMediaUrl(src)) {
                    list.add(SniffedVideo(src, json.optString("title", "Transmisión Web"), json.optString("poster", ""), duration = dur))
                }
            }
        } catch (_: Throwable) {}
        return list
    }

    fun isValidMediaUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val l = url.lowercase()
        if (l.contains("dailymotion.com/video/") || l.contains("dai.ly/") || l.contains("/embed/")) return false
        if (AdBlocker.isAd(url)) return false
        return BrowserUrlHelper.isPlayableStream(url)
    }
}
