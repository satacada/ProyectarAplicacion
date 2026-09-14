package com.omnicast.core.domain.models

/**
 * Perfil de calidad y rendimiento de la transmisión.
 */
data class StreamProfile(
    val mode: QualityMode,
    val width: Int,
    val height: Int,
    val frameRate: Int,
    val targetBitrateKbps: Int,
    val maxLatencyMs: Int
) {
    companion object {
        val GAMER_720P = StreamProfile(
            mode = QualityMode.GAMER_LOW_LATENCY,
            width = 1280,
            height = 720,
            frameRate = 60,
            targetBitrateKbps = 6000,
            maxLatencyMs = 45
        )

        val BALANCED_1080P = StreamProfile(
            mode = QualityMode.BALANCED,
            width = 1920,
            height = 1080,
            frameRate = 60,
            targetBitrateKbps = 10000,
            maxLatencyMs = 65
        )

        val CINEMA_HIGH_BITRATE = StreamProfile(
            mode = QualityMode.CINEMA,
            width = 1920,
            height = 1080,
            frameRate = 60,
            targetBitrateKbps = 16000,
            maxLatencyMs = 85
        )
    }
}

enum class QualityMode {
    GAMER_LOW_LATENCY,
    BALANCED,
    CINEMA
}
