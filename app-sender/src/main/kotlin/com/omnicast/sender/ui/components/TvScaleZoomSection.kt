package com.omnicast.sender.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun TvScaleZoomSection(
    sliderValue: Float,
    onSliderChange: (Float) -> Unit,
    onSelectStepId: (String) -> Unit
) {
    Surface(
        color = Color(0xFF101626),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF24324E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val activeStep = ZOOM_STEPS.getOrElse(sliderValue.roundToInt().coerceIn(0, ZOOM_STEPS.size - 1)) { ZOOM_STEPS[2] }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.AspectRatio, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(15.dp))
                    Text(
                        text = "PROPORCIÓN Y ZOOM EN TV",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Seleccionado:", color = Color(0xFF64748B), fontSize = 11.sp)
                    Surface(
                        color = Color(0xFF1E2B4A),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6))
                    ) {
                        Text(
                            text = activeStep.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF93C5FD),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Slider(
                value = sliderValue,
                onValueChange = { newVal ->
                    onSliderChange(newVal)
                    val step = ZOOM_STEPS[newVal.roundToInt().coerceIn(0, ZOOM_STEPS.size - 1)]
                    onSelectStepId(step.id)
                },
                valueRange = 0f..(ZOOM_STEPS.size - 1).toFloat(),
                steps = ZOOM_STEPS.size - 2,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF3B82F6),
                    activeTrackColor = Color(0xFF4F46E5),
                    inactiveTrackColor = Color(0xFF1E293B)
                )
            )

            val context = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ZOOM_STEPS.forEachIndexed { idx, step ->
                    val isCurrent = idx == sliderValue.roundToInt()
                    Text(
                        text = step.label,
                        fontSize = if (isCurrent) 10.sp else 9.sp,
                        color = if (isCurrent) Color(0xFF60A5FA) else Color(0xFF64748B),
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                        modifier = Modifier.clickable {
                            onSliderChange(idx.toFloat())
                            onSelectStepId(step.id)
                        }
                    )
                }
            }

            Surface(
                color = Color(0xFF0C2438),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clickable {
                        com.omnicast.sender.overlay.FloatingCalibrationOverlay.show(context)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎛️ ", fontSize = 14.sp)
                    Text(
                        text = "Enfocar y Mover en TV (Zoom y Arrastre)",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
