package com.omnicast.sender.ui.browser

import org.json.JSONObject

/**
 * Motor de análisis DOM avanzado estilo Web Video Caster.
 * Intercepta <video>, fetch, XHR y setters de HTMLMediaElement para capturar ok.ru y sitios protegidos.
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

            val fallbackHooks = """
                (function() {
                    try {
                        function reportVid(v) {
                            var s = v.currentSrc || v.src;
                            if (s && s.indexOf('blob:') !== 0 && s.indexOf('data:') !== 0) {
                                console.log('$CONSOLE_PREFIX' + JSON.stringify({type:'videoPlaying', video:s, title:document.title}));
                            }
                        }
                        document.addEventListener('play', function(e) { if (e.target && e.target.tagName === 'VIDEO') reportVid(e.target); }, true);
                        document.addEventListener('playing', function(e) { if (e.target && e.target.tagName === 'VIDEO') reportVid(e.target); }, true);
                        var origFetch = window.fetch;
                        if (origFetch) {
                            window.fetch = function() {
                                var u = arguments[0];
                                if (typeof u === 'string' && (u.indexOf('.m3u8') !== -1 || u.indexOf('.mp4') !== -1 || u.indexOf('.mpd') !== -1 || u.indexOf('mycdn') !== -1 || u.indexOf('/hls/') !== -1)) {
                                    console.log('$CONSOLE_PREFIX' + JSON.stringify({type:'video', src:u, title:document.title}));
                                }
                                return origFetch.apply(this, arguments);
                            };
                        }
                        var origOpen = XMLHttpRequest.prototype.open;
                        if (origOpen) {
                            XMLHttpRequest.prototype.open = function(m, u) {
                                if (typeof u === 'string' && (u.indexOf('.m3u8') !== -1 || u.indexOf('.mp4') !== -1 || u.indexOf('.mpd') !== -1 || u.indexOf('mycdn') !== -1 || u.indexOf('/hls/') !== -1)) {
                                    console.log('$CONSOLE_PREFIX' + JSON.stringify({type:'video', src:u, title:document.title}));
                                }
                                return origOpen.apply(this, arguments);
                            };
                        }
                    } catch(e) {}
                })();
            """.trimIndent()

            cachedWvcScript = "$wvc\n$fallbackHooks"
        }
        return cachedWvcScript!!
    }

    fun parseConsoleMessage(message: String): List<SniffedVideo> {
        val list = mutableListOf<SniffedVideo>()
        try {
            if (message.contains("\"ibMessage\":") || message.contains("\"type\":")) {
                val json = JSONObject(message)
                val type = json.optString("type")
                when (type) {
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
                        if (vList != null && vList.length() <= 2) {
                            for (i in 0 until vList.length()) {
                                val item = vList.optJSONObject(i) ?: continue
                                val poster = item.optString("poster", "")
                                val itemTitle = item.optString("title").takeIf { it.isNotBlank() } ?: pageTitle
                                val sources = item.optJSONArray("sources")
                                if (sources != null) {
                                    for (j in 0 until sources.length()) {
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
                }
            } else if (message.startsWith(CONSOLE_PREFIX)) {
                val jsonStr = message.removePrefix(CONSOLE_PREFIX)
                val json = JSONObject(jsonStr)
                val src = json.optString("src")
                if (isValidMediaUrl(src)) {
                    list.add(SniffedVideo(src, json.optString("title", "Transmisión Web"), json.optString("poster", "")))
                }
            }
        } catch (_: Throwable) {}
        return list
    }

    private fun isValidMediaUrl(url: String?): Boolean {
        if (url.isNullOrBlank() || url.startsWith("blob:") || url.startsWith("data:")) return false
        val lower = url.lowercase()
        if (lower.contains("stub.mp4") || lower.contains("dummy.mp4") || lower.contains("blank.mp4")) return false
        if (lower.endsWith(".html") || lower.endsWith(".htm") || lower.contains(".html?") || lower.contains(".htm?")) return false
        return lower.contains(".m3u8") || lower.contains(".mp4") || lower.contains(".mpd") || lower.contains(".webm") || lower.contains(".mkv") || lower.contains("mycdn.me") || lower.contains("/hls/") || lower.contains("/stream/") || lower.contains("/manifest")
    }
}
