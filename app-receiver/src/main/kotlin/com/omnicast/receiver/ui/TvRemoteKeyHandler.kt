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
        val isExitKey = keyCode == KeyEvent.KEYCODE_BACK ||
                        keyCode == KeyEvent.KEYCODE_ESCAPE ||
                        keyCode == KeyEvent.KEYCODE_BUTTON_B ||
                        keyCode == KeyEvent.KEYCODE_MEDIA_STOP ||
                        keyCode == KeyEvent.KEYCODE_DEL
        if (isExitKey && currentState !is ReceiverUiState.Standby) {
            val pv = exoPlayerManager.playerView
            if (currentState is ReceiverUiState.DirectStreamPlaying && pv != null && pv.isControllerFullyVisible) {
                pv.hideController()
                return true
            }
            digitalAudioReceiver?.stop(); exoPlayerManager.stop("Mando TV (Atrás/Stop)"); webRtcReceiver.resetConnection()
            onResetToStandby()
            return true
        }

        if (currentState is ReceiverUiState.ClientConnectedStandby) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE -> {
                    onResetToStandby()
                    return true
                }
            }
        }

        when (currentState) {
            is ReceiverUiState.DirectStreamPlaying -> {
                val pv = exoPlayerManager.playerView
                if (pv != null && pv.isControllerFullyVisible && event != null) {
                    if (pv.dispatchKeyEvent(event)) return true
                }

                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (pv != null && !pv.isControllerFullyVisible) {
                            pv.showController()
                            return true
                        }
                        if (exoPlayerManager.isPlaying) exoPlayerManager.pause() else exoPlayerManager.resume()
                        pv?.showController()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
                    KeyEvent.KEYCODE_SPACE -> {
                        if (exoPlayerManager.isPlaying) exoPlayerManager.pause() else exoPlayerManager.resume()
                        pv?.showController()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        val newPos = (exoPlayerManager.currentPositionMs + 10_000L).coerceAtMost(exoPlayerManager.durationMs)
                        exoPlayerManager.seekTo(newPos)
                        pv?.showController()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        val newPos = (exoPlayerManager.currentPositionMs - 10_000L).coerceAtLeast(0L)
                        exoPlayerManager.seekTo(newPos)
                        pv?.showController()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (pv != null && !pv.isControllerFullyVisible) {
                            pv.showController()
                            return true
                        }
                    }
                    KeyEvent.KEYCODE_MENU -> {
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
            is ReceiverUiState.WebStreamPlaying -> {
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
                    KeyEvent.KEYCODE_MENU -> {
                        youTubeControlFlow.tryEmit(SignalingMessage.PlayControl("CYCLE_ZOOM"))
                        return true
                    }
                }
            }
            is ReceiverUiState.ScreenMirroring -> {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MENU,
                    KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_SPACE -> {
                        webRtcReceiver.toggleScalingType()
                        val isLand = webRtcReceiver.isLandscape
                        val isFill = (webRtcReceiver.currentMode == "FILL")
                        val txt = if (isLand) {
                            if (isFill) "📺 Modo: Llenar Pantalla (16:9)" else "📺 Modo: Ajustar Proporción (FIT)"
                        } else {
                            if (isFill) "📱 Modo: Rellenar Pantalla (FILL Ampliado)" else "📱 Modo: Ajustar Pantalla (FIT 100%)"
                        }
                        Toast.makeText(context, txt, Toast.LENGTH_SHORT).show()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        val curZoom = webRtcReceiver.currentZoom
                        val newZoom = (curZoom + 0.15f).coerceAtMost(2.0f)
                        webRtcReceiver.setDisplayMode("ZOOM", zoom = newZoom, isLandscapeParam = webRtcReceiver.isLandscape)
                        Toast.makeText(context, "🔍 Zoom TV: ${(newZoom * 100).toInt()}%", Toast.LENGTH_SHORT).show()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        val curZoom = webRtcReceiver.currentZoom
                        val newZoom = (curZoom - 0.15f).coerceAtLeast(0.75f)
                        webRtcReceiver.setDisplayMode("ZOOM", zoom = newZoom, isLandscapeParam = webRtcReceiver.isLandscape)
                        Toast.makeText(context, "🔍 Zoom TV: ${(newZoom * 100).toInt()}%", Toast.LENGTH_SHORT).show()
                        return true
                    }
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
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_MENU -> {
                        scope.launch { sendSignaling(SignalingMessage.PlayControl("ROTATE")) }
                        Toast.makeText(context, "⟳ Rotando 90°", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
            }
            else -> Unit
        }
        return false
    }
}
