package com.omnicast.core.domain.ports

import com.omnicast.core.domain.models.StreamProfile
import kotlinx.coroutines.flow.StateFlow

/**
 * Puerto de abstracción para el motor de captura de pantalla.
 */
interface IScreenCaptureEngine {
    val state: StateFlow<CaptureState>
    
    fun startCapture(profile: StreamProfile)
    fun pauseCapture()
    fun resumeCapture()
    fun stopCapture()
}

enum class CaptureState {
    IDLE,
    PREPARING,
    CAPTURING,
    PAUSED,
    ERROR
}
