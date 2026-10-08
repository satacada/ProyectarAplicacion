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
import com.omnicast.sender.ui.theme.OmniThemeColors

/**
 * Cabecera con colores armónicos y botón de ayuda.
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onOpenDrawer != null) {
                IconButton(onClick = onOpenDrawer, modifier = Modifier.size(36.dp)) {
                    Text("☰", color = OmniThemeColors.CyanAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "OmniCast Pro",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OmniThemeColors.TextPrimary
            )
            Surface(
                color = OmniThemeColors.BlueAccent.copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(1.dp, OmniThemeColors.BlueAccent),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "v3.31",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OmniThemeColors.CyanAccent
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OmniTooltip(
                text = if (tooltipsEnabled) "💡 Modo Ayuda ON (Pasa el mouse para ver descripciones)"
                else "⚪ Modo Ayuda OFF (Toca para activar explicaciones)",
                enabled = true
            ) {
                Surface(
                    color = if (tooltipsEnabled) OmniThemeColors.AmberAccent.copy(alpha = 0.2f) else OmniThemeColors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (tooltipsEnabled) OmniThemeColors.AmberAccent else OmniThemeColors.CardBorder
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onToggleTooltips(!tooltipsEnabled) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(if (tooltipsEnabled) "💡" else "⚪", fontSize = 12.sp)
                        Text(
                            text = if (tooltipsEnabled) "Ayuda ON" else "Ayuda OFF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tooltipsEnabled) OmniThemeColors.AmberAccent else OmniThemeColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
