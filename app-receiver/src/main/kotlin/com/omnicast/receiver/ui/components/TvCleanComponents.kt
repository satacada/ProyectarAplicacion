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
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    updateState: com.omnicast.receiver.update.TvUpdateState = com.omnicast.receiver.update.TvUpdateState.Idle,
    onCheckUpdate: () -> Unit = {},
    onDownloadAndInstall: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFF0284C7), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("📺", fontSize = 18.sp)
            }
            Text("OmniCast TV", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            TvTopTab(label = "Recientes", isSelected = selectedTab == 0, onClick = { onSelectTab(0) })
            TvTopTab(label = "Todo", isSelected = selectedTab == 1, onClick = { onSelectTab(1) })
            TvTopTab(label = "Favoritos", isSelected = selectedTab == 2, onClick = { onSelectTab(2) })
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val currentVersion = remember {
                try {
                    "v" + context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (_: Throwable) { "v3.32.31" }
            }
            TvUpdateStatusBadge(
                updateState = updateState,
                currentVersion = currentVersion,
                onCheckUpdate = onCheckUpdate,
                onDownloadAndInstall = onDownloadAndInstall
            )
        }
    }
}

@Composable
fun TvTopTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = if (isSelected || isFocused) Color.White else Color(0xFF94A3B8),
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(modifier = Modifier.width(24.dp).height(2.dp).background(Color(0xFF38BDF8), RoundedCornerShape(1.dp)))
            }
        }
    }
}

@Composable
fun TvFolderRow(
    name: String,
    date: String,
    itemCount: Int,
    isExpanded: Boolean,
    items: List<TvPlaylistItem>,
    onClick: () -> Unit,
    onPlayItem: (TvPlaylistItem) -> Unit,
    onMenuItem: (TvPlaylistItem) -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isFocused) Color(0xFF2E2442) else Color(0xFF1E182A)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .clickable(onClick = onClick)
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color(0xFF38BDF8) else Color(0xFF2D223F),
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
                            .background(if (isExpanded) Color(0xFFD97706) else Color(0xFFF59E0B), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isExpanded) "📂" else "📁", fontSize = 20.sp)
                    }
                    Text(text = name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = if (itemCount > 0) "$itemCount película${if (itemCount != 1) "s" else ""} guardada${if (itemCount != 1) "s" else ""} • $date" else date,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        AnimatedVisibility(visible = isExpanded && items.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 4.dp, bottom = 8.dp)
            ) {
                items.forEach { item ->
                    TvPlaylistItemRow(
                        item = item,
                        onPlay = { onPlayItem(item) },
                        onMenu = { onMenuItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun TvRecentItemRow(
    item: TvHistoryItem,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF2E2442) else Color(0xFF1E182A)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color(0xFF38BDF8) else Color(0xFF2D223F),
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
                Text("▶", color = Color(0xFF10B981), fontSize = 16.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val pos = if (item.lastPositionMs > 0L) {
                        "Continuar: " + formatDuration(item.lastPositionMs) + " / " + formatDuration(item.durationMs)
                    } else "Listo para reproducir"
                    Text(pos, color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
            Text(text = formatFolderDate(item.updatedAt), color = Color(0xFF64748B), fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun TvBottomConnectionDock(
    ipAddress: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xEE0D0B14),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2D223F), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(12.dp).background(Color(0xFF10B981), CircleShape))
                Text("Listo para conectar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA7F3D0))
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dirección Local:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                Text("$ipAddress:8088", color = Color(0xFF38BDF8), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Text("Abre OmniCast en tu celular para transmitir pantalla, fotos o videos", color = Color(0xFF64748B), fontSize = 12.sp)
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
