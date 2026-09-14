package com.omnicast.sender.stream

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Almacena el último flujo multimedia y el estado de reproducción activa
 * para sincronizar la barra de control remoto y el asistente flotante.
 */
object ActiveStreamHolder {
    var streamUrl: String? = null
        private set
    var title: String? = null
        private set
    var headers: Map<String, String> = emptyMap()
        private set

    private val _isCasting = MutableStateFlow(false)
    val isCasting = _isCasting.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs = _durationMs.asStateFlow()

    private val _currentMediaTitle = MutableStateFlow("")
    val currentMediaTitle = _currentMediaTitle.asStateFlow()

    var onSendPlayControl: ((action: String, posMs: Long, vol: Float) -> Unit)? = null

    fun update(url: String, videoTitle: String? = null, hdrs: Map<String, String> = emptyMap()) {
        streamUrl = url
        title = videoTitle?.takeIf { it.isNotBlank() } ?: "Video Detectado"
        headers = hdrs
    }

    fun startCasting(url: String, videoTitle: String? = null, hdrs: Map<String, String> = emptyMap()) {
        update(url, videoTitle, hdrs)
        _currentMediaTitle.value = videoTitle?.takeIf { it.isNotBlank() } ?: "Video en Transmisión"
        _isCasting.value = true
        _isPlaying.value = true
        _positionMs.value = 0L
        _durationMs.value = 0L
    }

    fun updatePlayback(playing: Boolean, pos: Long, dur: Long) {
        _isPlaying.value = playing
        _positionMs.value = pos
        _durationMs.value = dur
    }

    fun stopCasting() {
        _isCasting.value = false
        _isPlaying.value = false
        onSendPlayControl?.invoke("STOP", 0L, 1.0f)
    }

    fun clear() {
        streamUrl = null
        title = null
        headers = emptyMap()
        _isCasting.value = false
    }

    fun hasStream(): Boolean = !streamUrl.isNullOrBlank()
}
