package com.omnicast.sender.ui.browser

import android.webkit.JavascriptInterface
import android.webkit.WebView

/**
 * Estabilizador de visibilidad, foco y cursor (caret) de campos de texto en WebView.
 * Erradica la escritura a ciegas y la inversión de texto (escritura al revés),
 * preservando el desplazamiento natural ('nearest') sin saltos destructivos de Compose/Chromium.
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
            if (window.__omniFocusScrollInjected) return;
            window.__omniFocusScrollInjected = true;

            function log(tag, msg) {
                try {
                    if (window.OmniInputBridge) window.OmniInputBridge.logInput(tag, msg);
                } catch(_) {}
            }

            document.addEventListener('focusin', function(e) {
                var target = e.target;
                if (!target) return;
                var tag = target.tagName ? target.tagName.toUpperCase() : '';
                if (tag === 'INPUT' || tag === 'TEXTAREA' || target.getAttribute('role') === 'search' || target.isContentEditable) {
                    var r = target.getBoundingClientRect();
                    var vh = window.innerHeight || document.documentElement.clientHeight || 800;
                    log('FOCUSIN', '<' + tag + ' type="' + (target.type || '') + '" id="' + (target.id || '') + '"> top=' + Math.round(r.top) + ', h=' + Math.round(r.height) + ', vh=' + vh);
                    
                    // Solo desplazar si el elemento está efectivamente oculto fuera de la pantalla visible
                    if (r.top < 0 || r.bottom > vh) {
                        setTimeout(function() {
                            try {
                                target.scrollIntoView({ behavior: 'auto', block: 'nearest', inline: 'nearest' });
                                log('SCROLL_NEAREST', 'Ajustado a viewport visible');
                            } catch(_) {
                                try { target.scrollIntoView(false); } catch(__) {}
                            }
                        }, 100);
                    }
                }
            }, true);

            document.addEventListener('input', function(e) {
                var target = e.target;
                if (!target) return;
                var tag = target.tagName ? target.tagName.toUpperCase() : '';
                if (tag !== 'INPUT' && tag !== 'TEXTAREA') return;
                var v = target.value || '';
                var ss = target.selectionStart;
                var se = target.selectionEnd;
                log('INPUT', '<' + tag + '> len=' + v.length + ' sel=[' + ss + ',' + se + '] data=' + (e.data || '') + ' comp=' + e.isComposing);

                // Erradicación de escritura inversa: si el cursor colapsa a 0 al insertar texto con longitud > 1
                if (e.inputType && e.inputType.indexOf('insert') === 0 && !e.isComposing) {
                    if (ss === 0 && se === 0 && v.length > 1) {
                        try {
                            target.setSelectionRange(v.length, v.length);
                            log('CARET_RESTORED', 'Cursor restaurado de 0 a fin=' + v.length);
                        } catch(_) {}
                    }
                }
            }, true);
        })();
    """.trimIndent()
}
