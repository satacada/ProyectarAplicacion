package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.sender.history.HistoryItem
import com.omnicast.sender.history.PlayHistoryManager
import com.omnicast.sender.playlist.Playlist
import com.omnicast.sender.playlist.PlaylistManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideMediaDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    onPlayOnTv: (url: String, title: String, headers: Map<String, String>, positionMs: Long) -> Unit,
    onOpenInBrowser: (url: String) -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = if (isOpen) DrawerValue.Open else DrawerValue.Closed)
    val context = LocalContext.current
    val historyManager = remember { PlayHistoryManager(context) }
    val playlistManager = remember { PlaylistManager(context) }

    LaunchedEffect(isOpen) {
        if (isOpen) drawerState.open() else drawerState.close()
    }

    LaunchedEffect(drawerState.isOpen) {
        if (!drawerState.isOpen && isOpen) onClose()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0F172A),
                modifier = Modifier.width(320.dp).fillMaxHeight()
            ) {
                DrawerSheetContent(
                    historyManager = historyManager,
                    playlistManager = playlistManager,
                    onClose = onClose,
                    onPlayOnTv = onPlayOnTv,
                    onOpenInBrowser = onOpenInBrowser
                )
            }
        },
        content = content
    )
}

@Composable
private fun DrawerSheetContent(
    historyManager: PlayHistoryManager,
    playlistManager: PlaylistManager,
    onClose: () -> Unit,
    onPlayOnTv: (String, String, Map<String, String>, Long) -> Unit,
    onOpenInBrowser: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var historyList by remember { mutableStateOf(historyManager.getHistory()) }
    var playlists by remember { mutableStateOf(playlistManager.getPlaylists()) }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("📺 Mis Videos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Historial y Guardadas", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
            IconButton(onClick = onClose) {
                Text("✕", color = Color(0xFF94A3B8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF1E293B),
            contentColor = Color(0xFF38BDF8)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0; historyList = historyManager.getHistory() },
                text = { Text("🕒 Historial (" + historyList.size + ")", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1; playlists = playlistManager.getPlaylists() },
                text = { Text("⭐ Guardadas (" + playlists.sumOf { it.items.size } + ")", fontSize = 12.sp) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vistos recientemente", fontSize = 11.sp, color = Color(0xFF64748B))
                if (historyList.isNotEmpty()) {
                    TextButton(
                        onClick = { historyManager.clear(keepFavorites = false); historyList = emptyList() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("🗑 Limpiar", fontSize = 11.sp, color = Color(0xFFEF4444))
                    }
                }
            }

            if (historyList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay videos en el historial", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(historyList, key = { it.url }) { item ->
                        HistoryTextCard(
                            item = item,
                            onPlay = { onPlayOnTv(item.url, item.title, item.headers, item.lastPositionMs); onClose() },
                            onOpenWeb = { onOpenInBrowser(item.pageUrl ?: item.url); onClose() },
                            onDelete = { historyManager.remove(item.url); historyList = historyManager.getHistory() }
                        )
                    }
                }
            }
        } else {
            SavedPlaylistsTextList(
                playlists = playlists,
                onPlay = { url, title, hdrs, pos -> onPlayOnTv(url, title, hdrs, pos); onClose() },
                onOpenWeb = { url -> onOpenInBrowser(url); onClose() },
                onRemoveItem = { plId, u -> playlistManager.removeItemFromPlaylist(plId, u); playlists = playlistManager.getPlaylists() }
            )
        }
    }
}

@Composable
private fun HistoryTextCard(
    item: HistoryItem,
    onPlay: () -> Unit,
    onOpenWeb: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                item.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val posStr = formatTime(item.lastPositionMs)
            val durStr = if (item.durationMs > 0L) formatTime(item.durationMs) else null
            val progressText = if (durStr != null) "▶ $posStr / $durStr" else if (item.lastPositionMs > 0L) "▶ $posStr" else "▶ 0:00"

            Text(
                progressText,
                color = if (item.lastPositionMs > 0L) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF6366F1), RoundedCornerShape(8.dp))
                    ) {
                        Text("📺", fontSize = 14.sp)
                    }
                    IconButton(
                        onClick = onOpenWeb,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                    ) {
                        Text("🌐", fontSize = 14.sp)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Text("✕", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SavedPlaylistsTextList(
    playlists: List<Playlist>,
    onPlay: (String, String, Map<String, String>, Long) -> Unit,
    onOpenWeb: (String) -> Unit,
    onRemoveItem: (String, String) -> Unit
) {
    if (playlists.isEmpty() || playlists.all { it.items.isEmpty() }) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No tienes videos guardados aún.\nUsa '⭐ Guardar' en el navegador.", color = Color(0xFF64748B), fontSize = 12.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            playlists.filter { it.items.isNotEmpty() }.forEach { pl ->
                item {
                    Text("📁 " + pl.name + " (" + pl.items.size + ")", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                items(pl.items, key = { pl.id + "_" + it.url }) { video ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(video.title, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val posText = if (video.lastPositionMs > 0L) "▶ " + formatTime(video.lastPositionMs) else "▶ 0:00"
                            Text(posText, color = Color(0xFF10B981), fontSize = 10.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = { onPlay(video.url, video.title, video.headers, video.lastPositionMs) },
                                        modifier = Modifier.size(30.dp).background(Color(0xFF6366F1), RoundedCornerShape(6.dp))
                                    ) { Text("📺", fontSize = 13.sp) }
                                    IconButton(
                                        onClick = { onOpenWeb(video.pageUrl ?: video.url) },
                                        modifier = Modifier.size(30.dp).background(Color(0xFF334155), RoundedCornerShape(6.dp))
                                    ) { Text("🌐", fontSize = 13.sp) }
                                }
                                IconButton(onClick = { onRemoveItem(pl.id, video.url) }, modifier = Modifier.size(24.dp)) {
                                    Text("✕", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return if (min >= 60) {
        val hr = min / 60
        val remMin = min % 60
        String.format(java.util.Locale.US, "%d:%02d:%02d", hr, remMin, sec)
    } else {
        String.format(java.util.Locale.US, "%d:%02d", min, sec)
    }
}
