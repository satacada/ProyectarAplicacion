package com.omnicast.receiver.ui

import android.content.Context
import android.view.KeyEvent
import android.widget.Toast
import com.omnicast.core.protocol.SignalingMessage
import com.omnicast.receiver.audio.DigitalAudioReceiver
import com.omnicast.receiver.directstream.ExoPlayerTvManager
import com.omnicast.receiver.webrtc.WebRtcReceiverManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * Gestor de eventos de teclado y mando a distancia D-Pad para Android TV.
 * Desacoplado para mantener TvReceiverActivity por debajo de las 300 líneas.
 */
object TvRemoteKeyHandler {

    fun handleKeyDown(
        context: Context,
        keyCode: Int,
        event: KeyEvent?,
        currentState: ReceiverUiState,
        exoPlayerManager: ExoPlayerTvManager,
        webRtcReceiver: WebRtcReceiverManager,
        digitalAudioReceiver: DigitalAudioReceiver?,
        youTubeControlFlow: MutableSharedFlow<SignalingMessage.PlayControl>,
        scope: CoroutineScope,
        sendSignaling: (SignalingMessage) -> Unit,
        onResetToStandby: () -> Unit
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && currentState !is ReceiverUiState.Standby) {
            digitalAudioReceiver?.stop(); exoPlayerManager.stop(); webRtcReceiver.resetConnection()
            onResetToStandby()
            return true
        }

        when (currentState) {
            is ReceiverUiState.DirectStreamPlaying -> {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
                    KeyEvent.KEYCODE_SPACE -> {
                        if (exoPlayerManager.isPlaying) {
                            exoPlayerManager.pause()
                            Toast.makeText(context, "⏸️ Pausado", Toast.LENGTH_SHORT).show()
                        } else {
                            exoPlayerManager.resume()
                            Toast.makeText(context, "▶️ Reproduciendo", Toast.LENGTH_SHORT).show()
                        }
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        val newPos = (exoPlayerManager.currentPositionMs + 10_000L).coerceAtMost(exoPlayerManager.durationMs)
                        exoPlayerManager.seekTo(newPos)
                        Toast.makeText(context, "⏩ +10s", Toast.LENGTH_SHORT).show()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        val newPos = (exoPlayerManager.currentPositionMs - 10_000L).coerceAtLeast(0L)
                        exoPlayerManager.seekTo(newPos)
                        Toast.makeText(context, "⏪ -10s", Toast.LENGTH_SHORT).show()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_MENU -> {
                        exoPlayerManager.toggleResizeMode()
                        val mode = exoPlayerManager.aspectRatioHelper.currentMode.value
                        Toast.makeText(context, "📺 Escala: ${mode.label}", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
            }
            is ReceiverUiState.YouTubePlaying -> {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        youTubeControlFlow.tryEmit(SignalingMessage.PlayControl("TOGGLE_PLAY"))
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        youTubeControlFlow.tryEmit(SignalingMessage.PlayControl("SEEK_REL", 10_000L))
                        Toast.makeText(context, "⏩ +10s", Toast.LENGTH_SHORT).show()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        youTubeControlFlow.tryEmit(SignalingMessage.PlayControl("SEEK_REL", -10_000L))
                        Toast.makeText(context, "⏪ -10s", Toast.LENGTH_SHORT).show()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_MENU -> {
                        youTubeControlFlow.tryEmit(SignalingMessage.PlayControl("TOGGLE_SUBTITLES"))
                        Toast.makeText(context, "💬 Alternando Subtítulos", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
            }
            is ReceiverUiState.ScreenMirroring -> {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MENU) {
                    val mode = webRtcReceiver.toggleScalingType()
                    val txt = if (mode == org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL) "Modo: Llenar Pantalla (16:9)" else "Modo: Ajustar Proporción (Fit)"
                    Toast.makeText(context, txt, Toast.LENGTH_SHORT).show()
                    return true
                }
            }
            is ReceiverUiState.ImageDisplaying -> {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_DPAD_UP -> {
                        scope.launch { sendSignaling(SignalingMessage.PlayControl("PREV")) }
                        Toast.makeText(context, "◀ Foto Anterior", Toast.LENGTH_SHORT).show()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        scope.launch { sendSignaling(SignalingMessage.PlayControl("NEXT")) }
                        Toast.makeText(context, "Foto Siguiente ▶", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
            }
            else -> Unit
        }
        return false
    }
}
