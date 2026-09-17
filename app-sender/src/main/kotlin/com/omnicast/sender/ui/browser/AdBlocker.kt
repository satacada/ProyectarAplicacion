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
        "giggle.com", "giggles.com", "giggleclick.com", "giggletrack.com", "giggleredirect.com"
    )

    private val AD_KEYWORDS = listOf(
        "/ads/", "/banner/", "adsystem", "advertisement", "popunder",
        "popup.js", "floating_ad", "adserver", "ads.js", "tracker.js", "/affiliate/",
        "zoneid=", "iclick-v", "ad_unit", "promoted", "sponsor", "partnerize", "tradedoubler",
        "s.click.aliexpress", "c.lazada", "shp.ee", "aff_c", "clickalgo", "giggle"
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
                function purgeAdModals() {
                    var selectors = ['div[class*="ad-box"]', 'div[class*="ad-container"]', 'div[id*="ad-banner"]', 'div[class*="ad-banner"]', 'a[href*="bet365"]', 'a[href*="1xbet"]', 'div[class*="popunder"]', '.ad-overlay', '#ad-overlay'];
                    selectors.forEach(function(s) { document.querySelectorAll(s).forEach(function(el) { if (!el.querySelector('video') && !el.querySelector('iframe')) el.remove(); }); });
                    var els = document.querySelectorAll('div, dialog, section');
                    for (var i = 0; i < els.length; i++) {
                        var el = els[i];
                        if (el.querySelector('video') || el.querySelector('iframe')) continue;
                        var txt = (el.innerText || '').toLowerCase();
                        if (txt.indexOf('desnuda') !== -1 || txt.indexOf('verme') !== -1 || txt.indexOf('18+') !== -1 || (txt.indexOf('cancelar') !== -1 && txt.indexOf('continuar') !== -1 && el.offsetWidth > 180 && el.offsetHeight > 100)) {
                            var st = window.getComputedStyle(el);
                            if (st.position === 'fixed' || st.position === 'absolute' || parseInt(st.zIndex, 10) > 100) {
                                el.remove();
                            }
                        }
                    }
                }
                document.addEventListener('click', function(e) {
                    var el = e.target;
                    while (el && el !== document.body) {
                        if (el.tagName === 'A') {
                            var h = el.getAttribute('href') || '';
                            if (h && (h.startsWith('http') || h.startsWith('//'))) {
                                var host = '';
                                try { host = new URL(h, window.location.href).hostname.toLowerCase(); } catch(err){}
                                if (host && host !== window.location.hostname && !host.endsWith(window.location.hostname)) {
                                    if (host.indexOf('aliexpress') !== -1 || host.indexOf('temu') !== -1 || host.indexOf('shein') !== -1 || host.indexOf('bet') !== -1 || host.indexOf('click') !== -1 || host.indexOf('track') !== -1 || host.indexOf('ad') !== -1) {
                                        e.preventDefault(); e.stopPropagation(); el.remove(); return false;
                                    }
                                }
                            }
                        }
                        el = el.parentElement;
                    }
                }, true);
                purgeAdModals();
                setInterval(purgeAdModals, 1500);
                if (window.MutationObserver && document.body) {
                    new MutationObserver(purgeAdModals).observe(document.body, { childList: true, subtree: true });
                }
            } catch(e) {}
        })();
    """.trimIndent()
}
