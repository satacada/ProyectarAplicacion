package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PowerSettingsNew
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
 * Cabecera con menú lateral, título con badge de versión ámbar, ayuda circular '?' y botón discreto de salida limpia.
 */
@Composable
fun SenderHeaderBar(
    tooltipsEnabled: Boolean = true,
    onToggleTooltips: (Boolean) -> Unit = {},
    onOpenHelp: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    onExitApp: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (onOpenDrawer != null) {
                Box(
                    modifier = Modifier.size(38.dp).background(Color(0xFF19202E), RoundedCornerShape(10.dp)).border(1.dp, Color(0xFF2E3A4E), RoundedCornerShape(10.dp)).clickable { onOpenDrawer() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Menu, contentDescription = "Menú", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp)) }
            }

            Text("OmniCast Pro", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = OmniThemeColors.TextPrimary)

            val context = androidx.compose.ui.platform.LocalContext.current
            val ver = androidx.compose.runtime.remember {
                try { "v" + context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (_: Throwable) { "v3.32.25" }
            }
            Surface(color = Color(0xFF382910), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706)), shape = RoundedCornerShape(12.dp)) {
                Text(text = ver, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFBBF24))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OmniTooltip(text = if (tooltipsEnabled) "Ayuda Activa: Descripciones y consejos." else "Ayuda Inactiva: Toca '?' para ver consejos.", enabled = true) {
                Box(
                    modifier = Modifier.size(38.dp).background(if (tooltipsEnabled) Color(0xFF09291D) else Color(0xFF19202E), CircleShape).border(1.2.dp, if (tooltipsEnabled) Color(0xFF10B981) else Color(0xFF334155), CircleShape).clickable { onToggleTooltips(!tooltipsEnabled) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = "Ayuda", tint = if (tooltipsEnabled) Color(0xFF34D399) else Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                }
            }

            if (onExitApp != null) {
                Box(
                    modifier = Modifier.size(38.dp).background(Color(0xFF2B1417), CircleShape).border(1.2.dp, Color(0xFFEF4444).copy(alpha = 0.6f), CircleShape).clickable { onExitApp() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Rounded.PowerSettingsNew, contentDescription = "Salir", tint = Color(0xFFF87171), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
