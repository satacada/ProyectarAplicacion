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

    private val SHIELD_SCRIPT: String = """
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
                    var isAdGateway = (u.indexOf('/out.php') !== -1 || u.indexOf('/pop.php') !== -1 ||
                                       u.indexOf('/ad.php') !== -1 || u.indexOf('/redirect.php') !== -1 ||
                                       u.indexOf('/click.php') !== -1 || u.indexOf('/delivery/') !== -1);
                    var isInsideIframe = (window.self !== window.top);

                    // Si es pasarela o se invoca desde un iframe reproductor, absorbe el popup sin cambiar de ventana
                    if (isAdGateway || isInsideIframe) {
                        return {
                            closed: false, name: target || '', location: { href: url },
                            focus: function() {}, blur: function() {}, close: function() { this.closed = true; }
                        };
                    }

                    // Ventana principal: delegar limpiamente en _realOpen para gestión nativa
                    return _realOpen ? _realOpen.apply(this, arguments) : null;
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

            // 5. Camuflaje Stealth Anti-Detección de WebView (Brave/Chrome Nativo)
            try {
                if (!window.chrome) { window.chrome = { app: { isInstalled: false }, runtime: {} }; }
                try {
                    Object.defineProperty(navigator, 'webdriver', {
                        get: function() { return false; },
                        configurable: true
                    });
                } catch(_) {}
            } catch(_) {}
        })();
    """.trimIndent()

    /**
     * Script de blindaje inyectado en la página web en document_start / onPageStarted.
     */
    fun getProtectionScript(): String = if (isEnabled) SHIELD_SCRIPT else ""

    /**
     * Evalúa si una navegación en `shouldOverrideUrlLoading` es una redirección retardada abusiva
     * iniciada por `setTimeout` o scripts sin interacción táctil real del usuario.
     */
    fun shouldBlockDelayedRedirect(hasGesture: Boolean, targetUrl: String, currentUrl: String): Boolean {
        if (!isEnabled || hasGesture) return false
        if (targetUrl.isBlank() || targetUrl.startsWith("about:")) return false
        if (targetUrl.lowercase().contains("/out.php") || targetUrl.lowercase().contains("/pop.php")) return true
        if (AdBlocker.isVerificationOrCaptcha(targetUrl)) return false
        if (AdBlockWhitelistManager.isWhitelisted(targetUrl)) return false

        val curHost = extractHost(currentUrl)
        val targetHost = extractHost(targetUrl)

        if (curHost != null && AdBlocker.isSearchEngine(curHost)) return false
        if (targetHost != null && (AdBlocker.isSearchEngine(targetHost) || AdBlocker.isKnownMediaHost(targetHost) || AdBlockWhitelistManager.isWhitelistedHost(targetHost))) {
            return false
        }

        if (curHost != null && targetHost != null) {
            if (targetHost == curHost || targetHost.endsWith(".$curHost") || curHost.endsWith(".$targetHost")) {
                return false
            }
        }

        val isAd = AdBlocker.isAd(targetUrl) ||
            (targetHost != null && AdBlockDatabase.isAdDomain(targetHost)) ||
            NativeAdBlockEngine.shouldBlock(targetUrl, currentUrl) ||
            (curHost != null && targetHost != null)

        if (isAd) {
            com.omnicast.sender.log.SenderLogger.w("DELAYED_REDIRECT_BLOCKED", "🛡️ Redirección sin gesto bloqueada: $targetHost (desde $curHost)", details = mapOf("target_host" to targetHost, "cur_host" to curHost, "target_url" to targetUrl))
            targetHost?.let { AdBlocker.learnAndBlockAdHost(it, curHost) }
            return true
        }
        return false
    }

    private fun extractHost(url: String): String? {
        return try {
            java.net.URI(url).host?.lowercase()
        } catch (_: Throwable) {
            try { Uri.parse(url).host?.lowercase() } catch (_: Throwable) { null }
        }
    }

    private fun extractBrand(host: String): String {
        val clean = host.removePrefix("www.").substringBefore(':')
        val parts = clean.split('.')
        return if (parts.size >= 2) parts[parts.size - 2] else clean
    }

    /**
     * Evalúa si una navegación en `shouldOverrideUrlLoading` es un secuestro de clic (click-jacking)
     * que intenta desviar al usuario hacia páginas publicitarias, buscadores o dominios externos
     * no autorizados desde un sitio web de streaming o películas.
     */
    @Suppress("UNUSED_PARAMETER")
    fun shouldBlockClickHijack(hasGesture: Boolean, targetUrl: String, currentUrl: String, isMediaPresent: Boolean = false): Boolean {
        if (!isEnabled || !hasGesture) return false
        val curHost = extractHost(currentUrl) ?: return false
        val targetHost = extractHost(targetUrl) ?: return false

        val targetLower = targetUrl.lowercase()
        if (targetLower.contains("/out.php") || targetLower.contains("/pop.php")) {
            com.omnicast.sender.log.SenderLogger.w("AD_GATEWAY_BLOCKED", "🛑 Pasarela bloqueada: $targetHost (desde $curHost)", details = mapOf("target_host" to targetHost, "cur_host" to curHost, "target_url" to targetUrl))
            return true
        }

        if (targetHost == curHost || targetHost.endsWith(".$curHost") || curHost.endsWith(".$targetHost") || AdBlocker.isKnownMediaHost(targetHost)) return false
        val curBrand = extractBrand(curHost); val targetBrand = extractBrand(targetHost)
        if (curBrand.length >= 4 && curBrand == targetBrand) return false
        if (AdBlocker.isVerificationOrCaptcha(targetUrl) || AdBlockWhitelistManager.isWhitelistedHost(targetHost)) return false

        val isTargetSearchEngine = AdBlocker.isSearchEngine(targetHost)
        val isSourceSearchEngine = AdBlocker.isSearchEngine(curHost)
        if (isSourceSearchEngine) return false

        if (isTargetSearchEngine) {
            com.omnicast.sender.log.SenderLogger.w("CLICK_HIJACK_BLOCKED", "🛑 Clic a buscador bloqueado: $targetHost desde $curHost", details = mapOf("target_host" to targetHost, "cur_host" to curHost, "target_url" to targetUrl))
            return true
        }

        if (AdBlockWhitelistManager.isWhitelistedHost(curHost)) return false

        try {
            com.omnicast.sender.log.SenderLogger.w("CLICK_HIJACK_BLOCKED", "🛑 Secuestro de clic bloqueado hacia: $targetHost desde $curHost", details = mapOf("target_host" to targetHost, "cur_host" to curHost, "target_url" to targetUrl))
            AdBlocker.learnAndBlockAdHost(targetHost, curHost)
        } catch (_: Throwable) {}
        return true
    }
}
