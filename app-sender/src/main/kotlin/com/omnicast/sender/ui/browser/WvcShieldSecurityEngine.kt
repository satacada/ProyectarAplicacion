package com.omnicast.sender.ui.browser

import android.net.Uri

/**
 * Motor modular de blindaje y seguridad inspirado en Web Video Caster.
 * Sigue el principio "Poner y Sacar": completamente desacoplado con toggle [isEnabled].
 *
 * Funcionalidades:
 * 1. Anulación preventiva de popups por `window.open` a nivel de JavaScript.
 * 2. Protección contra secuestro del botón atrás (throttling de `history.pushState`).
 * 3. Neutralización de WebSockets publicitarios (ws:// y wss://).
 * 4. Supresión de redirecciones retardadas sin gesto de usuario (setTimeout + location.href).
 */
object WvcShieldSecurityEngine {

    /**
     * Interruptor global "Poner y Sacar".
     * Si es false, todas las protecciones quedan inactivas inmediatamente.
     */
    var isEnabled: Boolean = true

    /**
     * Script de blindaje inyectado en la página web en document_start / onPageStarted.
     */
    fun getProtectionScript(): String {
        if (!isEnabled) return ""
        return """
            (function() {
                if (window.__omniWvcShieldActive) return;
                window.__omniWvcShieldActive = true;

                // 1. Anulación preventiva de popups automáticos por window.open
                try {
                    var _realOpen = window.open;
                    window.open = function(url, target, features) {
                        if (!url || url === 'about:blank') return null;
                        var u = url.toLowerCase();
                        if (u.indexOf('challenges.cloudflare') !== -1 || u.indexOf('turnstile') !== -1) {
                            return _realOpen ? _realOpen.apply(this, arguments) : null;
                        }
                        console.log('🛡️ [WVC Shield] Popup window.open bloqueado:', url);
                        return null;
                    };
                } catch(_) {}

                // 2. Protección contra secuestro del botón Atrás (history.pushState masivo)
                try {
                    var _realPush = window.history.pushState;
                    var _lastPush = 0;
                    var _burstCount = 0;
                    window.history.pushState = function(state, title, url) {
                        var now = Date.now();
                        if (now - _lastPush < 3000) {
                            _burstCount++;
                            if (_burstCount > 3) {
                                console.log('🛡️ [WVC Shield] Throttling de pushState abusivo');
                                return;
                            }
                        } else {
                            _burstCount = 0;
                            _lastPush = now;
                        }
                        return _realPush ? _realPush.apply(this, arguments) : null;
                    };
                } catch(_) {}

                // 3. Neutralización de WebSockets publicitarios
                try {
                    if (window.WebSocket) {
                        var _realWS = window.WebSocket;
                        window.WebSocket = function(url, protocols) {
                            var u = (url || '').toLowerCase();
                            if (u.indexOf('adsterra') !== -1 || u.indexOf('propeller') !== -1 ||
                                u.indexOf('popads') !== -1 || u.indexOf('trafficjunky') !== -1 ||
                                u.indexOf('exoclick') !== -1 || u.indexOf('clickadu') !== -1 ||
                                u.indexOf('banner') !== -1 || u.indexOf('syndication') !== -1) {
                                console.log('🛡️ [WVC Shield] WebSocket publicitario neutralizado:', url);
                                return {
                                    send: function() {},
                                    close: function() {},
                                    addEventListener: function() {},
                                    removeEventListener: function() {}
                                };
                            }
                            return new _realWS(url, protocols);
                        };
                    }
                } catch(_) {}
            })();
        """.trimIndent()
    }

    /**
     * Evalúa si una navegación en `shouldOverrideUrlLoading` es una redirección retardada abusiva
     * iniciada por `setTimeout` o scripts sin interacción táctil real del usuario.
     */
    fun shouldBlockDelayedRedirect(hasGesture: Boolean, targetUrl: String, currentUrl: String): Boolean {
        if (!isEnabled || hasGesture) return false
        if (targetUrl.isBlank() || targetUrl.startsWith("about:")) return false
        if (AdBlocker.isVerificationOrCaptcha(targetUrl)) return false
        if (AdBlockWhitelistManager.isWhitelisted(targetUrl) || AdBlocker.isSearchEngine(targetUrl)) return false

        val curHost = try { Uri.parse(currentUrl).host?.lowercase() } catch (_: Throwable) { null }
        val targetHost = try { Uri.parse(targetUrl).host?.lowercase() } catch (_: Throwable) { null }

        // Si es el mismo dominio (por ejemplo navegación interna del portal), no bloquear
        if (curHost != null && targetHost != null) {
            if (targetHost == curHost || targetHost.endsWith(".$curHost") || curHost.endsWith(".$targetHost")) {
                return false
            }
        }
        if (targetHost != null && AdBlocker.isKnownMediaHost(targetHost)) return false

        android.util.Log.w("WvcShieldSecurity", "🛡️ [WVC Shield] Redirección sin gesto táctil bloqueada: $targetHost")
        return true
    }
}
