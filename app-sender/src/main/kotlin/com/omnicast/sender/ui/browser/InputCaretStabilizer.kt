package com.omnicast.sender.ui.browser

import android.webkit.WebView

/**
 * Estabilizador de visibilidad y foco de campos de texto (Search/Inputs) en WebView.
 * Desplaza automáticamente el campo enfocado al centro visible sobre el teclado virtual
 * para erradicar la escritura a ciegas, preservando el comportamiento nativo de Chromium y React.
 */
object InputCaretStabilizer {

    fun inject(webView: WebView?) {
        if (webView == null) return
        webView.evaluateJavascript(SCRIPT, null)
    }

    private val SCRIPT = """
        (function() {
            if (window.__omniFocusScrollInjected) return;
            window.__omniFocusScrollInjected = true;

            document.addEventListener('focusin', function(e) {
                var target = e.target;
                if (!target) return;
                var tag = target.tagName ? target.tagName.toUpperCase() : '';
                if (tag === 'INPUT' || tag === 'TEXTAREA' || target.getAttribute('role') === 'search') {
                    setTimeout(function() {
                        try {
                            target.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        } catch(_) {
                            try { target.scrollIntoView(); } catch(__) {}
                        }
                    }, 150);
                }
            }, true);
        })();
    """.trimIndent()
}
