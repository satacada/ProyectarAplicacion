package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Selector animado y estilizado de los 3 modos de transmisión con acentos de color vibrantes.
 */
@Composable
fun CastModeSelectorBar(
    selectedTabIndex: Int,
    tooltipsEnabled: Boolean,
    onOpenBrowser: () -> Unit,
    onSelectTabIndex: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 1. Galería (Acento Esmeralda Neón)
        val isGalSelected = selectedTabIndex == 2
        OmniTooltip(
            text = "¿Quieres conocer esta opción?\nTransmite tus fotos a la TV con zoom táctil sincronizado.",
            enabled = tooltipsEnabled,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                color = if (isGalSelected) Color(0xFF065F46) else Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isGalSelected) Color(0xFF10B981) else Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTabIndex(2) }
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🖼️ Galería",
                        color = if (isGalSelected) Color(0xFFA7F3D0) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = if (isGalSelected) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            }
        }

        // 2. Casteo-TV (Acento Índigo/Azul Eléctrico)
        val isCastSelected = selectedTabIndex == 1
        OmniTooltip(
            text = "¿Quieres conocer esta opción?\nDuplica la pantalla del móvil a 60 FPS con baja latencia y audio interno.",
            enabled = tooltipsEnabled,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                color = if (isCastSelected) Color(0xFF1E3A8A) else Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isCastSelected) Color(0xFF38BDF8) else Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTabIndex(1) }
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📱 Casteo-TV",
                        color = if (isCastSelected) Color(0xFFBAE6FD) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = if (isCastSelected) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            }
        }

        // 3. Captura Web (Acento Cyan / Turquesa)
        OmniTooltip(
            text = "¿Quieres conocer esta opción?\nAbre el navegador web para ver partidos, bloquear anuncios e interceptar video a la TV.",
            enabled = tooltipsEnabled,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0891B2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenBrowser() }
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🌐 Web Video",
                        color = Color(0xFF67E8F9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
