package com.omnicast.sender.ui.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.omnicast.sender.ui.components.SavedVideoActionSheet
import com.omnicast.sender.ui.components.VerticalScrollbar
import com.omnicast.sender.ui.components.VideoSortFilterBar
import com.omnicast.sender.ui.components.VideoSortOrder
import com.omnicast.sender.ui.components.filterAndSortHistoryItems
import com.omnicast.sender.ui.utils.ShareUtils

/**
 * Vista de detalle de una lista de reproducción con búsqueda, ordenamiento y scroll lateral.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    onTransmit: (HistoryItem) -> Unit,
    onRemoveItem: (String) -> Unit
) {
    val context = LocalContext.current
    var actionItem by remember { mutableStateOf<HistoryItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf(VideoSortOrder.NEWEST) }
    val listState = rememberLazyListState()

    if (playlist.items.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Esta lista está vacía. Guarda videos desde el explorador.", color = Color(0xFF94A3B8), fontSize = 13.sp)
        }
        return
    }

    val displayedItems = remember(playlist.items, searchQuery, selectedSort) {
        filterAndSortHistoryItems(playlist.items, searchQuery, selectedSort)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        VideoSortFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedSort = selectedSort,
            onSortChange = { selectedSort = it },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (displayedItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                Text("No se encontraron videos para \"$searchQuery\"", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                ) {
                    items(displayedItems, key = { it.url }) { item ->
                        SavedPlaylistItemCard(
                            item = item,
                            onPlay = { onTransmit(item) },
                            onMenu = { actionItem = item },
                            onShare = { ShareUtils.shareVideo(context, item.title, item.url, item.pageUrl) },
                            onRemove = { onRemoveItem(item.url) }
                        )
                    }
                }
                VerticalScrollbar(lazyListState = listState)
            }
        }
    }

    actionItem?.let { itm ->
        SavedVideoActionSheet(
            item = itm,
            onPlay = { onTransmit(itm) },
            onShare = { ShareUtils.shareVideo(context, itm.title, itm.url, itm.pageUrl) },
            onDelete = { onRemoveItem(itm.url) },
            onDismiss = { actionItem = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedPlaylistItemCard(
    item: HistoryItem,
    onPlay: () -> Unit,
    onMenu: () -> Unit,
    onShare: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onMenu
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title + (if (item.requiresProxy) " 🔒" else ""),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val posText = if (item.lastPositionMs >= 5000L) {
                    val m = (item.lastPositionMs / 1000) / 60
                    val s = (item.lastPositionMs / 1000) % 60
                    "▶ Reanudar en %02d:%02d • ".format(m, s)
                } else ""
                val proxyNotice = if (item.requiresProxy) "🔒 Requiere emisor • " else ""
                Text(
                    text = proxyNotice + posText + item.url,
                    color = if (item.requiresProxy) Color(0xFFF97316) else if (item.lastPositionMs >= 5000L) Color(0xFF38BDF8) else Color(0xFF64748B),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (ShareUtils.canShare(item.url, item.pageUrl)) {
                    IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                        Text("🔗", fontSize = 13.sp)
                    }
                }
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("▶ TV", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Text("✕", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }
        }
    }
}
