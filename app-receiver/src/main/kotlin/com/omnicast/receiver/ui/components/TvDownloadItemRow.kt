package com.omnicast.receiver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.receiver.directstream.TvDownloadedVideo

@Composable
fun TvDownloadItemRow(
    video: TvDownloadedVideo,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRowFocused by remember { mutableStateOf(false) }
    var isPlayFocused by remember { mutableStateOf(false) }
    var isDeleteFocused by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isRowFocused || isPlayFocused) Color(0xFF1E293B) else Color(0xFF131B2E)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isRowFocused || isPlayFocused) 2.dp else 1.dp,
                color = if (isRowFocused || isPlayFocused) Color(0xFF38BDF8) else Color(0xFF25334D),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icono moderno de película
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0369A1)),
                contentAlignment = Alignment.Center
            ) {
                Text("🎬", fontSize = 20.sp)
            }

            // Título y metadatos
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "📦 ${video.formattedSize} • 📅 ${video.formattedDate} • ${if (video.isUsb) "🔌 USB" else "💾 Interno"}",
                    color = Color(0xFF34D399),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Botón Reproducir (Moderno D-Pad)
            Box(
                modifier = Modifier
                    .onFocusChanged { isPlayFocused = it.isFocused }
                    .focusable()
                    .clickable { onPlay() }
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isPlayFocused) Color(0xFF38BDF8) else Color(0xFF0284C7))
                    .border(1.dp, if (isPlayFocused) Color.White else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶ Reproducir",
                    color = if (isPlayFocused) Color.Black else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Botón Borrar (Moderno D-Pad)
            Box(
                modifier = Modifier
                    .onFocusChanged { isDeleteFocused = it.isFocused }
                    .focusable()
                    .clickable { onDelete() }
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDeleteFocused) Color(0xFFEF4444) else Color(0xFF3B1219))
                    .border(1.dp, if (isDeleteFocused) Color.White else Color(0xFF991B1B), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🗑️",
                    fontSize = 13.sp
                )
            }
        }
    }
}
