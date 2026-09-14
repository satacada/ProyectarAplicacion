package com.omnicast.sender.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.core.domain.models.StreamDetectionResult

/**
 * Sección interactiva para la captura y envío de flujos de video directos.
 */
@Composable
fun DirectStreamSection(
    onAnalyzeStream: (String?) -> Unit
) {
    var manualStreamUrl by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "⚡ Modo 1: Flujo Directo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
                Text(
                    "Sin consumo de batería",
                    fontSize = 12.sp,
                    color = Color(0xFF10B981)
                )
            }

            Text(
                "Verifica si tu app deportiva o enlace expone el stream para reproducirlo limpio en la TV Box con ExoPlayer nativo.",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )

            Button(
                onClick = { onAnalyzeStream(null) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔍 Verificar y Capturar Flujo de Video", fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = manualStreamUrl,
                onValueChange = { manualStreamUrl = it },
                label = { Text("O pega una URL directa (.m3u8, .mp4, web)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            if (manualStreamUrl.isNotBlank()) {
                Button(
                    onClick = { onAnalyzeStream(manualStreamUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⚡ Enviar URL Directa a TV Box")
                }
            }
        }
    }
}
