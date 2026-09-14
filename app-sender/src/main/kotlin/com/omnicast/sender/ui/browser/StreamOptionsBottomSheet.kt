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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hoja modal de opciones de video y calidades (Estilo Web Video Caster).
 * Despliega los flujos interceptados con sus calidades para enviar a la TV Box.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamOptionsBottomSheet(
    pageTitle: String,
    options: List<StreamQualityOption>,
    onSelectAndTransmit: (StreamQualityOption) -> Unit,
    onDismiss: () -> Unit,
    onClear: (() -> Unit)? = null
) {
    var selectedOption by remember { mutableStateOf(options.firstOrNull()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎬", fontSize = 26.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Videos Detectados (${options.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        pageTitle.take(40),
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                if (onClear != null) {
                    IconButton(onClick = onClear) {
                        Text("🗑", fontSize = 18.sp)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Text("✕", color = Color.LightGray, fontSize = 18.sp)
                }
            }

            Text(
                "Selecciona el flujo o calidad que deseas enviar a la TV Box:",
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(options) { opt ->
                    val isSelected = selectedOption == opt
                    val borderColor = if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155)
                    val bgColor = if (isSelected) Color(0xFF291B00) else Color(0xFF1E293B)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor, RoundedCornerShape(14.dp))
                            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable { selectedOption = opt }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                opt.label,
                                color = if (isSelected) Color(0xFFFDE68A) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (opt.resolution != "Auto") "Resolución: ${opt.resolution} • Tasa: ${opt.bitrate}"
                                else "Reproducción Automática Adaptativa sin cortes",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedOption = opt },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFFF59E0B),
                                unselectedColor = Color.Gray
                            )
                        )
                    }
                }
            }

            Button(
                onClick = { selectedOption?.let { onSelectAndTransmit(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = selectedOption != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "🚀 Transmitir a TV Box (ExoPlayer)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
