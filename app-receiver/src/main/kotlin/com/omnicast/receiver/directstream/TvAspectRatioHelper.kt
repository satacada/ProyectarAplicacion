package com.omnicast.receiver.directstream

import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TvAspectRatioMode(val resizeMode: Int, val label: String) {
    FIT(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Ajustar"),
    FILL_16_9(AspectRatioFrameLayout.RESIZE_MODE_FILL, "16:9 Estirado"),
    ZOOM(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Zoom (Sin Barras)")
}

/**
 * Gestor de relación de aspecto en caliente para ExoPlayer en Android TV.
 * Permite cambiar la escala directamente con el mando a distancia.
 */
class TvAspectRatioHelper {

    private val _currentMode = MutableStateFlow(TvAspectRatioMode.FIT)
    val currentMode: StateFlow<TvAspectRatioMode> = _currentMode.asStateFlow()

    fun cycleNextMode(): TvAspectRatioMode {
        val all = TvAspectRatioMode.values()
        val nextIdx = (_currentMode.value.ordinal + 1) % all.size
        val next = all[nextIdx]
        _currentMode.value = next
        return next
    }

    fun setMode(mode: TvAspectRatioMode) {
        _currentMode.value = mode
    }

    fun setModeByName(name: String): TvAspectRatioMode {
        val found = TvAspectRatioMode.values().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: TvAspectRatioMode.FIT
        _currentMode.value = found
        return found
    }
}
