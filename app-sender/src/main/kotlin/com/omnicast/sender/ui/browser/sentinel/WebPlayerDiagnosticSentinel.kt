package com.omnicast.sender.ui.browser.sentinel

import com.omnicast.sender.log.SenderLogger

/**
 * Centinela 2: Satélite modular desacoplado de diagnóstico y telemetría de reproductores web.
 * Principio "Poner y Sacar" (AGENTS.md):
 * Captura y despacha en tiempo real a Supabase errores nativos de <video>, estancamiento y logs críticos de reproductores.
 */
object WebPlayerDiagnosticSentinel {

    private const val PREFIX_VIDEO_ERROR = "OMNI_DOM_VIDEO_ERROR:"
    private const val PREFIX_VIDEO_STALLED = "OMNI_DOM_VIDEO_STALLED:"

    fun handleDiagnosticConsoleMessage(message: String, currentUrl: String): Boolean {
        if (message.startsWith(PREFIX_VIDEO_ERROR)) {
            val payload = message.removePrefix(PREFIX_VIDEO_ERROR).trim()
            SenderLogger.w(
                "DOM_VIDEO_ERROR",
                "⚠️ Error en etiqueta video del DOM",
                details = mapOf("payload" to payload, "page" to currentUrl)
            )
            return true
        }

        if (message.startsWith(PREFIX_VIDEO_STALLED)) {
            val payload = message.removePrefix(PREFIX_VIDEO_STALLED).trim()
            SenderLogger.w(
                "DOM_VIDEO_STALLED",
                "⏳ Etiqueta video estancada (stalled)",
                details = mapOf("payload" to payload, "page" to currentUrl)
            )
            return true
        }

        val lower = message.lowercase()
        val isPlayerLog = lower.contains("olplayer") || lower.contains("cant connect") || lower.contains("disable adblock") ||
                (lower.contains("vast") && (lower.contains("error") || lower.contains("aderror"))) ||
                (lower.contains("videojs") && lower.contains("error"))

        if (isPlayerLog) {
            SenderLogger.w(
                "WEB_PLAYER_LOG",
                "📺 Log reproductor web: $message",
                details = mapOf("msg" to message, "page" to currentUrl)
            )
            return true
        }

        return false
    }
}
