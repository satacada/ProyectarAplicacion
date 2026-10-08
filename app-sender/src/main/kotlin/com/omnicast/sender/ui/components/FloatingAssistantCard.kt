package com.omnicast.sender.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
 * Píldora ultra-compacta para activar/desactivar el Asistente Flotante sin ocupar espacio vertical.
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
        text = "¿Quieres conocer esta opción?\nActiva o desactiva la burbuja flotante para proyectar sobre otras aplicaciones.",
        enabled = tooltipsEnabled
    ) {
        Surface(
            color = if (isServiceActive) Color(0xFF064E3B) else Color(0xFF334155),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.clickable {
                if (!hasPermission) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                } else {
                    if (isServiceActive) {
                        FloatingIndicatorService.stop(context)
                        isServiceActive = false
                    } else {
                        FloatingIndicatorService.start(context)
                        isServiceActive = true
                    }
                }
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(if (isServiceActive) "🫧" else "⚪", fontSize = 12.sp)
                Text(
                    text = if (isServiceActive) "Asistente ON" else "Asistente OFF",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isServiceActive) Color(0xFF34D399) else Color(0xFFCBD5E1)
                )
            }
        }
    }
}
