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
 * Las tres opciones principales (Captura Web, Casteo-TV, Galería) con colores distintivos,
 * diseño compacto y descripciones detalladas condicionadas al foco de ayuda.
 */
@Composable
fun MainActionCards(
    isStreaming: Boolean,
    selectedProfile: StreamProfile,
    smoothBufferEnabled: Boolean,
    tvScaleMode: String,
    tooltipsEnabled: Boolean,
    onOpenBrowser: () -> Unit,
    onStartProjection: () -> Unit,
    onStopProjection: () -> Unit,
    onOpenProjectionConfig: () -> Unit,
    onOpenGallery: () -> Unit,
    onPickExternalFile: () -> Unit
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // ==========================================
        // 1. CAPTURA WEB — DISTINTIVO CYAN / CELESTE
        // ==========================================
        Surface(
            color = Color(0xFF0C2D48), // Azul océano profundo tintado
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF0284C7)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🌐", fontSize = 18.sp)
                        Text(
                            text = "Captura Web",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Button(
                        onClick = onOpenBrowser,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(7.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("🚀 Abrir Web", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                if (tooltipsEnabled) {
                    Text(
                        text = "💡 Navega sitios de fútbol o películas, bloquea anuncios y envía el stream directo a la TV sin gastar batería.",
                        color = Color(0xFFBAE6FD),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // ==========================================
        // 2. CASTEO-TV — DISTINTIVO ÍNDIGO / VIOLETA
        // ==========================================
        Surface(
            color = Color(0xFF261D4E), // Púrpura índigo tintado
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.2.dp,
                if (isStreaming) Color(0xFF10B981) else Color(0xFF6366F1)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📱", fontSize = 18.sp)
                        Text(
                            text = "Casteo-TV",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5B4FC)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = if (!isStreaming) onStartProjection else onStopProjection,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isStreaming) Color(0xFF4F46E5) else Color(0xFFDC2626)
                            ),
                            shape = RoundedCornerShape(7.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (!isStreaming) "📺 Proyectar" else "🛑 Detener",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF1E1B4B), RoundedCornerShape(7.dp))
                                .border(1.dp, Color(0xFF4338CA), RoundedCornerShape(7.dp))
                                .clickable { onOpenProjectionConfig() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚙️", fontSize = 14.sp)
                        }
                    }
                }

                // Resumen compacto en una sola línea sutil
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF191338), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Anti-Tirones: ${if (smoothBufferEnabled) "Sí" else "No"}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (smoothBufferEnabled) Color(0xFF34D399) else Color(0xFF94A3B8)
                    )
                    Text("•", color = Color(0xFF4338CA), fontSize = 10.sp)
                    Text(
                        text = "R/F: $rfShort",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF818CF8)
                    )
                    Text("•", color = Color(0xFF4338CA), fontSize = 10.sp)
                    Text(
                        text = "Zoom: $zoomDisplayLabel",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFBBF24)
                    )
                }

                if (tooltipsEnabled) {
                    Text(
                        text = "💡 Duplica la pantalla por hardware a 60 FPS con audio interno. Toca la tuerca ⚙️ para cambiar resolución o zoom.",
                        color = Color(0xFFC7D2FE),
                        fontSize = 10.sp
                    )
                }
            }
        }

        // ==========================================
        // 3. GALERÍA — DISTINTIVO VERDE ESMERALDA
        // ==========================================
        Surface(
            color = Color(0xFF063E2F), // Verde esmeralda profundo tintado
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF059669)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🖼️", fontSize = 18.sp)
                        Text(
                            text = "Galería",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6EE7B7)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onOpenGallery,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(7.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("📷 Fotos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        FilledTonalButton(
                            onClick = onPickExternalFile,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF0F5132)),
                            shape = RoundedCornerShape(7.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("📁 Otros", fontSize = 11.sp, color = Color(0xFFA7F3D0))
                        }
                    }
                }

                if (tooltipsEnabled) {
                    Text(
                        text = "💡 Transmite tus fotos en alta definición con zoom táctil y rotación sincronizada en tiempo real con la TV Box.",
                        color = Color(0xFFA7F3D0),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
