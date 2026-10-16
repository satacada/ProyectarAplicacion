package com.omnicast.sender.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

/**
 * Hoja modal de lista de videos detectados inspirada fielmente en Web Video Caster.
 * Presenta el desglose real de flujos: master (adaptive), index-v1 (1920x800), iframes, audios y en vivo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WvcVideoListSheet(
    pageTitle: String,
    options: List<StreamQualityOption>,
    onSelectAndTransmit: (StreamQualityOption) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    var routeThroughPhone by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(38.dp).background(Color(0xFFF59E0B), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("🎬", fontSize = 18.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Lista de videos (${options.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(pageTitle.ifBlank { "Transmisión Web" }, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onClear) { Text("🗑", fontSize = 17.sp) }
                IconButton(onClick = onDismiss) { Text("✕", color = Color.LightGray, fontSize = 17.sp) }
            }

            Spacer(Modifier.height(8.dp))

            // Toggle fiel a Web Video Caster: "Emitir el video a través del teléfono"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .clickable { routeThroughPhone = !routeThroughPhone }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = routeThroughPhone,
                    onCheckedChange = { routeThroughPhone = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFF59E0B), checkmarkColor = Color.Black)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Emitir el video a través del teléfono", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Enrutar por proxy local si el servidor bloquea la TV Box o da error 403", color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(options) { opt ->
                    WvcVideoListItem(
                        option = opt,
                        pageTitle = pageTitle,
                        routeThroughPhone = routeThroughPhone,
                        onTransmit = {
                            val effectiveOpt = if (routeThroughPhone) opt.copy(
                                healedHeaders = opt.healedHeaders.toMutableMap().apply { put("X-Force-Proxy", "true") }
                            ) else opt
                            onSelectAndTransmit(effectiveOpt)
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun WvcVideoListItem(
    option: StreamQualityOption,
    pageTitle: String,
    routeThroughPhone: Boolean,
    onTransmit: () -> Unit
) {
    val isLive = option.isLive
    val isMovie = option.isMovie
    val cardBg = if (isLive) Color(0xFF241010) else (if (option.isSuggested) Color(0xFF11221A) else Color(0xFF1E293B))
    val cardBorder = if (isLive) Color(0xFFEF4444) else (if (option.isSuggested) Color(0xFF10B981) else Color(0xFF334155))

    val displayName = option.fileName.ifBlank {
        if (option.isMaster) "master" else option.label.substringBefore(" (").replace("🎬 ", "").replace("▶ ", "")
    }
    val displayFormat = option.formatDetail.ifBlank {
        if (isLive) "m3u8 (en vivo)"
        else if (option.isMaster) "m3u8 (adaptive)"
        else if (option.resolution.isNotBlank()) "m3u8 (${option.resolution})"
        else "m3u8"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, cardBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).background(if (isLive) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isLive) "🔴" else "🎬", fontSize = 16.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (option.isSuggested) {
                            Box(modifier = Modifier.background(Color(0xFF059669), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                Text("Recomendado", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = displayFormat,
                        color = if (isLive) Color(0xFFFCA5A5) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (option.audioTracksSummary.isNotBlank()) {
                        Text("🔊 " + option.audioTracksSummary, color = Color(0xFF38BDF8), fontSize = 10.sp, maxLines = 1)
                    }
                    if (option.subtitleTracksSummary.isNotBlank()) {
                        Text("💬 " + option.subtitleTracksSummary, color = Color(0xFFA78BFA), fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onTransmit,
                colors = ButtonDefaults.buttonColors(containerColor = if (isLive) Color(0xFFEF4444) else (if (isMovie) Color(0xFF10B981) else Color(0xFFF59E0B))),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(if (routeThroughPhone) "Proxy 📱" else "Emitir ▶", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
