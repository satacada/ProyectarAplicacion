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
        // 1. CAPTURA WEB — DISTINTIVO CYAN EN PLOMO
        // ==========================================
        Surface(
            color = Color(0xFF202C3D), // Plomo slate tintado en azul/cyan
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF0284C7)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🌐", fontSize = 20.sp)
                        Text(
                            text = "Captura Web",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Button(
                        onClick = onOpenBrowser,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("🚀 Abrir Web", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                if (tooltipsEnabled) {
                    Text(
                        text = "? Navega sitios de fútbol o películas, bloquea anuncios y envía el stream directo a la TV sin gastar batería.",
                        color = Color(0xFFBAE6FD),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // ==========================================
        // 2. CASTEO-TV — DISTINTIVO VIOLETA EN PLOMO
        // ==========================================
        Surface(
            color = Color(0xFF282542), // Plomo slate tintado en violeta/índigo
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.2.dp,
                if (isStreaming) Color(0xFF10B981) else Color(0xFF6366F1)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        Text("📱", fontSize = 20.sp)
                        Text(
                            text = "Casteo-TV",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
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
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = if (!isStreaming) "📺 Proyectar" else "🛑 Detener",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF1E1C33), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF4B4673), RoundedCornerShape(8.dp))
                                .clickable { onOpenProjectionConfig() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚙️", fontSize = 15.sp)
                        }
                    }
                }

                // Resumen en vivo nítido sobre plomo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1C33), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF3E395E), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Anti-Tirones: ${if (smoothBufferEnabled) "Sí" else "No"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (smoothBufferEnabled) Color(0xFF34D399) else Color(0xFF94A3B8)
                    )
                    Text("•", color = Color(0xFF4B4673), fontSize = 11.sp)
                    Text(
                        text = "R/F: $rfShort",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF818CF8)
                    )
                    Text("•", color = Color(0xFF4B4673), fontSize = 11.sp)
                    Text(
                        text = "Zoom: $zoomDisplayLabel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24)
                    )
                }

                if (tooltipsEnabled) {
                    Text(
                        text = "? Duplica la pantalla por hardware a 60 FPS con audio interno. Toca la tuerca ⚙️ para cambiar resolución o zoom.",
                        color = Color(0xFFC7D2FE),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // ==========================================
        // 3. GALERÍA — DISTINTIVO ESMERALDA EN PLOMO
        // ==========================================
        Surface(
            color = Color(0xFF1B2F27), // Plomo slate tintado en verde esmeralda
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF059669)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🖼️", fontSize = 20.sp)
                        Text(
                            text = "Galería",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF6EE7B7)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onOpenGallery,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("📷 Fotos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        FilledTonalButton(
                            onClick = onPickExternalFile,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF123B2A)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("📁 Otros", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA7F3D0))
                        }
                    }
                }

                if (tooltipsEnabled) {
                    Text(
                        text = "? Transmite tus fotos en alta definición con zoom táctil y rotación sincronizada en tiempo real con la TV Box.",
                        color = Color(0xFFA7F3D0),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
