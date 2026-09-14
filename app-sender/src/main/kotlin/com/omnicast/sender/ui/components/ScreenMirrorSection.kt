package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.core.domain.models.QualityMode
import com.omnicast.core.domain.models.StreamProfile

/**
 * Sección para la proyección de pantalla completa universal a demanda.
 */
@Composable
fun ScreenMirrorSection(
    isStreaming: Boolean,
    selectedProfile: StreamProfile,
    onSelectProfile: (StreamProfile) -> Unit,
    onStartProjection: () -> Unit,
    onStopProjection: () -> Unit
) {
    val profiles = listOf(
        StreamProfile.GAMER_720P,
        StreamProfile.BALANCED_1080P,
        StreamProfile.CINEMA_HIGH_BITRATE
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "📺 Modo 2: Pantalla Completa a Demanda",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8)
            )

            Text(
                "Captura por hardware de la GPU a 60 FPS con ultra-baja latencia (<80ms). Universal para cualquier app de fútbol o juegos.",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )

            Text("Perfil de Transmisión", fontSize = 13.sp, color = Color.White)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                profiles.forEach { profile ->
                    val isSelected = profile == selectedProfile
                    val title = when (profile.mode) {
                        QualityMode.GAMER_LOW_LATENCY -> "720p Gamer"
                        QualityMode.BALANCED -> "1080p Balance"
                        QualityMode.CINEMA -> "1080p Cine"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSelected) Color(0xFF2563EB) else Color(0xFF334155),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectProfile(profile) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    "⚽ Consejo Deportivo: Abre tu app, pon el partido en pantalla completa y usa el botón flotante para transmitir limpio sin menús ni anuncios.",
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8)
                )
            }

            if (!isStreaming) {
                Button(
                    onClick = onStartProjection,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("📺 Proyectar en Pantalla", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onStopProjection,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("🛑 Detener Transmisión", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
