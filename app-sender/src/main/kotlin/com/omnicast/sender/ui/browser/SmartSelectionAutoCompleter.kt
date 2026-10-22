package com.omnicast.sender.ui.browser

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject

/**
 * Gestor inteligente de persistencia y replicación de selecciones del usuario en el navegador web.
 * Guarda de forma no invasiva las elecciones (checkboxes, radios, selectores y pestañas de servidor/idioma)
 * realizadas por el usuario en cada dominio y las replica automáticamente al volver a cargar.
 */
object SmartSelectionAutoCompleter {

    private const val PREFS_NAME = "omnicast_saved_form_selections"
    private const val BRIDGE_NAME = "OmniSelectionBridge"

    class SmartSelectionBridge(private val context: Context) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        @JavascriptInterface
        fun saveSelection(rawHost: String?, key: String?, dataJson: String?) {
            val host = cleanHost(rawHost) ?: return
            val k = key?.trim() ?: return
            val data = dataJson?.trim() ?: return
            if (k.isBlank() || data.isBlank()) return

            try {
                val currentRulesJson = prefs.getString("host_$host", "{}") ?: "{}"
                val json = JSONObject(currentRulesJson)
                json.put(k, JSONObject(data))
                prefs.edit().putString("host_$host", json.toString()).apply()
            } catch (t: Throwable) {
                android.util.Log.w("SmartSelection", "Error guardando seleccion: ${t.message}")
            }
        }

        @JavascriptInterface
        fun getSavedSelections(rawHost: String?): String {
            val host = cleanHost(rawHost) ?: return "{}"
            return prefs.getString("host_$host", "{}") ?: "{}"
        }

        @JavascriptInterface
        fun clearDomainSelections(rawHost: String?) {
            val host = cleanHost(rawHost) ?: return
            prefs.edit().remove("host_$host").apply()
        }

