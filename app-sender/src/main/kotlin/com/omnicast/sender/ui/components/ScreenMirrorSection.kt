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
    tvScaleMode: String = "EXTEND_16_9",
    onSelectTvScaleMode: (String) -> Unit = {},
    onStartProjection: () -> Unit,
    onStopProjection: () -> Unit
) {
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

            Text("Perfil de Transmisión (Resolución y FPS)", fontSize = 13.sp, color = Color.White)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileButton(
                    profile = StreamProfile.BALANCED_30FPS,
                    isSelected = selectedProfile.mode == QualityMode.BALANCED_30FPS || selectedProfile.mode == QualityMode.BALANCED,
                    onSelect = { onSelectProfile(StreamProfile.BALANCED_30FPS) },
                    modifier = Modifier.weight(1f)
                )
                ProfileButton(
                    profile = StreamProfile.FLUID_45FPS,
                    isSelected = selectedProfile.mode == QualityMode.FLUID_45FPS || selectedProfile.mode == QualityMode.GAMER_LOW_LATENCY,
                    onSelect = { onSelectProfile(StreamProfile.FLUID_45FPS) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileButton(
                    profile = StreamProfile.ULTRA_60FPS,
                    isSelected = selectedProfile.mode == QualityMode.ULTRA_60FPS,
                    onSelect = { onSelectProfile(StreamProfile.ULTRA_60FPS) },
                    modifier = Modifier.weight(1f)
                )
                ProfileButton(
                    profile = StreamProfile.MAX_60FPS,
                    isSelected = selectedProfile.mode == QualityMode.MAX_60FPS || selectedProfile.mode == QualityMode.CINEMA,
                    onSelect = { onSelectProfile(StreamProfile.MAX_60FPS) },
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Proporción en Pantalla de TV", fontSize = 13.sp, color = Color.White)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isExtend = tvScaleMode == "EXTEND_16_9"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isExtend) Color(0xFF0284C7) else Color(0xFF334155), RoundedCornerShape(8.dp))
                        .clickable { onSelectTvScaleMode("EXTEND_16_9") }
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📺 Llenar 16:9 (TV)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Sin bordes negros", color = Color(0xFFBAE6FD), fontSize = 10.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (!isExtend) Color(0xFF0284C7) else Color(0xFF334155), RoundedCornerShape(8.dp))
                        .clickable { onSelectTvScaleMode("ORIGINAL_FIT") }
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📱 Original (Fit)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Proporción móvil", color = Color(0xFFBAE6FD), fontSize = 10.sp)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "${selectedProfile.label} — ${selectedProfile.badge}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = selectedProfile.description.ifBlank { "Perfil configurado para la transmisión." },
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
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

@Composable
private fun ProfileButton(
    profile: StreamProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = if (isSelected) Color(0xFF2563EB) else Color(0xFF334155),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelect() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = profile.label,
                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = profile.badge,
                color = if (isSelected) Color(0xFF93C5FD) else Color(0xFF94A3B8),
                fontSize = 10.sp
            )
        }
    }
}
