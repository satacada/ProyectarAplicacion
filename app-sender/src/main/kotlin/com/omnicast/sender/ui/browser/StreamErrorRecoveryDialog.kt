package com.omnicast.sender.ui.browser

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.omnicast.sender.stream.ActiveStreamHolder

/**
 * Diálogo de contingencia inteligente ante bloqueos HTTP 403 de servidores de streaming.
 * Ofrece al usuario conmutar a Proyección de Pantalla con 1 solo toque.
 */
@Composable
fun StreamErrorRecoveryDialog(
    onStartMirroring: (() -> Unit)? = null,
    onRenewInBrowser: (() -> Unit)? = null
) {
    val errorState by ActiveStreamHolder.playbackError.collectAsState()

    if (errorState == "ERROR_FORBIDDEN_403") {
        AlertDialog(
            onDismissRequest = { ActiveStreamHolder.clearPlaybackError() },
            title = {
                Text(
                    text = "⚠️ Servidor Protegido (HTTP 403)",
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "El servidor web bloqueó la conexión directa o el token temporal expiró.\n\nPuedes abrir la página en el navegador para renovar el flujo y validación de Cloudflare automáticamente, o proyectar la pantalla de tu celular.",
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    if (onRenewInBrowser != null) {
                        Button(
                            onClick = {
                                ActiveStreamHolder.clearPlaybackError()
                                onRenewInBrowser()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A00))
                        ) {
                            Text("🔄 Renovar", color = Color.White)
                        }
                    }
                    Button(
                        onClick = {
                            ActiveStreamHolder.clearPlaybackError()
                            onStartMirroring?.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                    ) {
                        Text("📺 Proyectar", color = Color(0xFF0F172A))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { ActiveStreamHolder.clearPlaybackError() }) {
                    Text("Cerrar", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
