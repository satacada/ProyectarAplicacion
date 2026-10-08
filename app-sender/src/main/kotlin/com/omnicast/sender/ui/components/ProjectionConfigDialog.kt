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
            color = Color(0xFF262D3D), // Plomo grafito oscuro, cómodo a la vista
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF434E66)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cabecera con Título, Indicador de Ayuda '?' y Cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ Ajustes de Proyección",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF1F5F9)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Indicador de Ayuda interno '?' (Verde = ON, Plomo = OFF)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = if (dialogTooltipsEnabled) Color(0xFF064E3B) else Color(0xFF333D52),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .border(
                                    width = 1.2.dp,
                                    color = if (dialogTooltipsEnabled) Color(0xFF10B981) else Color(0xFF52607D),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .clickable { dialogTooltipsEnabled = !dialogTooltipsEnabled },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "?",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (dialogTooltipsEnabled) Color(0xFF6EE7B7) else Color(0xFFCBD5E1)
                            )
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Text("✕", color = Color(0xFF94A3B8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 1. Perfil de Transmisión con Radio Buttons compactos
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Perfil de Transmisión (Resolución y FPS)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )

                    PROFILE_OPTIONS.forEach { opt ->
                        val isSelected = selectedProfile.mode == opt.profile.mode ||
                                (opt.profile.mode == QualityMode.BALANCED_30FPS && selectedProfile.mode == QualityMode.BALANCED)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isSelected) Color(0xFF333D56) else Color(0xFF1E2430),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF6366F1) else Color(0xFF333D52),
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
                                        selectedColor = Color(0xFF818CF8),
                                        unselectedColor = Color(0xFF64748B)
                                    )
                                )
                                Text(
                                    text = opt.displayLabel,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFFF1F5F9) else Color(0xFFCBD5E1)
                                )
                            }
                            if (dialogTooltipsEnabled) {
                                Text(
                                    text = "? ${opt.infoTip}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
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
                        .background(Color(0xFF1E2430), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF333D52), RoundedCornerShape(10.dp))
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
                                color = Color(0xFFF1F5F9),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (smoothBufferEnabled) "Activado (Buffer 100ms en TV)" else "Desactivado (Ultra-baja latencia cruda)",
                                color = if (smoothBufferEnabled) Color(0xFF10B981) else Color(0xFF94A3B8),
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
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0xFF333D52)
                            )
                        )
                    }
                    if (dialogTooltipsEnabled) {
                        Text(
                            text = "? Buffer elástico en la TV para evitar micro-cortes y tirones de imagen en redes Wi-Fi con interferencia.",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }

                // 3. Proporción y Zoom en TV reducido con opción activa resaltada
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E2430), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF333D52), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val activeStep = ZOOM_STEPS.getOrElse(sliderValue.roundToInt().coerceIn(0, ZOOM_STEPS.size - 1)) { ZOOM_STEPS[2] }
                    Text(
                        text = "Proporción y Zoom en TV",
                        color = Color(0xFFF1F5F9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

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
                            thumbColor = Color(0xFF6366F1),
                            activeTrackColor = Color(0xFF6366F1),
                            inactiveTrackColor = Color(0xFF333D52)
                        )
                    )

                    // Opciones de zoom resaltando visiblemente la activa
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ZOOM_STEPS.forEachIndexed { idx, step ->
                            val isCurrent = step.id == activeStep.id
                            Surface(
                                color = if (isCurrent) Color(0xFF4F46E5) else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)) else null,
                                modifier = Modifier.clickable {
                                    sliderValue = idx.toFloat()
                                    onSelectTvScaleMode(step.id)
                                }
                            ) {
                                Text(
                                    text = step.label,
                                    fontSize = if (isCurrent) 10.sp else 9.sp,
                                    color = if (isCurrent) Color.White else Color(0xFF94A3B8),
                                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = if (isCurrent) 5.dp else 2.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (dialogTooltipsEnabled) {
                        Text(
                            text = "? Ajusta el tamaño y encuadre en la TV: desde reducir al 75% hasta 16:9 pantalla completa.",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }

                // Botón neutral de cerrar
                FilledTonalButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF333D52)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text("Cerrar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}
