package com.omnicast.sender.ui.browser

import android.webkit.WebView

/**
 * Estabilizador universal de foco y posición de cursor (Caret) para Chromium WebView.
 * Resuelve el fallo nativo en componentes React controlados (como el buscador de Dailymotion),
 * donde cada pulsación de tecla provoca una reconciliación del DOM virtual que reinicia el cursor
 * al índice 0, generando escritura invertida ("sorraC" en lugar de "Carros").
 */
object InputCaretStabilizer {

    fun inject(webView: WebView?) {
        if (webView == null) return
        webView.evaluateJavascript(SCRIPT, null)
    }

    private val SCRIPT = """
        (function() {
            if (window.__omniCaretStabilizerInjected) return;
            window.__omniCaretStabilizerInjected = true;

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
                                } catch(_) {}
                            }
                        }
                    };
                    Object.defineProperty(HTMLInputElement.prototype, 'value', desc);
                }
            } catch(_) {}

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
                            } catch(_) {}
                        }
                    }
                });
            }, true);

            document.addEventListener('focusin', function(e) {
                var target = e.target;
                if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA')) {
                    try {
                        if (!target.hasAttribute('autocorrect')) target.setAttribute('autocorrect', 'off');
                        if (!target.hasAttribute('spellcheck')) target.setAttribute('spellcheck', 'false');
                    } catch(_) {}
                }
            }, true);
        })();
    """.trimIndent()
}
