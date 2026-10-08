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
import com.omnicast.sender.ui.theme.OmniThemeColors

/**
 * Las tres opciones principales de OmniCast presentadas de forma limpia, armónica y agradable.
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // OPCIÓN 1: CAPTURA WEB
        OmniTooltip(
            text = "¿Quieres conocer esta opción?\nNavega en sitios web de fútbol o películas, bloquea anuncios y transmite video a la TV.",
            enabled = tooltipsEnabled
        ) {
            Surface(
                color = OmniThemeColors.CardSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OmniThemeColors.CyanAccent.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🌐 Captura Web",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OmniThemeColors.CyanAccent
                        )
                        Text(
                            text = "Navegador sin anuncios con extracción de video",
                            fontSize = 11.sp,
                            color = OmniThemeColors.TextSecondary
                        )
                    }

                    Button(
                        onClick = onOpenBrowser,
                        colors = ButtonDefaults.buttonColors(containerColor = OmniThemeColors.CyanAccent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("🚀 Abrir Web", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                }
            }
        }

        // OPCIÓN 2: CASTEO-TV (PROYECCIÓN)
        OmniTooltip(
            text = "¿Quieres conocer esta opción?\nDuplica la pantalla del móvil a 60 FPS con baja latencia y audio interno.",
            enabled = tooltipsEnabled
        ) {
            Surface(
                color = OmniThemeColors.CardSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isStreaming) OmniThemeColors.EmeraldAccent else OmniThemeColors.IndigoAccent.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📱 Casteo-TV",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = OmniThemeColors.IndigoAccent
                            )
                            Text(
                                text = if (isStreaming) "🟢 Proyectando en tiempo real" else "Duplica la pantalla completa de tu teléfono",
                                fontSize = 11.sp,
                                color = if (isStreaming) OmniThemeColors.EmeraldAccent else OmniThemeColors.TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(OmniThemeColors.InputBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, OmniThemeColors.CardBorder, RoundedCornerShape(8.dp))
                                .clickable { onOpenProjectionConfig() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚙️", fontSize = 16.sp)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OmniThemeColors.InputBackground, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Anti-Tirones: ${if (smoothBufferEnabled) "Sí" else "No"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (smoothBufferEnabled) OmniThemeColors.EmeraldAccent else OmniThemeColors.TextSecondary
                        )
                        Text("•", color = OmniThemeColors.TextMuted, fontSize = 10.sp)
                        Text(
                            text = "R/F: $rfShort",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniThemeColors.CyanAccent
                        )
                        Text("•", color = OmniThemeColors.TextMuted, fontSize = 10.sp)
                        Text(
                            text = "Zoom: $zoomDisplayLabel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniThemeColors.AmberAccent
                        )
                    }

                    Button(
                        onClick = if (!isStreaming) onStartProjection else onStopProjection,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isStreaming) OmniThemeColors.IndigoAccent else OmniThemeColors.RedAccent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text(
                            text = if (!isStreaming) "📺 Iniciar Proyección" else "🛑 Detener Proyección",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // OPCIÓN 3: GALERÍA DE FOTOS
        OmniTooltip(
            text = "¿Quieres conocer esta opción?\nTransmite tus fotos a la TV Box en alta definición con zoom táctil y rotación sincronizada.",
            enabled = tooltipsEnabled
        ) {
            Surface(
                color = OmniThemeColors.CardSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OmniThemeColors.EmeraldAccent.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🖼️ Galería",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OmniThemeColors.EmeraldAccent
                        )
                        Text(
                            text = "Transmite fotos a la TV con zoom táctil",
                            fontSize = 11.sp,
                            color = OmniThemeColors.TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onOpenGallery,
                            colors = ButtonDefaults.buttonColors(containerColor = OmniThemeColors.EmeraldAccent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("📷 Fotos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        FilledTonalButton(
                            onClick = onPickExternalFile,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("📁 Otros", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
