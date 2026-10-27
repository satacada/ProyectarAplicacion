package com.omnicast.sender.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tarjeta de video unificada con selector interactivo de calidades (Adaptive por defecto).
 * Agrupa todas las variantes (1080p, 720p, 480p) bajo un único título y miniatura.
 */
@Composable
fun WvcVideoListItem(
    group: List<StreamQualityOption>,
    pageTitle: String,
    onTransmit: (StreamQualityOption) -> Unit
) {
    if (group.isEmpty()) return

    val defaultOption = remember(group) {
        group.firstOrNull { it.resolution.equals("Adaptive", ignoreCase = true) }
            ?: group.firstOrNull { it.isMaster }
            ?: group.first()
    }
    var selectedOption by remember(group) { mutableStateOf(defaultOption) }
    var showMenu by remember { mutableStateOf(false) }

    val isLive = selectedOption.isLive
    val displayName = pageTitle.takeIf { it.isNotBlank() && it != "Cargando..." && it != "Transmisión Web" }
        ?: selectedOption.fileName.ifBlank { "Video Web" }

    val streamHostOrUrl = selectedOption.streamUrl
    val variantName = selectedOption.fileName.ifBlank {
        if (selectedOption.isMaster) "master" else "video"
    }

    val displayFormat = selectedOption.formatDetail.ifBlank {
        if (isLive) "m3u8 (en vivo)"
        else if (selectedOption.isMaster) "m3u8"
        else if (selectedOption.resolution.isNotBlank()) "m3u8 (${selectedOption.resolution})"
        else "m3u8"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTransmit(selectedOption) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail cuadrado naranja idéntico a WVC
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFFF9800), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }

                Spacer(Modifier.width(12.dp))

                // Desglose de información principal
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = streamHostOrUrl,
                        color = Color(0xFF9E9E9E),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = variantName,
                        color = Color(0xFF9E9E9E),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = displayFormat,
                        color = Color(0xFF4CAF50),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Opciones", tint = Color(0xFF9E9E9E), modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Emitir ahora (${selectedOption.resolution})", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = { showMenu = false; onTransmit(selectedOption) }
                        )
                        if (selectedOption.audioTracksSummary.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Audio: ${selectedOption.audioTracksSummary}", color = Color(0xFF38BDF8), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = { showMenu = false }
                            )
                        }
                        if (selectedOption.subtitleTracksSummary.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Subtítulos: ${selectedOption.subtitleTracksSummary}", color = Color(0xFFA78BFA), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                }
            }

            // Selector interactivo de calidades si hay más de 1 opción
            if (group.size > 1) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 0.8.dp)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calidad:",
                        color = Color(0xFFB0B0B0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    group.forEach { opt ->
                        val isSelected = opt.streamUrl == selectedOption.streamUrl
                        QualitySelectorChip(
                            label = opt.resolution.ifBlank { "Adaptive" },
                            isSelected = isSelected,
                            onClick = { selectedOption = opt }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QualitySelectorChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFFFF9800) else Color(0xFF2C2C2C)
    val textColor = if (isSelected) Color.Black else Color.White
    val borderColor = if (isSelected) Color(0xFFFFB74D) else Color(0xFF3D3D3D)

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
