package com.omnicast.sender.service

import com.omnicast.core.protocol.SignalingMessage

data class TvZoomPreset(
    val id: String,
    val label: String,
    val mode: String,
    val zoom: Float,
    val panY: Float = 0f
)

/**
 * Gestor de zoom gradual para la TV.
 * Permite ajustar la imagen a la pantalla, no sólo agrandar sino también reducir
 * para que no se corten las caras ni los bordes del teléfono en la TV.
 */
object TvZoomManager {

    val PRESETS = listOf(
        TvZoomPreset("FIT_100", "📱 Fit Original (100%)", "FIT", 1.0f),
        TvZoomPreset("ZOOM_85", "🔍 Zoom TV: 85% (-15% Reduciendo)", "ZOOM", 0.85f),
        TvZoomPreset("ZOOM_75", "🔍 Zoom TV: 75% (-25% Reduciendo Max)", "ZOOM", 0.75f),
        TvZoomPreset("ZOOM_115", "🔍 Zoom TV: 115% (+15% Aumentando)", "ZOOM", 1.15f),
        TvZoomPreset("ZOOM_130", "🔍 Zoom TV: 130% (+30% Aumentando)", "ZOOM", 1.30f),
        TvZoomPreset("FILL_16_9", "📺 Zoom TV: 16:9 (Llenar Pantalla)", "FILL", 1.0f)
    )

    private var currentIndex = 0

    fun getCurrent(): TvZoomPreset = PRESETS[currentIndex]

    fun resetToFit(): TvZoomPreset {
        currentIndex = 0
        return PRESETS[0]
    }

    fun cycleNext(): TvZoomPreset {
        currentIndex = (currentIndex + 1) % PRESETS.size
        return PRESETS[currentIndex]
    }

    fun findPreset(idOrMode: String): TvZoomPreset {
        if (idOrMode == "APP") return TvZoomPreset("APP", "📲 Por Aplicación", "APP", 1.0f)
        if (idOrMode == "FULL_SCREEN") return TvZoomPreset("FULL_SCREEN", "📱 Pantalla Completa", "FULL_SCREEN", 1.0f)
        return PRESETS.firstOrNull { it.id == idOrMode }
            ?: PRESETS.firstOrNull { it.mode == idOrMode && it.zoom == 1.0f }
            ?: PRESETS.first()
    }

    fun toSignalingMessage(preset: TvZoomPreset, isLandscape: Boolean? = null): SignalingMessage.SetDisplayMode {
        return SignalingMessage.SetDisplayMode(preset.mode, preset.zoom, preset.panY, isLandscape = isLandscape)
    }
}
