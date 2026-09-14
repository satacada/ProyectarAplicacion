package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Diálogo de Ayuda e Información de Versión v2.3
 */
@Composable
fun SenderHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("OmniCast Pro", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                Surface(
                    color = Color(0xFF0284C7),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "v2.3.0",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HelpSectionItem(
                    title = "🌐 1. Navegador Web (Estilo Web Caster)",
                    description = "Toca en 'Abrir Navegador Web'. Visita cualquier página de videos (Dailymotion, etc.). El interceptor capturará flujos HLS (.m3u8) y MP4 directamente sin anuncios."
                )
                HelpSectionItem(
                    title = "📺 2. Reproducir y Cambiar Videos",
                    description = "Pulsa el botón flotante amarillo con los videos encontrados y toca 'Reproducir en TV'. Para cambiar de video, navega a otro y tócalo de nuevo: la TV cambiará al instante sin congelarse."
                )
                HelpSectionItem(
                    title = "📱 3. Proyección de Pantalla y Apps",
                    description = "Para juegos o apps protegidas, usa 'Iniciar Proyección'. Se capturará la pantalla y audio interno a 60fps con latencia ultra-baja (<80ms)."
                )
                HelpSectionItem(
                    title = "🛡️ 4. Bloqueador de Anuncios Activo",
                    description = "El navegador bloquea popups y scripts de publicidad invasivos automáticamente."
                )
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
private fun HelpSectionItem(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF38BDF8))
        Text(description, fontSize = 12.sp, color = Color(0xFFCBD5E1), lineHeight = 16.sp)
    }
}
