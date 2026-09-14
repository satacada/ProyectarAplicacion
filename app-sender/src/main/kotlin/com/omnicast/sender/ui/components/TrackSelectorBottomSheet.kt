package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.core.protocol.SignalingMessage

/**
 * Hoja inferior para seleccionar pistas de audio (idioma) y activar/desactivar subtítulos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSelectorBottomSheet(
    tracksInfo: SignalingMessage.TracksInfo?,
    onToggleSubtitles: () -> Unit,
    onSelectTextTrack: (Int) -> Unit,
    onSelectAudioTrack: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "💬 Idioma y Subtítulos",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (tracksInfo == null || (tracksInfo.audioTracks.isEmpty() && tracksInfo.textTracks.isEmpty())) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ℹ️ No se detectaron pistas adicionales.", color = Color.LightGray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("El reproductor usará el audio y subtítulos predeterminados de la fuente.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                // Sección Subtítulos
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Subtítulos", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (tracksInfo.subtitlesEnabled) "Activados" else "Desactivados", color = if (tracksInfo.subtitlesEnabled) Color(0xFF38BDF8) else Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = tracksInfo.subtitlesEnabled,
                        onCheckedChange = { onToggleSubtitles() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF0284C7)
                        )
                    )
                }

                if (tracksInfo.subtitlesEnabled && tracksInfo.textTracks.isNotEmpty()) {
                    Text("Pista de Subtítulos:", color = Color.LightGray, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                    tracksInfo.textTracks.forEachIndexed { idx, name ->
                        val selected = idx == tracksInfo.selectedTextIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTextTrack(idx) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectTextTrack(idx) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name, color = if (selected) Color(0xFF38BDF8) else Color.White, fontSize = 14.sp)
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 12.dp))

                // Sección Audio / Idioma
                if (tracksInfo.audioTracks.isNotEmpty()) {
                    Text("Idioma / Pista de Audio:", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
                    tracksInfo.audioTracks.forEachIndexed { idx, name ->
                        val selected = idx == tracksInfo.selectedAudioIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectAudioTrack(idx) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectAudioTrack(idx) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name, color = if (selected) Color(0xFF38BDF8) else Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cerrar", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            }
        }
    }
}
