package com.omnicast.sender.ui.browser.sentinel

import android.util.Log
import android.webkit.WebView
import com.omnicast.sender.log.SenderLogger

/**
 * Centinela 2: Satélite modular desacoplado de diagnóstico y telemetría de reproductores web.
 * Principio "Poner y Sacar" (AGENTS.md):
 * Captura y despacha en tiempo real a Supabase errores nativos de <video>, estancamiento y logs críticos de reproductores.
 */
object WebPlayerDiagnosticSentinel {

    private const val PREFIX_VIDEO_ERROR = "OMNI_DOM_VIDEO_ERROR:"
    private const val PREFIX_VIDEO_STALLED = "OMNI_DOM_VIDEO_STALLED:"

    fun initWebDebugging() {
        try {
            WebView.setWebContentsDebuggingEnabled(true)
        } catch (_: Throwable) {}
    }

    fun inspectApiRequest(url: String, headers: Map<String, String>?) {
        if (url.contains("/api/")) {
            val summary = headers?.entries?.joinToString(", ") { "${it.key}=${it.value.take(35)}" } ?: "sin cabeceras"
            Log.i("OMNICAST_API", "📡 API Req: $url | Headers: [$summary]")
        }
    }

    fun handleHttpError(url: String, statusCode: Int, reason: String, referer: String?) {
        SenderLogger.w(
            "OMNICAST_HTTP_ERR",
            "⚠️ HTTP Error $statusCode ($reason): $url (Ref: $referer)",
            details = mapOf("code" to statusCode, "url" to url, "reason" to reason)
        )
    }

    fun handleDiagnosticConsoleMessage(message: String, currentUrl: String): Boolean {
        if (message.startsWith(PREFIX_VIDEO_ERROR)) {
            val payload = message.removePrefix(PREFIX_VIDEO_ERROR).trim()
            val activeUrl = com.omnicast.sender.stream.ActiveStreamHolder.streamUrl.orEmpty()
            if (activeUrl.isNotBlank()) com.omnicast.core.domain.usecases.StreamCandidateValidator.markCdnHostOffline(activeUrl)
            SenderLogger.w(
                "DOM_VIDEO_ERROR",
                "⚠️ Error en etiqueta video del DOM",
                details = mapOf("payload" to payload, "page" to currentUrl, "streamUrl" to activeUrl)
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
            val isFatal = lower.contains("manifestloaderror") || lower.contains("media_err_network")
            if (isFatal) {
                val activeUrl = com.omnicast.sender.stream.ActiveStreamHolder.streamUrl.orEmpty()
                if (activeUrl.isNotBlank()) {
                    com.omnicast.core.domain.usecases.StreamCandidateValidator.markCdnHostOffline(activeUrl)
                    com.omnicast.sender.stream.ActiveStreamHolder.notifyPlaybackError("MANIFEST_LOAD_ERROR")
                }
            }
            SenderLogger.w(
                "WEB_PLAYER_LOG",
                "📺 Log reproductor web: $message",
                details = mapOf("msg" to message, "page" to currentUrl)
            )
            return true
        }

        if (lower.contains("error") || lower.contains("uncaught") || lower.contains("failed to load") || lower.contains("403") || lower.contains("turnstile")) {
            Log.w("BROWSER_JS_DIAG", "🔍 [Console] $message (en $currentUrl)")
        }

        return false
    }
}
