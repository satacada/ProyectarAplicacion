package com.omnicast.sender.service

import com.omnicast.sender.overlay.FloatingIndicatorService
import com.omnicast.sender.stream.ActiveStreamHolder

enum class ActiveCastMode {
    IDLE,
    SCREEN_MIRROR,
    WEB_STREAM,
    GALLERY
}

/**
 * Gestor del modo activo de proyección para coordinar el menú contextual del asistente flotante.
 */
object CastSessionState {

    var isGalleryCasting: Boolean = false
        set(value) {
            field = value
            FloatingIndicatorService.updateMode()
        }

    fun getCurrentMode(): ActiveCastMode {
        return when {
            ProjectionForegroundService.isProjecting -> ActiveCastMode.SCREEN_MIRROR
            ActiveStreamHolder.isCasting.value -> ActiveCastMode.WEB_STREAM
            isGalleryCasting -> ActiveCastMode.GALLERY
            else -> ActiveCastMode.IDLE
        }
    }
}
