package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.omnicast.core.domain.models.StreamProfile

/**
 * Tarjeta de acción de Casteo-TV en la pantalla principal.
 * Contiene el botón de Proyectar/Detener, la tuerca de configuración y el resumen en vivo.
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

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📱 Casteo-TV (Pantalla Completa)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isStreaming) "🟢 Transmitiendo a la TV" else "Duplica la pantalla del móvil a baja latencia",
                        fontSize = 12.sp,
                        color = if (isStreaming) Color(0xFF10B981) else Color(0xFF94A3B8)
                    )
                }

                OmniTooltip(
                    text = "¿Quieres conocer esta opción?\nAbre el panel para cambiar resolución, FPS, suavizado anti-tirones y zoom.",
                    enabled = tooltipsEnabled
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF334155), RoundedCornerShape(10.dp))
                            .clickable { onOpenConfig() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚙️", fontSize = 18.sp)
                    }
                }
            }

            // Resumen en vivo solicitado por el usuario sin tener que dar clic en la rueda
            OmniTooltip(
                text = "Resumen de transmisión activo:\nAnti-Tirones, Resolución/FPS y proporción en TV seleccionada.",
                enabled = tooltipsEnabled
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Anti-Tirones: ${if (smoothBufferEnabled) "Activado" else "Desactivado"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (smoothBufferEnabled) Color(0xFF10B981) else Color(0xFF94A3B8)
                    )
                    Text("•", color = Color(0xFF475569), fontSize = 11.sp)
                    Text(
                        text = "R/F: $rfShort",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8)
                    )
                    Text("•", color = Color(0xFF475569), fontSize = 11.sp)
                    Text(
                        text = "Zoom: $zoomDisplayLabel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFBBF24)
                    )
                }
            }

            // Botón de acción principal
            OmniTooltip(
                text = "¿Quieres conocer esta opción?\n" +
                        if (!isStreaming) "Inicia la captura y duplicación de pantalla por GPU a la TV Box."
                        else "Detiene la transmisión actual y libera los recursos de captura.",
                enabled = tooltipsEnabled
            ) {
                if (!isStreaming) {
                    Button(
                        onClick = onStartProjection,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("📺 Iniciar Proyección", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = onStopProjection,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("🛑 Detener Transmisión", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
