package com.omnicast.sender.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Elemento de lista agrupado por película o video con chips interactivos de calidad.
 */
@Composable
fun WvcVideoGroupItem(
    options: List<StreamQualityOption>,
    pageTitle: String,
    onTransmit: (StreamQualityOption) -> Unit
) {
    var selectedOption by remember(options) { mutableStateOf(options.first()) }
    val context = androidx.compose.ui.platform.LocalContext.current
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

    val isTestedSuccess = selectedOption.isTestedSuccess
    val isTestedFailed = selectedOption.isTestedFailed
    val isRecommended = (selectedOption.reputationScore >= 75 || selectedOption.isSuggested) && !isTestedFailed && !isTestedSuccess
    val thumbBg = when {
        isTestedSuccess -> Color(0xFF10B981)
        isTestedFailed -> Color(0xFF4B5563)
        isRecommended -> Color(0xFFF59E0B)
        else -> Color(0xFF475569)
    }
    val statusBadge = when {
        isTestedSuccess -> " • 🟢 [EN EMISIÓN]"
        isTestedFailed -> " • 🔴 [INACCESIBLE]"
        isRecommended -> " • ⭐ [RECOMENDADO]"
        else -> ""
    }
    val statusColor = when {
        isTestedSuccess -> Color(0xFF10B981)
        isTestedFailed -> Color(0xFFEF4444)
        isRecommended -> Color(0xFFF59E0B)
        else -> Color(0xFF94A3B8)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isTestedFailed) 0.55f else 1.0f)
            .then(if (isTestedSuccess) Modifier.background(Color(0x1A10B981)) else Modifier)
            .clickable { onTransmit(selectedOption) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(60.dp).background(thumbBg, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = displayName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = streamHostOrUrl, color = Color(0xFF9E9E9E), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = variantName, color = Color(0xFF9E9E9E), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = displayFormat + statusBadge, color = statusColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

            if (options.size > 1) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    options.forEach { opt ->
                        val isSel = opt.streamUrl == selectedOption.streamUrl
                        val chipLabel = opt.resolution.ifBlank { opt.fileName.take(6) }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSel) Color(0xFFFF9800) else Color(0xFF262626),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedOption = opt }
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = chipLabel,
                                color = if (isSel) Color.Black else Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
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
                DropdownMenuItem(
                    text = { Text("⬇️ Descargar película", color = Color(0xFF10B981), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        showMenu = false
                        com.omnicast.sender.ui.utils.MediaDownloadHelper.downloadMedia(
                            context,
                            selectedOption.streamUrl,
                            displayName,
                            selectedOption.healedHeaders
                        )
                    }
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
}
