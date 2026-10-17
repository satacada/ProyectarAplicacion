package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.sender.stream.StreamResumeManager
import com.omnicast.sender.ui.theme.OmniThemeColors

/**
 * Tarjeta de control de reproducción remota visible en la pantalla principal
 * cuando la TV Box está reproduciendo una película o video capturado.
 */
@Composable
fun MoviePlaybackControlCard(
    title: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onStop: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Movie, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                    Column {
                        Text(
                            text = title.ifBlank { "Película en Transmisión" },
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                            Text(
                                text = "Reproduciendo en TV Box",
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onOpenBrowser,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Rounded.Language, contentDescription = "Navegador", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                }
            }

            // Barra de progreso y tiempos
            if (durationMs > 0L) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = positionMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                        onValueChange = { onSeek(it.toLong()) },
                        valueRange = 0f..durationMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8),
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth().height(24.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = StreamResumeManager.formatTime(positionMs),
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        Text(
                            text = StreamResumeManager.formatTime(durationMs),
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Botones de acción rápida: -10s, Play/Pause, +10s, Detener
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { onSeek((positionMs - 10_000L).coerceAtLeast(0L)) },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF334155)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("10s", color = Color.White, fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onPlayPause,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                        Text(
                            text = if (isPlaying) "Pausa" else "Reanudar",
                            color = Color(0xFF0F172A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { onSeek(if (durationMs > 0L) (positionMs + 10_000L).coerceAtMost(durationMs) else positionMs + 10_000L) },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF334155)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("10s", color = Color.White, fontSize = 12.sp)
                        Icon(Icons.Rounded.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = "Detener", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
