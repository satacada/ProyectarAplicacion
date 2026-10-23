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

    fun inject(webView: WebView?) {
        if (webView == null) return
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

            // 1. Intercepción del setter de valor en HTMLInputElement para reconciliación React
            try {
                var desc = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');
                if (desc && desc.set) {
                    var originalSet = desc.set;
                    desc.set = function(val) {
                        var isFocused = (document.activeElement === this);
                        var oldLen = this.value ? this.value.length : 0;
                        originalSet.call(this, val);
                        if (isFocused && typeof val === 'string' && val.length > oldLen) {
                            if (this.selectionStart === 0 && this.selectionEnd === 0) {
                                try {
                                    this.setSelectionRange(val.length, val.length);
                                    log('CARET_SETTER', 'Caret restaurado a fin=' + val.length);
                                } catch(_) {}
                            }
                        }
                    };
                    Object.defineProperty(HTMLInputElement.prototype, 'value', desc);
                }
            } catch(_) {}

            // 2. Anclaje de caret post-renderizado para inputs controlados
            document.addEventListener('input', function(e) {
                var target = e.target;
                if (!target || (target.tagName !== 'INPUT' && target.tagName !== 'TEXTAREA')) return;
                var currentLen = (target.value || '').length;
                if (currentLen === 0) return;

                requestAnimationFrame(function() {
                    if (document.activeElement === target) {
                        var len = (target.value || '').length;
                        if (target.selectionStart === 0 && target.selectionEnd === 0 && len > 0) {
                            try {
                                target.setSelectionRange(len, len);
                                log('CARET_RAF', 'Caret restaurado en RAF a fin=' + len);
                            } catch(_) {}
                        }
                    }
                });
            }, true);

            // 3. Desplazamiento visible inmediato al centro sobre el teclado (Erradicación de escritura a ciegas)
            document.addEventListener('focusin', function(e) {
                var target = e.target;
                if (!target) return;
                var tag = target.tagName ? target.tagName.toUpperCase() : '';
                if (tag === 'INPUT' || tag === 'TEXTAREA' || target.getAttribute('role') === 'search' || target.isContentEditable) {
                    var r = target.getBoundingClientRect();
                    log('FOCUSIN', '<' + tag + ' id="' + (target.id || '') + '"> top=' + Math.round(r.top) + ', h=' + Math.round(r.height));
                    setTimeout(function() {
                        try {
                            target.scrollIntoView({ block: 'center', behavior: 'smooth' });
                            log('SCROLL_CENTER', 'Centrado en viewport visible');
                        } catch(_) {
                            try { target.scrollIntoView(); } catch(__) {}
                        }
                    }, 120);
                }
            }, true);
        })();
    """.trimIndent()
}
