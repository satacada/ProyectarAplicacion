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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Cabecera ultra-compacta y animada con colores modernos para tema oscuro.
 */
@Composable
fun SenderHeaderBar(
    tooltipsEnabled: Boolean = true,
    onToggleTooltips: (Boolean) -> Unit = {},
    onOpenHelp: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onOpenDrawer != null) {
                IconButton(onClick = onOpenDrawer, modifier = Modifier.size(32.dp)) {
                    Text("☰", color = Color(0xFF38BDF8), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "OmniCast",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFF8FAFC)
            )
            Surface(
                color = Color(0xFF0284C7),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "v3.31",
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FloatingAssistantCard(tooltipsEnabled = tooltipsEnabled)

            OmniTooltip(
                text = if (tooltipsEnabled) "💡 Tooltips de ayuda activos (pasa el cursor)" else "⚪ Tooltips desactivados",
                enabled = true
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            color = if (tooltipsEnabled) Color(0xFF854D0E) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (tooltipsEnabled) Color(0xFFEAB308) else Color(0xFF334155),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onToggleTooltips(!tooltipsEnabled) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (tooltipsEnabled) "💡" else "⚪", fontSize = 12.sp)
                }
            }
        }
    }
}
