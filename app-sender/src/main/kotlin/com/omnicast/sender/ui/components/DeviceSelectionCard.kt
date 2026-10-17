package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Tv
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
    secondaryIps: Set<String> = emptySet(),
    onToggleSecondaryIp: (String) -> Unit = {},
    onManualIpChange: (String) -> Unit,
    onManualPortChange: (String) -> Unit,
    onSelectDevice: (DeviceTarget) -> Unit,
    onClearIp: () -> Unit = {},
    onConnect: () -> Unit = {}
) {
    OmniTooltip(
        text = "¿Quieres conocer esta opción?\nEnlaza el celular con tu Android TV Box en la misma red Wi-Fi.",
        enabled = tooltipsEnabled
    ) {
        Surface(
            color = Color(0xFF141C30),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, if (isConnected) Color(0xFF10B981) else Color(0xFF2A3A5E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Fila Superior: Icono TV, Títulos y Badge de Estado
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.size(42.dp).background(Color(0xFF1C2744), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Tv, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp)) }

                        Column {
                            Text(text = "DISPOSITIVO PRINCIPAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF818CF8), letterSpacing = 0.8.sp)
                            Text(text = "TV Receptora", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = OmniThemeColors.TextPrimary)
                        }
                    }

                    Surface(
                        color = if (isConnected) Color(0xFF09291D) else Color(0xFF0E1424),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isConnected) Color(0xFF10B981) else Color(0xFF28354E)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(if (isConnected) Color(0xFF10B981) else Color(0xFF94A3B8), CircleShape))
                            Text(if (isConnected) "En Línea" else "Desconectado", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isConnected) Color(0xFF6EE7B7) else Color(0xFF94A3B8))
                        }
                    }
                }

                // Subtítulo: Estado LAN y etiqueta
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Listo para vincular por red local", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Text(text = "Wi-Fi LAN", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                }

                val activeIp = manualIp.trim().ifBlank { selectedDevice?.ipAddress ?: "" }
                val connectedDev = discoveredDevices.firstOrNull { it.ipAddress == activeIp }
                val otherDevices = discoveredDevices
                    .distinctBy { it.ipAddress }
                    .filter { if (activeIp.isNotBlank()) it.ipAddress != activeIp else true }

                // Fila de Entrada: IP, Puerto y Botón Conectar con resaltado de enlazado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1.8f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dirección IP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isConnected) Color(0xFF6EE7B7) else Color(0xFF94A3B8)
                            )
                            if (connectedDev != null) {
                                Text(
                                    text = connectedDev.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConnected) Color(0xFF10B981) else Color(0xFF38BDF8),
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(if (isConnected) Color(0xFF0F231D) else Color(0xFF0D1424), RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isConnected) 1.5.dp else 1.dp,
                                    color = if (isConnected) Color(0xFF10B981) else Color(0xFF24324E),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(6.dp)
                                    .background(if (isConnected) Color(0xFF10B981) else Color(0xFF64748B), CircleShape)
                            )
                            BasicTextField(
                                value = manualIp,
                                onValueChange = onManualIpChange,
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(
                                    color = if (isConnected) Color(0xFF6EE7B7) else OmniThemeColors.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (manualIp.isNotBlank()) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Borrar",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp).clickable { onClearIp() }.padding(horizontal = 2.dp)
                                )
                            }
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
                                .border(
                                    width = if (isConnected) 1.5.dp else 1.dp,
                                    color = if (isConnected) Color(0xFF10B981) else Color(0xFF24324E),
                                    shape = RoundedCornerShape(8.dp)
                                )
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
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(if (isConnected) Icons.Rounded.Check else Icons.Rounded.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(
                                text = if (isConnected) "Listo" else "Conectar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Lista de receptores adicionales sin duplicar el dispositivo enlazado
                if (otherDevices.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (activeIp.isNotBlank()) "Otros Receptores Detectados:" else "Dispositivos Receptores Detectados:",
                                color = OmniThemeColors.TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (activeIp.isNotBlank()) "Marca para transmitir también" else "Toca para conectar",
                                color = Color(0xFF64748B),
                                fontSize = 9.sp
                            )
                        }

                        otherDevices.forEach { dev ->
                            val isPrimary = activeIp.isBlank() && dev == otherDevices.first()
                            ReceiverDeviceRow(
                                device = dev,
                                isPrimary = isPrimary,
                                isChecked = isPrimary || secondaryIps.contains(dev.ipAddress),
                                onToggleCheck = { onToggleSecondaryIp(dev.ipAddress) },
                                onSetPrimary = {
                                    onSelectDevice(dev)
                                    onManualIpChange(dev.ipAddress)
                                    onManualPortChange(dev.port.toString())
                                }
                            )
                        }
                    }
                }

                if (tooltipsEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                        Text(
                            text = "Enlaza el celular con tu Android TV Box en la misma red Wi-Fi para transmitir video, fotos y pantalla.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
