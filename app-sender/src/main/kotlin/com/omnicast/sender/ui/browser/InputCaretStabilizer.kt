package com.omnicast.sender.ui.browser

import android.webkit.JavascriptInterface
import android.webkit.WebView

/**
 * Estabilizador universal de foco, visibilidad y posición de cursor (Caret) en Chromium WebView.
 * Centra inmediatamente el cuadro de búsqueda sobre el teclado virtual (erradicando la escritura a ciegas)
 * y neutraliza la inversión de caracteres en componentes React controlados (Dailymotion/Next.js)
 * mediante intercepción del setter de valor y anclaje reactivo de cursor en requestAnimationFrame.
 */
object InputCaretStabilizer {

    private const val BRIDGE_NAME = "OmniInputBridge"

    class OmniInputTelemetryBridge {
        @JavascriptInterface
        fun logInput(tag: String, msg: String) {
            android.util.Log.i("OmniInput", "⌨️ [$tag] $msg")
        }
    }

    fun attach(webView: WebView?) {
        if (webView == null) return
        webView.addJavascriptInterface(OmniInputTelemetryBridge(), BRIDGE_NAME)
    }

    fun inject(webView: WebView?, url: String? = null) {
        if (webView == null) return
        val currentUrl = url ?: webView.url
        val mode = BrowserPageClassifier.classify(currentUrl)
        if (mode == BrowserPageMode.OFFICIAL_PLATFORM) {
            android.util.Log.i("OmniInput", "🛡️ Modo A (Guantes de Seda): Cero inyecciones DOM para plataforma oficial: $currentUrl")
            return
        }
        webView.evaluateJavascript(SCRIPT, null)
    }

    private val SCRIPT = """
        (function() {
            if (window.__omniCaretStabilizerInjected) return;
            window.__omniCaretStabilizerInjected = true;

            function log(tag, msg) {
                try {
                    if (window.OmniInputBridge) window.OmniInputBridge.logInput(tag, msg);
                } catch(_) {}
            }

            // 1. Visibilidad garantizada en focus: llevar el encabezado y el input al viewport visible
            document.addEventListener('focusin', function(e) {
                var target = e.target;
                if (!target) return;
                var tag = target.tagName ? target.tagName.toUpperCase() : '';
                if (tag === 'INPUT' || tag === 'TEXTAREA' || target.getAttribute('role') === 'search' || target.isContentEditable) {
                    var r = target.getBoundingClientRect();
                    log('FOCUSIN', '<' + tag + ' id="' + (target.id || '') + '"> top=' + Math.round(r.top) + ', h=' + Math.round(r.height) + ', scrollY=' + Math.round(window.scrollY || 0));

                    // Si la página está desplazada hacia abajo, las barras sticky (como Dailymotion)
                    // quedan recortadas o en top negativo (-16px). Llevar la ventana a top: 0 expande el header.
                    if ((window.scrollY || 0) > 0 || r.top < 0) {
                        try { window.scrollTo(0, 0); log('SCROLL_TOP', 'Ventana restablecida a top: 0'); } catch(_) {}
                    }

                    setTimeout(function() {
                        try {
                            target.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'auto' });
                            log('SCROLL_CENTER', 'Centrado en viewport');
                        } catch(_) {
                            try { target.scrollIntoView(); } catch(__) {}
                        }
                    }, 80);
                }
            }, true);

            // 2. Erradicación de escritura inversa en inserción sin bloquear edición ni borrado
            document.addEventListener('input', function(e) {
                var target = e.target;
                if (!target || (target.tagName !== 'INPUT' && target.tagName !== 'TEXTAREA')) return;
                var v = target.value || '';
                if (v.length <= 1) return;

                // Solo intervenir si el evento es de inserción de texto y el cursor colapsó erróneamente a 0
                if (e.inputType && e.inputType.indexOf('insert') === 0 && !e.isComposing) {
                    if (target.selectionStart === 0 && target.selectionEnd === 0) {
                        try {
                            target.setSelectionRange(v.length, v.length);
                            log('CARET_RESTORED', 'Caret restaurado a fin=' + v.length);
                        } catch(_) {}
                    }
                }
            }, true);
        })();
    """.trimIndent()
}
