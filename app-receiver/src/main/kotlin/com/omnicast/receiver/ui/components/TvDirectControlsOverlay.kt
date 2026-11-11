package com.omnicast.receiver.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.receiver.directstream.PvrRecordProgress
import com.omnicast.receiver.ui.TvRemoteKeyHandler
import kotlinx.coroutines.delay

/**
 * Superposición desacoplada de controles D-Pad para ExoPlayer en TV Box.
 * Mantiene TvDirectStreamScreen.kt estrictamente por debajo del límite de 300 líneas.
 */
@Composable
fun TvDirectControlsOverlay(
    title: String,
    streamUrl: String,
    isControlsVisible: Boolean,
    isPlaying: Boolean,
    currentPosMs: Long,
    durationMs: Long,
    currentQuality: String,
    subsEnabled: Boolean,
    currentResizeLabel: String,
    pvrProgress: PvrRecordProgress,
    onSeekRelative: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onCycleQuality: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onToggleResize: () -> Unit,
    onExit: () -> Unit,
    onRecordClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isControlsVisible) {
        TvRemoteKeyHandler.notifyControlsVisibilityChanged(isControlsVisible)
        if (isControlsVisible) {
            runCatching { playFocusRequester.requestFocus() }
            delay(100L)
            runCatching { playFocusRequester.requestFocus() }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            TvRemoteKeyHandler.notifyControlsVisibilityChanged(false)
        }
    }

    // 1. Controles Centrales Clásicos de ExoPlayer (-10s, Play/Pausa, +10s)
    AnimatedVisibility(
        visible = isControlsVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvFocusCircleButton(
                    text = "Retroceder 10s",
                    icon = Icons.Rounded.Replay10,
                    size = 56.dp,
                    onClick = {
                        onSeekRelative(-10_000L)
                        onUserInteraction()
                    }
                )
                TvFocusCircleButton(
                    text = if (isPlaying) "Pausar" else "Reproducir",
                    icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    size = 76.dp,
                    isPrimary = true,
                    focusRequester = playFocusRequester,
                    onClick = {
                        onTogglePlay()
                        onUserInteraction()
                    }
                )
                TvFocusCircleButton(
                    text = "Adelantar 10s",
                    icon = Icons.Rounded.Forward10,
                    size = 56.dp,
                    onClick = {
                        onSeekRelative(10_000L)
                        onUserInteraction()
                    }
                )
            }
        }
    }

    // 2. Barra Inferior Interactiva para TV (Línea de tiempo seleccionable con D-Pad, Opciones y Tuerca)
    AnimatedVisibility(
        visible = isControlsVisible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF2070D18))))
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title.ifBlank { "Transmisión en TV Box" },
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )

                TvInteractiveTimeline(
                    currentPosMs = currentPosMs,
                    durationMs = durationMs,
                    onSeekTo = { targetMs ->
                        onSeekTo(targetMs)
                        onUserInteraction()
                    },
                    onUserInteraction = onUserInteraction
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TvPlayerButton(label = "📺 $currentQuality", onClick = {
                            onCycleQuality()
                            onUserInteraction()
                        })
                        TvPlayerButton(label = if (subsEnabled) "💬 Sub: Sí" else "💬 Sub: No", onClick = {
                            onToggleSubtitles()
                            onUserInteraction()
                        })
                        TvPlayerButton(label = "🔲 $currentResizeLabel", onClick = {
                            onToggleResize()
                            onUserInteraction()
                        })
                        TvPlayerButton(label = "🚪 Salir", isDanger = true, onClick = onExit)
                    }

                    val recBtnLabel = when {
                        pvrProgress.isRecording -> "🔴 REC ${formatTvTime(pvrProgress.durationSeconds * 1000L)} ⏹"
                        else -> "🔴 REC"
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TvPlayerButton(
                            label = recBtnLabel,
                            isDanger = pvrProgress.isRecording,
                            onClick = {
                                onRecordClick()
                                onUserInteraction()
                            }
                        )
                        TvPlayerButton(label = "⚙ Ajustes", isSettings = true, onClick = {
                            onOpenSettings()
                            onUserInteraction()
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun TvFocusCircleButton(
    text: String = "",
    icon: ImageVector? = null,
    size: Dp,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
    focusRequester: FocusRequester? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isFocused -> if (isPrimary) Color(0xFFFF7A00) else Color(0xFF38BDF8)
        isPrimary -> Color(0xD9E65100)
        else -> Color(0xCC0F172A)
    }
    val contentColor = if (isFocused) Color.Black else Color.White

    Box(
        modifier = Modifier
            .size(size)
            .scale(if (isFocused) 1.18f else 1.0f)
            .background(bgColor, CircleShape)
            .border(
                width = if (isFocused) 3.dp else 1.5.dp,
                color = if (isFocused) Color.White else Color(0xFF475569),
                shape = CircleShape
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text.ifBlank { null },
                tint = contentColor,
                modifier = Modifier.size(size * 0.54f)
            )
        } else {
            Text(
                text = text,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
