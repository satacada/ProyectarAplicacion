package com.omnicast.sender.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SubdirectoryArrowRight
import androidx.compose.material.icons.rounded.Tv
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
import com.omnicast.sender.ui.utils.MediaDownloadHelper
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
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) {
                items(displayed, key = { it.url }) { item ->
                    HistoryTextCard(
                        item = item,
                        onPlay = { onPlay(item) },
                        onOpenWeb = {
                            val target = item.pageUrl?.takeIf { it.isNotBlank() } ?: item.headers["Referer"]?.takeIf { it.isNotBlank() } ?: item.url
                            onOpenWeb(target)
                        },
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
    var showMenu by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clickable { onOpenWeb() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val posStr = ShareUtils.formatTime(item.lastPositionMs)
                val durStr = if (item.durationMs > 0L) ShareUtils.formatTime(item.durationMs) else null
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = if (item.lastPositionMs > 0L) Color(0xFF38BDF8) else Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                    val progressText = if (durStr != null) "$posStr / $durStr" else if (item.lastPositionMs > 0L) posStr else "0:00"
                    Text(progressText, color = if (item.lastPositionMs > 0L) Color(0xFF38BDF8) else Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Opciones", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.Tv, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp)) },
                        text = { Text("Proyectar en TV", color = Color.White, fontSize = 13.sp) },
                        onClick = { showMenu = false; onPlay() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.Language, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp)) },
                        text = { Text("Abrir en Web", color = Color.White, fontSize = 13.sp) },
                        onClick = { showMenu = false; onOpenWeb() }
                    )
                    if (ShareUtils.canShare(item.url, item.pageUrl)) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp)) },
                            text = { Text("Compartir enlace", color = Color.White, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                ShareUtils.shareVideo(context, item.title, item.url, item.pageUrl)
                            }
                        )
                    }
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.FileDownload, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp)) },
                        text = { Text("Descargar película", color = Color(0xFF10B981), fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            MediaDownloadHelper.downloadMedia(context, item.url, item.title, item.headers ?: emptyMap())
                        }
                    )
                    HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp)) },
                        text = { Text("Eliminar", color = Color(0xFFEF4444), fontSize = 13.sp) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
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
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                filteredPlaylists.forEach { pl ->
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Folder, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Text("${pl.name} (${pl.items.size} películas)", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    items(pl.items, key = { pl.id + "_" + it.url }) { video ->
                        var showSavedMenu by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.SubdirectoryArrowRight, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp).padding(end = 4.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).combinedClickable(
                                    onClick = {
                                        val target = video.pageUrl?.takeIf { it.isNotBlank() } ?: video.headers?.get("Referer")?.takeIf { it.isNotBlank() } ?: video.url
                                        onOpenWeb(target)
                                    },
                                    onLongClick = { actionItem = pl.id to video }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(video.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(11.dp))
                                            val posText = if (video.lastPositionMs > 0L) ShareUtils.formatTime(video.lastPositionMs) else "0:00"
                                            Text(posText, color = Color(0xFF10B981), fontSize = 10.sp)
                                        }
                                    }
                                    Box {
                                        IconButton(onClick = { showSavedMenu = true }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Rounded.MoreVert, contentDescription = "Opciones", tint = Color(0xFF94A3B8), modifier = Modifier.size(17.dp))
                                        }
                                        DropdownMenu(
                                            expanded = showSavedMenu,
                                            onDismissRequest = { showSavedMenu = false },
                                            modifier = Modifier.background(Color(0xFF1E293B))
                                        ) {
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(Icons.Rounded.Tv, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(17.dp)) },
                                                text = { Text("Proyectar en TV", color = Color.White, fontSize = 13.sp) },
                                                onClick = { showSavedMenu = false; onPlay(video.url, video.title, video.headers ?: emptyMap(), video.lastPositionMs) }
                                            )
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(Icons.Rounded.Language, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(17.dp)) },
                                                text = { Text("Abrir en Web", color = Color.White, fontSize = 13.sp) },
                                                onClick = { showSavedMenu = false; onOpenWeb(video.pageUrl ?: video.url) }
                                            )
                                            if (ShareUtils.canShare(video.url, video.pageUrl)) {
                                                DropdownMenuItem(
                                                    leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(17.dp)) },
                                                    text = { Text("Compartir enlace", color = Color.White, fontSize = 13.sp) },
                                                    onClick = {
                                                        showSavedMenu = false
                                                        ShareUtils.shareVideo(context, video.title, video.url, video.pageUrl)
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(Icons.Rounded.FileDownload, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(17.dp)) },
                                                text = { Text("Descargar película", color = Color(0xFF10B981), fontSize = 13.sp) },
                                                onClick = {
                                                    showSavedMenu = false
                                                    MediaDownloadHelper.downloadMedia(context, video.url, video.title, video.headers ?: emptyMap())
                                                }
                                            )
                                            HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp)) },
                                                text = { Text("Eliminar de guardados", color = Color(0xFFEF4444), fontSize = 13.sp) },
                                                onClick = { showSavedMenu = false; onRemoveItem(pl.id, video.url) }
                                            )
                                        }
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
