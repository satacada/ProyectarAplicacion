package com.omnicast.sender.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Barra de encabezado superior con título, distintivo de versión v2.3 y botón de Ayuda.
 */
@Composable
fun SenderHeaderBar(onOpenHelp: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("OmniCast Pro", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Surface(
                    color = Color(0xFF0284C7),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "v3.21",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Text("Transmisión Universal sin Google Cast", fontSize = 12.sp, color = Color(0xFF94A3B8))
        }
        IconButton(onClick = onOpenHelp) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Ayuda y Versión",
                tint = Color(0xFF38BDF8)
            )
        }
    }
}
