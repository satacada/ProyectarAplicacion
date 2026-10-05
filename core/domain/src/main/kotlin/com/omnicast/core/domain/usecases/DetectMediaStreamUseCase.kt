package com.omnicast.core.domain.usecases

import com.omnicast.core.domain.models.MediaStream
import com.omnicast.core.domain.models.StreamCandidate
import com.omnicast.core.domain.ports.IMediaStreamExtractorPort
import com.omnicast.core.domain.ports.IStreamTelemetryFilterPort

/**
 * Caso de uso del núcleo hexagonal para la detección orquestada de flujos multimedia.
 * Desacopla la lógica de negocio de la UI, la WebView y los adaptadores específicos.
 */
class DetectMediaStreamUseCase(
    private val telemetryFilter: IStreamTelemetryFilterPort,
    extractors: List<IMediaStreamExtractorPort>
) {
    private val sortedExtractors = extractors.sortedByDescending { it.priority }

    /**
     * Evalúa el candidato. Retorna el MediaStream extraído o null si no es un flujo reproducible.
     */
    fun execute(candidate: StreamCandidate): MediaStream? {
        if (candidate.url.isBlank()) return null
        if (telemetryFilter.isTelemetryOrGarbage(candidate.url)) return null

        for (extractor in sortedExtractors) {
            if (extractor.canHandle(candidate)) {
                val stream = extractor.extract(candidate)
                if (stream != null) return stream
            }
        }
        return null
    }

    /**
     * Valida rápidamente si una URL es un flujo potencial antes de procesar cabeceras.
     */
    fun isStreamCandidate(url: String): Boolean {
        if (url.isBlank()) return false
        if (telemetryFilter.isTelemetryOrGarbage(url)) return false
        val mockCandidate = StreamCandidate(url = url)
        return sortedExtractors.any { it.canHandle(mockCandidate) }
    }
}