        private fun cleanHost(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            return raw.lowercase().trim().removePrefix("www.")
        }
    }

    fun attach(webView: WebView, context: Context) {
        webView.addJavascriptInterface(SmartSelectionBridge(context), BRIDGE_NAME)
    }

    fun getCombinedInjectionScript(): String {
        return """
            (function() {
                var h = (window.location.hostname || '').toLowerCase();
                var t = (document.title || '').toLowerCase();
                if (h.indexOf('cloudflare') !== -1 || t.indexOf('just a moment') !== -1 || (window.location.pathname || '').indexOf('cdn-cgi') !== -1) return;

                // Persistencia y replicación automática de selecciones (checkbox, radios, selects, servidores)
                if (window.__omniSmartSelectionInjected) {
                    if (window.__omniApplySelections) window.__omniApplySelections();
                    return;
                }
                window.__omniSmartSelectionInjected = true;

                var host = window.location.hostname.toLowerCase().replace(/^www\./, '');
                if (!host || !window.OmniSelectionBridge) return;

                function getElementKey(el) {
                    if (!el) return null;
                    if (el.id) return '#' + el.id;
                    if (el.name) {
                        var val = el.value ? ('[value="' + el.value + '"]') : '';
                        return el.tagName.toLowerCase() + '[name="' + el.name + '"]' + val;
                    }
                    var dataServer = el.getAttribute('data-server') || el.getAttribute('data-id') || el.getAttribute('data-opt');
                    if (dataServer) {
                        return el.tagName.toLowerCase() + '[data-server="' + dataServer + '"]';
                    }
                    var txt = (el.innerText || el.textContent || '').trim();
                    if (txt && txt.length > 0 && txt.length < 35) {
                        return el.tagName.toLowerCase() + ':contains("' + txt + '")';
                    }
                    return null;
                }

                // Listener reactivo a eventos del usuario (Fail-Fast sin bucles pesados)
                document.addEventListener('change', function(e) {
                    if (!e.isTrusted || !window.OmniSelectionBridge) return;
                    var el = e.target;
                    if (!el || !el.tagName) return;
                    var tag = el.tagName.toUpperCase();
                    var key = getElementKey(el);
                    if (!key) return;

                    if (tag === 'INPUT') {
                        var type = (el.type || '').toLowerCase();
                        if (type === 'checkbox') {
                            window.OmniSelectionBridge.saveSelection(host, key, JSON.stringify({
                                type: 'checkbox',
                                checked: el.checked
                            }));
                        } else if (type === 'radio') {
                            window.OmniSelectionBridge.saveSelection(host, key, JSON.stringify({
                                type: 'radio',
                                checked: el.checked,
                                name: el.name,
                                value: el.value
                            }));
                        }
                    } else if (tag === 'SELECT') {
                        window.OmniSelectionBridge.saveSelection(host, key, JSON.stringify({
                            type: 'select',
                            value: el.value,
                            selectedIndex: el.selectedIndex
                        }));
                    }
                }, true);

                document.addEventListener('click', function(e) {
                    if (!e.isTrusted || !window.OmniSelectionBridge) return;
                    var el = e.target;
                    if (!el) return;
                    var btn = el.closest('button, [data-server], [data-lang], .server-item, .lang-item, .opt-item');
                    if (btn) {
                        var key = getElementKey(btn);
                        var txt = (btn.innerText || btn.textContent || '').trim();
                        if (key) {
                            window.OmniSelectionBridge.saveSelection(host, key, JSON.stringify({
                                type: 'click_opt',
                                text: txt
                            }));
                        }
                    }
                }, true);

                window.__omniApplySelections = function() {
                    if (!window.OmniSelectionBridge) return;
                    var raw = window.OmniSelectionBridge.getSavedSelections(host);
                    if (!raw || raw === '{}') return;
                    var selections;
                    try { selections = JSON.parse(raw); } catch(_) { return; }
                    if (!selections || typeof selections !== 'object') return;

                    Object.keys(selections).forEach(function(k) {
                        var item = selections[k];
                        if (!item || !item.type) return;

                        try {
                            if (item.type === 'checkbox') {
                                var el = document.querySelector(k);
                                if (el && el.type === 'checkbox' && el.checked !== item.checked) {
                                    el.checked = item.checked;
                                    el.dispatchEvent(new Event('change', { bubbles: true }));
                                }
                            } else if (item.type === 'radio') {
                                var el = document.querySelector(k);
                                if (el && !el.checked) {
                                    el.checked = true;
                                    el.dispatchEvent(new Event('change', { bubbles: true }));
                                }
                            } else if (item.type === 'select') {
                                var el = document.querySelector(k);
                                if (el && el.value !== item.value) {
                                    el.value = item.value;
                                    el.dispatchEvent(new Event('change', { bubbles: true }));
                                }
                            } else if (item.type === 'click_opt') {
                                var el = null;
                                if (!k.includes(':contains(')) {
                                    el = document.querySelector(k);
                                }
                                if (!el && item.text) {
                                    var candidates = document.querySelectorAll('button, a, li, span');
                                    for (var i = 0; i < candidates.length; i++) {
                                        var c = candidates[i];
                                        if ((c.innerText || c.textContent || '').trim() === item.text && c.offsetParent !== null) {
                                            el = c;
                                            break;
                                        }
                                    }
                                }
                                if (el && !el.classList.contains('active') && !el.classList.contains('selected') && !el.getAttribute('data-omni-applied')) {
                                    el.setAttribute('data-omni-applied', 'true');
                                    el.click();
                                }
                            }
                        } catch(_) {}
                    });
                };

                if (document.readyState === 'complete' || document.readyState === 'interactive') {
                    window.__omniApplySelections();
                } else {
                    document.addEventListener('DOMContentLoaded', window.__omniApplySelections);
                }
                setTimeout(window.__omniApplySelections, 600);
                setTimeout(window.__omniApplySelections, 1600);
            })();
        """.trimIndent()
    }
}
