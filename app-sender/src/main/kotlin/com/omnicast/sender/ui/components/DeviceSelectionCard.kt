package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import com.omnicast.sender.ui.theme.OmniThemeColors

/**
 * Tarjeta índigo Cyber-Dark para la configuración y enlace con la TV Receptora.
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
        text = "¿Quieres conocer esta opción?\nEnlaza el celular con tu Android TV Box en la misma red Wi-Fi.",
        enabled = tooltipsEnabled
    ) {
        Surface(
            color = Color(0xFF141C30),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.2.dp,
                if (isConnected) Color(0xFF10B981) else Color(0xFF2A3A5E)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fila Superior: Icono TV, Títulos y Badge de Estado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFF1C2744), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📺", fontSize = 20.sp)
                        }

                        Column {
                            Text(
                                text = "DISPOSITIVO PRINCIPAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF818CF8),
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "TV Receptora",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OmniThemeColors.TextPrimary
                            )
                        }
                    }

                    Surface(
                        color = if (isConnected) Color(0xFF09291D) else Color(0xFF0E1424),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isConnected) Color(0xFF10B981) else Color(0xFF28354E)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = if (isConnected) "● En Línea" else "○ Desconectado",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) Color(0xFF6EE7B7) else Color(0xFF94A3B8)
                        )
                    }
                }

                // Subtítulo: Estado LAN y etiqueta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Listo para vincular por red local",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "Wi-Fi LAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }

                // Fila de Entrada: IP, Puerto y Botón Conectar alineados armónicamente
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1.8f)) {
                        Text(
                            text = "Dirección IP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(Color(0xFF0D1424), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF24324E), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✛", color = Color(0xFF64748B), fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
                            BasicTextField(
                                value = manualIp,
                                onValueChange = onManualIpChange,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(
                                    color = OmniThemeColors.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = "Puerto",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(Color(0xFF0D1424), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF24324E), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = manualPort,
                                onValueChange = onManualPortChange,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(
                                    color = OmniThemeColors.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) Color(0xFF059669) else Color(0xFF4F46E5)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(40.dp)
                    ) {
                        Text(
                            text = if (isConnected) "✓ Listo" else "⚡ Conectar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                if (discoveredDevices.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Detectadas:", color = OmniThemeColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        discoveredDevices.forEach { dev ->
                            val isSel = selectedDevice?.id == dev.id
                            Surface(
                                color = if (isSel) Color(0xFF1E3A5F) else Color(0xFF0D1424),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) Color(0xFF38BDF8) else Color(0xFF24324E)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable {
                                    onSelectDevice(dev)
                                    onManualIpChange(dev.ipAddress)
                                    onManualPortChange(dev.port.toString())
                                }
                            ) {
                                Text(
                                    text = "${dev.name.take(15)} (${dev.ipAddress})",
                                    maxLines = 1,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (tooltipsEnabled) {
                    Text(
                        text = "💡 Enlaza el celular con tu Android TV Box en la misma red Wi-Fi para transmitir video, fotos y pantalla.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
