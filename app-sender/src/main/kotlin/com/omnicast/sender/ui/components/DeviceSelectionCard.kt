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
import com.omnicast.core.domain.models.DeviceTarget

/**
 * Tarjeta de selección de receptor TV Box (vía mDNS o IP directa).
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
        text = "¿Quieres conocer esta opción?\n" +
                "Conecta tu teléfono a la TV Box en la misma red Wi-Fi local para transmitir pantalla, videos o fotos.",
        enabled = tooltipsEnabled
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📡 TV Box Receptora", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = if (isConnected) "🟢 En Línea" else "⚪ Desconectado",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isConnected) Color(0xFF10B981) else Color(0xFF94A3B8)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = manualIp,
                    onValueChange = onManualIpChange,
                    label = { Text("IP de TV Box") },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = manualPort,
                    onValueChange = onManualPortChange,
                    label = { Text("Puerto") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) Color(0xFF059669) else Color(0xFF2563EB)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Text(
                    if (isConnected) "✓ Conectado a TV ($manualIp)" else "⚡ Conectar con TV",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            if (discoveredDevices.isNotEmpty()) {
                Text("Dispositivos descubiertos en red local:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                discoveredDevices.forEach { device ->
                    val isSelected = selectedDevice?.id == device.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF0F172A),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) Color(0xFF38BDF8) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onSelectDevice(device)
                                onManualIpChange(device.ipAddress)
                                onManualPortChange(device.port.toString())
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(device.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("${device.ipAddress}:${device.port}", color = Color.LightGray, fontSize = 12.sp)
                            }
                            if (isSelected) {
                                Text("✓ Seleccionado", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
}
