package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Barra segmentada para seleccionar el modo de transmisión o lanzar el navegador integrado.
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
            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OmniTooltip(
            text = "¿Quieres conocer esta opción?\nAbre el navegador web para ver partidos, bloquear anuncios e interceptar video a la TV.",
            enabled = tooltipsEnabled,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                    .clickable { onOpenBrowser() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🌐 Captura Web", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        OmniTooltip(
            text = "¿Quieres conocer esta opción?\nDuplica la pantalla del móvil a 60 FPS con ultra-baja latencia y audio interno.",
            enabled = tooltipsEnabled,
            modifier = Modifier.weight(1f)
        ) {
            val isSel = selectedTabIndex == 1
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSel) Color(0xFF0284C7) else Color(0xFF334155), RoundedCornerShape(8.dp))
                    .clickable { onSelectTabIndex(1) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("📱 Casteo-TV", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        OmniTooltip(
            text = "¿Quieres conocer esta opción?\nTransmite tus fotos y recuerdos en alta definición con zoom táctil sincronizado.",
            enabled = tooltipsEnabled,
            modifier = Modifier.weight(1f)
        ) {
            val isSel = selectedTabIndex == 2
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSel) Color(0xFF0284C7) else Color(0xFF334155), RoundedCornerShape(8.dp))
                    .clickable { onSelectTabIndex(2) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🖼️ Galería", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
