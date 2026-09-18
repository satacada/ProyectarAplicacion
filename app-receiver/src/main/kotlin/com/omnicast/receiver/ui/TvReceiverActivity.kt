package com.omnicast.receiver.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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
    private var digitalAudioReceiver: DigitalAudioReceiver? = null
    private var statusJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        localIpAddress.value = getLocalIpAddress()
        com.omnicast.receiver.log.ReceiverLogger.init(this)

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
        }
        digitalAudioReceiver = DigitalAudioReceiver()
        signalingServer = LocalSignalingServer(port = 8088).apply { start() }
        nsdAdvertiser = NsdAdvertiserManager(this).apply { registerService(port = 8088) }

        lifecycleScope.launch {
            signalingServer.clientStatus.collect { st ->
                when (st) {
                    is ClientStatus.Connected -> if (receiverState.value is ReceiverUiState.Standby) receiverState.value = ReceiverUiState.ClientConnectedStandby(st.clientAddress)
                    is ClientStatus.Disconnected -> if (receiverState.value is ReceiverUiState.ClientConnectedStandby) receiverState.value = ReceiverUiState.Standby
                }
            }
        }
        lifecycleScope.launch { signalingServer.incomingMessages.collect { handleSignalingMessage(it) } }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF38BDF8), background = androidx.compose.ui.graphics.Color(0xFF0F172A))) {
                when (val state = receiverState.value) {
                    is ReceiverUiState.Standby -> TvStandbyScreen(ipAddress = localIpAddress.value)
                    is ReceiverUiState.ClientConnectedStandby -> TvConnectedStandbyScreen(clientAddress = state.clientAddress)
                    is ReceiverUiState.ScreenMirroring -> TvMirroringActiveScreen(webRtcReceiver, state.resolution, state.fps)
                    is ReceiverUiState.DirectStreamPlaying -> TvDirectStreamScreen(exoPlayerManager, state.title, state.streamUrl)
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
                        }
                    )
                    is ReceiverUiState.ImageDisplaying -> TvImageScreen(state.imageUrl, state.title, state.scale, state.panX, state.panY)
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
                digitalAudioReceiver?.stop(); webRtcReceiver.resetConnection()
                val title = message.title?.ifBlank { "Transmisión Web" } ?: "Transmisión Web"
                val ytId = message.headers["X-YouTube-Id"] ?: extractYouTubeId(message.mediaUrl)
                if (!ytId.isNullOrBlank()) {
                    exoPlayerManager.stop(); receiverState.value = ReceiverUiState.YouTubePlaying(ytId, title, System.currentTimeMillis())
                } else {
                    receiverState.value = ReceiverUiState.DirectStreamPlaying(message.mediaUrl, title)
                    exoPlayerManager.playStream(message.mediaUrl, message.headers, title); startPlaybackStatusReporting()
                }
            }
            is SignalingMessage.PlayControl -> {
                if (receiverState.value is ReceiverUiState.YouTubePlaying) {
                    youTubeControlFlow.tryEmit(message)
                }
                when (message.action) {
                    "PLAY" -> exoPlayerManager.resume()
                    "PAUSE" -> exoPlayerManager.pause()
                    "STOP" -> {
                        exoPlayerManager.stop()
                        receiverState.value = ReceiverUiState.Standby
                    }
                    "SEEK" -> exoPlayerManager.seekTo(message.positionMs)
                    "VOLUME" -> applySystemAndPlayerVolume(message.volume)
                    "SUBTITLE_TOGGLE", "TOGGLE_SUBTITLES" -> exoPlayerManager.toggleSubtitles()
                    "SET_SUBTITLE_TRACK" -> message.param?.toIntOrNull()?.let { exoPlayerManager.selectTextTrack(it) }
                    "SET_AUDIO_TRACK" -> message.param?.toIntOrNull()?.let { exoPlayerManager.selectAudioTrack(it) }
                }
            }
            is SignalingMessage.ImageTransform -> {
                val cur = receiverState.value
                if (cur is ReceiverUiState.ImageDisplaying) receiverState.value = cur.copy(scale = message.scale, panX = message.panX, panY = message.panY)
            }
            is SignalingMessage.SetDisplayMode -> webRtcReceiver.setDisplayMode(message.mode, message.zoom, message.panY)
            is SignalingMessage.ShowImage -> {
                digitalAudioReceiver?.stop(); exoPlayerManager.stop(); webRtcReceiver.resetConnection()
                receiverState.value = ReceiverUiState.ImageDisplaying(message.imageUrl, message.title ?: "Foto de Galería")
            }
            is SignalingMessage.Disconnect -> {
                digitalAudioReceiver?.stop(); exoPlayerManager.stop(); webRtcReceiver.resetConnection(); receiverState.value = ReceiverUiState.Standby
            }
            else -> Unit
        }
    }

    private fun startPlaybackStatusReporting() {
        statusJob?.cancel()
        statusJob = lifecycleScope.launch {
            while (true) {
                delay(1000)
                if (receiverState.value is ReceiverUiState.DirectStreamPlaying) {
                    val st = if (exoPlayerManager.isPlaying) "PLAYING" else "PAUSED"
                    signalingServer.sendMessage(SignalingMessage.PlaybackStatus(st, exoPlayerManager.currentPositionMs, exoPlayerManager.durationMs))
                } else break
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && receiverState.value !is ReceiverUiState.Standby) {
            digitalAudioReceiver?.stop(); exoPlayerManager.stop(); webRtcReceiver.resetConnection()
            receiverState.value = ReceiverUiState.Standby; return true
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            val cur = receiverState.value
            if (cur is ReceiverUiState.YouTubePlaying) {
                try { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com/watch?v=${cur.videoId}")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) } catch (_: Throwable) {}
                return true
            }
            val mode = webRtcReceiver.toggleScalingType()
            val txt = if (mode == org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL) "Modo: Llenar Pantalla (16:9)" else "Modo: Ajustar Proporción (Fit)"
            Toast.makeText(this, txt, Toast.LENGTH_SHORT).show(); return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy(); com.omnicast.receiver.log.ReceiverLogger.stop()
        digitalAudioReceiver?.stop(); nsdAdvertiser.unregisterService()
        signalingServer.stop(); webRtcReceiver.release(); exoPlayerManager.release()
    }
}
