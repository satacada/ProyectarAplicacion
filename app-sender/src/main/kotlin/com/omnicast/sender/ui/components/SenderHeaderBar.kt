package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Barra superior minimalista con título, versión, activador global de Tooltips de ayuda y menú lateral.
 */
@Composable
fun SenderHeaderBar(
    tooltipsEnabled: Boolean = true,
    onToggleTooltips: (Boolean) -> Unit = {},
    onOpenHelp: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onOpenDrawer != null) {
                IconButton(onClick = onOpenDrawer, modifier = Modifier.size(36.dp)) {
                    Text("☰", color = Color(0xFF38BDF8), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("OmniCast Pro", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Surface(
                        color = Color(0xFF0284C7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "v3.31",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Text("Transmisión Universal a TV Box", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Activador global de Tooltips de ayuda solicitado por el usuario
            OmniTooltip(
                text = if (tooltipsEnabled) "💡 Tooltips de Ayuda ACTIVADOS (Pasa el mouse para ver descripciones)"
                else "⚪ Tooltips de Ayuda DESACTIVADOS (Toca para habilitar explicaciones al pasar el mouse)",
                enabled = true
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = if (tooltipsEnabled) Color(0xFF064E3B) else Color(0xFF334155),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (tooltipsEnabled) Color(0xFF10B981) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onToggleTooltips(!tooltipsEnabled) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(if (tooltipsEnabled) "💡" else "⚪", fontSize = 12.sp)
                        Text(
                            text = if (tooltipsEnabled) "Ayuda ON" else "Ayuda OFF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tooltipsEnabled) Color(0xFF6EE7B7) else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            IconButton(onClick = onOpenHelp, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Acerca de y Ayuda",
                    tint = Color(0xFF38BDF8)
                )
            }
        }
    }
}
