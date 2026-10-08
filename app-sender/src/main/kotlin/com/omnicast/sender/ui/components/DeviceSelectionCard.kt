package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.core.domain.models.DeviceTarget

/**
 * Barra ultra-compacta y estilizada de conexión a TV Receptora.
 */
@Composable
fun DeviceSelectionCard(
    discoveredDevices: List<DeviceTarget>,
    selectedDevice: DeviceTarget?,
    manualIp: String,
    manualPort: String,
    isConnected: Boolean = false,
    tooltipsEnabled: Boolean = true,
    onManualIpChange: (String) -> Unit,
    onManualPortChange: (String) -> Unit,
    onSelectDevice: (DeviceTarget) -> Unit,
    onConnect: () -> Unit = {}
) {
    OmniTooltip(
        text = "¿Quieres conocer esta opción?\nConexión directa por Wi-Fi a tu Android TV Box.",
        enabled = tooltipsEnabled
    ) {
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isConnected) Color(0xFF10B981) else Color(0xFF334155)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "📡 TV:",
                    color = if (isConnected) Color(0xFF34D399) else Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                BasicTextField(
                    value = manualIp,
                    onValueChange = onManualIpChange,
                    singleLine = true,
                    modifier = Modifier
                        .weight(2f)
                        .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                )

                Text(":", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 12.sp)

                BasicTextField(
                    value = manualPort,
                    onValueChange = onManualPortChange,
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.9f)
                        .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                )

                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) Color(0xFF059669) else Color(0xFF2563EB)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = if (isConnected) "🟢 Enlazado" else "⚡ Conectar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
