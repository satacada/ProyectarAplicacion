package com.omnicast.core.domain.models

/**
 * Resultado inmutable del análisis de extracción de flujos de video.
 * Informa con precisión al usuario si la fuente fue capturada o si la app es cerrada/DRM.
 */
data class StreamDetectionResult(
    val status: Status,
    val mediaUrl: String? = null,
    val format: String = "HLS",
    val title: String? = null,
    val userMessage: String
) {
    enum class Status {
        SUCCESS,
        UNSUPPORTED_PROTECTED_APP,
        NO_STREAM_DETECTED
    }

    val isSuccess: Boolean
        get() = status == Status.SUCCESS
}
