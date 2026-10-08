package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
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

data class ZoomSliderStep(val id: String, val label: String)

val ZOOM_STEPS = listOf(
    ZoomSliderStep("ZOOM_130", "130%"),
    ZoomSliderStep("ZOOM_115", "115%"),
    ZoomSliderStep("FIT_100", "Fit 100%"),
    ZoomSliderStep("FILL_16_9", "16:9"),
    ZoomSliderStep("ZOOM_85", "85%"),
    ZoomSliderStep("ZOOM_75", "75%")
)

private data class ProfileOption(
    val profile: StreamProfile,
    val displayLabel: String,
    val infoTip: String
)

private val PROFILE_OPTIONS = listOf(
    ProfileOption(
        StreamProfile.MAX_60FPS,
        "1280p@60fps - Experimental",
        "Alta exigencia de CPU/GPU. Para teléfonos de gama alta con señal Wi-Fi 5GHz limpia."
    ),
    ProfileOption(
        StreamProfile.BALANCED_30FPS,
        "1280p@30fps - Estable",
        "Configuración recomendada. Máxima estabilidad de transmisión sin cortes."
    ),
    ProfileOption(
        StreamProfile.CINEMA_1080P_30FPS,
        "1080p@30fps - Cine",
        "Máxima definición y nitidez visual para ver películas y series."
    ),
    ProfileOption(
        StreamProfile.FLUID_45FPS,
        "720p@45fps - Fluido",
        "Excelente balance entre velocidad de refresco y bajo consumo de red."
    ),
    ProfileOption(
        StreamProfile.ULTRA_60FPS,
        "540p@60fps - 60fps",
        "60 cuadros por segundo reales. Ideal para juegos y partidos de fútbol dinámicos."
    )
)

@Composable
fun ProjectionConfigDialog(
    selectedProfile: StreamProfile,
    onSelectProfile: (StreamProfile) -> Unit,
    smoothBufferEnabled: Boolean,
    onToggleSmoothBuffer: (Boolean) -> Unit,
    tvScaleMode: String,
    onSelectTvScaleMode: (String) -> Unit,
    tooltipsEnabled: Boolean = true,
    onDismiss: () -> Unit
) {
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
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ Configuración de Proyección",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text("✕", color = Color(0xFF94A3B8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 1. Perfil de Transmisión con Radio Buttons
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Perfil de Transmisión (Resolución y FPS)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    PROFILE_OPTIONS.forEach { opt ->
                        val isSelected = selectedProfile.mode == opt.profile.mode ||
                                (opt.profile.mode == QualityMode.BALANCED_30FPS && selectedProfile.mode == QualityMode.BALANCED)
                        OmniTooltip(
                            text = opt.infoTip,
                            enabled = tooltipsEnabled
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isSelected) Color(0xFF0F172A) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectProfile(opt.profile) }
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectProfile(opt.profile) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF38BDF8),
                                        unselectedColor = Color(0xFF64748B)
                                    )
                                )
                                Text(
                                    text = opt.displayLabel,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF38BDF8) else Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF334155))

                // 2. Suavizado Anti-Tirones
                OmniTooltip(
                    text = "¿Quieres conocer esta opción?\n" +
                            "Buffer elástico de 100ms en la TV para evitar micro-cortes y tirones de imagen en redes con interferencia.",
                    enabled = tooltipsEnabled
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🛡️ Suavizado Anti-Tirones",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (smoothBufferEnabled) "Activado (Buffer 100ms en TV)" else "Desactivado (Ultra-baja latencia cruda)",
                                color = if (smoothBufferEnabled) Color(0xFF10B981) else Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = smoothBufferEnabled,
                            onCheckedChange = onToggleSmoothBuffer,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedThumbColor = Color(0xFFCBD5E1),
                                uncheckedTrackColor = Color(0xFF475569)
                            )
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF334155))

                // 3. Proporción y Zoom en TV (Slider Horizontal)
                OmniTooltip(
                    text = "¿Quieres conocer esta opción?\n" +
                            "Ajusta el tamaño y encuadre en la TV: desde reducir al 75% para que no se corten los bordes del móvil, hasta 16:9 para llenar toda la pantalla.",
                    enabled = tooltipsEnabled
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activeStep = ZOOM_STEPS.getOrElse(sliderValue.roundToInt().coerceIn(0, ZOOM_STEPS.size - 1)) { ZOOM_STEPS[2] }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Proporción y Zoom en TV",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = Color(0xFF0284C7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = activeStep.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = Color(0xFF0284C7),
                                inactiveTrackColor = Color(0xFF334155)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ZOOM_STEPS.forEach { step ->
                                Text(
                                    text = step.label,
                                    fontSize = 10.sp,
                                    color = if (step.id == activeStep.id) Color(0xFF38BDF8) else Color(0xFF64748B),
                                    fontWeight = if (step.id == activeStep.id) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("Listo", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
