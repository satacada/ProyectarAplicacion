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
        val duration: Float = 0f,
        val frameUrl: String = "",
        val subtitles: List<com.omnicast.core.protocol.SignalingMessage.SubtitleTrack> = emptyList()
    )

    fun getInjectionScript(context: android.content.Context): String =
        DynamicSnifferManager.getInjectionScript(context)

    private fun parseTextTracks(json: JSONObject?): List<com.omnicast.core.protocol.SignalingMessage.SubtitleTrack> {
        if (json == null) return emptyList()
        val arr = json.optJSONArray("textTracks") ?: return emptyList()
        val res = mutableListOf<com.omnicast.core.protocol.SignalingMessage.SubtitleTrack>()
        for (i in 0 until arr.length()) {
            val t = arr.optJSONObject(i) ?: continue
            val url = t.optString("url").ifBlank { t.optString("src").ifBlank { t.optString("file") } }
            if (url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))) {
                val lang = t.optString("lang").ifBlank { t.optString("label").takeIf { it.isNotBlank() } }
                res.add(com.omnicast.core.protocol.SignalingMessage.SubtitleTrack(url = url, lang = lang))
            }
        }
        return res
    }

    fun parseConsoleMessage(message: String): List<SniffedVideo> {
        val list = mutableListOf<SniffedVideo>()
        try {
            val cleanMessage = if (message.startsWith(CONSOLE_PREFIX)) message.removePrefix(CONSOLE_PREFIX).trim() else message.trim()
            if (cleanMessage.contains("\"ibMessage\":") || cleanMessage.contains("\"type\":")) {
                val json = JSONObject(cleanMessage)
                val frameUrl = json.optString("from").ifBlank { json.optString("frameUrl", "") }
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
                                    duration = dur,
                                    frameUrl = frameUrl,
                                    subtitles = parseTextTracks(json)
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
                                val itemSubs = parseTextTracks(item)
                                val sources = item.optJSONArray("sources")
                                if (sources != null) {
                                    for (j in 0 until sources.length()) {
                                        if (list.size >= 25) break
                                        val sObj = sources.optJSONObject(j) ?: continue
                                        val sDur = sObj.optDouble("duration", itemDur.toDouble()).toFloat()
                                        if (sDur in 0.1f..59.9f) continue
                                        val sUrl = sObj.optString("source")
                                        val label = sObj.optString("label", "Calidad Original")
                                        val sSubs = parseTextTracks(sObj).ifEmpty { itemSubs }
                                        if (isValidMediaUrl(sUrl)) {
                                            list.add(SniffedVideo(sUrl, itemTitle, poster, false, label, sDur, frameUrl, sSubs))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "video" -> {
                        val src = json.optString("src")
                        val dur = json.optDouble("duration", 0.0).toFloat()
                        val lbl = json.optString("label").takeIf { it.isNotBlank() }
                        if (dur in 0.1f..59.9f) return emptyList()
                        if (isValidMediaUrl(src)) {
                            list.add(SniffedVideo(src, json.optString("title", "Transmisión Web"), json.optString("poster", ""), duration = dur, label = lbl, frameUrl = frameUrl, subtitles = parseTextTracks(json)))
                        }
                    }
                }
            } else if (message.startsWith(CONSOLE_PREFIX)) {
                val jsonStr = message.removePrefix(CONSOLE_PREFIX).trim()
                val json = JSONObject(jsonStr)
                val src = json.optString("src")
                val dur = json.optDouble("duration", 0.0).toFloat()
                val lbl = json.optString("label").takeIf { it.isNotBlank() }
                val frameUrl = json.optString("from").ifBlank { json.optString("frameUrl", "") }
                if (dur in 0.1f..59.9f) return emptyList()
                if (isValidMediaUrl(src)) {
                    list.add(SniffedVideo(src, json.optString("title", "Transmisión Web"), json.optString("poster", ""), duration = dur, label = lbl, frameUrl = frameUrl, subtitles = parseTextTracks(json)))
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
