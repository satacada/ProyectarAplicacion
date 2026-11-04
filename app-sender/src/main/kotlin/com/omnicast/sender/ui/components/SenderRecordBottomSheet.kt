package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.core.protocol.SignalingMessage
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenderRecordBottomSheet(
    telemetry: SignalingMessage.RecordTelemetry?,
    onStartRecord: (useUsb: Boolean, resolution: Int, timerMinutes: Int, folder: String) -> Unit,
    onStopRecord: (discard: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    if (telemetry == null) return

    var useUsb by remember { mutableStateOf(false) }
    var selectedResolution by remember { mutableIntStateOf(telemetry.availableResolutions.firstOrNull() ?: 1080) }
    var selectedTimerMinutes by remember { mutableIntStateOf(if (telemetry.isLive) 60 else 0) }

    val freeBytes = if (useUsb) telemetry.usbFreeBytes else telemetry.freeBytes
    val usableBytes = if (useUsb) telemetry.usbUsableBytes else telemetry.usableBytes
    val canRecord = usableBytes > 0L && (if (useUsb) telemetry.isUsbAvailable else telemetry.canRecord)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabecera
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(12.dp).background(Color(0xFFEF4444), CircleShape))
                Text(
                    text = if (telemetry.isRecording) "🔴 Grabación en Curso en TV" else "🔴 Grabar en TV Box",
                    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = telemetry.currentTitle.ifBlank { "Transmisión Activa" },
                color = Color(0xFF94A3B8), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )

            if (telemetry.isRecording) {
                // Estado en caliente si ya se está grabando
                Surface(
                    color = Color(0xFF1E293B), shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (telemetry.isMovieDownload) "📥 Descargando película: ${telemetry.progressPct}%" else "🔴 Grabando TV en Vivo",
                            color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "⏱️ Tiempo transcurrido: ${telemetry.recordedDurationSec / 60}m ${telemetry.recordedDurationSec % 60}s",
                            color = Color(0xFFFCA5A5), fontSize = 12.sp
                        )
                        Text(
                            text = "📁 Carpeta: ${telemetry.targetFolder}",
                            color = Color(0xFF94A3B8), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onStopRecord(false); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)
                    ) {
                        Text("💾 Conservar", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onStopRecord(true); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                        shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)
                    ) {
                        Text("🗑️ Descartar", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Selector de Almacenamiento
                if (telemetry.isUsbAvailable) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StorageChoiceBtn("Memoria Interna", isSelected = !useUsb, modifier = Modifier.weight(1f)) { useUsb = false }
                        StorageChoiceBtn("USB Externo", isSelected = useUsb, modifier = Modifier.weight(1f)) { useUsb = true }
                    }
                }

                // Telemetría de Espacio en TV Box
                Surface(
                    color = Color(0xFF1E293B), shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🟢 Espacio Libre en TV:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text("${formatBytes(freeBytes)} libres", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🛡️ Blindaje Anti-Brick:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text(if (useUsb) "500 MB (USB)" else "2.0 GB (Intocable)", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("💾 Espacio Grabable Útil:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text("${formatBytes(usableBytes)} útiles", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(
                            text = "📁 Guardará en: ${if (useUsb) telemetry.usbPath.ifBlank { "USB" } else telemetry.internalPath}",
                            color = Color(0xFF64748B), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!canRecord) {
                    Surface(color = Color(0xFF3B1219), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)), modifier = Modifier.fillMaxWidth()) {
                        Text(telemetry.warningMessage ?: "Espacio bloqueado por seguridad en TV Box (< 2.0 GB libres). Conecta un USB.", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    }
                }

                // Selector de Resoluciones Reales
                if (telemetry.availableResolutions.isNotEmpty()) {
                    Text("📺 Calidades Detectadas en Flujo:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        telemetry.availableResolutions.forEach { res ->
                            StorageChoiceBtn(
                                label = "${res}p",
                                isSelected = selectedResolution == res,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedResolution = res }
                            )
                        }
                    }
                }

                // Selector de Temporizador
                if (telemetry.isLive) {
                    Text("⏰ Temporizador: ${if (selectedTimerMinutes <= 0) "Continuo" else "$selectedTimerMinutes min"}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(30 to "30m", 60 to "60m", 90 to "90m", 0 to "Infinito").forEach { (min, label) ->
                            StorageChoiceBtn(label, isSelected = selectedTimerMinutes == min, modifier = Modifier.weight(1f)) { selectedTimerMinutes = min }
                        }
                    }
                }

                Button(
                    onClick = {
                        val folder = if (useUsb) telemetry.usbPath else telemetry.internalPath
                        onStartRecord(useUsb, selectedResolution, selectedTimerMinutes, folder)
                        onDismiss()
                    },
                    enabled = canRecord,
                    colors = ButtonDefaults.buttonColors(containerColor = if (canRecord) Color(0xFFDC2626) else Color(0xFF475569)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(if (telemetry.isLive) "🔴 Iniciar Grabación PVR en TV" else "📥 Descargar Película en TV", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun StorageChoiceBtn(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
        modifier = modifier.height(34.dp).clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 1024.0) String.format(Locale.US, "%.2f GB", mb / 1024.0) else String.format(Locale.US, "%.0f MB", mb)
}
