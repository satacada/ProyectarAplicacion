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

/**
 * Tarjeta moderna y minimalista del Asistente Flotante.
 * Elimina la saturación de texto y expone un switch directo y tooltip reactivo.
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
                "El Asistente Flotante es una burbuja que permanece sobre otras apps " +
                "(Facebook, Chrome, fútbol) para proyectar pantalla o capturar streams " +
                "sin necesidad de volver a abrir OmniCast.",
        enabled = tooltipsEnabled
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isServiceActive) Color(0xFF064E3B) else Color(0xFF334155),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isServiceActive) "🫧" else "⚪",
                            fontSize = 18.sp
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Asistente Flotante",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = when {
                                !hasPermission -> "⚠️ Requiere permiso de superposición"
                                isServiceActive -> "🟢 Activo sobre tus aplicaciones"
                                else -> "⚪ Inactivo — Toca para activar"
                            },
                            fontSize = 12.sp,
                            color = when {
                                !hasPermission -> Color(0xFFFBBF24)
                                isServiceActive -> Color(0xFF34D399)
                                else -> Color(0xFF94A3B8)
                            }
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Permitir", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                            checkedTrackColor = Color(0xFF059669),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF475569)
                        )
                    )
                }
            }
        }
    }
}
