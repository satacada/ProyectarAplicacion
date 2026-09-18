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
        val label: String? = null
    )

    fun getInjectionScript(context: android.content.Context): String {
        if (cachedWvcScript == null) {
            val wvc = try {
                context.assets.open("wvc_sniffer.js").bufferedReader().use { it.readText() }
            } catch (_: Throwable) { "" }
            val fallbackHooks = WvcSnifferScripts.buildFallbackHooks(CONSOLE_PREFIX)
            cachedWvcScript = "$wvc\n$fallbackHooks"
        }
        return cachedWvcScript!!
    }

    fun parseConsoleMessage(message: String): List<SniffedVideo> {
        val list = mutableListOf<SniffedVideo>()
        try {
            val cleanMessage = if (message.startsWith(CONSOLE_PREFIX)) message.removePrefix(CONSOLE_PREFIX).trim() else message.trim()
            if (cleanMessage.contains("\"ibMessage\":") || cleanMessage.contains("\"type\":")) {
                val json = JSONObject(cleanMessage)
                when (json.optString("type")) {
                    "videoPlaying" -> {
                        val videoUrl = json.optString("video")
                        if (isValidMediaUrl(videoUrl)) {
                            list.add(
                                SniffedVideo(
                                    src = videoUrl,
                                    title = json.optString("title", "Video en Reproducción"),
                                    poster = json.optString("poster", ""),
                                    isPlaying = true
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
                                val poster = item.optString("poster", "")
                                val itemTitle = item.optString("title").takeIf { it.isNotBlank() } ?: pageTitle
                                val sources = item.optJSONArray("sources")
                                if (sources != null) {
                                    for (j in 0 until sources.length()) {
                                        if (list.size >= 25) break
                                        val sObj = sources.optJSONObject(j) ?: continue
                                        val sUrl = sObj.optString("source")
                                        val label = sObj.optString("label", "Calidad Original")
                                        if (isValidMediaUrl(sUrl)) {
                                            list.add(SniffedVideo(sUrl, itemTitle, poster, false, label))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "video" -> {
                        val src = json.optString("src")
                        if (isValidMediaUrl(src)) {
                            list.add(SniffedVideo(src, json.optString("title", "Transmisión Web"), json.optString("poster", "")))
                        }
                    }
                }
            } else if (message.startsWith(CONSOLE_PREFIX)) {
                val jsonStr = message.removePrefix(CONSOLE_PREFIX).trim()
                val json = JSONObject(jsonStr)
                val src = json.optString("src")
                if (isValidMediaUrl(src)) {
                    list.add(SniffedVideo(src, json.optString("title", "Transmisión Web"), json.optString("poster", "")))
                }
            }
        } catch (_: Throwable) {}
        return list
    }

    fun isValidMediaUrl(url: String?): Boolean {
        if (url.isNullOrBlank() || url.startsWith("blob:") || url.startsWith("data:")) return false
        val lower = url.lowercase()
        if (AdBlocker.isAd(url)) return false
        if (lower.contains("stub.mp4") || lower.contains("dummy.mp4") || lower.contains("blank.mp4") || lower.contains("ad.mp4")) return false
        if (lower.endsWith(".html") || lower.endsWith(".htm") || lower.contains(".html?") || lower.contains(".htm?")) return false
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".svg") || lower.endsWith(".ico") || lower.contains(".jpg?") || lower.contains(".jpeg?") || lower.contains(".png?") || lower.contains(".webp?")) return false
        if (lower.endsWith(".css") || lower.endsWith(".js") || lower.endsWith(".woff") || lower.endsWith(".woff2") || lower.endsWith(".ttf") || lower.contains(".css?") || lower.contains(".js?")) return false
        if (lower.contains("i.mycdn.me") || lower.contains("st.mycdn.me") || lower.contains("static.mycdn.me")) return false
        if (lower.contains(".m3u8") || lower.contains(".mp4") || lower.contains(".mpd") || lower.contains(".webm") || lower.contains(".mkv")) return true
        if (lower.contains("/hls/") || lower.contains("/stream/") || lower.contains("/manifest") || lower.contains("chunklist")) return true
        if (lower.contains("morencius.com") || lower.contains("swdyu.com") || lower.contains("cloudemb.com")) return true
        if (lower.contains("vd.mycdn.me") || lower.contains("vd2.mycdn.me") || lower.contains("vd3.mycdn.me") || lower.contains("vd4.mycdn.me") || (lower.contains("mycdn.me") && (lower.contains("/video/") || lower.contains("cmd=video") || lower.contains("sig=")))) return true
        if (lower.contains("ok.ru") && (lower.contains("videometa") || lower.contains("api/video") || lower.contains("cmd=video"))) return true
        return false
    }
}
