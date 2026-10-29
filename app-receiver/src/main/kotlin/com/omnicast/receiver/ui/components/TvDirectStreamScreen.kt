package com.omnicast.receiver.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.omnicast.receiver.directstream.ExoPlayerTvManager
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
    var showSettingsDialog by remember { mutableStateOf(false) }
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
        if (isControlsVisible && isPlaying && !showSettingsDialog) {
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

        DisposableEffect(exoPlayerManager) {
            exoPlayerManager.onResizeModeChanged = { mode -> currentResizeMode.intValue = mode }
            exoPlayerManager.onZoomChanged = { z -> currentZoom.floatValue = z }
            onDispose {
                exoPlayerManager.onResizeModeChanged = null
                exoPlayerManager.onZoomChanged = null
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
                    val z = currentZoom.floatValue
                    pv.scaleX = if (z > 0f) z else 1.0f
                    pv.scaleY = if (z > 0f) z else 1.0f
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 1. Controles Centrales Clásicos de ExoPlayer (-10s flecha circular, Play/Pausa, +10s flecha circular)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvCircleButton(
                    text = "Retroceder 10s",
                    icon = Icons.Rounded.Replay10,
                    size = 56.dp,
                    onClick = {
                        val next = (currentPosMs - 10_000L).coerceAtLeast(0L)
                        exoPlayerManager.seekTo(next)
                        currentPosMs = next
                        userInteractionCount++
                    }
                )
                TvCircleButton(
                    text = if (isPlaying) "Pausar" else "Reproducir",
                    icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    size = 76.dp,
                    isPrimary = true,
                    onClick = {
                        if (isPlaying) exoPlayerManager.pause() else exoPlayerManager.resume()
                        isPlaying = !isPlaying
                        userInteractionCount++
                    }
                )
                TvCircleButton(
                    text = "Adelantar 10s",
                    icon = Icons.Rounded.Forward10,
                    size = 56.dp,
                    onClick = {
                        val maxDur = if (durationMs > 0L) durationMs else Long.MAX_VALUE
                        val next = (currentPosMs + 10_000L).coerceAtMost(maxDur)
                        exoPlayerManager.seekTo(next)
                        currentPosMs = next
                        userInteractionCount++
                    }
                )
            }
        }

        // 2. Barra Inferior Interactiva para TV (Línea de tiempo seleccionable con D-Pad, Opciones y Tuerca)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF2070D18))))
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Título
                Text(
                    text = title.ifBlank { "Transmisión en TV Box" },
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )

                // Línea de tiempo interactiva (seleccionable con el control remoto de la TV)
                TvInteractiveTimeline(
                    currentPosMs = currentPosMs,
                    durationMs = durationMs,
                    onSeekTo = { targetMs ->
                        exoPlayerManager.seekTo(targetMs)
                        currentPosMs = targetMs
                    },
                    onUserInteraction = { userInteractionCount++ }
                )

                // Fila de Opciones: Subtítulos, Calidad, Escala, Salir y Tuerca Ajustes a la derecha
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TvPlayerButton(label = "📺 $currentQuality", onClick = {
                            currentQuality = exoPlayerManager.cycleVideoQuality()
                            Toast.makeText(context, "📺 Calidad: $currentQuality", Toast.LENGTH_SHORT).show()
                            userInteractionCount++
                        })
                        TvPlayerButton(label = if (subsEnabled) "💬 Sub: Sí" else "💬 Sub: No", onClick = {
                            subsEnabled = exoPlayerManager.toggleSubtitles()
                            Toast.makeText(context, if (subsEnabled) "💬 Subtítulos Activados" else "💬 Subtítulos Desactivados", Toast.LENGTH_SHORT).show()
                            userInteractionCount++
                        })
                        TvPlayerButton(label = "🔲 $currentResizeLabel", onClick = {
                            exoPlayerManager.toggleResizeMode()
                            currentResizeLabel = exoPlayerManager.aspectRatioHelper.currentMode.value.label
                            userInteractionCount++
                        })
                        TvPlayerButton(label = "🚪 Salir", isDanger = true, onClick = {
                            exoPlayerManager.stop("Botón Salir TV")
                            onCloseClick()
                        })
                    }

                    // Tuerca de Ajustes en la parte inferior derecha
                    TvPlayerButton(
                        label = "⚙ Ajustes",
                        isSettings = true,
                        onClick = {
                            showSettingsDialog = true
                            userInteractionCount++
                        }
                    )
                }
            }
        }

        if (showSettingsDialog) {
            TvPlayerSettingsDialog(
                title = title,
                isSaved = isSaved,
                currentQuality = currentQuality,
                onCycleQuality = {
                    val q = exoPlayerManager.cycleVideoQuality()
                    currentQuality = q
                    q
                },
                currentAudioTrack = exoPlayerManager.getCurrentAudioTrackLabel(),
                onCycleAudio = {
                    exoPlayerManager.cycleAudioTrack()
                },
                subtitlesEnabled = subsEnabled,
                onToggleSubtitles = {
                    val st = exoPlayerManager.toggleSubtitles()
                    subsEnabled = st
                    st
                },
                onSaveToWatchLater = onSaveToWatchLater,
                onExitPlayback = onCloseClick,
                onDismiss = { showSettingsDialog = false; userInteractionCount++ }
            )
        }
    }
}


