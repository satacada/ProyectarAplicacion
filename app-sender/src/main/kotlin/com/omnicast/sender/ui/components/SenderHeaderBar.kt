package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.sender.ui.theme.OmniThemeColors

/**
 * Cabecera con foco de ayuda visual por color (Verde = ON, Plomo = OFF, sin texto) y título.
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
                IconButton(onClick = onOpenDrawer, modifier = Modifier.size(34.dp)) {
                    Text("☰", color = OmniThemeColors.CyanAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "OmniCast Pro",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OmniThemeColors.TextPrimary
            )
            Surface(
                color = OmniThemeColors.BlueAccent.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, OmniThemeColors.BlueAccent.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "v3.31",
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = OmniThemeColors.CyanAccent
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Foco de Ayuda visual por color (Verde = ON, Plomo = OFF, sin palabras ON/OFF)
            OmniTooltip(
                text = if (tooltipsEnabled) "Foco Activo: Descripciones detalladas habilitadas."
                else "Foco Inactivo: Toca para ver descripciones y consejos detallados.",
                enabled = true
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (tooltipsEnabled) Color(0xFF064E3B) else Color(0xFF334155),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (tooltipsEnabled) Color(0xFF10B981) else Color(0xFF475569),
                            shape = CircleShape
                        )
                        .clickable { onToggleTooltips(!tooltipsEnabled) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💡",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
        }
    }
}
