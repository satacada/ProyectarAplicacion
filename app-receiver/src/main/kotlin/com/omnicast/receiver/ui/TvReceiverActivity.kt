package com.omnicast.receiver.ui

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.omnicast.core.protocol.SignalingMessage
import com.omnicast.receiver.audio.DigitalAudioReceiver
import com.omnicast.receiver.directstream.ExoPlayerTvManager
import com.omnicast.receiver.server.ClientStatus
import com.omnicast.receiver.server.LocalSignalingServer
import com.omnicast.receiver.discovery.NsdAdvertiserManager
import com.omnicast.receiver.ui.components.*
import com.omnicast.receiver.webrtc.WebRtcReceiverManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

class TvReceiverActivity : ComponentActivity() {

    private val receiverState = mutableStateOf<ReceiverUiState>(ReceiverUiState.Standby)
    private val localIpAddress = mutableStateOf("127.0.0.1")
    private val youTubeControlFlow = MutableSharedFlow<SignalingMessage.PlayControl>(extraBufferCapacity = 16)
    private lateinit var webRtcReceiver: WebRtcReceiverManager
    private lateinit var exoPlayerManager: ExoPlayerTvManager
    private lateinit var signalingServer: LocalSignalingServer
    private lateinit var nsdAdvertiser: NsdAdvertiserManager
    private lateinit var historyManager: com.omnicast.receiver.directstream.TvPlaybackHistoryManager
    private lateinit var playlistManager: com.omnicast.receiver.directstream.TvPlaylistManager
    private lateinit var updateManager: com.omnicast.receiver.update.TvUpdateManager
    private var digitalAudioReceiver: DigitalAudioReceiver? = null
    private var statusJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); localIpAddress.value = getLocalIpAddress()
        com.omnicast.receiver.log.ReceiverLogger.init(this)
        val ver = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (_: Throwable) { "?" }
        com.omnicast.receiver.log.ReceiverLogger.log("TV_RECEIVER", "🚀 OmniCast Receiver v$ver en ${localIpAddress.value}:8088")
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) { com.omnicast.receiver.directstream.TvMediaCacheManager.clearDiskCache(this@TvReceiverActivity) }
        webRtcReceiver = WebRtcReceiverManager(this).apply {
            initialize()
            onLocalIceCandidate = { c ->
                lifecycleScope.launch {
                    signalingServer.sendMessage(SignalingMessage.IceCandidate(c.sdpMid, c.sdpMLineIndex, c.sdp))
                }
            }
            onConnectionClosed = { runOnUiThread { if (receiverState.value is ReceiverUiState.ScreenMirroring) receiverState.value = ReceiverUiState.Standby } }
        }
        exoPlayerManager = ExoPlayerTvManager(this).apply {
            onTracksChanged = { info -> lifecycleScope.launch { signalingServer.sendMessage(info) } }
            onPlaybackFailed = { runOnUiThread { Toast.makeText(this@TvReceiverActivity, "⚠️ Servidor no disponible o bloqueado (Error HTTP)", Toast.LENGTH_SHORT).show(); if (receiverState.value is ReceiverUiState.DirectStreamPlaying) { receiverState.value = ReceiverUiState.Standby; lifecycleScope.launch { signalingServer.sendMessage(SignalingMessage.PlaybackStatus("STOPPED", 0L, 0L)) } } } }
        }
        digitalAudioReceiver = DigitalAudioReceiver()
        historyManager = com.omnicast.receiver.directstream.TvPlaybackHistoryManager(this)
        playlistManager = com.omnicast.receiver.directstream.TvPlaylistManager(this)
        updateManager = com.omnicast.receiver.update.TvUpdateManager(this)
        signalingServer = LocalSignalingServer(port = 8088).apply { start() }
        nsdAdvertiser = NsdAdvertiserManager(this).apply { registerService(port = 8088) }
        lifecycleScope.launch { delay(2500); updateManager.checkForUpdates() }

        lifecycleScope.launch {
            signalingServer.clientStatus.collect { st ->
                when (st) {
                    is ClientStatus.Connected -> {
                        if (receiverState.value is ReceiverUiState.Standby) receiverState.value = ReceiverUiState.ClientConnectedStandby(st.clientAddress)
                        (receiverState.value as? ReceiverUiState.DirectStreamPlaying)?.let { cur ->
                            val pState = if (exoPlayerManager.isPlaying) "PLAYING" else "PAUSED"
                            signalingServer.sendMessage(SignalingMessage.PlaybackStatus(pState, exoPlayerManager.currentPositionMs, exoPlayerManager.durationMs, cur.streamUrl, cur.title))
                        }
                    }
                    is ClientStatus.Disconnected -> {
                        digitalAudioReceiver?.stop(); webRtcReceiver.resetConnection()
                        if (receiverState.value !is ReceiverUiState.Standby && receiverState.value !is ReceiverUiState.DirectStreamPlaying) {
                            receiverState.value = ReceiverUiState.Standby
                        }
                    }
                }
            }
        }
        lifecycleScope.launch { signalingServer.incomingMessages.collect { handleSignalingMessage(it) } }

        setContent {
            val historyList by historyManager.history.collectAsState()
            val playlistsList by playlistManager.playlists.collectAsState()
            val otaState by updateManager.updateState.collectAsState()
            MaterialTheme(colorScheme = darkColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF38BDF8), background = androidx.compose.ui.graphics.Color(0xFF0F172A))) {
                when (val state = receiverState.value) {
                    is ReceiverUiState.Standby -> TvStandbyCleanScreen(
                        ipAddress = localIpAddress.value, history = historyList, playlists = playlistsList,
                        onPlayHistory = { playHistoryItem(it) }, onPlayPlaylist = { playPlaylistItem(it) },
                        updateState = otaState, onCheckUpdate = { lifecycleScope.launch { updateManager.checkForUpdates() } },
                        onDownloadAndInstall = { url ->
                            lifecycleScope.launch {
                                if (otaState is com.omnicast.receiver.update.TvUpdateState.ReadyToInstall) {
                                    (otaState as com.omnicast.receiver.update.TvUpdateState.ReadyToInstall).apkFile.let { updateManager.triggerApkInstall(it) }
                                } else updateManager.downloadAndInstall(url)
                            }
                        }
                    )
                    is ReceiverUiState.ClientConnectedStandby -> TvConnectedStandbyScreen(state.clientAddress) { receiverState.value = ReceiverUiState.Standby }
                    is ReceiverUiState.ScreenMirroring -> TvMirroringActiveScreen(
                        webRtcReceiver, state.resolution, state.fps,
                        onCloseClick = { digitalAudioReceiver?.stop(); webRtcReceiver.resetConnection(); receiverState.value = ReceiverUiState.Standby }
                    )
                    is ReceiverUiState.DirectStreamPlaying -> TvDirectStreamScreen(
                        exoPlayerManager, state.title, state.streamUrl,
                        onCloseClick = { exoPlayerManager.stop("Botón Cerrar TV"); receiverState.value = ReceiverUiState.Standby }
                    )
                    is ReceiverUiState.YouTubePlaying -> TvYouTubeScreen(
                        videoId = state.videoId,
                        title = state.title,
                        timestamp = state.timestamp,
                        commandFlow = youTubeControlFlow,
                        onPlaybackStatus = { pos, dur, playing ->
                            lifecycleScope.launch {
                                val st = if (playing) "PLAYING" else "PAUSED"
                                signalingServer.sendMessage(SignalingMessage.PlaybackStatus(st, pos, dur))
                            }
                        },
                        onCloseClick = { receiverState.value = ReceiverUiState.Standby }
                    )
                    is ReceiverUiState.ImageDisplaying -> TvImageScreen(
                        imageUrl = state.imageUrl,
                        title = state.title,
                        scale = state.scale,
                        panX = state.panX,
                        panY = state.panY,
                        rotation = state.rotation,
                        onPrevClick = {
                            lifecycleScope.launch { signalingServer.sendMessage(SignalingMessage.PlayControl("PREV")) }
                            Toast.makeText(this@TvReceiverActivity, "◀ Foto Anterior", Toast.LENGTH_SHORT).show()
                        },
                        onNextClick = {
                            lifecycleScope.launch { signalingServer.sendMessage(SignalingMessage.PlayControl("NEXT")) }
                            Toast.makeText(this@TvReceiverActivity, "Foto Siguiente ▶", Toast.LENGTH_SHORT).show()
                        },
                        onCloseClick = { receiverState.value = ReceiverUiState.Standby },
                        onRotateClick = { (receiverState.value as? ReceiverUiState.ImageDisplaying)?.let { receiverState.value = it.copy(rotation = (it.rotation + 90f) % 360f) } }
                    )
                }
            }
        }
    }

    private fun handleSignalingMessage(message: SignalingMessage) {
        when (message) {
            is SignalingMessage.Offer -> {
                exoPlayerManager.stop(); digitalAudioReceiver?.start(); webRtcReceiver.resetConnection()
                receiverState.value = ReceiverUiState.ScreenMirroring("${message.width}x${message.height}", message.fps)
                webRtcReceiver.setSmoothBuffer(message.smoothBuffer)
                webRtcReceiver.handleOffer(message.sdp) { sdp -> lifecycleScope.launch { signalingServer.sendMessage(SignalingMessage.Answer(sdp)) } }
            }
            is SignalingMessage.IceCandidate -> webRtcReceiver.addRemoteIceCandidate(message.sdpMid, message.sdpMLineIndex, message.candidate)
            is SignalingMessage.DirectStreamHandoff -> {
                runCatching {
                    digitalAudioReceiver?.stop(); webRtcReceiver.resetConnection()
                    val title = message.title?.ifBlank { "Transmisión Web" } ?: "Transmisión Web"
                    val ytId = message.headers["X-YouTube-Id"] ?: extractYouTubeId(message.mediaUrl)
                    if (!ytId.isNullOrBlank()) {
                        exoPlayerManager.stop(); receiverState.value = ReceiverUiState.YouTubePlaying(ytId, title, System.currentTimeMillis())
                    } else {
                        receiverState.value = ReceiverUiState.DirectStreamPlaying(message.mediaUrl, title)
                        exoPlayerManager.playStream(message.mediaUrl, message.headers, title, message.startPositionMs)
                        lifecycleScope.launch { historyManager.addOrUpdate(message.mediaUrl, title, message.headers, message.startPositionMs) }
                        startPlaybackStatusReporting()
                    }
                }.onFailure { err ->
                    com.omnicast.receiver.log.ReceiverLogger.log("TvReceiverActivity", "Error al procesar DirectStreamHandoff: ${err.message}", err)
                    receiverState.value = ReceiverUiState.Standby
                }
            }
            is SignalingMessage.PlayControl -> {
                if (receiverState.value is ReceiverUiState.YouTubePlaying) {
                    youTubeControlFlow.tryEmit(message)
                }
                when (message.action) {
                    "PLAY" -> {
                        if (message.positionMs > 0L && !exoPlayerManager.isNearEnd()) exoPlayerManager.seekTo(message.positionMs)
                        exoPlayerManager.resume()
                    }
                    "PAUSE" -> exoPlayerManager.pause()
                    "STOP" -> { exoPlayerManager.stop("Comando STOP celular"); receiverState.value = ReceiverUiState.Standby }
                    "SEEK" -> exoPlayerManager.seekTo(message.positionMs)
                    "VOLUME" -> applySystemAndPlayerVolume(message.volume)
                    "SUBTITLE_TOGGLE", "TOGGLE_SUBTITLES" -> exoPlayerManager.toggleSubtitles()
                    "SET_SUBTITLE_TRACK" -> message.param?.toIntOrNull()?.let { exoPlayerManager.selectTextTrack(it) }
                    "SET_AUDIO_TRACK" -> message.param?.toIntOrNull()?.let { exoPlayerManager.selectAudioTrack(it) }
                    "ROTATE" -> (receiverState.value as? ReceiverUiState.ImageDisplaying)?.let { receiverState.value = it.copy(rotation = (it.rotation + 90f) % 360f) }
                }
            }
            is SignalingMessage.ImageTransform -> {
                val cur = receiverState.value
                if (cur is ReceiverUiState.ImageDisplaying) receiverState.value = cur.copy(scale = message.scale, panX = message.panX, panY = message.panY, rotation = message.rotation)
            }
            is SignalingMessage.SetDisplayMode -> {
                webRtcReceiver.setDisplayMode(message.mode, message.zoom, message.panY, message.panX)
                exoPlayerManager.setDisplayMode(message.mode, message.zoom)
            }
            is SignalingMessage.ShowImage -> {
                digitalAudioReceiver?.stop(); exoPlayerManager.stop("Casteo Foto"); webRtcReceiver.resetConnection()
                receiverState.value = ReceiverUiState.ImageDisplaying(message.imageUrl, message.title ?: "Foto de Galería")
            }
            is SignalingMessage.SyncPlaylists -> {
                lifecycleScope.launch { playlistManager.syncPlaylistsJson(message.playlistsJson) }
            }
            is SignalingMessage.Disconnect -> {
                digitalAudioReceiver?.stop(); exoPlayerManager.stop("Desconexión"); webRtcReceiver.resetConnection(); receiverState.value = ReceiverUiState.Standby
            }
            else -> Unit
        }
    }

    private fun startPlaybackStatusReporting() {
        statusJob?.cancel()
        statusJob = lifecycleScope.launch {
            while (true) {
                delay(1000)
                val cur = receiverState.value
                if (cur is ReceiverUiState.DirectStreamPlaying) {
                    val isPlaying = exoPlayerManager.isPlaying
                    val pos = exoPlayerManager.currentPositionMs
                    val dur = exoPlayerManager.durationMs
                    val st = if (isPlaying) "PLAYING" else "PAUSED"
                    signalingServer.sendMessage(SignalingMessage.PlaybackStatus(st, pos, dur, cur.streamUrl, cur.title))
                    historyManager.updateProgress(cur.streamUrl, pos, dur); playlistManager.updateProgress(cur.streamUrl, pos)
                } else {
                    signalingServer.sendMessage(SignalingMessage.PlaybackStatus("STOPPED", 0L, 0L))
                    break
                }
            }
        }
    }

    private fun applySystemAndPlayerVolume(vol: Float) {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = (vol.coerceIn(0f, 1f) * maxVol).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
            }
        } catch (_: Throwable) {}
        exoPlayerManager.setVolume(vol)
    }

    private fun getLocalIpAddress(): String = try {
        NetworkInterface.getNetworkInterfaces()?.toList()?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address && it.hostAddress != "127.0.0.1" }?.hostAddress ?: "127.0.0.1"
    } catch (_: Throwable) { "127.0.0.1" }

    private fun extractYouTubeId(url: String): String? =
        Regex("(?:watch\\?v=|shorts/|youtu\\.be/|embed/)([a-zA-Z0-9_-]{11})").find(url)?.groupValues?.get(1)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && TvRemoteKeyHandler.handleKeyDown(
                context = this,
                keyCode = event.keyCode,
                event = event,
                currentState = receiverState.value, exoPlayerManager = exoPlayerManager,
                webRtcReceiver = webRtcReceiver, digitalAudioReceiver = digitalAudioReceiver,
                youTubeControlFlow = youTubeControlFlow, scope = lifecycleScope,
                sendSignaling = { lifecycleScope.launch { signalingServer.sendMessage(it) } },
                onResetToStandby = { receiverState.value = ReceiverUiState.Standby }
            )) return true
        return super.dispatchKeyEvent(event)
    }

    private fun playHistoryItem(item: com.omnicast.receiver.directstream.TvHistoryItem) {
        digitalAudioReceiver?.stop(); webRtcReceiver.resetConnection()
        receiverState.value = ReceiverUiState.DirectStreamPlaying(item.mediaUrl, item.title)
        exoPlayerManager.playStream(item.mediaUrl, item.headers, item.title, item.lastPositionMs); startPlaybackStatusReporting()
    }

    private fun playPlaylistItem(item: com.omnicast.receiver.directstream.TvPlaylistItem) {
        digitalAudioReceiver?.stop(); webRtcReceiver.resetConnection()
        receiverState.value = ReceiverUiState.DirectStreamPlaying(item.url, item.title)
        exoPlayerManager.playStream(item.url, item.headers, item.title, playlistManager.getResumePosition(item)); startPlaybackStatusReporting()
    }

    override fun onDestroy() {
        super.onDestroy(); com.omnicast.receiver.log.ReceiverLogger.stop()
        digitalAudioReceiver?.stop(); nsdAdvertiser.unregisterService()
        signalingServer.stop(); webRtcReceiver.release(); exoPlayerManager.release()
    }
}
