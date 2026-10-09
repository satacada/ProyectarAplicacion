package com.omnicast.sender.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⛶", fontSize = 13.sp, color = Color(0xFF818CF8))
                    Text(
                        text = "PROPORCIÓN Y ZOOM EN TV",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }

                Surface(
                    color = Color(0xFF1E2B4A),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6))
                ) {
                    Text(
                        text = activeStep.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF93C5FD),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
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
        }
    }
}
