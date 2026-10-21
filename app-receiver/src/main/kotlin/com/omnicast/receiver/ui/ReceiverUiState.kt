package com.omnicast.receiver.ui

/**
 * Estados de interfaz reactivos de la aplicación receptora en TV Box.
 */
sealed class ReceiverUiState {
    object Standby : ReceiverUiState()
    data class ClientConnectedStandby(val clientAddress: String) : ReceiverUiState()
    data class ScreenMirroring(val resolution: String, val fps: Int) : ReceiverUiState()
    data class DirectStreamPlaying(val streamUrl: String, val title: String) : ReceiverUiState()
    data class YouTubePlaying(val videoId: String, val title: String, val timestamp: Long = System.currentTimeMillis()) : ReceiverUiState()
    data class WebStreamPlaying(
        val streamUrl: String,
        val title: String,
        val headers: Map<String, String> = emptyMap(),
        val startPositionMs: Long = 0L,
        val timestamp: Long = System.currentTimeMillis()
    ) : ReceiverUiState()
    data class ImageDisplaying(
        val imageUrl: String,
        val title: String,
        val scale: Float = 1f,
        val panX: Float = 0f,
        val panY: Float = 0f,
        val rotation: Float = 0f
    ) : ReceiverUiState()
}
