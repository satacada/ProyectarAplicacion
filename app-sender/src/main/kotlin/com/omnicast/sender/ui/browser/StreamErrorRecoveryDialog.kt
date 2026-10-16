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
    // Diálogo eliminado a solicitud del usuario: no mostrar ventana de servidor protegido 403
}
