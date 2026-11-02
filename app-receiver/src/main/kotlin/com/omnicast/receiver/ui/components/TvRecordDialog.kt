package com.omnicast.receiver.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omnicast.receiver.directstream.*
import kotlinx.coroutines.launch

@Composable
fun TvRecordDialog(
    title: String,
    streamUrl: String,
    headers: Map<String, String>,
    durationMs: Long,
    isLive: Boolean,
    onDismiss: () -> Unit,
    onRecordingStarted: () -> Unit
) {
    val context = LocalContext.current
    val units = remember { TvStorageSafetyManager.detectAvailableStorageUnits(context) }
    var selectedUnitIndex by remember { mutableIntStateOf(units.indexOfFirst { it.canRecord }.coerceAtLeast(0)) }
    val currentUnit = units.getOrNull(selectedUnitIndex) ?: units.firstOrNull()

    var selectedResolution by remember { mutableIntStateOf(1080) }
    var selectedBitrate by remember { mutableStateOf(VideoBitratePreset.FULL_HD_1080P) }
    var selectedTimerMinutes by remember { mutableIntStateOf(if (isLive) 60 else 0) }

    val usableBytes = currentUnit?.usableBytes ?: 0L
    val maxSafeMin = remember(usableBytes, selectedBitrate) {
        TvStorageSafetyManager.calculateMaxSafeMinutes(usableBytes, selectedBitrate)
    }
    val autoCapped = remember(selectedTimerMinutes, maxSafeMin) {
        TvRecordTimerManager.calculateAutoCappedMinutes(selectedTimerMinutes, maxSafeMin)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141A29)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.width(520.dp).wrapContentHeight().border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isLive) "🔴 Grabación Inteligente PVR (TV en Vivo)" else "📥 Guardar Película en TV",
                    color = Color(0xFF38BDF8), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                )

                Text(
                    text = title.ifBlank { "Transmisión en Vivo" },
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )

                // Selector de Unidades
                Text("Selecciona Unidad de Almacenamiento:", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    units.forEachIndexed { idx, u ->
                        val isSel = idx == selectedUnitIndex
                        TvDialogSelectBtn(
                            text = "${if (isSel) "● " else "○ "}${u.name} (${TvStorageSafetyManager.formatBytes(u.usableBytes)} útil)",
                            isSelected = isSel,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedUnitIndex = idx }
                        )
                    }
                }

                // Alerta de Espacio Insuficiente o Reserva
                if (currentUnit != null && !currentUnit.canRecord) {
                    Surface(color = Color(0xFF3B1219), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)), modifier = Modifier.fillMaxWidth()) {
                        Text(currentUnit.warningMessage ?: "Espacio bloqueado por seguridad.", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
                    }
                } else if (currentUnit != null) {
                    // Telemetría de Espacio y Seguridad
                    Surface(color = Color(0xFF0F172A), shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🟢 Espacio Libre Real:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text("${TvStorageSafetyManager.formatBytes(currentUnit.freeBytes)} libres", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🛡️ Reserva Sagrada Intocable:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(if (currentUnit.isUsb) "500 MB (USB)" else "2.0 GB (Blindaje Anti-Brick)", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("💾 Espacio Grabable Útil:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text("${TvStorageSafetyManager.formatBytes(currentUnit.usableBytes)} útiles", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("⏱️ Tiempo Máximo Seguro Estimado:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text("~${maxSafeMin} minutos (${selectedBitrate.label.substringBefore(" (")})", color = Color(0xFFA78BFA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Selector de Resolución / Calidad
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("📺 Calidad de Grabación:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            1080 to "🌟 1080p",
                            720 to "📺 720p",
                            480 to "480p",
                            360 to "360p"
                        ).forEach { (res, label) ->
                            TvDialogSelectBtn(
                                text = label,
                                isSelected = selectedResolution == res,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selectedResolution = res
                                    selectedBitrate = when (res) {
                                        1080 -> VideoBitratePreset.FULL_HD_1080P
                                        720 -> VideoBitratePreset.HD_720P
                                        480 -> VideoBitratePreset.SD_480P
                                        else -> VideoBitratePreset.LOW_360P
                                    }
                                }
                            )
                        }
                    }
                }

                // Selector de Temporizador (para Live PVR)
                if (isLive) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("⏰ Temporizador (Modo \"Me voy de casa\"): ${if (autoCapped.first <= 0) "Continuo" else "${autoCapped.first} min"}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(30 to "30m", 45 to "45m", 60 to "60m", 90 to "90m", 120 to "120m", 0 to "Infinito").forEach { (min, label) ->
                                TvDialogSelectBtn(
                                    text = label,
                                    isSelected = selectedTimerMinutes == min,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedTimerMinutes = min }
                                )
                            }
                        }
                        if (autoCapped.second != null) {
                            Text(autoCapped.second!!, color = Color(0xFFF59E0B), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Botones de Acción
                val canStart = currentUnit?.canRecord == true && usableBytes > 0L
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvDialogActionBtn(
                        text = if (isLive) "🔴 Iniciar Grabación PVR" else "📥 Descargar Película",
                        bgBase = if (canStart) Color(0xFFDC2626) else Color(0xFF475569),
                        enabled = canStart,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!canStart) return@TvDialogActionBtn
                            val effectiveTimer = autoCapped.first
                            val isUsb = currentUnit?.isUsb == true
                            if (isLive) {
                                PvrStreamRecorder.startRecording(
                                    context = context,
                                    title = title,
                                    url = streamUrl,
                                    headers = headers,
                                    useUsb = isUsb,
                                    targetHeight = selectedResolution,
                                    timerMinutes = effectiveTimer,
                                    onStopped = { reason ->
                                        Toast.makeText(context, reason, Toast.LENGTH_LONG).show()
                                    }
                                )
                            } else {
                                TvDownloadManager.activeJob = PvrStreamRecorder.recorderScope.launch {
                                    TvDownloadManager.downloadMovieInBridgeMode(
                                        context = context,
                                        title = title,
                                        url = streamUrl,
                                        headers = headers,
                                        expectedBytes = durationMs.takeIf { it > 0 }?.let { (it / 1000L) * 165_000L } ?: 800_000_000L,
                                        useUsb = isUsb,
                                        onProgress = {}
                                    )
                                }
                            }
                            onRecordingStarted()
                            onDismiss()
                        }
                    )

                    TvDialogActionBtn(
                        text = "✕ Cancelar",
                        bgBase = Color(0xFF334155),
                        enabled = true,
                        modifier = Modifier.width(110.dp),
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun TvDialogSelectBtn(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> Color(0xFF38BDF8)
        isSelected -> Color(0xFF0369A1)
        else -> Color(0xFF1E293B)
    }
    Surface(
        color = bg, shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0xFF475569)),
        modifier = modifier.height(34.dp).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = text, color = if (focused) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun TvDialogActionBtn(text: String, bgBase: Color, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        color = if (focused && enabled) Color(0xFF38BDF8) else bgBase,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color.Transparent),
        modifier = modifier.height(40.dp).onFocusChanged { focused = it.isFocused }.focusable(enabled).clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = text, color = if (focused && enabled) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
