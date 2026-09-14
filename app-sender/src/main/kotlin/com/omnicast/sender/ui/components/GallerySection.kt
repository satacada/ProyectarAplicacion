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
fun GallerySection(onPickImage: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "🖼️ Modo 3: Proyectar Fotos de Galería",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8)
            )
            Text(
                "Transmite cualquier fotografía o imagen de tu dispositivo directamente a la TV Box en su resolución y nitidez original.",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
            Button(
                onClick = onPickImage,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("🖼️ Seleccionar y Proyectar Foto", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
