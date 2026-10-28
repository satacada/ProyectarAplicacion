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
        "doo.la", "ds2play.com", "ds2video.com", "fastream.to", "dropcdn.io", "dr0pstream.com"
    )

    fun init(context: android.content.Context) {
        AdBlockDatabase.init(context)
    }

    fun isSearchEngine(host: String?): Boolean {
        if (host == null) return false
        val h = host.lowercase()
        return h.contains("google.") || h.contains("yandex.") || h.contains("ya.ru") || h.contains("dzen.ru") ||
               h.contains("bing.") || h.contains("msn.") || h.contains("duckduckgo.") || h.contains("duck.com") ||
               h.contains("yahoo.") || h.contains("brave.com") || h.contains("ecosia.") || h.contains("startpage.") ||
               h.contains("qwant.") || h.contains("baidu.") || h.contains("naver.") || h.contains("ask.com") ||
               h.startsWith("search.")
    }

    fun isVerificationOrCaptcha(urlOrHost: String?): Boolean {
        if (urlOrHost.isNullOrBlank()) return false
        val h = urlOrHost.lowercase()
        return h.contains("cloudflare") || h.contains("turnstile") ||
               h.contains("challenges") || h.contains("hcaptcha") ||
               h.contains("recaptcha") || h.contains("captcha")
    }

    fun isVideoAd(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("quickcore.fun") || lower.contains("preroll") ||
               lower.contains("ad-delivery") || lower.contains("/ads/") ||
               lower.contains("imasdk") || lower.contains("ad.mp4") ||
               lower.contains("vast") || lower.contains("spotx") ||
               lower.contains("doubleclick") || lower.contains("holdbitter") ||
               lower.contains("/gambling/") || lower.contains("bonus-") ||
               lower.contains("subduepaler") || lower.contains("/lp/v/") ||
               lower.contains("adsterra") || lower.contains("propeller") ||
               lower.contains("exoclick") || lower.contains("trafficjunky") ||
               lower.contains("clickadu") || lower.contains("hilltopads") ||
               lower.contains("banner-ads") || lower.contains("player-banner") ||
               lower.contains("luugy.com") || lower.contains("frs2c.com")
    }

    private val l1HostCache = android.util.LruCache<String, Boolean>(1024)

    fun isAd(url: String): Boolean {
        if (isVerificationOrCaptcha(url)) return false
        if (isVideoAd(url)) return true
        val host = try { java.net.URI(url).host?.lowercase() } catch (_: Throwable) { null }
        if (host != null) {
            val cached = l1HostCache.get(host)
            if (cached != null) return cached
        }
        val isBlocked = evaluateIsAd(url, host)
        if (host != null) l1HostCache.put(host, isBlocked)
        return isBlocked
    }

    private fun evaluateIsAd(url: String, host: String?): Boolean {
        if (AdBlockWhitelistManager.isWhitelisted(url)) {
            val lower = url.lowercase()
            return lower.contains("an.yandex.") || lower.contains("mc.yandex.")
        }
        val lower = url.lowercase()
        if (lower.contains("yandex.") || lower.contains("ya.ru") || lower.contains("dzen.ru") || lower.contains("yastatic.net")) {
            return lower.contains("an.yandex.") || lower.contains("mc.yandex.")
        }
        if (AdBlockDatabase.isAdDomain(host)) return true
        if (AdBlockDatabase.matchesKeyword(lower)) return true
        return false
    }

    fun addAdDomain(domain: String): Boolean { l1HostCache.evictAll(); return AdBlockDatabase.get().addDomain(domain) }
    fun addAdKeyword(keyword: String): Boolean { l1HostCache.evictAll(); return AdBlockDatabase.get().addKeyword(keyword) }
    fun removeAdDomain(domain: String): Boolean { l1HostCache.evictAll(); return AdBlockDatabase.get().removeDomain(domain) }

    fun isKnownMediaHost(host: String): Boolean {
        val lower = host.lowercase()
        for (m in KNOWN_MEDIA_HOSTS) {
            if (lower == m || lower.endsWith(".$m") || lower.contains(m.substringBefore("."))) return true
        }
        return lower.contains("embed") || lower.contains("player") || lower.contains("video") || lower.contains("stream") || lower.contains("cdn")
    }

    fun learnAndBlockAdHost(urlOrHost: String?, currentMovieHost: String? = null): Boolean {
        if (urlOrHost.isNullOrBlank()) return false
        val host = if (urlOrHost.contains("://")) {
            try { java.net.URI(urlOrHost).host?.lowercase() } catch (_: Throwable) { null }
        } else {
            urlOrHost.lowercase().trim()
        } ?: return false
        if (host.isBlank() || host.length < 4 || !host.contains(".")) return false
        if (currentMovieHost != null) {
            val cur = currentMovieHost.lowercase().trim()
            if (host == cur || host.endsWith(".$cur") || cur.endsWith(".$host")) return false
        }
        if (isVerificationOrCaptcha(host)) return false
        if (AdBlockWhitelistManager.isWhitelistedHost(host) || isSearchEngine(host) || isKnownMediaHost(host)) return false
        if (host.contains("movie") || host.contains("pelis") || host.contains("cine") || host.contains("series") || host.contains("film") || host.contains("stream")) return false
        if (host.endsWith("reypelis.net") || host.endsWith("tioplus.app") || host.contains("sololatino") || host.contains("pelisserieshoy")) return false
        val hasAdPattern = host.contains("ad") || host.contains("pop") || host.contains("click") ||
            host.contains("track") || host.contains("traffic") || host.contains("banner") ||
            host.contains("monet") || host.contains("syndic") || host.contains("bet") ||
            host.contains("promo") || host.contains("yield") || host.contains("affil")
        if (!hasAdPattern && !AdBlockDatabase.isAdDomain(host)) return false
        if (AdBlockDatabase.isAdDomain(host)) return false
        val added = addAdDomain(host)
        if (added) {
            android.util.Log.i("AdBlocker", "🧠 [Auto-Learning] Dominio publicitario emergente guardado en SQLite: $host")
            AdBlockRuleSyncManager.reportLearnedDomainToCloud(host, currentMovieHost)
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
                    try {
                        var css = 'div[class*="popunder"], div[id*="popunder"], .ad-overlay, #ad-overlay, .click-shield, div[class*="click-shield"], div[class*="player-banner"], div[id*="ad_wrapper"], div[class*="vast-"], div[class*="banner-ads"] { display: none !important; pointer-events: none !important; }';
                        var st = doc.createElement('style');
                        st.textContent = css;
                        (doc.head || doc.documentElement).appendChild(st);
                    } catch(__) {}
                    var selectors = [
                        'div[class*="ad-box"]', 'div[class*="ad-container"]', 'div[id*="ad-banner"]',
                        'div[class*="ad-banner"]', 'a[href*="bet365"]', 'a[href*="1xbet"]',
                        'div[class*="popunder"]', 'div[id*="popunder"]', '.ad-overlay', '#ad-overlay',
                        'div[class*="click-shield"]', 'div[class*="video-click-shield"]',
                        'div[class*="player-banner"]', 'div[id*="ad_wrapper"]', 'div[class*="vast-"]',
                        'div[class*="banner-ads"]', 'div[id*="banner-ads"]',
                        '.vp_layer_fake', 'div[class*="fake-player"]'
                    ];
                    for (var s = 0; s < selectors.length; s++) {
                        try {
                            var found = doc.querySelectorAll(selectors[s]);
                            for (var f = 0; f < found.length; f++) {
                                var item = found[f];
                                if (!item.querySelector('video, iframe, input, form, textarea, select, button, a')) item.remove();
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
                // Solo neutraliza velos transparentes que no sean botones ni enlaces
                document.addEventListener('click', function(e) {
                    var el = e.target;
                    if (!el) return;
                    if (el.closest && el.closest('input, textarea, select, button, form, [role="button"], [contenteditable="true"], [role="search"], [role="textbox"], [role="combobox"]')) return;
                    if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.tagName === 'SELECT' || el.tagName === 'FORM' || el.tagName === 'BUTTON') return;
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
                                if ((op <= 0.08 || (isTrans && noText)) && !el.querySelector('video, iframe, button, input, a')) {
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
                }, true);
            } catch(e) {}
        })();
    """.trimIndent()
}
