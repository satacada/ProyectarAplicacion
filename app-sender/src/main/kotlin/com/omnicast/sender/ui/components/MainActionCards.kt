package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.core.domain.models.StreamProfile
import com.omnicast.sender.ui.theme.OmniThemeColors

/**
 * Las tres opciones principales en Cyber-Dark:
 * 1. Casteo a Tv (tarjeta violeta con estado inferior).
 * 2. Fila de dos columnas: Captura Web (ámbar) y Galería (magenta: Fotos y Videos).
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
        "FIT_100", "ORIGINAL_FIT" -> "100%"
        "ZOOM_85" -> "85%"
        "ZOOM_75" -> "75%"
        "ZOOM_115" -> "115%"
        "ZOOM_130" -> "130%"
        "FILL_16_9", "EXTEND_16_9" -> "16:9"
        else -> tvScaleMode
    }
    val rfShort = selectedProfile.label.replace(" @ ", "@")

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ==========================================
        // 1. CASTEO A TV — VIOLETA CYBER-DARK
        // ==========================================
        Surface(
            color = Color(0xFF1D1730),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF6D28D9)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFF2B1F47), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            VectorTvIcon(modifier = Modifier.size(22.dp), color = Color(0xFFA78BFA))
                        }
                        Text(
                            text = "Casteo a Tv",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF1F5F9)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { if (isStreaming) onStopProjection() else onStartProjection() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStreaming) Color(0xFFDC2626) else Color(0xFF7C3AED)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                if (!isStreaming) CastTvIcon(modifier = Modifier.size(15.dp), color = Color.White)
                                else Icon(Icons.Rounded.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (isStreaming) "Detener" else "Proyectar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF281C44), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF6D28D9), RoundedCornerShape(10.dp))
                                .clickable { onOpenProjectionConfig() },
                            contentAlignment = Alignment.Center
                        ) {
                            VectorGearIcon(modifier = Modifier.size(19.dp), color = Color(0xFFDDD6FE))
                        }
                    }
                }

                // Barra de información inferior estilizada (sin palabra ESTADO y en una sola línea)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF100C1C), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "$rfShort • Zoom: $zoomDisplayLabel • Anti-Tirones: ${if (smoothBufferEnabled) "Si" else "No"}",
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                if (tooltipsEnabled) {
                    Text(
                        text = "? Proyecta la pantalla y el audio interno con baja latencia y sin micrófono.",
                        color = Color(0xFFDDD6FE),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // ==========================================
        // 2. FILA DE 2 COLUMNAS: CAPTURA WEB Y GALERÍA
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // COLUMNA IZQUIERDA: CAPTURA WEB (ÁMBAR)
            Surface(
                color = Color(0xFF221B0F),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFD97706)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF332510), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VectorGlobeIcon(modifier = Modifier.size(16.dp), color = Color(0xFFFBBF24))
                        VectorArrowRightIcon(modifier = Modifier.size(14.dp), color = Color(0xFFFBBF24))
                        VectorTvIcon(modifier = Modifier.size(16.dp), color = Color(0xFFFBBF24))
                    }

                    Text(
                        text = "Captura Web",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Navegador embebido para streaming",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 13.sp,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Button(
                        onClick = onOpenBrowser,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            VectorGlobeIcon(modifier = Modifier.size(15.dp), color = Color.Black)
                            Text("Abrir Web", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        }
                    }
                }
            }

            // COLUMNA DERECHA: GALERÍA (MAGENTA / BERRY)
            Surface(
                color = Color(0xFF24121F),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFBE185D)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF38142C), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VectorImageIcon(modifier = Modifier.size(16.dp), color = Color(0xFFF43F5E))
                        VectorArrowRightIcon(modifier = Modifier.size(14.dp), color = Color(0xFFF43F5E))
                        VectorTvIcon(modifier = Modifier.size(16.dp), color = Color(0xFFF43F5E))
                    }

                    Text(
                        text = "Galería",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Fotos locales y archivos multimedia",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 13.sp,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onOpenGallery,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                VectorImageIcon(modifier = Modifier.size(14.dp), color = Color.White)
                                Text("Fotos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Button(
                            onClick = onPickExternalFile,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381328)),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9D174D)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text("• Videos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBCFE8))
                        }
                    }
                }
            }
        }
    }
}
