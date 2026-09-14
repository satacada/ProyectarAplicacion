package com.omnicast.sender.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InAppBrowserCard(onOpenBrowser: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🌐 Navegador Web con Extractor (Estilo Web Video Caster)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
            Text("Navega por sitios de fútbol o videos (Win, Fútbol Libre, Web) e intercepta el flujo HLS (.m3u8) o MP4 directo a la TV Box sin gastar batería.", fontSize = 13.sp, color = Color.LightGray)
            Button(
                onClick = onOpenBrowser,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("🚀 Abrir Navegador y Detectar Videos", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
