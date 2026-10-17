package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RemoteExpandedControls(
    title: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    volume: Float,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onStop: (() -> Unit)? = null,
    onSaveToPlaylist: (() -> Unit)? = null,
    onDeleteFromSaved: (() -> Unit)? = null,
    onSelectQuality: (() -> Unit)? = null,
    isSaved: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp)) {

        // ========================================================
        // SECCIÓN 2: PELÍCULA, LISTAS (Bookmark / Bookmark-X) Y CALIDAD
        // ========================================================
        Surface(
            color = Color(0xFF141D2B),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223147)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = title.ifBlank { "Transmisión Web" },
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                        Text("Transmisión Lista", color = Color(0xFF6EE7B7), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onSaveToPlaylist != null) {
                        IconButton(
                            onClick = onSaveToPlaylist,
                            modifier = Modifier
                                .size(34.dp)
                                .background(if (isSaved) Color(0xFF064E3B) else Color(0xFF1A2335), RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSaved) Color(0xFF10B981) else Color(0xFF2C3B59), RoundedCornerShape(8.dp))
                        ) {
                            BookmarkOutlineIcon(
                                modifier = Modifier.size(17.dp),
                                color = if (isSaved) Color(0xFF10B981) else Color(0xFF38BDF8),
                                isFilled = isSaved
                            )
                        }
                    }
                    if (onDeleteFromSaved != null) {
                        IconButton(
                            onClick = onDeleteFromSaved,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF261922), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF4C1D24), RoundedCornerShape(8.dp))
                        ) {
                            BookmarkRemoveIcon(modifier = Modifier.size(17.dp))
                        }
                    }
                    if (onStop != null) {
                        IconButton(
                            onClick = onStop,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF2D1515), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = "Detener", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // ========================================================
        // SECCIÓN 3: LÍNEA DE TIEMPO, CONTROLES DE REPRODUCCIÓN Y VOLUMEN
        // ========================================================
        Surface(
            color = Color(0xFF141D2B),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223147)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Barra de Tiempo Scrubbing
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    var sliderPos by remember(positionMs) { mutableFloatStateOf(positionMs.toFloat()) }
                    val effectiveDur = if (durationMs > 0L) durationMs.toFloat() else 1000f

                    Slider(
                        value = sliderPos.coerceIn(0f, effectiveDur),
                        onValueChange = { sliderPos = it },
                        onValueChangeFinished = { onSeekTo(sliderPos.toLong()) },
                        valueRange = 0f..effectiveDur,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF7A00),
                            activeTrackColor = Color(0xFFFF7A00),
                            inactiveTrackColor = Color(0xFF1E283D)
                        ),
                        modifier = Modifier.fillMaxWidth().height(18.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatTimeString(positionMs), color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(formatTimeString(durationMs), color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 2. Fila de Controles de Reproducción: -10s | Replay10 | Play/Pause | Forward10 | +10s
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "-10s", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 6.dp))
                    Box(
                        modifier = Modifier.size(38.dp).background(Color(0xFF161F2E), CircleShape).border(1.dp, Color(0xFF26354D), CircleShape).clickable { onSkip(-10000L) },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Replay10, contentDescription = "-10s", tint = Color(0xFFCBD5E1), modifier = Modifier.size(20.dp)) }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier.size(48.dp).background(Color(0xFFFF7A00), CircleShape).clickable { onPlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (isPlaying) "Pausa" else "Reproducir", tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier.size(38.dp).background(Color(0xFF161F2E), CircleShape).border(1.dp, Color(0xFF26354D), CircleShape).clickable { onSkip(10000L) },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Forward10, contentDescription = "+10s", tint = Color(0xFFCBD5E1), modifier = Modifier.size(20.dp)) }

                    Text(text = "+10s", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                }

                // 3. Fila de Volumen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var curVol by remember(volume) { mutableFloatStateOf(volume) }
                    Icon(if (curVol <= 0.01f) Icons.Rounded.VolumeMute else Icons.Rounded.VolumeUp, contentDescription = "Volumen", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    Slider(
                        value = curVol,
                        onValueChange = { curVol = it },
                        onValueChangeFinished = { onVolumeChange(curVol) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00B4D8),
                            activeTrackColor = Color(0xFF00B4D8),
                            inactiveTrackColor = Color(0xFF1E283D)
                        ),
                        modifier = Modifier.weight(1f).height(18.dp).padding(horizontal = 6.dp)
                    )
                    Text("${(curVol * 100).toInt()}%", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun formatTimeString(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000
    val sec = totalSec % 60
    val min = (totalSec / 60) % 60
    val hrs = totalSec / 3600
    return if (hrs > 0) String.format("%02d:%02d:%02d", hrs, min, sec) else String.format("%02d:%02d", min, sec)
}
