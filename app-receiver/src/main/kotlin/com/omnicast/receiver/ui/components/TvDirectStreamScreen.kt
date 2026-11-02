package com.omnicast.receiver.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.ui.PlayerView
import com.omnicast.receiver.directstream.ExoPlayerTvManager
import com.omnicast.receiver.directstream.PvrStreamRecorder
import com.omnicast.receiver.directstream.TvRecordTimerManager
import kotlinx.coroutines.delay

@Composable
fun TvDirectStreamScreen(
    exoPlayerManager: ExoPlayerTvManager,
    title: String,
    streamUrl: String = "",
    isSaved: Boolean = false,
    onSaveToWatchLater: () -> Unit = {},
    onCloseClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val pvrProgress by PvrStreamRecorder.progress.collectAsState()
    val timerState by TvRecordTimerManager.timerState.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showRecordDialog by remember { mutableStateOf(false) }
    var showStopConfirm by remember { mutableStateOf(false) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var userInteractionCount by remember { mutableIntStateOf(0) }
    var currentQuality by remember { mutableStateOf(exoPlayerManager.getCurrentVideoQuality()) }
    var subsEnabled by remember { mutableStateOf(exoPlayerManager.areSubtitlesEnabled) }
    var currentResizeLabel by remember { mutableStateOf(exoPlayerManager.aspectRatioHelper.currentMode.value.label) }

    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(exoPlayerManager.isPlaying) }

    LaunchedEffect(Unit) {
        exoPlayerManager.isControlsShowing = true
        while (true) {
            currentPosMs = exoPlayerManager.currentPositionMs
            durationMs = exoPlayerManager.durationMs
            isPlaying = exoPlayerManager.isPlaying
            delay(500)
        }
    }

    LaunchedEffect(userInteractionCount, isPlaying) {
        if (isControlsVisible && isPlaying && !showSettingsDialog && !showRecordDialog && !showStopConfirm) {
            delay(5000)
            isControlsVisible = false
            exoPlayerManager.isControlsShowing = false
        }
    }

    DisposableEffect(exoPlayerManager) {
        exoPlayerManager.onWakeControls = {
            isControlsVisible = exoPlayerManager.isControlsShowing
            userInteractionCount++
        }
        onDispose {
            exoPlayerManager.onWakeControls = null
            exoPlayerManager.playerView = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        val player = exoPlayerManager.player
        val currentResizeMode = remember { mutableIntStateOf(exoPlayerManager.resizeMode) }
        val currentZoom = remember { mutableFloatStateOf(exoPlayerManager.currentZoom) }
        val currentPanX = remember { mutableFloatStateOf(exoPlayerManager.currentPanX) }
        val currentPanY = remember { mutableFloatStateOf(exoPlayerManager.currentPanY) }

        DisposableEffect(exoPlayerManager) {
            exoPlayerManager.onResizeModeChanged = { mode -> currentResizeMode.intValue = mode }
            exoPlayerManager.onZoomChanged = { z -> currentZoom.floatValue = z }
            exoPlayerManager.onTransformChanged = { z, px, py ->
                currentZoom.floatValue = z; currentPanX.floatValue = px; currentPanY.floatValue = py
            }
            onDispose {
                exoPlayerManager.onResizeModeChanged = null
                exoPlayerManager.onZoomChanged = null
                exoPlayerManager.onTransformChanged = null
            }
        }

        if (player != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        resizeMode = currentResizeMode.intValue
                        subtitleView?.visibility = android.view.View.VISIBLE
                        setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        exoPlayerManager.playerView = this
                    }
                },
                update = { pv ->
                    if (pv.player != player) pv.player = player
                    pv.resizeMode = currentResizeMode.intValue
                    val z = if (currentZoom.floatValue > 0f) currentZoom.floatValue else 1.0f
                    pv.scaleX = z
                    pv.scaleY = z
                    val w = pv.width.toFloat()
                    val h = pv.height.toFloat()
                    val px = currentPanX.floatValue
                    val py = currentPanY.floatValue
                    pv.translationX = if (kotlin.math.abs(px) <= 2.5f) px * w else px * (w / 1080f)
                    pv.translationY = if (kotlin.math.abs(py) <= 2.5f) py * h else py * (h / 1920f)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Superposición completa de controles D-Pad
        TvDirectControlsOverlay(
            title = title,
            streamUrl = streamUrl,
            isControlsVisible = isControlsVisible,
            isPlaying = isPlaying,
            currentPosMs = currentPosMs,
            durationMs = durationMs,
            currentQuality = currentQuality,
            subsEnabled = subsEnabled,
            currentResizeLabel = currentResizeLabel,
            pvrProgress = pvrProgress,
            onSeekRelative = { delta ->
                val maxDur = if (durationMs > 0L) durationMs else Long.MAX_VALUE
                val next = (currentPosMs + delta).coerceIn(0L, maxDur)
                exoPlayerManager.seekTo(next)
                currentPosMs = next
            },
            onTogglePlay = {
                if (isPlaying) exoPlayerManager.pause() else exoPlayerManager.resume()
                isPlaying = !isPlaying
            },
            onCycleQuality = {
                currentQuality = exoPlayerManager.cycleVideoQuality()
                Toast.makeText(context, "📺 Calidad: $currentQuality", Toast.LENGTH_SHORT).show()
            },
            onToggleSubtitles = {
                subsEnabled = exoPlayerManager.toggleSubtitles()
                Toast.makeText(context, if (subsEnabled) "💬 Subtítulos Activados" else "💬 Subtítulos Desactivados", Toast.LENGTH_SHORT).show()
            },
            onToggleResize = {
                exoPlayerManager.toggleResizeMode()
                currentResizeLabel = exoPlayerManager.aspectRatioHelper.currentMode.value.label
            },
            onExit = {
                exoPlayerManager.stop("Botón Salir TV")
                onCloseClick()
            },
            onRecordClick = {
                if (pvrProgress.isRecording) {
                    showStopConfirm = true
                } else {
                    showRecordDialog = true
                }
            },
            onOpenSettings = { showSettingsDialog = true },
            onSeekTo = { targetMs ->
                exoPlayerManager.seekTo(targetMs)
                currentPosMs = targetMs
            },
            onUserInteraction = { userInteractionCount++ }
        )

        if (showSettingsDialog) {
            TvPlayerSettingsDialog(
                title = title, isSaved = isSaved, currentQuality = currentQuality,
                onCycleQuality = { val q = exoPlayerManager.cycleVideoQuality(); currentQuality = q; q },
                currentAudioTrack = exoPlayerManager.getCurrentAudioTrackLabel(),
                onCycleAudio = { exoPlayerManager.cycleAudioTrack() },
                subtitlesEnabled = subsEnabled,
                onToggleSubtitles = { val st = exoPlayerManager.toggleSubtitles(); subsEnabled = st; st },
                onSaveToWatchLater = onSaveToWatchLater, onExitPlayback = onCloseClick,
                onDismiss = { showSettingsDialog = false; userInteractionCount++ }
            )
        }

        if (showRecordDialog && streamUrl.isNotBlank()) {
            val isLive = durationMs <= 0L ||
                title.contains("news", ignoreCase = true) ||
                title.contains("sky", ignoreCase = true) ||
                title.contains("directo", ignoreCase = true) ||
                title.contains("envivo", ignoreCase = true) ||
                title.contains("en vivo", ignoreCase = true) ||
                title.contains("live", ignoreCase = true) ||
                streamUrl.contains("live", ignoreCase = true) ||
                streamUrl.contains("linear", ignoreCase = true) ||
                streamUrl.contains("chunklist", ignoreCase = true)

            TvRecordDialog(
                title = title.ifBlank { if (isLive) "Canal en Vivo" else "Película en reproducción" },
                streamUrl = streamUrl, headers = exoPlayerManager.currentHeaders,
                durationMs = durationMs, isLive = isLive,
                onDismiss = { showRecordDialog = false; userInteractionCount++ },
                onRecordingStarted = { userInteractionCount++ }
            )
        }

        TvConfirmStopRecordDialog(
            isOpen = showStopConfirm,
            recordedDurationSec = pvrProgress.durationSeconds,
            onKeep = {
                showStopConfirm = false
                PvrStreamRecorder.stopRecording("⏹ Grabación guardada")
                Toast.makeText(context, "💾 Grabación conservada exitosamente", Toast.LENGTH_SHORT).show()
                userInteractionCount++
            },
            onDiscard = {
                showStopConfirm = false
                PvrStreamRecorder.stopAndDiscardRecording("🗑️ Grabación descartada y eliminada")
                Toast.makeText(context, "🗑️ Grabación descartada y eliminada", Toast.LENGTH_SHORT).show()
                userInteractionCount++
            },
            onContinue = {
                showStopConfirm = false
                userInteractionCount++
            }
        )

        // Insignia flotante de grabación OSD (siempre visible arriba a la derecha sobre cualquier control)
        TvRecFloatingBadge(
            isRecording = pvrProgress.isRecording,
            durationSeconds = pvrProgress.durationSeconds,
            remainingSeconds = if (timerState.isRunning && timerState.remainingSeconds > 0) timerState.remainingSeconds else -1L,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 28.dp, top = 28.dp).zIndex(99f)
        )
    }
}
