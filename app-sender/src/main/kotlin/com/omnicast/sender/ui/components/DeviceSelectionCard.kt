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
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isConnected) OmniThemeColors.EmeraldAccent.copy(alpha = 0.6f) else OmniThemeColors.CardBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📡 TV Receptora",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OmniThemeColors.TextPrimary
                    )
                    Text(
                        text = if (isConnected) "🟢 En Línea" else "⚪ Desconectado",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isConnected) OmniThemeColors.EmeraldAccent else OmniThemeColors.TextSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(2f)) {
                        Text("IP de la TV", fontSize = 10.sp, color = OmniThemeColors.TextMuted)
                        BasicTextField(
                            value = manualIp,
                            onValueChange = onManualIpChange,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(OmniThemeColors.InputBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, OmniThemeColors.CardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            textStyle = TextStyle(
                                color = OmniThemeColors.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Puerto", fontSize = 10.sp, color = OmniThemeColors.TextMuted)
                        BasicTextField(
                            value = manualPort,
                            onValueChange = onManualPortChange,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(OmniThemeColors.InputBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, OmniThemeColors.CardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            textStyle = TextStyle(
                                color = OmniThemeColors.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) OmniThemeColors.EmeraldAccent else OmniThemeColors.BlueAccent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(top = 14.dp)
                            .height(36.dp)
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
                        Text("Detectadas:", color = OmniThemeColors.TextMuted, fontSize = 10.sp)
                        discoveredDevices.take(2).forEach { dev ->
                            val isSel = selectedDevice?.id == dev.id
                            Surface(
                                color = if (isSel) OmniThemeColors.BlueAccent.copy(alpha = 0.3f) else OmniThemeColors.InputBackground,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) OmniThemeColors.CyanAccent else OmniThemeColors.CardBorder
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
                                    color = if (isSel) OmniThemeColors.CyanAccent else OmniThemeColors.TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
