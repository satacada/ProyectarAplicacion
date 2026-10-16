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
import com.omnicast.sender.stream.ActiveStreamHolder

/**
 * Diálogo de contingencia inteligente ante bloqueos HTTP 403 de servidores de streaming.
 * Ofrece al usuario conmutar a Proyección de Pantalla con 1 solo toque.
 */
@Composable
fun StreamErrorRecoveryDialog(
    onStartMirroring: (() -> Unit)? = null
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
                    text = "El servidor web de este video bloquea la transmisión directa a Smart TVs.\n\nComo la película sí se reproduce en tu celular, puedes proyectar la pantalla para verla en la TV con audio sincronizado.",
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        ActiveStreamHolder.clearPlaybackError()
                        onStartMirroring?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Text("📺 Proyectar Pantalla", color = Color(0xFF0F172A))
                }
            },
            dismissButton = {
                TextButton(onClick = { ActiveStreamHolder.clearPlaybackError() }) {
                    Text("Entendido", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
