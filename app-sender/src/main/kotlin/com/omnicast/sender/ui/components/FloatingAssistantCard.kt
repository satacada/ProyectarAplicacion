package com.omnicast.sender.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BubbleChart
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
 * Tarjeta verde esmeralda con iluminación neón para el Asistente Flotante sobre otras apps.
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
            color = Color(0xFF0C201C),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.2.dp,
                if (isServiceActive) Color(0xFF10B981) else Color(0xFF1B3D34)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = if (isServiceActive) Color(0xFF0F362C) else Color(0xFF142420),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                if (isServiceActive) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF26423A),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.BubbleChart, contentDescription = null, tint = if (isServiceActive) Color(0xFF34D399) else Color(0xFF64748B), modifier = Modifier.size(20.dp))
                    }

                    Column {
                        Text(
                            text = "Asistente Flotante",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = OmniThemeColors.TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (isServiceActive) Color(0xFF10B981) else Color(0xFF94A3B8),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Text(
                                text = if (isServiceActive) "Activo sobre otras apps" else "Inactivo — Toca para activar",
                                fontSize = 11.sp,
                                color = if (isServiceActive) Color(0xFF6EE7B7) else Color(0xFF94A3B8)
                            )
                        }
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
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)
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
                            checkedTrackColor = Color(0xFF10B981),
                            uncheckedThumbColor = OmniThemeColors.TextSecondary,
                            uncheckedTrackColor = Color(0xFF1B2824)
                        )
                    )
                }
            }
        }
    }
}
