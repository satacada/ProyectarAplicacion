package com.omnicast.sender.ui.browser

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Diálogo de ayuda con el detalle de los controles del navegador.
 */
@Composable
fun BrowserHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                Text("Detalle de Controles", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HelpItem("Flujos", "Muestra la lista de videos capturados para emitir a la TV.")
                HelpItem("Control", "Muestra u oculta los controles de reproducción de la TV.")
                HelpItem("Recargar", "Actualiza la página actual dentro del buscador.")
                HelpItem("Pausa / Reproducir", "Pausa o reanuda la reproducción en el televisor.")
                HelpItem("Retroceder / Adelantar", "Salta 10 segundos en la película.")
                HelpItem("Volumen", "Regula el volumen de sonido en el televisor.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Entendido", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
private fun HelpItem(title: String, desc: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(desc, color = Color(0xFFCBD5E1), fontSize = 11.sp)
    }
}
