package com.omnicast.core.domain.ports

/**
 * Puerto de abstracción para la captura de audio digital interno a 48kHz.
 */
interface IAudioCaptureEngine {
    fun startAudioCapture(sampleRate: Int = 48000, isStereo: Boolean = true)
    fun setMuted(isMuted: Boolean)
    fun stopAudioCapture()
}
