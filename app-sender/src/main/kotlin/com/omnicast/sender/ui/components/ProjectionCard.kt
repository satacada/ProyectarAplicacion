package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.core.domain.models.StreamProfile

/**
 * Tarjeta compacta y vibrante de control de Casteo-TV.
 */
@Composable
fun ProjectionCard(
    isStreaming: Boolean,
    selectedProfile: StreamProfile,
    smoothBufferEnabled: Boolean,
    tvScaleMode: String,
    onStartProjection: () -> Unit,
    onStopProjection: () -> Unit,
    onOpenConfig: () -> Unit,
    tooltipsEnabled: Boolean = true
) {
    val zoomDisplayLabel = when (tvScaleMode) {
        "FIT_100", "ORIGINAL_FIT" -> "Fit 100%"
        "ZOOM_85" -> "85%"
        "ZOOM_75" -> "75%"
        "ZOOM_115" -> "115%"
        "ZOOM_130" -> "130%"
        "FILL_16_9", "EXTEND_16_9" -> "16:9"
        else -> tvScaleMode
    }

    val rfShort = selectedProfile.label.replace(" @ ", "@")

    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isStreaming) Color(0xFF10B981) else Color(0xFF3B82F6)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OmniTooltip(
                    text = if (!isStreaming) "Inicia la captura y duplicación de pantalla en la TV Box."
                    else "Detiene la transmisión actual.",
                    enabled = tooltipsEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Button(
                        onClick = if (!isStreaming) onStartProjection else onStopProjection,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isStreaming) Color(0xFF2563EB) else Color(0xFFDC2626)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(if (!isStreaming) Icons.Rounded.Tv else Icons.Rounded.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (!isStreaming) "Iniciar Proyección" else "Detener Proyección",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                OmniTooltip(
                    text = "Ajustes: Resolución, FPS, suavizado anti-tirones y proporción de TV.",
                    enabled = tooltipsEnabled
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF475569), RoundedCornerShape(8.dp))
                            .clickable { onOpenConfig() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Ajustes", tint = Color(0xFFCBD5E1), modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Resumen en vivo en una línea compacta
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Anti-Tirones: ${if (smoothBufferEnabled) "Sí" else "No"}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (smoothBufferEnabled) Color(0xFF34D399) else Color(0xFF94A3B8)
                )
                Text("•", color = Color(0xFF475569), fontSize = 10.sp)
                Text(
                    text = "R/F: $rfShort",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
                Text("•", color = Color(0xFF475569), fontSize = 10.sp)
                Text(
                    text = "Zoom: $zoomDisplayLabel",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24)
                )
            }
        }
    }
}
