package com.omnicast.sender.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.omnicast.sender.overlay.FloatingIndicatorService
import com.omnicast.sender.ui.theme.OmniThemeColors

/**
 * Tarjeta armónica para activar o desactivar el Asistente Flotante con superposición.
 */
@Composable
fun FloatingAssistantCard(
    tooltipsEnabled: Boolean = true
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
        )
    }
    var isServiceActive by remember { mutableStateOf(FloatingIndicatorService.isRunning) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else true
                hasPermission = granted
                if (granted && !isServiceActive) {
                    FloatingIndicatorService.start(context)
                    isServiceActive = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    OmniTooltip(
        text = "¿Quieres conocer esta opción?\n" +
                "Activa una burbuja flotante sobre cualquier app para transmitir o controlar la TV sin abrir OmniCast.",
        enabled = tooltipsEnabled
    ) {
        Surface(
            color = OmniThemeColors.CardSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isServiceActive) OmniThemeColors.EmeraldAccent.copy(alpha = 0.6f) else OmniThemeColors.CardBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                color = if (isServiceActive) OmniThemeColors.EmeraldAccent.copy(alpha = 0.2f)
                                else OmniThemeColors.InputBackground,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isServiceActive) "🫧" else "⚪", fontSize = 16.sp)
                    }

                    Column {
                        Text(
                            text = "Asistente Flotante",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = OmniThemeColors.TextPrimary
                        )
                        Text(
                            text = if (isServiceActive) "🟢 Activo sobre otras apps" else "⚪ Inactivo — Toca para activar",
                            fontSize = 11.sp,
                            color = if (isServiceActive) OmniThemeColors.EmeraldAccent else OmniThemeColors.TextSecondary
                        )
                    }
                }

                if (!hasPermission) {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OmniThemeColors.AmberAccent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Permitir", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                } else {
                    Switch(
                        checked = isServiceActive,
                        onCheckedChange = { enable ->
                            if (enable) {
                                FloatingIndicatorService.start(context)
                                isServiceActive = true
                            } else {
                                FloatingIndicatorService.stop(context)
                                isServiceActive = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = OmniThemeColors.EmeraldAccent,
                            uncheckedThumbColor = OmniThemeColors.TextSecondary,
                            uncheckedTrackColor = OmniThemeColors.InputBackground
                        )
                    )
                }
            }
        }
    }
}
