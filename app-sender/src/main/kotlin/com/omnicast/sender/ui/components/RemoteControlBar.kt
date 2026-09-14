package com.omnicast.sender.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RemoteControlBar(
    title: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    volume: Float,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onStop: () -> Unit,
    onSelectQuality: (() -> Unit)? = null,
    onSelectTracks: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xF00F172A)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.ifBlank { "Transmitiendo a TV Box" },
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(text = "${formatTime(positionMs)} / ${formatTime(durationMs)}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                IconButton(onClick = onStop) { Text("🛑", fontSize = 18.sp) }
            }

            if (durationMs > 0) {
                var sliderPos by remember(positionMs) { mutableStateOf(positionMs.toFloat()) }
                Slider(
                    value = sliderPos.coerceIn(0f, durationMs.toFloat()),
                    onValueChange = { sliderPos = it },
                    onValueChangeFinished = { onSeekTo(sliderPos.toLong()) },
                    valueRange = 0f..durationMs.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth().height(22.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilledTonalButton(onClick = { onSkip(-10000L) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("⏪ -10s", fontSize = 11.sp)
                }
                Button(
                    onClick = onPlayPause,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text(if (isPlaying) "⏸ Pausa" else "▶ Reanudar", fontSize = 12.sp, color = Color.White)
                }
                FilledTonalButton(onClick = { onSkip(30000L) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("⏩ +30s", fontSize = 11.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var curVol by remember(volume) { mutableStateOf(volume) }
                Text(if (curVol <= 0.01f) "🔇" else if (curVol < 0.5f) "🔉" else "🔊", fontSize = 14.sp)
                Slider(
                    value = curVol,
                    onValueChange = { curVol = it },
                    onValueChangeFinished = { onVolumeChange(curVol) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF10B981), activeTrackColor = Color(0xFF059669)),
                    modifier = Modifier.weight(1f).height(20.dp).padding(horizontal = 6.dp)
                )
                Text("${(curVol * 100).toInt()}%", color = Color(0xFF94A3B8), fontSize = 11.sp)

                if (onSelectQuality != null) {
                    FilledTonalButton(onClick = onSelectQuality, contentPadding = PaddingValues(horizontal = 6.dp), modifier = Modifier.padding(start = 6.dp)) {
                        Text("🎬 Calidad", fontSize = 10.sp)
                    }
                }
                if (onSelectTracks != null) {
                    FilledTonalButton(onClick = onSelectTracks, contentPadding = PaddingValues(horizontal = 6.dp), modifier = Modifier.padding(start = 4.dp)) {
                        Text("💬 Audio/Sub", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000
    val sec = totalSec % 60
    val min = (totalSec / 60) % 60
    val hrs = totalSec / 3600
    return if (hrs > 0) String.format("%02d:%02d:%02d", hrs, min, sec) else String.format("%02d:%02d", min, sec)
}
