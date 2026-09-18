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
                        function isMediaStr(u) {
                            if (!u || typeof u !== 'string') return false;
                            var l = u.toLowerCase();
                            if (l.indexOf('.jpg') !== -1 || l.indexOf('.png') !== -1 || l.indexOf('.webp') !== -1 || l.indexOf('i.mycdn.me') !== -1 || l.indexOf('st.mycdn.me') !== -1) return false;
                            return l.indexOf('.m3u8') !== -1 || l.indexOf('.mp4') !== -1 || l.indexOf('.mpd') !== -1 || l.indexOf('vd.mycdn') !== -1 || l.indexOf('/hls/') !== -1 || l.indexOf('cmd=video') !== -1;
                        }
                        function reportVid(v) {
                            if (!v) return;
                            var s = v.currentSrc || v.src;
                            if (s && s.indexOf('blob:') !== 0 && s.indexOf('data:') !== 0) {
                                console.log('$CONSOLE_PREFIX' + JSON.stringify({type:'videoPlaying', video:s, title:document.title}));
                            }
                        }
                        var vids = document.getElementsByTagName('video');
                        for (var vi = 0; vi < vids.length; vi++) { reportVid(vids[vi]); }
                        document.addEventListener('play', function(e) { if (e.target && e.target.tagName === 'VIDEO') reportVid(e.target); }, true);
                        document.addEventListener('playing', function(e) { if (e.target && e.target.tagName === 'VIDEO') reportVid(e.target); }, true);
                        var origFetch = window.fetch;
                        if (origFetch) {
                            window.fetch = function() {
                                var a = arguments[0];
                                var u = (typeof a === 'string') ? a : (a && a.url ? a.url : '');
                                if (isMediaStr(u)) {
                                    console.log('$CONSOLE_PREFIX' + JSON.stringify({type:'video', src:u, title:document.title}));
                                }
                                return origFetch.apply(this, arguments);
                            };
                        }
                        var origOpen = XMLHttpRequest.prototype.open;
                        if (origOpen) {
                            XMLHttpRequest.prototype.open = function(m, u) {
                                if (isMediaStr(u)) {
                                    console.log('$CONSOLE_PREFIX' + JSON.stringify({type:'video', src:u, title:document.title}));
                                }
                                return origOpen.apply(this, arguments);
                            };
                        }
                        var origSrcDescriptor = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'src');
                        if (origSrcDescriptor && origSrcDescriptor.set) {
                            var origSet = origSrcDescriptor.set;
                            Object.defineProperty(HTMLMediaElement.prototype, 'src', {
                                set: function(val) {
                                    if (typeof val === 'string' && val.indexOf('blob:') !== 0 && val.indexOf('data:') !== 0) {
                                        console.log('$CONSOLE_PREFIX' + JSON.stringify({type:'videoPlaying', video:val, title:document.title}));
                                    }
                                    return origSet.call(this, val);
                                }
                            });
                        }
                        function sniffOkRuDom() {
                            try {
                                var elms = document.querySelectorAll('[data-options*="metadata"], [data-module="OKVideo"], div.vp-layer');
                                for (var i = 0; i < elms.length; i++) {
                                    var raw = elms[i].getAttribute('data-options');
                                    if (!raw) continue;
                                    try {
                                        var p = JSON.parse(raw);
                                        var meta = (p.flashvars && p.flashvars.metadata) ? p.flashvars.metadata : null;
                                        if (typeof meta === 'string') { try { meta = JSON.parse(meta); } catch(e) {} }
                                        if (meta) {
                                            var t = (meta.movie && meta.movie.title) ? meta.movie.title : document.title;
                                            if (meta.hlsManifestUrl) {
                                                console.log('$CONSOLE_PREFIX' + JSON.stringify({type:'video', src: meta.hlsManifestUrl, title: t}));
                                            }
                                            if (meta.videos && meta.videos.length > 0) {
                                                for (var v = 0; v < meta.videos.length; v++) {
                                                    if (meta.videos[v].url) {
                                                        console.log('$CONSOLE_PREFIX' + JSON.stringify({type:'video', src: meta.videos[v].url, title: t}));
                                                    }
                                                }
                                            }
                                        }
                                    } catch(e) {}
                                }
                            } catch(e) {}
                        }
                        sniffOkRuDom();
                        setTimeout(sniffOkRuDom, 1500);
                        setTimeout(sniffOkRuDom, 3500);
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
            val cleanMessage = if (message.startsWith(CONSOLE_PREFIX)) message.removePrefix(CONSOLE_PREFIX).trim() else message.trim()
            if (cleanMessage.contains("\"ibMessage\":") || cleanMessage.contains("\"type\":")) {
                val json = JSONObject(cleanMessage)
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

    private fun isValidMediaUrl(url: String?): Boolean {
        if (url.isNullOrBlank() || url.startsWith("blob:") || url.startsWith("data:")) return false
        val lower = url.lowercase()
        if (lower.contains("stub.mp4") || lower.contains("dummy.mp4") || lower.contains("blank.mp4")) return false
        if (lower.endsWith(".html") || lower.endsWith(".htm") || lower.contains(".html?") || lower.contains(".htm?")) return false
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".svg") || lower.endsWith(".ico") || lower.contains(".jpg?") || lower.contains(".jpeg?") || lower.contains(".png?") || lower.contains(".webp?")) return false
        if (lower.endsWith(".css") || lower.endsWith(".js") || lower.endsWith(".woff") || lower.endsWith(".woff2") || lower.endsWith(".ttf") || lower.contains(".css?") || lower.contains(".js?")) return false
        if (lower.contains("i.mycdn.me") || lower.contains("st.mycdn.me") || lower.contains("static.mycdn.me")) return false
        if (lower.contains(".m3u8") || lower.contains(".mp4") || lower.contains(".mpd") || lower.contains(".webm") || lower.contains(".mkv")) return true
        if (lower.contains("/hls/") || lower.contains("/stream/") || lower.contains("/manifest")) return true
        if (lower.contains("vd.mycdn.me") || lower.contains("vd2.mycdn.me") || lower.contains("vd3.mycdn.me") || lower.contains("vd4.mycdn.me") || (lower.contains("mycdn.me") && (lower.contains("/video/") || lower.contains("cmd=video") || lower.contains("sig=")))) return true
        if (lower.contains("ok.ru") && (lower.contains("videometa") || lower.contains("api/video") || lower.contains("cmd=video"))) return true
        return false
    }
}
