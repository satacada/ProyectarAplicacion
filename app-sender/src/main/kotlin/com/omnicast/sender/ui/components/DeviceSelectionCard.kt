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
import com.omnicast.sender.ui.theme.OmniThemeColors

/**
 * Tarjeta armónica para la configuración y enlace con la TV Receptora.
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
            color = OmniThemeColors.CardSurface,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.2.dp,
                if (isConnected) OmniThemeColors.EmeraldAccent else OmniThemeColors.CardBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📺", fontSize = 20.sp)
                        Text(
                            text = "TV Receptora",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OmniThemeColors.TextPrimary
                        )
                    }

                    Surface(
                        color = if (isConnected) Color(0xFF064E3B) else Color(0xFF1E2430),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isConnected) Color(0xFF10B981) else Color(0xFF434E66)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = if (isConnected) "● En Línea" else "○ Desconectado",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) Color(0xFF6EE7B7) else Color(0xFF94A3B8)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(2f)) {
                        Text(
                            text = "Dirección IP de la TV",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniThemeColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BasicTextField(
                            value = manualIp,
                            onValueChange = onManualIpChange,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(OmniThemeColors.InputBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, OmniThemeColors.CardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            textStyle = TextStyle(
                                color = OmniThemeColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Puerto",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniThemeColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BasicTextField(
                            value = manualPort,
                            onValueChange = onManualPortChange,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(OmniThemeColors.InputBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, OmniThemeColors.CardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            textStyle = TextStyle(
                                color = OmniThemeColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) OmniThemeColors.EmeraldAccent else OmniThemeColors.BlueAccent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(top = 18.dp)
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Detectadas:", color = OmniThemeColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        discoveredDevices.take(2).forEach { dev ->
                            val isSel = selectedDevice?.id == dev.id
                            Surface(
                                color = if (isSel) Color(0xFF1E3A5F) else Color(0xFF1B202B),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) Color(0xFF38BDF8) else Color(0xFF434E66)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable {
                                    onSelectDevice(dev)
                                    onManualIpChange(dev.ipAddress)
                                    onManualPortChange(dev.port.toString())
                                }
                            ) {
                                Text(
                                    text = "${dev.name} (${dev.ipAddress})",
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
                        color = OmniThemeColors.TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
