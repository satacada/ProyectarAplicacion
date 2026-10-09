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
 * Cabecera con menú lateral, título con badge de versión ámbar e icono de ayuda circular '?'.
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (onOpenDrawer != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF19202E), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF2E3A4E), RoundedCornerShape(10.dp))
                        .clickable { onOpenDrawer() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("☰", color = Color(0xFF94A3B8), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "OmniCast Pro",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OmniThemeColors.TextPrimary
            )

            Surface(
                color = Color(0xFF382910),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "v3.32.6",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFBBF24)
                )
            }
        }

        OmniTooltip(
            text = if (tooltipsEnabled) "Ayuda Activa: Descripciones y consejos habilitados."
            else "Ayuda Inactiva: Toca '?' para ver consejos explicativos.",
            enabled = true
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = if (tooltipsEnabled) Color(0xFF09291D) else Color(0xFF19202E),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.2.dp,
                        color = if (tooltipsEnabled) Color(0xFF10B981) else Color(0xFF334155),
                        shape = CircleShape
                    )
                    .clickable { onToggleTooltips(!tooltipsEnabled) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (tooltipsEnabled) Color(0xFF34D399) else Color(0xFF94A3B8)
                )
            }
        }
    }
}
