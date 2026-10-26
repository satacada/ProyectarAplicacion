package com.omnicast.sender.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
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
 * Presenta el diseño limpio con thumbnail naranja y desglose de 4 líneas por flujo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WvcVideoListSheet(
    pageTitle: String,
    options: List<StreamQualityOption>,
    onSelectAndTransmit: (StreamQualityOption) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onRescan: (() -> Unit)? = null
) {
    var routeThroughPhone by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .background(Color(0xFF121212))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Lista de videos",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (onRescan != null) {
                    IconButton(onClick = onRescan, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Re-escanear", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Limpiar", tint = Color(0xFF9E9E9E), modifier = Modifier.size(20.dp))
                }
            }

            // Fila: Emitir el video a través del teléfono       [ ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { routeThroughPhone = !routeThroughPhone }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Emitir el video a través del teléfono",
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Checkbox(
                    checked = routeThroughPhone,
                    onCheckedChange = { routeThroughPhone = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFFF9800),
                        checkmarkColor = Color.Black,
                        uncheckedColor = Color(0xFFB0B0B0)
                    )
                )
            }

            HorizontalDivider(color = Color(0xFF262626), thickness = 0.8.dp)

            if (options.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(38.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No se han detectado videos todavía", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Reproduce el video en la página web o pulsa re-escanear para capturar el flujo.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    if (onRescan != null) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onRescan,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A00)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Volver a escanear", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(options) { opt ->
                        WvcVideoListItem(
                            option = opt,
                            pageTitle = pageTitle,
                            onTransmit = {
                                val effectiveOpt = if (routeThroughPhone) opt.copy(
                                    healedHeaders = opt.healedHeaders.toMutableMap().apply { put("X-Force-Proxy", "true") }
                                ) else opt
                                onSelectAndTransmit(effectiveOpt)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WvcVideoListItem(
    option: StreamQualityOption,
    pageTitle: String,
    onTransmit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isLive = option.isLive
    val displayName = pageTitle.takeIf { it.isNotBlank() && it != "Cargando..." && it != "Transmisión Web" }
        ?: option.fileName.ifBlank { "Video Web" }

    val streamHostOrUrl = option.streamUrl
    val variantName = option.fileName.ifBlank {
        if (option.isMaster) "master" else "video"
    }

    val displayFormat = option.formatDetail.ifBlank {
        if (isLive) "m3u8 (en vivo)"
        else if (option.isMaster) "m3u8"
        else if (option.resolution.isNotBlank()) "m3u8 (${option.resolution})"
        else "m3u8"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTransmit() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail cuadrado naranja idéntico a Web Video Caster
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color(0xFFFF9800), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }

        Spacer(Modifier.width(14.dp))

        // Desglose de 4 líneas exactas de WVC
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
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = displayFormat,
                color = Color(0xFF4CAF50),
                fontSize = 12.sp,
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
                    text = { Text("Emitir ahora", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = { showMenu = false; onTransmit() }
                )
                if (option.audioTracksSummary.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text("Audio: ${option.audioTracksSummary}", color = Color(0xFF38BDF8), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = { showMenu = false }
                    )
                }
                if (option.subtitleTracksSummary.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text("Subtítulos: ${option.subtitleTracksSummary}", color = Color(0xFFA78BFA), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = { showMenu = false }
                    )
                }
            }
        }
    }
}
