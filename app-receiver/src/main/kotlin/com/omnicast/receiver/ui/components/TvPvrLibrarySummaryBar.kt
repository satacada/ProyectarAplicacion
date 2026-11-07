package com.omnicast.receiver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.receiver.directstream.TvPvrRecordingFile
import com.omnicast.receiver.directstream.TvStorageSafetyManager
import com.omnicast.receiver.directstream.TvStorageUnit
import com.omnicast.receiver.directstream.VideoBitratePreset

/**
 * Barra de Resumen de Biblioteca PVR y Estado de Almacenamiento Seguro para Android TV.
 * Refleja el conteo de grabaciones, tamaño acumulado, reserva sagrada (2.0GB TV / 500MB USB)
 * y estimación de tiempo de grabación disponible a 1080p.
 */
@Composable
fun TvPvrLibrarySummaryBar(
    recordings: List<TvPvrRecordingFile>,
    storageUnit: TvStorageUnit?,
    modifier: Modifier = Modifier,
    preset: VideoBitratePreset = VideoBitratePreset.FULL_HD_1080P
) {
    val totalSize = remember(recordings) { recordings.sumOf { it.sizeBytes } }
    val usableBytes = storageUnit?.usableBytes ?: 0L
    val freeBytes = storageUnit?.freeBytes ?: 0L
    val isUsb = storageUnit?.isUsb == true
    val reserveText = if (isUsb) "500 MB (USB)" else "2.0 GB (TV Anti-Brick)"

    val maxSafeMin = remember(usableBytes, preset) {
        TvStorageSafetyManager.calculateMaxSafeMinutes(usableBytes, preset)
    }

    val availableTimeStr = if (maxSafeMin >= 60) {
        val h = maxSafeMin / 60
        val m = maxSafeMin % 60
        if (m == 0L) "${h}h disp." else "${h}h ${m}m disp."
    } else {
        "$maxSafeMin min disp."
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF25334D), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Chip 1: Contador y Tamaño de Grabaciones PVR
            SummaryMetricChip(
                icon = "📼",
                title = "Grabaciones PVR",
                value = "${recordings.size} videos",
                subtitle = "Total: ${TvStorageSafetyManager.formatBytes(totalSize)}",
                accentColor = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            )

            // Chip 2: Espacio Grabable Útil y Reserva Sagrada
            SummaryMetricChip(
                icon = if (isUsb) "🔌" else "💾",
                title = "Espacio Útil",
                value = TvStorageSafetyManager.formatBytes(usableBytes),
                subtitle = "🛡️ Sagrada: $reserveText",
                accentColor = if (usableBytes > 0) Color(0xFF34D399) else Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )

            // Chip 3: Estimación de Tiempo Disponible
            SummaryMetricChip(
                icon = "⏱️",
                title = "Tiempo Disp. (1080p)",
                value = availableTimeStr,
                subtitle = "Bitrate: ~3.5 Mbps",
                accentColor = Color(0xFFA78BFA),
                modifier = Modifier.weight(1f)
            )

            // Chip 4: Unidad Activa y Espacio Libre Total
            SummaryMetricChip(
                icon = if (isUsb) "💽" else "🖥️",
                title = storageUnit?.name ?: "Almacenamiento",
                value = "${TvStorageSafetyManager.formatBytes(freeBytes)} libres",
                subtitle = if (storageUnit?.canRecord == true) "Listo para grabar" else "Espacio protegido",
                accentColor = if (storageUnit?.canRecord == true) Color(0xFFF59E0B) else Color(0xFFF87171),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryMetricChip(
    icon: String,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF131B2E))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(icon, fontSize = 12.sp)
                Text(
                    text = title,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                color = accentColor,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Color(0xFF64748B),
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
