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
        if (isAdBlockEnabled && (progress == 0 || progress == 30 || progress == 60 || progress == 85 || progress == 100)) {
            webView.evaluateJavascript(AdBlocker.getAdHidingScript(), null)
        }
        if (progress == 60 || progress == 85 || progress == 100) {
            webView.evaluateJavascript(SmartSelectionAutoCompleter.getCombinedInjectionScript(), null)
            webView.evaluateJavascript(getPreScanScript(), null)
        }
    }

    fun getPreScanScript(): String = """
        (function() {
            try {
                var h = (window.location.hostname || '').toLowerCase();
                var t = (document.title || '').toLowerCase();
                if (h.indexOf('dailymotion.com') !== -1 || h.indexOf('youtube.com') !== -1 || h.indexOf('ok.ru') !== -1 || h.indexOf('google.') !== -1 || h.indexOf('cloudflare') !== -1 || t.indexOf('just a moment') !== -1) return;

                // 1. Activación de lazy-src en iframes para permitir carga interactiva
                var iframes = document.querySelectorAll('iframe');
                for (var i = 0; i < iframes.length; i++) {
                    var ifr = iframes[i];
                    var lazySrc = ifr.getAttribute('data-src') || ifr.getAttribute('data-lazy-src') || '';
                    if (!ifr.src && lazySrc && lazySrc.indexOf('http') === 0) {
                        ifr.src = lazySrc;
                    }
                }

                // 2. Neutralización reactiva de click-shields y velos transparentes
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
        // Opción 2 (Play-First): No pre-detectar especulativamente en frío para evitar tokens 403
    }
}
