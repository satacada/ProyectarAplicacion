package com.omnicast.sender.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hoja modal de videos detectados inspirada en video_list_layout y video_list_item de Web Video Caster.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WvcVideoListSheet(
    pageTitle: String,
    options: List<StreamQualityOption>,
    onSelectAndTransmit: (StreamQualityOption) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            // Header estilo WVC
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFF59E0B), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎬", fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Videos Detectados (${options.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        pageTitle.ifBlank { "Transmisión Web" },
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onClear) {
                    Text("🗑", fontSize = 18.sp)
                }
                IconButton(onClick = onDismiss) {
                    Text("✕", color = Color.LightGray, fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🎬", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = pageTitle.ifBlank { "Video en reproducción" },
                        color = Color(0xFFFDE68A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                "Selecciona la calidad o flujo para reproducir en tu TV Box:",
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(options) { opt ->
                    WvcVideoListItem(
                        option = opt,
                        pageTitle = pageTitle,
                        onTransmit = { onSelectAndTransmit(opt) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun WvcVideoListItem(
    option: StreamQualityOption,
    pageTitle: String,
    onTransmit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "🎬 ${pageTitle.ifBlank { "Video detectado" }}",
                color = Color(0xFFFDE68A),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val badgeColor = if (option.streamUrl.contains(".m3u8", true)) Color(0xFF38BDF8) else Color(0xFFA855F7)
                val badgeText = if (option.streamUrl.contains(".m3u8", true)) "HLS" else "MP4"

                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(badgeText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    option.label,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                if (option.resolution != "Auto") "Resolución: ${option.resolution} • Tasa: ${option.bitrate}"
                else "Reproducción Automática Adaptativa (Máxima fluidez sin cortes)",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onTransmit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Text("🚀 Transmitir a TV Box (ExoPlayer)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
