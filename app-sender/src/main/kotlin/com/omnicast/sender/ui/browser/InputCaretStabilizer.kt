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

    class OmniInputTelemetryBridge(private val webView: WebView?) {
        @JavascriptInterface
        fun logInput(tag: String, msg: String) {
            android.util.Log.i("OmniInput", "⌨️ [$tag] $msg")
        }

        @JavascriptInterface
        fun onFocusIn(text: String, placeholder: String) {
            android.util.Log.i("OmniInput", "⌨️ [NATIVE_FOCUS] text='$text', ph='$placeholder'")
            WebInputAssistantManager.open(text, placeholder, webView)
        }

        @JavascriptInterface
        fun onSuggestions(json: String) {
            try {
                val arr = org.json.JSONArray(json)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i).trim()
                    if (s.isNotBlank() && !list.contains(s)) list.add(s)
                }
                if (list.isNotEmpty()) WebInputAssistantManager.setSuggestions(list)
            } catch (_: Throwable) {}
        }
    }

    fun attach(webView: WebView?) {
        if (webView == null) return
        webView.addJavascriptInterface(OmniInputTelemetryBridge(webView), BRIDGE_NAME)
        WebInputAssistantManager.attachWebView(webView)
    }

    fun injectText(webView: WebView?, text: String) {
        val wv = webView ?: WebInputAssistantManager.getWebView() ?: return
        val escaped = org.json.JSONObject.quote(text)
        val js = """
            (function() {
                var el = window.__omniLastFocusedInput || document.activeElement;
                if (!el || (el.tagName !== 'INPUT' && el.tagName !== 'TEXTAREA')) {
                    el = document.querySelector('input[type="search"], input[name="q"], input[name="search"], input[id*="search"], input:not([type="hidden"]), textarea');
                }
                if (el) {
                    var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value')?.set;
                    if (setter) { setter.call(el, $escaped); } else { el.value = $escaped; }
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    try {
                        el.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true }));
                        el.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true }));
                    } catch(_) {}
                    setTimeout(function() {
                        try {
                            var items = document.querySelectorAll('[role="option"], [role="listbox"] li, [class*="suggestion" i], [class*="SearchSuggestion" i], [class*="Autocomplete" i] li, [data-testid*="suggestion" i]');
                            var list = [];
                            for (var i = 0; i < items.length && list.length < 6; i++) {
                                var t = (items[i].innerText || items[i].textContent || '').trim();
                                if (t && t.length > 1 && list.indexOf(t) === -1) list.push(t);
                            }
                            if (list.length > 0 && window.OmniInputBridge && window.OmniInputBridge.onSuggestions) {
                                window.OmniInputBridge.onSuggestions(JSON.stringify(list));
                            }
                        } catch(_) {}
                    }, 180);
                }
            })();
        """.trimIndent()
        wv.post { wv.evaluateJavascript(js, null) }
    }

    fun submitInput(webView: WebView?, text: String) {
        val wv = webView ?: WebInputAssistantManager.getWebView() ?: return
        val q = text.trim()
        if (q.isEmpty()) return
        wv.post {
            val pageUrl = wv.url ?: ""
            if (pageUrl.contains("dailymotion.com")) {
                android.util.Log.i("OmniInput", "🚀 [SEARCH_NAV] Navegando en Dailymotion: '$q'")
                val encoded = android.net.Uri.encode(q)
                wv.loadUrl("https://www.dailymotion.com/search/$encoded")
                return@post
            }
            val escaped = org.json.JSONObject.quote(q)
            val js = """
                (function() {
                    var el = window.__omniLastFocusedInput || document.activeElement;
                    if (!el || (el.tagName !== 'INPUT' && el.tagName !== 'TEXTAREA')) {
                        el = document.querySelector('input[type="search"], input[name="q"], input[name="search"], input[id*="search"], input:not([type="hidden"]), textarea');
                    }
                    if (el) {
                        var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value')?.set;
                        if (setter) { setter.call(el, $escaped); } else { el.value = $escaped; }
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                        el.dispatchEvent(new Event('change', { bubbles: true }));
                        var form = el.closest ? el.closest('form') : el.form;
                        if (form) {
                            try {
                                var submitEvent = new Event('submit', { bubbles: true, cancelable: true });
                                form.dispatchEvent(submitEvent);
                            } catch(_) {}
                        }
                        var ev = { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true };
                        el.dispatchEvent(new KeyboardEvent('keydown', ev));
                        el.dispatchEvent(new KeyboardEvent('keypress', ev));
                        el.dispatchEvent(new KeyboardEvent('keyup', ev));
                    }
                })();
            """.trimIndent()
            wv.evaluateJavascript(js, null)
        }
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
                    window.__omniLastFocusedInput = target;
                    try {
                        var textVal = target.value || '';
                        var phVal = target.placeholder || target.getAttribute('aria-label') || 'Buscar...';
                        if (window.OmniInputBridge && window.OmniInputBridge.onFocusIn) {
                            window.OmniInputBridge.onFocusIn(textVal, phVal);
                        }
                    } catch(_) {}
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
