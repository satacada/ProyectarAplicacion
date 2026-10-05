package com.omnicast.core.domain.models

/**
 * Entidad inmutable que representa un flujo multimedia listo para su reproducción en la TV Box.
 */
data class MediaStream(
    val url: String,
    val format: String,
    val title: String = "Transmisión",
    val isMaster: Boolean = false,
    val qualityLabel: String? = null,
    val headers: Map<String, String> = emptyMap()
)
