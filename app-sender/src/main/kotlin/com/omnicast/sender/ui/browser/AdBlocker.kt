package com.omnicast.sender.ui.browser

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlocker {

    private val AD_DOMAINS = hashSetOf(
        "doubleclick.net", "googleads.g.doubleclick.net", "googlesyndication.com",
        "adservice.google.com", "adnxs.com", "adsterra.com", "popads.net",
        "propellerads.com", "exoclick.com", "juicyads.com", "trafficfactory.biz",
        "onclickmega.com", "onclickalgo.com", "monetag.com", "adsco.re", "histats.com",
        "adcolony.com", "inmobi.com", "taboola.com", "outbrain.com", "mgid.com",
        "clksite.com", "hilltopads.com", "a-ads.com", "adtrue.com", "richaudience.com",
        "creativecdn.com", "adkernel.com", "smartadserver.com", "rubiconproject.com",
        "pubmatic.com", "openx.net", "criteo.com", "contextweb.com", "casalemedia.com",
        "popcash.net", "adcash.com", "alwingulla.com", "amung.us", "yllix.com",
        "zeroredirect.com", "ptads.com", "syndication.exoclick.com", "eclkmpbn.com",
        "admob.com", "applovin.com", "unity3d.ads", "vungle.com", "chartboost.com",
        "bet365.com", "1xbet.com", "melbet.com", "betano.com", "stake.com", "wplay.co",
        "playdoit.mx", "caliente.mx", "rushbet.co", "betsson.com", "coolbet.com",
        "ad-maven.com", "admaven.com", "clickadu.com", "deloton.com", "dolohen.com",
        "evadav.com", "galaksion.com", "streamcrypt.net", "trafficstars.com",
        "shein.com", "shein.top", "temu.com", "aliexpress.com", "shopee.com", "lazada.com",
        "amazon.com", "ebay.com", "alibaba.com", "sh.st", "ouo.io", "linkvertise.com",
        "tsyndicate.com", "popmyads.com", "directrev.com", "revenuehits.com",
        "giggle.com", "giggles.com", "giggleclick.com", "giggletrack.com", "giggleredirect.com",
        "cat-bolt.com", "imasdk.googleapis.com", "securepubads.g.doubleclick.net",
        "pagead2.googlesyndication.com", "ad-delivery.net", "adkeeper.com", "trafficjunky.com",
        "whocares.club", "pemsrv.com", "gdtcdn.com", "mc.yandex.ru", "yandex.ru", "luugy.com",
        "google-analytics.com", "googletagmanager.com"
    )

    private val AD_KEYWORDS = listOf(
        "/ads/", "/banner/", "adsystem", "advertisement", "popunder",
        "popup.js", "floating_ad", "adserver", "ads.js", "tracker.js", "/affiliate/",
        "zoneid=", "iclick-v", "ad_unit", "promoted", "sponsor", "partnerize", "tradedoubler",
        "s.click.aliexpress", "c.lazada", "shp.ee", "aff_c", "clickalgo", "giggle",
        "/creatives/", "/creative/", "cat-bolt", "imasdk", "securepubads",
        "preroll", "pre-roll", "midroll", "postroll", "/vast", "/vpaid", "ad-delivery",
        "clmap", "metrika", "analytics"
    )

    private val KNOWN_MEDIA_HOSTS = hashSetOf(
        "ok.ru", "mycdn.me", "vk.com", "dailymotion.com", "youtube.com", "youtu.be",
        "streamwish.to", "filemoon.sx", "doodstream.com", "mixdrop.co", "vidmoly.me",
        "voe.sx", "vidoza.net", "streamtape.com", "mp4upload.com", "upstream.to",
        "waaw.to", "netu.tv", "vidguard.to", "uqload.com", "luluvdo.com",
        "streamlare.com", "wolfstream.tv", "streamcloud.club", "embedgram.com",
        "vidhide.com", "vidhidepro.com", "vidhidepre.com", "embed69.org",
        // Pelismart y sus players embebidos
        "pelismart.mov", "pelismart.com", "pelismart.online",
        "vidurl.to", "fembed.com", "fplayer.info", "swdyu.com",
        "doo.la", "ds2play.com", "ds2video.com"
    )

    fun isAd(url: String): Boolean {
        val lower = url.lowercase()
        if (lower.contains("cat-bolt") || lower.contains("/creatives/") || lower.contains("securepubads") ||
            lower.contains("imasdk") || lower.contains("doubleclick") || lower.contains("/vast") ||
            lower.contains("/vpaid") || lower.contains("preroll") || lower.contains("ad-delivery")
        ) return true
        val host = try { java.net.URI(url).host?.lowercase() } catch (_: Throwable) { null }
        if (host != null) {
            for (domain in AD_DOMAINS) {
                if (host == domain || host.endsWith(".$domain")) return true
            }
        }
        for (domain in AD_DOMAINS) { if (lower.contains(domain)) return true }
        for (keyword in AD_KEYWORDS) { if (lower.contains(keyword)) return true }
        return false
    }

    fun isKnownMediaHost(host: String): Boolean {
        val lower = host.lowercase()
        for (m in KNOWN_MEDIA_HOSTS) {
            if (lower == m || lower.endsWith(".$m") || lower.contains(m.substringBefore("."))) return true
        }
        return lower.contains("embed") || lower.contains("player") || lower.contains("video") || lower.contains("stream") || lower.contains("cdn")
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
                function cleanDoc(doc) {
                    if (!doc) return;
                    var selectors = [
                        'div[class*="ad-box"]', 'div[class*="ad-container"]', 'div[id*="ad-banner"]',
                        'div[class*="ad-banner"]', 'a[href*="bet365"]', 'a[href*="1xbet"]',
                        'div[class*="popunder"]', 'div[id*="popunder"]', '.ad-overlay', '#ad-overlay',
                        'div[class*="click-shield"]', 'div[class*="video-click-shield"]',
                        '.vp_layer_fake', 'div[class*="fake-player"]', 'div[class*="play-overlay"]',
                        'div[id*="overlay-container"]', 'div[class*="backdrop-pop"]', 'div[class*="click-layer"]'
                    ];
                    for (var s = 0; s < selectors.length; s++) {
                        try {
                            var found = doc.querySelectorAll(selectors[s]);
                            for (var f = 0; f < found.length; f++) {
                                var item = found[f];
                                if (!item.querySelector('video') && !item.querySelector('iframe')) item.remove();
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
