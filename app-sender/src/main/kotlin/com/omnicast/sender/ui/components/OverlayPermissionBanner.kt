package com.omnicast.sender.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
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
 * Componente reactivo que gestiona y controla el Asistente Flotante Permanente
 * sobre cualquier aplicación (Facebook, Deportes, YouTube, etc.).
 */
@Composable
fun OverlayPermissionBanner() {
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
                if (granted) {
                    FloatingIndicatorService.start(context)
                    isServiceActive = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            FloatingIndicatorService.start(context)
            isServiceActive = true
        }
    }

    if (!hasPermission) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF854D0E)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFEF08A), modifier = Modifier.size(16.dp))
                        Text(
                            "Activar Asistente Flotante Permanente",
                            color = Color(0xFFFEF08A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        "Permite controlar, detectar video o proyectar sobre Facebook, apps de fútbol y juegos.",
                        color = Color(0xFFFDE047),
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEAB308), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Conceder", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFFA7F3D0), modifier = Modifier.size(16.dp))
                        Text(
                            "Asistente Flotante Activo",
                            color = Color(0xFFA7F3D0),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        "Burbuja visible sobre tus apps. En Ajustes de tu teléfono: activa 'Aparecer encima' y 'Sin restricciones de batería' para que permanezca activa en segundo plano en cualquier marca (Samsung, Motorola, Huawei, Xiaomi, etc.).",
                        color = Color(0xFFD1FAE5),
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = {
                        if (isServiceActive) {
                            FloatingIndicatorService.stop(context)
                            isServiceActive = false
                        } else {
                            FloatingIndicatorService.start(context)
                            isServiceActive = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServiceActive) Color(0xFF047857) else Color(0xFF2563EB)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (isServiceActive) "Visible" else "Mostrar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
