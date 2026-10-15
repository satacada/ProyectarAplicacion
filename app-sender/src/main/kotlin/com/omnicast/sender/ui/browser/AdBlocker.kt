package com.omnicast.sender.ui.browser

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlocker {

    private val KNOWN_MEDIA_HOSTS = hashSetOf(
        "ok.ru", "mycdn.me", "vk.com", "dailymotion.com", "youtube.com", "youtu.be",
        "yandex.com", "yandex.ru", "ya.ru", "dzen.ru", "yastatic.net",
        "streamwish.to", "filemoon.sx", "doodstream.com", "mixdrop.co", "vidmoly.me",
        "voe.sx", "vidoza.net", "streamtape.com", "mp4upload.com", "upstream.to",
        "waaw.to", "netu.tv", "vidguard.to", "uqload.com", "luluvdo.com",
        "streamlare.com", "wolfstream.tv", "streamcloud.club", "embedgram.com",
        "vidhide.com", "vidhidepro.com", "vidhidepre.com", "embed69.org",
        "pelismart.mov", "pelismart.com", "pelismart.online", "pelismart.vip", "pelismart.net", "pelismart.lat", "pelismart.app", "pelismart.org",
        "vidurl.to", "fembed.com", "fplayer.info", "swdyu.com",
        "doo.la", "ds2play.com", "ds2video.com"
    )

    fun init(context: android.content.Context) {
        AdBlockDatabase.init(context)
    }

    fun isSearchEngine(host: String?): Boolean {
        if (host == null) return false
        val h = host.lowercase()
        return h.contains("yandex.") || h.contains("ya.ru") || h.contains("dzen.ru") ||
               h.contains("google.") || h.contains("bing.") || h.contains("duckduckgo.")
    }

    fun isAd(url: String): Boolean {
        if (AdBlockWhitelistManager.isWhitelisted(url)) {
            val lower = url.lowercase()
            if (lower.contains("an.yandex.") || lower.contains("mc.yandex.")) return true
            return false
        }
        val lower = url.lowercase()
        // Permitir completamente Yandex (.com, .ru, ya.ru, yastatic.net, dzen.ru) salvo subdominios de anuncios/métricas
        if (lower.contains("yandex.") || lower.contains("ya.ru") || lower.contains("dzen.ru") || lower.contains("yastatic.net")) {
            if (lower.contains("an.yandex.") || lower.contains("mc.yandex.")) return true
            return false
        }
        val host = try { java.net.URI(url).host?.lowercase() } catch (_: Throwable) { null }
        if (AdBlockDatabase.isAdDomain(host)) return true
        if (AdBlockDatabase.matchesKeyword(lower)) return true
        return false
    }

    fun addAdDomain(domain: String) = AdBlockDatabase.get().addDomain(domain)
    fun addAdKeyword(keyword: String) = AdBlockDatabase.get().addKeyword(keyword)
    fun removeAdDomain(domain: String) = AdBlockDatabase.get().removeDomain(domain)

    fun isKnownMediaHost(host: String): Boolean {
        val lower = host.lowercase()
        for (m in KNOWN_MEDIA_HOSTS) {
            if (lower == m || lower.endsWith(".$m") || lower.contains(m.substringBefore("."))) return true
        }
        return lower.contains("embed") || lower.contains("player") || lower.contains("video") || lower.contains("stream") || lower.contains("cdn")
    }

    fun learnAndBlockAdHost(urlOrHost: String?): Boolean {
        if (urlOrHost.isNullOrBlank()) return false
        val host = if (urlOrHost.contains("://")) {
            try { java.net.URI(urlOrHost).host?.lowercase() } catch (_: Throwable) { null }
        } else {
            urlOrHost.lowercase().trim()
        } ?: return false
        if (host.isBlank() || host.length < 4 || !host.contains(".")) return false
        if (AdBlockWhitelistManager.isWhitelistedHost(host) || isSearchEngine(host) || isKnownMediaHost(host)) return false
        if (AdBlockDatabase.isAdDomain(host)) return false
        val added = addAdDomain(host)
        if (added) {
            android.util.Log.i("AdBlocker", "🧠 [Auto-Learning] Dominio publicitario aprendido por rechazo y guardado en SQLite: $host")
        }
        return added
    }

    fun isExternalRedirect(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("intent://") || lower.startsWith("market://") ||
               lower.startsWith("aliexpress://") || lower.startsWith("whatsapp://") ||
               lower.startsWith("tg://") || lower.startsWith("tel:") || lower.startsWith("mailto:")
    }

    fun createEmptyResponse(): WebResourceResponse =
        WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))

    fun getAdHidingScript(): String = """
        (function() {
            try {
                var h = (window.location.hostname || '').toLowerCase();
                if (h.indexOf('dailymotion.com') !== -1 || h.indexOf('youtube.com') !== -1 || h.indexOf('google.') !== -1 || h.indexOf('bing.') !== -1 || h.indexOf('yandex.') !== -1) {
                    return;
                }
                function cleanDoc(doc) {
                    if (!doc) return;
                    var selectors = [
                        'div[class*="ad-box"]', 'div[class*="ad-container"]', 'div[id*="ad-banner"]',
                        'div[class*="ad-banner"]', 'a[href*="bet365"]', 'a[href*="1xbet"]',
                        'div[class*="popunder"]', 'div[id*="popunder"]', '.ad-overlay', '#ad-overlay',
                        'div[class*="click-shield"]', 'div[class*="video-click-shield"]',
                        '.vp_layer_fake', 'div[class*="fake-player"]', 'div[class*="play-overlay"]'
                    ];
                    for (var s = 0; s < selectors.length; s++) {
                        try {
                            var found = doc.querySelectorAll(selectors[s]);
                            for (var f = 0; f < found.length; f++) {
                                var item = found[f];
                                if (!item.querySelector('video, iframe, input, form, textarea, select')) item.remove();
                            }
                        } catch(e) {}
                    }
                }

                function cleanAll() {
                    cleanDoc(document);
                    var iframes = document.querySelectorAll('iframe');
                    for (var k = 0; k < iframes.length; k++) {
                        try { cleanDoc(iframes[k].contentDocument); } catch(e) {}
                    }
                }

                cleanAll();

                if (window.__omniAdBlockInjected) return;
                window.__omniAdBlockInjected = true;

                // Neutralización reactiva en fase de captura del evento click:
                // Se evalúa únicamente el elemento tocado por el usuario, con costo de CPU 0 en reposo
                document.addEventListener('click', function(e) {
                    var el = e.target;
                    if (!el) return;
                    if (el.closest && el.closest('input, textarea, select, button, form, a, [contenteditable="true"], [role="search"], [role="textbox"], [role="combobox"]')) return;
                    if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.tagName === 'SELECT' || el.tagName === 'BUTTON' || el.tagName === 'FORM') return;
                    if (el !== document.body && el !== document.documentElement) {
                        var cs = window.getComputedStyle(el);
                        if (cs && (cs.position === 'fixed' || cs.position === 'absolute')) {
                            var z = parseInt(cs.zIndex, 10);
                            var rect = el.getBoundingClientRect();
                            var w = window.innerWidth || 360;
                            var h = window.innerHeight || 640;
                            if (!isNaN(z) && z >= 5 && rect.width >= w * 0.5 && rect.height >= h * 0.3) {
                                var op = parseFloat(cs.opacity);
                                var bg = cs.backgroundColor;
                                var isTrans = (bg === 'transparent' || bg === 'rgba(0, 0, 0, 0)' || bg === 'rgba(255, 255, 255, 0)');
                                var noText = !el.innerText || el.innerText.trim().length === 0;
                                if ((op <= 0.08 || (isTrans && noText)) && !el.querySelector('video, iframe, button, input')) {
                                    e.preventDefault(); e.stopPropagation(); e.stopImmediatePropagation();
                                    el.style.pointerEvents = 'none';
                                    el.remove();
                                    var under = document.elementFromPoint(e.clientX, e.clientY);
                                    if (under && under !== el) under.click();
                                    return false;
                                }
                            }
                        }
                    }
                    var cur = el;
                    while (cur && cur !== document.body) {
                        if (cur.tagName === 'A') {
                            var h = cur.getAttribute('href') || '';
                            if (h && (h.startsWith('http') || h.startsWith('//'))) {
                                var host = '';
                                try { host = new URL(h, window.location.href).hostname.toLowerCase(); } catch(err){}
                                if (host && host !== window.location.hostname && !host.endsWith(window.location.hostname)) {
                                    if (host.indexOf('aliexpress') !== -1 || host.indexOf('temu') !== -1 || host.indexOf('shein') !== -1 || host.indexOf('bet') !== -1 || host.indexOf('click') !== -1 || host.indexOf('track') !== -1 || host.indexOf('pop') !== -1) {
                                        e.preventDefault(); e.stopPropagation(); cur.remove(); return false;
                                    }
                                }
                            }
                        }
                        cur = cur.parentElement;
                    }
                }, true);
            } catch(e) {}
        })();
    """.trimIndent()
}
