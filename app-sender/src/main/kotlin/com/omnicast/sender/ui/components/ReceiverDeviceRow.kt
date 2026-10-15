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
import com.omnicast.core.domain.models.DeviceTarget
import com.omnicast.sender.ui.theme.OmniThemeColors

/**
 * Fila compacta e interactiva para cada receptor Android TV descubierto en la red local.
 */
@Composable
fun ReceiverDeviceRow(
    device: DeviceTarget,
    isPrimary: Boolean,
    isChecked: Boolean,
    onToggleCheck: () -> Unit,
    onSetPrimary: () -> Unit
) {
    Surface(
        color = if (isPrimary) Color(0xFF1E3A5F) else if (isChecked) Color(0xFF09291D) else Color(0xFF0D1424),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPrimary) Color(0xFF38BDF8) else if (isChecked) Color(0xFF10B981) else Color(0xFF24324E)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!isPrimary) onToggleCheck() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        if (isPrimary) Color(0xFF38BDF8) else if (isChecked) Color(0xFF10B981) else Color(0xFF1E293B),
                        RoundedCornerShape(4.dp)
                    )
                    .border(
                        1.dp,
                        if (isPrimary) Color(0xFF38BDF8) else if (isChecked) Color(0xFF10B981) else Color(0xFF475569),
                        RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isChecked) Text("✓", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }

            Text("📺", fontSize = 13.sp)

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = device.name.take(18),
                        fontSize = 10.sp,
                        fontWeight = if (isPrimary || isChecked) FontWeight.Bold else FontWeight.Medium,
                        color = if (isPrimary) Color(0xFF38BDF8) else if (isChecked) Color(0xFF6EE7B7) else OmniThemeColors.TextPrimary,
                        maxLines = 1
                    )
                    if (isPrimary) {
                        Surface(color = Color(0xFF38BDF8).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("Principal", color = Color(0xFF38BDF8), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    } else if (isChecked) {
                        Surface(color = Color(0xFF10B981).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("Secundaria", color = Color(0xFF6EE7B7), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }
                Text("${device.ipAddress}:${device.port}", fontSize = 9.sp, color = Color(0xFF94A3B8))
            }

            if (!isPrimary) {
                Text(
                    text = "Fijar Principal",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF818CF8),
                    modifier = Modifier
                        .background(Color(0xFF222F4C), RoundedCornerShape(4.dp))
                        .clickable { onSetPrimary() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}
