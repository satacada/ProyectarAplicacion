package com.omnicast.sender.service

import com.omnicast.sender.overlay.FloatingIndicatorService
import com.omnicast.sender.stream.ActiveStreamHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ActiveCastMode {
    IDLE,
    SCREEN_MIRROR,
    WEB_STREAM,
    GALLERY
}

/**
 * Gestor reactivo del modo activo de proyección para coordinar la UI y el asistente flotante.
 */
object CastSessionState {

    private val _isProjectingFlow = MutableStateFlow(false)
    val isProjectingFlow: StateFlow<Boolean> = _isProjectingFlow.asStateFlow()

    private val _isGalleryCastingFlow = MutableStateFlow(false)
    val isGalleryCastingFlow: StateFlow<Boolean> = _isGalleryCastingFlow.asStateFlow()

    var isGalleryCasting: Boolean
        get() = _isGalleryCastingFlow.value
        set(value) {
            _isGalleryCastingFlow.value = value
            FloatingIndicatorService.updateMode()
        }

    fun setProjecting(projecting: Boolean) {
        _isProjectingFlow.value = projecting
        FloatingIndicatorService.updateMode()
    }

    fun getCurrentMode(): ActiveCastMode {
        return when {
            _isProjectingFlow.value || ProjectionForegroundService.isProjecting -> ActiveCastMode.SCREEN_MIRROR
            ActiveStreamHolder.isCasting.value -> ActiveCastMode.WEB_STREAM
            _isGalleryCastingFlow.value -> ActiveCastMode.GALLERY
            else -> ActiveCastMode.IDLE
        }
    }
}
