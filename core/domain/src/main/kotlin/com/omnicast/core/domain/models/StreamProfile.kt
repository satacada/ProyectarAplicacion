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
    val maxLatencyMs: Int,
    val label: String = "",
    val badge: String = "",
    val description: String = ""
) {
    companion object {
        val BALANCED_30FPS = StreamProfile(
            mode = QualityMode.BALANCED_30FPS,
            width = 1920,
            height = 1280,
            frameRate = 30,
            targetBitrateKbps = 3500,
            maxLatencyMs = 65,
            label = "1280p @ 30 FPS",
            badge = "✅ Estable",
            description = "1280px altura @ 30 FPS (~24M px/s) — Calidad nítida y óptima sin saturar el encoder."
        )

        val FLUID_45FPS = StreamProfile(
            mode = QualityMode.FLUID_45FPS,
            width = 1280,
            height = 720,
            frameRate = 45,
            targetBitrateKbps = 3000,
            maxLatencyMs = 50,
            label = "720p @ 45 FPS",
            badge = "⚡ Fluido",
            description = "720px altura @ 45 FPS (~27M px/s) — Movimiento suave, balance ideal para series y streaming."
        )

        val ULTRA_60FPS = StreamProfile(
            mode = QualityMode.ULTRA_60FPS,
            width = 960,
            height = 540,
            frameRate = 60,
            targetBitrateKbps = 2500,
            maxLatencyMs = 40,
            label = "540p @ 60 FPS",
            badge = "🚀 60 FPS",
            description = "540px altura @ 60 FPS (~20M px/s) — Ultra fluido a 60 FPS reales sin saturación, ideal para deportes y juegos."
        )

        val MAX_60FPS = StreamProfile(
            mode = QualityMode.MAX_60FPS,
            width = 1920,
            height = 1280,
            frameRate = 60,
            targetBitrateKbps = 4500,
            maxLatencyMs = 65,
            label = "1280p @ 60 FPS",
            badge = "⚠️ Experimental",
            description = "1280px altura @ 60 FPS (~48M px/s) — Carga muy alta. Puede saturar procesadores de gama media."
        )

        val CINEMA_1080P_30FPS = StreamProfile(
            mode = QualityMode.CINEMA_1080P_30FPS,
            width = 1920,
            height = 1080,
            frameRate = 30,
            targetBitrateKbps = 4500,
            maxLatencyMs = 65,
            label = "1080p @ 30 FPS",
            badge = "🎬 1080p Cine",
            description = "1080px altura @ 30 FPS (~35M px/s) — Máxima definición para películas y series con movimiento estable."
        )

        val ALL_PROFILES = listOf(
            CINEMA_1080P_30FPS,
            BALANCED_30FPS,
            FLUID_45FPS,
            ULTRA_60FPS,
            MAX_60FPS
        )

        // Alias retrocompatibles
        val BALANCED_1080P = BALANCED_30FPS
        val GAMER_720P = FLUID_45FPS
        val CINEMA_HIGH_BITRATE = MAX_60FPS
    }
}

enum class QualityMode {
    CINEMA_1080P_30FPS,
    BALANCED_30FPS,
    FLUID_45FPS,
    ULTRA_60FPS,
    MAX_60FPS,
    GAMER_LOW_LATENCY,
    BALANCED,
    CINEMA
}
