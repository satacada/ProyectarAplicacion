package com.omnicast.core.domain.usecases

import com.omnicast.core.domain.models.StreamDetectionResult
import com.omnicast.core.domain.ports.IStreamExtractor

/**
 * Caso de uso y motor de análisis sintáctico de flujos de video.
 * Identifica enlaces HLS (.m3u8), DASH (.mpd), contenedores (.mp4, .mkv) y transmisiones web.
 */
class UrlStreamExtractor : IStreamExtractor {

    private val urlRegex = "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)".toRegex()

    override fun extractStream(input: String?): StreamDetectionResult {
        if (input.isNullOrBlank()) {
            return StreamDetectionResult(
                status = StreamDetectionResult.Status.NO_STREAM_DETECTED,
                userMessage = "No se encontró ningún enlace en el portapapeles ni texto copiado."
            )
        }

        val match = urlRegex.find(input)?.value
        if (match.isNullOrBlank()) {
            return StreamDetectionResult(
                status = StreamDetectionResult.Status.NO_STREAM_DETECTED,
                userMessage = "El contenido analizado no contiene un enlace web o de video válido."
            )
        }

        val lower = match.lowercase()
        return when {
            lower.contains(".m3u8") -> {
                StreamDetectionResult(
                    status = StreamDetectionResult.Status.SUCCESS,
                    mediaUrl = match,
                    format = "HLS",
                    title = "Transmisión HLS en Vivo",
                    userMessage = "Flujo HLS (.m3u8) detectado. Se reproducirá con aceleración por hardware en la TV."
                )
            }
            lower.contains(".mpd") -> {
                StreamDetectionResult(
                    status = StreamDetectionResult.Status.SUCCESS,
                    mediaUrl = match,
                    format = "DASH",
                    title = "Transmisión DASH",
                    userMessage = "Flujo DASH (.mpd) detectado. Listo para emitir en la TV Box."
                )
            }
            lower.endsWith(".mp4") || lower.contains(".mp4?") -> {
                StreamDetectionResult(
                    status = StreamDetectionResult.Status.SUCCESS,
                    mediaUrl = match,
                    format = "MP4",
                    title = "Video MP4",
                    userMessage = "Video directo MP4 detectado."
                )
            }
            lower.contains("youtube.com") || lower.contains("youtu.be") || lower.contains("facebook.com") -> {
                StreamDetectionResult(
                    status = StreamDetectionResult.Status.SUCCESS,
                    mediaUrl = match,
                    format = "WEB_STREAM",
                    title = "Transmisión Web",
                    userMessage = "Enlace de video web detectado. Enviando directo a la TV Box."
                )
            }
            lower.contains("mycdn.me") || lower.contains("dailymotion.com") || lower.contains("vimeo.com") || lower.contains(".webm") || lower.contains(".mkv") -> {
                StreamDetectionResult(
                    status = StreamDetectionResult.Status.SUCCESS,
                    mediaUrl = match,
                    format = "DIRECT_VIDEO",
                    title = "Video Web",
                    userMessage = "Video web detectado. Enviando a la TV Box."
                )
            }
            else -> {
                StreamDetectionResult(
                    status = StreamDetectionResult.Status.UNSUPPORTED_PROTECTED_APP,
                    userMessage = "El enlace no contiene un video reproducible directamente. Usa la 'Proyección de Pantalla Completa' para ver tu contenido en la TV."
                )
            }
        }
    }

    override fun isStreamUrl(url: String): Boolean {
        val lower = url.lowercase()
        if (lower.contains("stub.mp4") || lower.contains("dummy.mp4")) return false
        return lower.contains(".m3u8") ||
               lower.contains(".mpd") ||
               lower.contains(".mp4") ||
               lower.contains(".webm") ||
               lower.contains("mycdn.me") ||
               lower.contains("youtube.com") ||
               lower.contains("youtu.be")
    }
}
