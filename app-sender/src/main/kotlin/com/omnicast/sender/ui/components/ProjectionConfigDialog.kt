package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omnicast.core.domain.models.StreamProfile
import kotlin.math.roundToInt

@Composable
fun ProjectionConfigDialog(
    selectedProfile: StreamProfile,
    onSelectProfile: (StreamProfile) -> Unit,
    smoothBufferEnabled: Boolean,
    onToggleSmoothBuffer: (Boolean) -> Unit,
    tvScaleMode: String,
    onSelectTvScaleMode: (String) -> Unit,
    tooltipsEnabled: Boolean = false,
    onDismiss: () -> Unit
) {
    var dialogTooltipsEnabled by remember { mutableStateOf(tooltipsEnabled) }
    var tempProfile by remember { mutableStateOf(selectedProfile) }
    var tempSmoothBuffer by remember { mutableStateOf(smoothBufferEnabled) }
    var tempScaleMode by remember { mutableStateOf(tvScaleMode) }

    val initialZoomIdx = remember(tempScaleMode) {
        val found = ZOOM_STEPS.indexOfFirst {
            it.id == tempScaleMode ||
                    (tempScaleMode == "EXTEND_16_9" && it.id == "FILL_16_9") ||
                    (tempScaleMode == "ORIGINAL_FIT" && it.id == "FIT_100")
        }
        if (found >= 0) found.toFloat() else 2f
    }
    var sliderValue by remember { mutableFloatStateOf(initialZoomIdx) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF111622),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF28354E)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cabecera: Icono tuerca, Título, Subtítulo y Botones (?, ✕)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFF18233C), CircleShape)
                                .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚙️", fontSize = 18.sp)
                        }

                        Column {
                            Text(
                                text = "Ajustes de Proyección",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Calidad de pantalla en tiempo real",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = if (dialogTooltipsEnabled) Color(0xFF09291D) else Color(0xFF19202E),
                                    shape = CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (dialogTooltipsEnabled) Color(0xFF10B981) else Color(0xFF334155),
                                    CircleShape
                                )
                                .clickable { dialogTooltipsEnabled = !dialogTooltipsEnabled },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "?",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dialogTooltipsEnabled) Color(0xFF34D399) else Color(0xFF94A3B8)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF19202E), CircleShape)
                                .border(1.dp, Color(0xFF334155), CircleShape)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 1. Perfil de Transmisión
                ProfileSelectionSection(
                    tempProfile = tempProfile,
                    onSelectProfile = { tempProfile = it }
                )

                // 2. Suavizado Anti-Tirones
                Surface(
                    color = Color(0xFF0C1F1C),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF183D36)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFF13362E), CircleShape)
                                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🛡️", fontSize = 15.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Suavizado Anti-Tirones", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(
                                    text = if (tempSmoothBuffer) "Activado (Buffer 100ms)" else "Desactivado",
                                    color = if (tempSmoothBuffer) Color(0xFF34D399) else Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        Switch(
                            checked = tempSmoothBuffer,
                            onCheckedChange = { tempSmoothBuffer = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0xFF142420)
                            )
                        )
                    }
                }

                // 3. Proporción y Zoom en TV
                TvScaleZoomSection(
                    sliderValue = sliderValue,
                    onSliderChange = { sliderValue = it },
                    onSelectStepId = { tempScaleMode = it }
                )

                // Botón Guardar y Aplicar
                Button(
                    onClick = {
                        onSelectProfile(tempProfile)
                        onToggleSmoothBuffer(tempSmoothBuffer)
                        onSelectTvScaleMode(tempScaleMode)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("✓ Guardar y Aplicar", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.White)
                }

                // Cerrar sin guardar
                Box(
                    modifier = Modifier.fillMaxWidth().clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cerrar sin guardar",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
