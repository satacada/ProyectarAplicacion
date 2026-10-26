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
    var subtitles: List<com.omnicast.core.protocol.SignalingMessage.SubtitleTrack> = emptyList()
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

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError = _playbackError.asStateFlow()

    private val _isProxied = MutableStateFlow(false)
    val isProxied = _isProxied.asStateFlow()

    fun setProxied(proxied: Boolean) { _isProxied.value = proxied }
    fun notifyPlaybackError(err: String?) { _playbackError.value = err }
    fun clearPlaybackError() { _playbackError.value = null }

    var onSendPlayControl: ((action: String, posMs: Long, vol: Float, param: String?) -> Unit)? = null

    fun update(url: String, videoTitle: String? = null, hdrs: Map<String, String> = emptyMap(), subs: List<com.omnicast.core.protocol.SignalingMessage.SubtitleTrack> = emptyList()) {
        streamUrl = url
        val clean = videoTitle?.takeIf { it.isNotBlank() && it != "Cargando..." && it != "Transmisión" } ?: "Video Detectado"
        title = clean
        headers = hdrs
        if (subs.isNotEmpty()) subtitles = subs
    }

    fun clearDetectedStream() {
        streamUrl = null
        title = null
        headers = emptyMap()
        subtitles = emptyList()
    }

    fun clear() {
        streamUrl = null
        title = null
        headers = emptyMap()
        subtitles = emptyList()
        _isCasting.value = false
        _isPlaying.value = false
        _positionMs.value = 0L
        _durationMs.value = 0L
        _currentMediaTitle.value = ""
        _tracksInfo.value = null
        _playbackError.value = null
        _isProxied.value = false
        com.omnicast.sender.overlay.FloatingIndicatorService.updateMode()
    }

    fun startCasting(url: String, videoTitle: String? = null, hdrs: Map<String, String> = emptyMap(), subs: List<com.omnicast.core.protocol.SignalingMessage.SubtitleTrack> = emptyList()) {
        update(url, videoTitle, hdrs, subs)
        _currentMediaTitle.value = videoTitle?.takeIf { it.isNotBlank() } ?: "Video en Transmisión"
        _isCasting.value = true
        _isPlaying.value = false
        _positionMs.value = 0L
        _durationMs.value = 0L
        _tracksInfo.value = null
        _isProxied.value = url.contains(":8092")
        com.omnicast.sender.overlay.FloatingIndicatorService.updateMode()
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

    fun stopCasting(notifyTv: Boolean = true) {
        _isCasting.value = false
        _isPlaying.value = false
        _tracksInfo.value = null
        com.omnicast.sender.overlay.FloatingIndicatorService.updateMode()
        if (notifyTv) {
            onSendPlayControl?.invoke("STOP", 0L, 1.0f, null)
        }
    }

    fun stopCastingAndClear() {
        stopCasting()
        clearDetectedStream()
    }

    fun hasStream(): Boolean = !streamUrl.isNullOrBlank()
}
