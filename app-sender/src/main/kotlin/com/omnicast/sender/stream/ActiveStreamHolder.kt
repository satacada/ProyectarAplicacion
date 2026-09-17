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

    private val _tracksInfo = MutableStateFlow<com.omnicast.core.protocol.SignalingMessage.TracksInfo?>(null)
    val tracksInfo = _tracksInfo.asStateFlow()

    var onSendPlayControl: ((action: String, posMs: Long, vol: Float, param: String?) -> Unit)? = null

    fun update(url: String, videoTitle: String? = null, hdrs: Map<String, String> = emptyMap()) {
        streamUrl = url
        title = videoTitle?.takeIf { it.isNotBlank() } ?: "Video Detectado"
        headers = hdrs
    }

    fun clear() {
        streamUrl = null
        title = null
        headers = emptyMap()
        _isCasting.value = false
        _isPlaying.value = false
        _positionMs.value = 0L
        _durationMs.value = 0L
        _currentMediaTitle.value = ""
        _tracksInfo.value = null
    }

    fun startCasting(url: String, videoTitle: String? = null, hdrs: Map<String, String> = emptyMap()) {
        update(url, videoTitle, hdrs)
        _currentMediaTitle.value = videoTitle?.takeIf { it.isNotBlank() } ?: "Video en Transmisión"
        _isCasting.value = true
        _isPlaying.value = true
        _positionMs.value = 0L
        _durationMs.value = 0L
        _tracksInfo.value = null
    }

    fun updatePlayback(playing: Boolean, pos: Long, dur: Long) {
        _isPlaying.value = playing
        _positionMs.value = pos
        _durationMs.value = dur
    }

    fun updateTracksInfo(info: com.omnicast.core.protocol.SignalingMessage.TracksInfo) {
        _tracksInfo.value = info
    }

    private val _volume = MutableStateFlow(1.0f)
    val volume = _volume.asStateFlow()

    fun setVolume(vol: Float) {
        _volume.value = vol.coerceIn(0f, 1f)
    }

    fun stopCasting() {
        _isCasting.value = false
        _isPlaying.value = false
        _tracksInfo.value = null
        onSendPlayControl?.invoke("STOP", 0L, 1.0f, null)
    }

    fun hasStream(): Boolean = !streamUrl.isNullOrBlank()
}
