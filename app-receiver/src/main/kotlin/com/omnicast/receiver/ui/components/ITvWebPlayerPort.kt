package com.omnicast.receiver.ui.components

import kotlinx.coroutines.flow.StateFlow

/**
 * Estado agnóstico del reproductor web (HTML5 / YouTube).
 */
data class TvWebPlayerState(
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isControlsVisible: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val currentQuality: String = "Auto",
    val availableQualities: List<String> = emptyList(),
    val currentSubtitle: String = "Desactivados",
    val availableSubtitles: List<String> = emptyList(),
    val aspectRatio: String = "FIT"
)

/**
 * Puerto Hexagonal de Control de Reproductores Web en TV (HTML5 y YouTube).
 * Desacopla la lógica de transporte de los componentes de interfaz gráfica nativa D-Pad.
 */
interface ITvWebPlayerPort {
    val state: StateFlow<TvWebPlayerState>
    fun play()
    fun pause()
    fun togglePlay()
    fun seekRelative(deltaMs: Long)
    fun seekTo(positionMs: Long)
    fun setQuality(quality: String)
    fun setSubtitle(subtitle: String)
    fun toggleAspectRatio()
    fun showControls(autoHideMs: Long = 4000L)
    fun hideControls()
    fun toggleSettings()
    fun closeSettings()
}
