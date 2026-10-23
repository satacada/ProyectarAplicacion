package com.omnicast.sender.ui.browser

import android.content.Context
import android.webkit.WebView

/**
 * Motor de pre-análisis y escaneo de páginas de películas y streaming (Modo B).
 * Extrae iframes de servidores, tabs de reproducción y neutraliza velos click-shield
 * transparentes sin bloquear el hilo principal ni interferir en plataformas oficiales.
 */
object MoviePagePreScanner {

    fun injectPageScripts(
        webView: WebView?,
        url: String?,
        progress: Int,
        isAdBlockEnabled: Boolean,
        isChallenge: Boolean,
        context: Context
    ) {
        if (webView == null || isChallenge || AdBlocker.isVerificationOrCaptcha(url)) return
        val mode = BrowserPageClassifier.classify(url)
        if (mode == BrowserPageMode.OFFICIAL_PLATFORM) {
            // Modo A: "Guantes de Seda". Cero scripts DOM intrusivos en plataformas oficiales
            return
        }

        // Modo B: "Escaneo Total de Películas"
        if (isAdBlockEnabled && (progress == 30 || progress == 60 || progress == 100)) {
            webView.evaluateJavascript(AdBlocker.getAdHidingScript(), null)
        }
        if (progress == 60 || progress == 85 || progress == 100) {
            webView.evaluateJavascript(SmartSelectionAutoCompleter.getCombinedInjectionScript(), null)
            webView.evaluateJavascript(WvcDomSniffer.getInjectionScript(context), null)
            webView.evaluateJavascript(getPreScanScript(), null)
        }
    }

    fun getPreScanScript(): String = """
        (function() {
            try {
                var h = (window.location.hostname || '').toLowerCase();
                var t = (document.title || '').toLowerCase();
                if (h.indexOf('dailymotion.com') !== -1 || h.indexOf('youtube.com') !== -1 || h.indexOf('ok.ru') !== -1 || h.indexOf('google.') !== -1 || h.indexOf('cloudflare') !== -1 || t.indexOf('just a moment') !== -1) return;

                // 1. Pre-análisis y activación de iframes de servidores (Streamtape, Voe, Filemoon, Dood, etc.)
                var iframes = document.querySelectorAll('iframe');
                for (var i = 0; i < iframes.length; i++) {
                    var ifr = iframes[i];
                    var lazySrc = ifr.getAttribute('data-src') || ifr.getAttribute('data-lazy-src') || '';
                    if (!ifr.src && lazySrc && lazySrc.indexOf('http') === 0) {
                        ifr.src = lazySrc;
                    }
                    var effectiveSrc = (ifr.src || lazySrc || '').toLowerCase();
                    if (effectiveSrc && (effectiveSrc.indexOf('/embed') !== -1 || effectiveSrc.indexOf('/player') !== -1 || effectiveSrc.indexOf('/v/') !== -1 || effectiveSrc.indexOf('stream') !== -1 || effectiveSrc.indexOf('filemoon') !== -1 || effectiveSrc.indexOf('voe') !== -1 || effectiveSrc.indexOf('dood') !== -1)) {
                        console.log('OMNICAST_SERVER_CANDIDATE:' + (ifr.src || lazySrc));
                    }
                }

                // 2. Pre-análisis de tabs de servidores y opciones de idioma (Dooplay / Cuevana / Pelisplus)
                var serverBtns = document.querySelectorAll('[data-url], [data-src], [data-video], .server-item, .play-server, li[id*="player"]');
                var maxCount = Math.min(serverBtns.length, 12);
                for (var j = 0; j < maxCount; j++) {
                    var btn = serverBtns[j];
                    var sUrl = btn.getAttribute('data-url') || btn.getAttribute('data-src') || btn.getAttribute('data-video') || '';
                    if (sUrl && sUrl.indexOf('http') === 0 && !sUrl.match(/\.(png|jpe?g|gif|webp|svg|css|js)$/i)) {
                        console.log('OMNICAST_SERVER_CANDIDATE:' + sUrl);
                    }
                }

                // 3. Neutralización reactiva de click-shields y velos transparentes
                var overlays = document.querySelectorAll('.click-shield, .play-overlay, .vp_layer_fake, div[class*="click-shield"], div[class*="fake-player"], div[id*="click-shield"]');
                for (var k = 0; k < overlays.length; k++) {
                    var ov = overlays[k];
                    if (!ov.querySelector('video, iframe, input, form, textarea, select')) {
                        ov.style.pointerEvents = 'none';
                        ov.style.display = 'none';
                    }
                }
            } catch(e) {}
        })();
    """.trimIndent()

    fun handleServerCandidate(
        serverUrl: String,
        pageUrl: String,
        pageTitle: String?,
        onStreamFound: (url: String, headers: Map<String, String>, isMaster: Boolean) -> Unit
    ) {
        if (serverUrl.isBlank() || serverUrl.contains("favicon")) return
        val candidate = com.omnicast.core.domain.models.StreamCandidate(
            url = serverUrl, pageUrl = pageUrl,
            headers = emptyMap(), userAgent = UA_MOBILE, pageTitle = pageTitle
        )
        val detected = com.omnicast.sender.ui.browser.extractors.StreamExtractorRegistry.useCase.execute(candidate)
        if (detected != null) {
            val hdrs = BrowserUrlHelper.buildSafeHeaders(detected.url, pageUrl, UA_MOBILE, detected.headers)
            onStreamFound(detected.url, hdrs, detected.isMaster)
        }
    }
}
