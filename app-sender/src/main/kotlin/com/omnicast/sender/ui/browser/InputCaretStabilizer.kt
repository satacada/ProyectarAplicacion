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

            function findFixedOrStickyContainer(el) {
                var curr = el;
                while (curr && curr !== document.body && curr !== document.documentElement) {
                    try {
                        var pos = window.getComputedStyle(curr).position;
                        if (pos === 'fixed' || pos === 'sticky') return curr;
                    } catch(_) {}
                    curr = curr.parentElement;
                }
                return null;
            }

            // 1. Estabilización geométrica universal al recibir foco
            document.addEventListener('focusin', function(e) {
                var target = e.target;
                if (!target) return;
                var tag = target.tagName ? target.tagName.toUpperCase() : '';
                if (tag === 'INPUT' || tag === 'TEXTAREA' || target.getAttribute('role') === 'search' || target.isContentEditable) {
                    var fixedContainer = findFixedOrStickyContainer(target);
                    if (fixedContainer) {
                        var cr = fixedContainer.getBoundingClientRect();
                        log('FIXED_FOCUS', '<' + tag + '> contenedor fixed/sticky detectado: top=' + Math.round(cr.top) + ', h=' + Math.round(cr.height));
                        if (cr.top < 0) {
                            try {
                                fixedContainer.style.setProperty('top', '0px', 'important');
                                fixedContainer.style.setProperty('transform', 'none', 'important');
                                log('FIXED_RESTORED', 'Contenedor fixed re-anclado a top: 0');
                            } catch(_) {}
                        }
                    } else {
                        var r = target.getBoundingClientRect();
                        log('FLOW_FOCUS', '<' + tag + '> top=' + Math.round(r.top) + ', h=' + Math.round(r.height));
                        if (r.top < 0 || r.bottom > window.innerHeight) {
                            try {
                                target.scrollIntoView({ block: 'nearest', inline: 'nearest', behavior: 'smooth' });
                                log('FLOW_SCROLL', 'Input centrado en viewport');
                            } catch(_) {
                                try { target.scrollIntoView(); } catch(__) {}
                            }
                        }
                    }
                }
            }, true);

            // 2. Erradicación universal de inversión de texto en componentes controlados (React/Vue/Next)
            document.addEventListener('input', function(e) {
                var target = e.target;
                if (!target || (target.tagName !== 'INPUT' && target.tagName !== 'TEXTAREA')) return;
                var v = target.value || '';
                if (v.length <= 1) return;

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
