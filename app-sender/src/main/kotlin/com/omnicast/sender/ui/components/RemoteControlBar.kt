package com.omnicast.sender.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
    onSelectQuality: (() -> Unit)? = null
) {
    var isExpanded by rememberSaveable { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xF20F172A)),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                var dragAccumulator = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount
                    },
                    onDragEnd = {
                        if (dragAccumulator > 25f) isExpanded = false
                        else if (dragAccumulator < -25f) isExpanded = true
                        dragAccumulator = 0f
                    }
                )
            }
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            // Manija interactiva deslizable arriba/abajo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isExpanded) "▼ Desliza abajo para minimizar" else "▲ Desliza arriba para ver controles",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isExpanded) {
                // Modo Compacto / Minimizado: ocupa poco espacio y permite navegar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = title.ifBlank { "Transmitiendo a TV Box" },
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "▲ Desliza arriba o toca para ver controles",
                            color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = onPlayPause,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) { Text(if (isPlaying) "⏸" else "▶", fontSize = 12.sp, color = Color.White) }

                        IconButton(onClick = onStop, modifier = Modifier.size(32.dp)) {
                            Text("🛑", fontSize = 14.sp)
                        }
                    }
                }
            } else {
                // Modo Expandido: Todos los controles completos
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
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
                                Text(
                                    text = "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                                    color = Color(0xFF94A3B8), fontSize = 11.sp
                                )
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
                        }
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
