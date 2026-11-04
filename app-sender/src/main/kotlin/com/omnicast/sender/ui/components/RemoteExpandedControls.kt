package com.omnicast.sender.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Subtitles
import com.omnicast.core.protocol.SignalingMessage
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.sender.network.SignalingClientHolder
import com.omnicast.sender.service.TvZoomManager

@Composable
fun RemoteExpandedControls(
    title: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    volume: Float,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onStop: (() -> Unit)? = null,
    onSaveToPlaylist: (() -> Unit)? = null,
    onDeleteFromSaved: (() -> Unit)? = null,
    onSelectQuality: (() -> Unit)? = null,
    isSaved: Boolean = false
) {
    val context = LocalContext.current
    val tracksInfo by com.omnicast.sender.stream.ActiveStreamHolder.tracksInfo.collectAsState()
    val qualityLabels = remember { listOf("Auto", "1080p", "720p", "540p", "360p", "270p") }
    var qualityIdx by remember { mutableIntStateOf(0) }
    var showRecordSheet by remember { mutableStateOf(false) }
    val recordTelemetry by com.omnicast.sender.stream.ActiveStreamHolder.recordTelemetry.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp)) {

        // ========================================================
        // SECCIÓN 2: PELÍCULA, LISTAS (Bookmark / Bookmark-X) Y CALIDAD
        // ========================================================
        Surface(
            color = Color(0xFF141D2B),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223147)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = title.ifBlank { "Transmisión Web" },
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                        Text("Transmisión Lista", color = Color(0xFF6EE7B7), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onSaveToPlaylist != null) {
                        IconButton(onClick = onSaveToPlaylist, modifier = Modifier.size(26.dp)) {
                            BookmarkOutlineIcon(modifier = Modifier.size(18.dp), color = if (isSaved) Color(0xFF10B981) else Color(0xFF38BDF8), isFilled = isSaved)
                        }
                    }
                    if (onDeleteFromSaved != null) {
                        IconButton(onClick = onDeleteFromSaved, modifier = Modifier.size(26.dp)) {
                            BookmarkRemoveIcon(modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(
                        onClick = {
                            val preset = TvZoomManager.cycleNext()
                            SignalingClientHolder.get().sendMessage(TvZoomManager.toSignalingMessage(preset))
                            Toast.makeText(context, preset.label, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Rounded.AspectRatio, contentDescription = "Zoom TV", tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            SignalingClientHolder.get().sendMessage(SignalingMessage.PlayControl("CYCLE_AUDIO"))
                            val list = tracksInfo?.audioTracks.orEmpty()
                            val nextMsg = if (list.size > 1) {
                                val nextI = ((tracksInfo?.selectedAudioIndex ?: 0) + 1) % list.size
                                "🔊 Audio: ${list[nextI]}"
                            } else "🔊 Pista de Audio: ${list.firstOrNull() ?: "Original"}"
                            Toast.makeText(context, nextMsg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Rounded.VolumeUp, contentDescription = "Pista de Audio", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            SignalingClientHolder.get().sendMessage(SignalingMessage.PlayControl("TOGGLE_SUBTITLES"))
                            Toast.makeText(context, "💬 Alternando Subtítulos", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Rounded.Subtitles, contentDescription = "Subtítulos", tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                    }
                    Box(
                        modifier = Modifier
                            .height(26.dp)
                            .clickable {
                                qualityIdx = (qualityIdx + 1) % qualityLabels.size
                                SignalingClientHolder.get().sendMessage(SignalingMessage.PlayControl("CYCLE_QUALITY"))
                                Toast.makeText(context, "📺 Calidad: ${qualityLabels[qualityIdx]}", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(Icons.Rounded.Hd, contentDescription = "Calidad", tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                            Text(qualityLabels[qualityIdx], color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ========================================================
        // SECCIÓN 3: LÍNEA DE TIEMPO, CONTROLES DE REPRODUCCIÓN Y VOLUMEN
        // ========================================================
        Surface(
            color = Color(0xFF141D2B),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223147)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Barra de Tiempo Scrubbing
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    var sliderPos by remember(positionMs) { mutableFloatStateOf(positionMs.toFloat()) }
                    val effectiveDur = if (durationMs > 0L) durationMs.toFloat() else 1000f

                    Slider(
                        value = sliderPos.coerceIn(0f, effectiveDur),
                        onValueChange = { sliderPos = it },
                        onValueChangeFinished = { onSeekTo(sliderPos.toLong()) },
                        valueRange = 0f..effectiveDur,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF7A00),
                            activeTrackColor = Color(0xFFFF7A00),
                            inactiveTrackColor = Color(0xFF1E283D)
                        ),
                        modifier = Modifier.fillMaxWidth().height(18.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatTimeString(positionMs), color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(formatTimeString(durationMs), color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 2. Fila de Controles de Reproducción con Detener al final de la línea
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "-10s", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 4.dp))
                        Box(
                            modifier = Modifier.size(30.dp).background(Color(0xFF161F2E), CircleShape).border(1.dp, Color(0xFF26354D), CircleShape).clickable { onSkip(-10000L) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Replay10, contentDescription = "-10s", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp)) }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier.size(34.dp).background(Color(0xFFFF7A00), CircleShape).clickable { onPlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (isPlaying) "Pausa" else "Reproducir", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier.size(30.dp).background(Color(0xFF161F2E), CircleShape).border(1.dp, Color(0xFF26354D), CircleShape).clickable { onSkip(10000L) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Forward10, contentDescription = "+10s", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp)) }

                        Text(text = "+10s", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
                    }

                    val isRec = recordTelemetry?.isRecording == true
                    IconButton(
                        onClick = {
                            SignalingClientHolder.get().sendMessage(SignalingMessage.PlayControl("REQUEST_RECORD_INFO"))
                            showRecordSheet = true
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(28.dp)
                            .background(if (isRec) Color(0xFF7F1D1D) else Color(0xFF2D1515), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = if (isRec) 1.0f else 0.5f), RoundedCornerShape(6.dp))
                    ) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFFEF4444), CircleShape))
                    }

                    if (onStop != null) {
                        IconButton(
                            onClick = onStop,
                            modifier = Modifier.align(Alignment.CenterEnd).size(28.dp).background(Color(0xFF2D1515), RoundedCornerShape(6.dp)).border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = "Detener flujo", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // 3. Fila de Volumen
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    var curVol by remember(volume) { mutableFloatStateOf(volume) }
                    Icon(if (curVol <= 0.01f) Icons.Rounded.VolumeMute else Icons.Rounded.VolumeUp, contentDescription = "Volumen", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    Slider(
                        value = curVol, onValueChange = { curVol = it }, onValueChangeFinished = { onVolumeChange(curVol) }, valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00B4D8), activeTrackColor = Color(0xFF00B4D8), inactiveTrackColor = Color(0xFF1E283D)),
                        modifier = Modifier.weight(1f).height(18.dp).padding(horizontal = 6.dp)
                    )
                    Text("${(curVol * 100).toInt()}%", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showRecordSheet) {
        SenderRecordBottomSheet(
            telemetry = recordTelemetry ?: SignalingMessage.RecordTelemetry(canRecord = true, internalPath = "/storage/emulated/0/Download/PVR_Grabaciones"),
            onStartRecord = { useUsb, res, timer, folder ->
                SignalingClientHolder.get().sendMessage(SignalingMessage.PlayControl("START_RECORD", param = "useUsb=$useUsb,res=$res,timer=$timer,folder=$folder"))
                Toast.makeText(context, "🔴 Orden de grabación enviada a TV Box", Toast.LENGTH_SHORT).show()
            },
            onStopRecord = { discard ->
                SignalingClientHolder.get().sendMessage(SignalingMessage.PlayControl("STOP_RECORD", param = if (discard) "DISCARD" else "KEEP"))
                Toast.makeText(context, if (discard) "🗑️ Descarte enviado a TV Box" else "💾 Finalización enviada a TV Box", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showRecordSheet = false }
        )
    }
}

fun formatTimeString(ms: Long): String = if (ms <= 0L) "00:00" else ms.let {
    val s = it / 1000; val h = s / 3600; val m = (s / 60) % 60; val sec = s % 60
    if (h > 0) String.format("%02d:%02d:%02d", h, m, sec) else String.format("%02d:%02d", m, sec)
}
