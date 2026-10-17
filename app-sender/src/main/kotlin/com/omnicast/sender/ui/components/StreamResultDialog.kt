package com.omnicast.sender.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.core.domain.models.StreamDetectionResult

/**
 * Diálogo interactivo que responde de inmediato al usuario si el flujo directo
 * pudo ser capturado o si se debe proceder a la proyección de pantalla completa a demanda.
 */
@Composable
fun StreamResultDialog(
    result: StreamDetectionResult,
    onSendDirectStream: (String) -> Unit,
    onStartScreenMirroring: () -> Unit,
    onDismiss: () -> Unit
) {
    val detectedUrl = result.mediaUrl

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (result.isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = if (result.isSuccess) Color(0xFF10B981) else Color(0xFFF59E0B),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (result.isSuccess) "Flujo Directo Capturado" else "Información de la Transmisión",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (result.isSuccess) Color(0xFF10B981) else Color(0xFFF59E0B)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = result.userMessage,
                    color = Color(0xFFCBD5E1),
                    fontSize = 14.sp
                )
                if (detectedUrl != null) {
                    Text(
                        text = "Fuente: $detectedUrl",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            if (result.isSuccess && detectedUrl != null) {
                Button(
                    onClick = {
                        onSendDirectStream(detectedUrl)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Enviar Directo a TV", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = {
                        onDismiss()
                        onStartScreenMirroring()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Proyectar Pantalla Completa", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cerrar", color = Color(0xFF94A3B8))
            }
        }
    )
}
