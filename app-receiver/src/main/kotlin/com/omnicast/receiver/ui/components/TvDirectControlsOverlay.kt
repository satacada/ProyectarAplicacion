package com.omnicast.receiver.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.receiver.directstream.PvrRecordProgress

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
                TvCircleButton(
                    text = "Retroceder 10s",
                    icon = Icons.Rounded.Replay10,
                    size = 56.dp,
                    onClick = {
                        onSeekRelative(-10_000L)
                        onUserInteraction()
                    }
                )
                TvCircleButton(
                    text = if (isPlaying) "Pausar" else "Reproducir",
                    icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    size = 76.dp,
                    isPrimary = true,
                    onClick = {
                        onTogglePlay()
                        onUserInteraction()
                    }
                )
                TvCircleButton(
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

                    val recBtnLabel = when {
                        pvrProgress.isRecording -> "🔴 REC ${formatTvTime(pvrProgress.durationSeconds * 1000L)} ⏹"
                        isLive -> "🔴 REC"
                        else -> "📥 Guardar"
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
