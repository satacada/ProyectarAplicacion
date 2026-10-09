package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    streamCount: Int = 1,
    streamSubtitle: String = "HLS • m3u8 stream directo",
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    volume: Float,
    onShowDetectedStreams: () -> Unit = {},
    onRescanStreams: () -> Unit = {},
    onClearStreams: () -> Unit = {},
    onTransmitLatest: () -> Unit = {},
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSaveToPlaylist: (() -> Unit)? = null,
    onDeleteFromSaved: (() -> Unit)? = null,
    onSelectQuality: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
        // Cabecera: Rayo + Flujos (N) [Click para ver descendente] + Reset/Recapturar + Transmitir (Icono Casteo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onShowDetectedStreams() }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFF132F26), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚡", fontSize = 16.sp)
                }
                Text(
                    text = "Flujos ($streamCount)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onClearStreams,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF2A1B24), CircleShape)
                        .border(1.dp, Color(0xFF4C1D24), CircleShape)
                ) {
                    ModernTrashIcon(modifier = Modifier.size(18.dp), color = Color(0xFFF87171))
                }
                IconButton(
                    onClick = onTransmitLatest,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF10B981), CircleShape)
                ) {
                    CastTvIcon(modifier = Modifier.size(20.dp), color = Color.White)
                }
            }
        }

        // Título de la película + Botones intuitivos modernos (Bookmark y Borrar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = title.ifBlank { "Transmisión Web" },
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                    Text("Transmisión Lista", color = Color(0xFF6EE7B7), fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                            .size(36.dp)
                            .background(Color(0xFF1A2335), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF2C3B59), RoundedCornerShape(10.dp))
                    ) {
                        BookmarkOutlineIcon(modifier = Modifier.size(19.dp), color = Color(0xFF38BDF8))
                    }
                }
                if (onDeleteFromSaved != null) {
                    IconButton(
                        onClick = onDeleteFromSaved,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF251A22), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF3F212E), RoundedCornerShape(10.dp))
                    ) {
                        ModernTrashIcon(modifier = Modifier.size(18.dp), color = Color(0xFFF87171))
                    }
                }
            }
        }

        // Barra de Tiempo Scrubbing
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
                modifier = Modifier.fillMaxWidth().height(20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatTimeString(positionMs), color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(formatTimeString(durationMs), color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Fila de Controles de Reproducción: -10s afuera | ↺ | ❚❚ / ▶ Naranja | ↻ | +10s afuera
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "-10s", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 8.dp))
            Box(
                modifier = Modifier.size(42.dp).background(Color(0xFF161F2E), CircleShape).border(1.dp, Color(0xFF26354D), CircleShape).clickable { onSkip(-10000L) },
                contentAlignment = Alignment.Center
            ) { Text("↺", fontSize = 20.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold) }

            Spacer(modifier = Modifier.width(18.dp))

            Box(
                modifier = Modifier.size(52.dp).background(Color(0xFFFF7A00), CircleShape).clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (isPlaying) "❚❚" else "▶", fontSize = if (isPlaying) 18.sp else 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(18.dp))

            Box(
                modifier = Modifier.size(42.dp).background(Color(0xFF161F2E), CircleShape).border(1.dp, Color(0xFF26354D), CircleShape).clickable { onSkip(10000L) },
                contentAlignment = Alignment.Center
            ) { Text("↻", fontSize = 20.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold) }

            Text(text = "+10s", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
        }

        // Fila Inferior: Control de Volumen y Calidad
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var curVol by remember(volume) { mutableFloatStateOf(volume) }
                Text(if (curVol <= 0.01f) "🔇" else "🔊", fontSize = 13.sp)
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
                    modifier = Modifier.weight(1f).height(20.dp).padding(horizontal = 6.dp)
                )
                Text("${(curVol * 100).toInt()}%", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            if (onSelectQuality != null) {
                Surface(
                    color = Color(0xFF162030),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF28354E)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable { onSelectQuality() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🎛️", fontSize = 11.sp)
                        Text("Calidad", color = Color(0xFFCBD5E1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
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
