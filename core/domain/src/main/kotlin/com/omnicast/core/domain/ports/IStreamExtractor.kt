package com.omnicast.core.domain.ports

import com.omnicast.core.domain.models.StreamDetectionResult

/**
 * Puerto de interfaz pura para la extracción e inspección de flujos de video.
 * Siguiendo Clean Architecture, desacopla el análisis de la plataforma Android.
 */
interface IStreamExtractor {

    /**
     * Analiza el texto o URI suministrado (ej: portapapeles, enlaces web, streams)
     * e identifica si corresponde a una fuente de transmisión válida.
     */
    fun extractStream(input: String?): StreamDetectionResult

    /**
     * Valida si una URL apunta a un stream reproducible por el receptor (HLS, MP4, DASH, etc.).
     */
    fun isStreamUrl(url: String): Boolean
}
