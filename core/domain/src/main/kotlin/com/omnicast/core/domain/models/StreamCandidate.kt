package com.omnicast.core.domain.models

/**
 * Representa una solicitud de red o recurso candidato a ser evaluado por los extractores.
 */
data class StreamCandidate(
    val url: String,
    val pageUrl: String = "",
    val headers: Map<String, String> = emptyMap(),
    val userAgent: String = "",
    val pageTitle: String? = null
)
