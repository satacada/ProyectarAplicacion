package com.omnicast.core.domain.ports

import com.omnicast.core.domain.models.MediaStream
import com.omnicast.core.domain.models.StreamCandidate

/**
 * Puerto de salida para adaptadores de extracción de flujos multimedia ("poner y sacar").
 * Cada extractor implementa este contrato de forma 100% aislada sin acoplamiento a otros.
 */
interface IMediaStreamExtractorPort {
    val priority: Int get() = 0

    /**
     * Determina si este extractor sabe procesar la solicitud o candidato multimedia.
     */
    fun canHandle(candidate: StreamCandidate): Boolean

    /**
     * Extrae el flujo multimedia formateado y normalizado, o null si no corresponde.
     */
    fun extract(candidate: StreamCandidate): MediaStream?
}
