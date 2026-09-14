package com.omnicast.sender.stream

import android.content.ClipboardManager
import android.content.Context
import com.omnicast.core.domain.models.StreamDetectionResult
import com.omnicast.core.domain.ports.IStreamExtractor
import com.omnicast.core.domain.usecases.UrlStreamExtractor

/**
 * Adaptador de plataforma Android para extraer flujos multimedia desde el Portapapeles
 * o desde entradas directas del usuario.
 */
class ClipboardStreamExtractor(
    private val context: Context,
    private val delegateExtractor: IStreamExtractor = UrlStreamExtractor()
) : IStreamExtractor by delegateExtractor {

    /**
     * Inspecciona el portapapeles del sistema Android en busca de un enlace de transmisión.
     */
    fun extractFromClipboard(): StreamDetectionResult {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipData = clipboard?.primaryClip

            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0)?.coerceToText(context)?.toString()
                if (!text.isNullOrBlank()) {
                    val result = extractStream(text)
                    if (result.isSuccess) {
                        return result
                    }
                }
            }

            StreamDetectionResult(
                status = StreamDetectionResult.Status.UNSUPPORTED_PROTECTED_APP,
                userMessage = "No se detectó un enlace directo en el portapapeles o la app tiene el reproductor protegido internamente. Puedes usar 'Proyectar Pantalla Completa' para ver tu evento en la TV."
            )
        } catch (e: Throwable) {
            StreamDetectionResult(
                status = StreamDetectionResult.Status.UNSUPPORTED_PROTECTED_APP,
                userMessage = "No se pudo acceder al portapapeles: ${e.message}"
            )
        }
    }
}
