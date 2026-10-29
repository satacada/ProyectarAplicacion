package com.omnicast.receiver.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.receiver.directstream.TvHistoryItem
import com.omnicast.receiver.directstream.TvPlaylistItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TvCleanTopBar(
    selectedTab: Int, onSelectTab: (Int) -> Unit, isConnected: Boolean = false,
    updateState: com.omnicast.receiver.update.TvUpdateState = com.omnicast.receiver.update.TvUpdateState.Idle,
    onCheckUpdate: () -> Unit = {}, onDownloadAndInstall: (String) -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(34.dp).background(if (isConnected) Color(0xFF022C22) else Color(0xFF0284C7), RoundedCornerShape(10.dp))
                    .border(if (isConnected) 1.5.dp else 0.dp, if (isConnected) Color(0xFF34D399) else Color.Transparent, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) { Text("📺", fontSize = 18.sp) }
            Text("OmniCast TV", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            if (isConnected) {
                Surface(color = Color(0xFF022C22), shape = RoundedCornerShape(12.dp), modifier = Modifier.border(1.5.dp, Color(0xFF6EE7B7), RoundedCornerShape(12.dp))) {
                    Text("🟢 VINCULADO", color = Color(0xFFECFDF5), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            TvTopTab(label = "Recientes", isSelected = selectedTab == 0, isConnected = isConnected, onClick = { onSelectTab(0) })
            TvTopTab(label = "Todo", isSelected = selectedTab == 1, isConnected = isConnected, onClick = { onSelectTab(1) })
            TvTopTab(label = "Favoritos", isSelected = selectedTab == 2, isConnected = isConnected, onClick = { onSelectTab(2) })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val currentVersion = remember { try { "v" + context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (_: Throwable) { "v3.32.31" } }
            TvUpdateStatusBadge(updateState = updateState, currentVersion = currentVersion, onCheckUpdate = onCheckUpdate, onDownloadAndInstall = onDownloadAndInstall)
        }
    }
}

@Composable
fun TvTopTab(label: String, isSelected: Boolean, isConnected: Boolean = false, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Box(modifier = Modifier.onFocusChanged { isFocused = it.isFocused }.focusable().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = if (isSelected || isFocused) Color.White else (if (isConnected) Color(0xFFD1FAE5) else Color(0xFF94A3B8)), fontSize = 16.sp, fontWeight = if (isSelected || isConnected) FontWeight.Bold else FontWeight.Normal)
            if (isSelected) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(modifier = Modifier.width(28.dp).height(3.dp).background(if (isConnected) Color.White else Color(0xFF38BDF8), RoundedCornerShape(1.5.dp)))
            }
        }
    }
}

@Composable
fun TvFolderRow(name: String, date: String, itemCount: Int, isConnected: Boolean = false, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val cardBg = if (isFocused) (if (isConnected) Color(0xFF064E3B) else Color(0xFF2E2442)) else (if (isConnected) Color(0xE6022C22) else Color(0xFF1E182A))
    val borderCol = if (isFocused) Color.White else (if (isConnected) Color(0xFF34D399) else Color(0xFF2D223F))

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .border(
                width = if (isFocused) 2.5.dp else 1.5.dp,
                color = borderCol,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(if (isFocused) Color(0xFFD97706) else Color(0xFFF59E0B), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📁", fontSize = 20.sp)
                }
                Column {
                    Text(text = name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (itemCount > 0) "$itemCount película${if (itemCount != 1) "s" else ""} guardada${if (itemCount != 1) "s" else ""}" else "Sin películas",
                        color = if (isConnected) Color(0xFFD1FAE5) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = if (isConnected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = date, color = if (isConnected) Color(0xFFA7F3D0) else Color(0xFF64748B), fontSize = 11.sp)
                Text("▶", color = if (isFocused || isConnected) Color.White else Color(0xFF64748B), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun TvRecentItemRow(
    item: TvHistoryItem,
    isConnected: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val cardBg = when {
        isFocused -> if (isConnected) Color(0xFF064E3B) else Color(0xFF2E2442)
        isConnected -> Color(0xE6022C22)
        else -> Color(0xFF1E182A)
    }
    val borderCol = when {
        isFocused -> Color.White
        isConnected -> Color(0xFF34D399)
        else -> Color(0xFF2D223F)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .border(
                width = if (isFocused) 2.5.dp else 1.5.dp,
                color = borderCol,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("▶", color = if (isConnected) Color(0xFF6EE7B7) else Color(0xFF10B981), fontSize = 16.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val pos = if (item.lastPositionMs > 0L) {
                        "Continuar: " + formatDuration(item.lastPositionMs) + " / " + formatDuration(item.durationMs)
                    } else "Listo para reproducir"
                    Text(pos, color = if (isConnected) Color(0xFFD1FAE5) else Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
            Text(text = formatFolderDate(item.updatedAt), color = if (isConnected) Color(0xFFA7F3D0) else Color(0xFF64748B), fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun TvBottomConnectionDock(
    ipAddress: String,
    connectedClientIp: String? = null,
    modifier: Modifier = Modifier
) {
    val isConnected = !connectedClientIp.isNullOrBlank()
    Surface(
        color = if (isConnected) Color(0xF2022C22) else Color(0xEE0D0B14),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(if (isConnected) 2.dp else 1.dp, if (isConnected) Color(0xFF6EE7B7) else Color(0xFF2D223F), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(12.dp).background(if (isConnected) Color(0xFF34D399) else Color(0xFFF59E0B), CircleShape))
                Text(
                    if (isConnected) "🟢 Celular Conectado ($connectedClientIp)" else "Listo para vincular",
                    fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (isConnected) Color.White else Color(0xFFCBD5E1)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (isConnected) "Enlace activo:" else "Dirección Local:", color = if (isConnected) Color(0xFFD1FAE5) else Color(0xFF94A3B8), fontSize = 13.sp)
                Text(if (isConnected) "Listo para proyectar" else "$ipAddress:8088", color = if (isConnected) Color(0xFF6EE7B7) else Color(0xFF38BDF8), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            }

            Text(
                if (isConnected) "Envía desde tu celular o elige una película arriba" else "Abre OmniCast en tu celular para conectar",
                color = if (isConnected) Color(0xFFECFDF5) else Color(0xFF64748B), fontSize = 12.sp, fontWeight = if (isConnected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

fun formatFolderDate(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
        sdf.format(Date(timestamp))
    } catch (_: Throwable) {
        "2026/06/21 15:12"
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return if (min >= 60) {
        val hr = min / 60
        val remMin = min % 60
        String.format(Locale.US, "%d:%02d:%02d", hr, remMin, sec)
    } else {
        String.format(Locale.US, "%d:%02d", min, sec)
    }
}
