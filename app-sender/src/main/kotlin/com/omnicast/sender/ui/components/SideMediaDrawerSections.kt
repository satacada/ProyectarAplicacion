package com.omnicast.sender.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.omnicast.sender.playlist.Playlist
import com.omnicast.sender.ui.utils.ShareUtils

@Composable
fun DrawerHistoryList(
    historyList: List<HistoryItem>,
    searchQuery: String,
    selectedSort: VideoSortOrder,
    onPlay: (HistoryItem) -> Unit,
    onOpenWeb: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val displayed = remember(historyList, searchQuery, selectedSort) {
        filterAndSortHistoryItems(historyList, searchQuery, selectedSort)
    }
    val listState = rememberLazyListState()

    if (displayed.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                if (searchQuery.isNotBlank()) "No se encontraron videos para \"$searchQuery\""
                else "No hay videos en el historial",
                color = Color(0xFF64748B),
                fontSize = 13.sp
            )
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) {
                items(displayed, key = { it.url }) { item ->
                    HistoryTextCard(
                        item = item,
                        onPlay = { onPlay(item) },
                        onOpenWeb = { onOpenWeb(item.pageUrl ?: item.url) },
                        onDelete = { onDelete(item.url) }
                    )
                }
            }
            VerticalScrollbar(lazyListState = listState)
        }
    }
}

@Composable
fun HistoryTextCard(
    item: HistoryItem,
    onPlay: () -> Unit,
    onOpenWeb: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val posStr = ShareUtils.formatTime(item.lastPositionMs)
            val durStr = if (item.durationMs > 0L) ShareUtils.formatTime(item.durationMs) else null
            val progressText = if (durStr != null) "▶ $posStr / $durStr" else if (item.lastPositionMs > 0L) "▶ $posStr" else "▶ 0:00"
            Text(progressText, color = if (item.lastPositionMs > 0L) Color(0xFF38BDF8) else Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onPlay, modifier = Modifier.size(32.dp).background(Color(0xFF6366F1), RoundedCornerShape(8.dp))) {
                        Text("📺", fontSize = 14.sp)
                    }
                    IconButton(onClick = onOpenWeb, modifier = Modifier.size(32.dp).background(Color(0xFF334155), RoundedCornerShape(8.dp))) {
                        Text("🌐", fontSize = 14.sp)
                    }
                    if (ShareUtils.canShare(item.url, item.pageUrl)) {
                        IconButton(
                            onClick = { ShareUtils.shareVideo(context, item.title, item.url, item.pageUrl) },
                            modifier = Modifier.size(32.dp).background(Color(0xFF0284C7), RoundedCornerShape(8.dp))
                        ) { Text("🔗", fontSize = 14.sp) }
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) { Text("✕", color = Color(0xFF94A3B8), fontSize = 13.sp) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerSavedPlaylistsList(
    playlists: List<Playlist>,
    searchQuery: String,
    selectedSort: VideoSortOrder,
    onPlay: (String, String, Map<String, String>, Long) -> Unit,
    onOpenWeb: (String) -> Unit,
    onRemoveItem: (String, String) -> Unit
) {
    val context = LocalContext.current
    var actionItem by remember { mutableStateOf<Pair<String, HistoryItem>?>(null) }
    val listState = rememberLazyListState()

    val filteredPlaylists = remember(playlists, searchQuery, selectedSort) {
        playlists.mapNotNull { pl ->
            val sortedItems = filterAndSortHistoryItems(pl.items, searchQuery, selectedSort)
            if (sortedItems.isNotEmpty()) pl.copy(items = sortedItems) else null
        }
    }

    if (filteredPlaylists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                if (searchQuery.isNotBlank()) "No se encontraron videos para \"$searchQuery\""
                else "No tienes videos guardados aún.\nUsa '⭐ Guardar' en el navegador.",
                color = Color(0xFF64748B),
                fontSize = 12.sp
            )
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                filteredPlaylists.forEach { pl ->
                    item {
                        Text("📂 ${pl.name} (${pl.items.size} películas)", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    items(pl.items, key = { pl.id + "_" + it.url }) { video ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("↳", color = Color(0xFF38BDF8), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).combinedClickable(
                                    onClick = { onPlay(video.url, video.title, video.headers ?: emptyMap(), video.lastPositionMs) },
                                    onLongClick = { actionItem = pl.id to video }
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(video.title, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val posText = if (video.lastPositionMs > 0L) "▶ " + ShareUtils.formatTime(video.lastPositionMs) else "▶ 0:00"
                                    Text(posText, color = Color(0xFF10B981), fontSize = 10.sp)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            IconButton(onClick = { onPlay(video.url, video.title, video.headers ?: emptyMap(), video.lastPositionMs) }, modifier = Modifier.size(30.dp).background(Color(0xFF6366F1), RoundedCornerShape(6.dp))) { Text("📺", fontSize = 13.sp) }
                                            IconButton(onClick = { onOpenWeb(video.pageUrl ?: video.url) }, modifier = Modifier.size(30.dp).background(Color(0xFF334155), RoundedCornerShape(6.dp))) { Text("🌐", fontSize = 13.sp) }
                                            if (ShareUtils.canShare(video.url, video.pageUrl)) {
                                                IconButton(onClick = { ShareUtils.shareVideo(context, video.title, video.url, video.pageUrl) }, modifier = Modifier.size(30.dp).background(Color(0xFF0284C7), RoundedCornerShape(6.dp))) { Text("🔗", fontSize = 13.sp) }
                                            }
                                        }
                                        IconButton(onClick = { onRemoveItem(pl.id, video.url) }, modifier = Modifier.size(24.dp)) { Text("✕", color = Color(0xFF94A3B8), fontSize = 12.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            VerticalScrollbar(lazyListState = listState)
        }

        actionItem?.let { (plId, v) ->
            SavedVideoActionSheet(
                item = v,
                onPlay = { onPlay(v.url, v.title, v.headers ?: emptyMap(), v.lastPositionMs) },
                onShare = { ShareUtils.shareVideo(context, v.title, v.url, v.pageUrl) },
                onDelete = { onRemoveItem(plId, v.url) },
                onDismiss = { actionItem = null }
            )
        }
    }
}
