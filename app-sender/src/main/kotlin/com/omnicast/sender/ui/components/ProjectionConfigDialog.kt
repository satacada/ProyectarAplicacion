package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.omnicast.core.domain.models.QualityMode
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
    val initialZoomIdx = remember(tvScaleMode) {
        val found = ZOOM_STEPS.indexOfFirst {
            it.id == tvScaleMode ||
                    (tvScaleMode == "EXTEND_16_9" && it.id == "FILL_16_9") ||
                    (tvScaleMode == "ORIGINAL_FIT" && it.id == "FIT_100")
        }
        if (found >= 0) found.toFloat() else 2f
    }
    var sliderValue by remember { mutableFloatStateOf(initialZoomIdx) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cabecera con Título, Foco de Ayuda interno y Cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ Ajustes de Proyección",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Foco de Ayuda interno (Verde = ON, Plomo = OFF)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = if (dialogTooltipsEnabled) Color(0xFFD1FAE5) else Color(0xFFE2E8F0),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .border(
                                    width = 1.2.dp,
                                    color = if (dialogTooltipsEnabled) Color(0xFF10B981) else Color(0xFF94A3B8),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .clickable { dialogTooltipsEnabled = !dialogTooltipsEnabled },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💡", fontSize = 14.sp)
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Text("✕", color = Color(0xFF64748B), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 1. Perfil de Transmisión con Radio Buttons compactos
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Perfil de Transmisión (Resolución y FPS)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )

                    PROFILE_OPTIONS.forEach { opt ->
                        val isSelected = selectedProfile.mode == opt.profile.mode ||
                                (opt.profile.mode == QualityMode.BALANCED_30FPS && selectedProfile.mode == QualityMode.BALANCED)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF818CF8) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectProfile(opt.profile) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectProfile(opt.profile) },
                                    modifier = Modifier.size(20.dp),
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF4F46E5),
                                        unselectedColor = Color(0xFF94A3B8)
                                    )
                                )
                                Text(
                                    text = opt.displayLabel,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF4338CA) else Color(0xFF1E293B)
                                )
                            }
                            if (dialogTooltipsEnabled) {
                                Text(
                                    text = opt.infoTip,
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(start = 26.dp, top = 2.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Suavizado Anti-Tirones
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🛡️ Suavizado Anti-Tirones",
                                color = Color(0xFF0F172A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (smoothBufferEnabled) "Activado (Buffer 100ms en TV)" else "Desactivado (Ultra-baja latencia cruda)",
                                color = if (smoothBufferEnabled) Color(0xFF059669) else Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Switch(
                            checked = smoothBufferEnabled,
                            onCheckedChange = onToggleSmoothBuffer,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                    if (dialogTooltipsEnabled) {
                        Text(
                            text = "💡 Buffer elástico en la TV para evitar micro-cortes y tirones de imagen en redes Wi-Fi con interferencia.",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                }

                // 3. Proporción y Zoom en TV reducido
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val activeStep = ZOOM_STEPS.getOrElse(sliderValue.roundToInt().coerceIn(0, ZOOM_STEPS.size - 1)) { ZOOM_STEPS[2] }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Proporción y Zoom en TV",
                            color = Color(0xFF0F172A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = Color(0xFFEEF2FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = activeStep.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4338CA),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Slider(
                        value = sliderValue,
                        onValueChange = { newVal ->
                            sliderValue = newVal
                            val step = ZOOM_STEPS[newVal.roundToInt().coerceIn(0, ZOOM_STEPS.size - 1)]
                            onSelectTvScaleMode(step.id)
                        },
                        valueRange = 0f..(ZOOM_STEPS.size - 1).toFloat(),
                        steps = ZOOM_STEPS.size - 2,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF4F46E5),
                            activeTrackColor = Color(0xFF4F46E5),
                            inactiveTrackColor = Color(0xFFCBD5E1)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ZOOM_STEPS.forEach { step ->
                            Text(
                                text = step.label,
                                fontSize = 9.sp,
                                color = if (step.id == activeStep.id) Color(0xFF4F46E5) else Color(0xFF64748B),
                                fontWeight = if (step.id == activeStep.id) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    if (dialogTooltipsEnabled) {
                        Text(
                            text = "💡 Ajusta el tamaño y encuadre en la TV: desde reducir al 75% hasta 16:9 pantalla completa.",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                }

                // Botón neutral de cerrar (sin azul invasivo)
                FilledTonalButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text("Cerrar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF334155))
                }
            }
        }
    }
}
