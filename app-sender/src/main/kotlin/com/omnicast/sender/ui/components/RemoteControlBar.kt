package com.omnicast.sender.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
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
    onSelectQuality: (() -> Unit)? = null,
    onSaveToPlaylist: (() -> Unit)? = null,
    onDeleteFromSaved: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    isSaved: Boolean = false
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        color = Color(0xF8111624),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF26334D)),
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
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            // Tirador superior deslizable
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color(0xFF1B2233), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isExpanded) "Desliza abajo para minimizar" else "Desliza arriba para ver controles",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!isExpanded) {
                // Modo Compacto / Minimizado (estilo WVC 44dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { isExpanded = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Expandir", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isExpanded = true }
                    ) {
                        Text(
                            text = title.ifBlank { "Transmitiendo a TV Box" },
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(5.dp).background(Color(0xFF10B981), CircleShape))
                            Text("En TV", color = Color(0xFF6EE7B7), fontSize = 10.sp)
                        }
                    }

                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color(0xFFFF7A00), CircleShape)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color(0xFF2D1515), CircleShape)
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = "Detener", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                    }

                    if (onDismiss != null) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Ocultar", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
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
                    RemoteExpandedControls(
                        title = title,
                        isPlaying = isPlaying,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        volume = volume,
                        onPlayPause = onPlayPause,
                        onSeekTo = onSeekTo,
                        onSkip = onSkip,
                        onVolumeChange = onVolumeChange,
                        onStop = onStop,
                        onSaveToPlaylist = onSaveToPlaylist,
                        onDeleteFromSaved = onDeleteFromSaved,
                        onSelectQuality = onSelectQuality,
                        isSaved = isSaved
                    )
                }
            }
        }
    }
}
