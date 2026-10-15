package com.omnicast.receiver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.receiver.directstream.TvHistoryItem
import com.omnicast.receiver.directstream.TvPlaylist
import com.omnicast.receiver.directstream.TvPlaylistItem

@Composable
fun TvStandbyCleanScreen(
    ipAddress: String,
    history: List<TvHistoryItem> = emptyList(),
    playlists: List<TvPlaylist> = emptyList(),
    onPlayHistory: (TvHistoryItem) -> Unit = {},
    onPlayPlaylist: (TvPlaylistItem) -> Unit = {},
    updateState: com.omnicast.receiver.update.TvUpdateState = com.omnicast.receiver.update.TvUpdateState.Idle,
    onCheckUpdate: () -> Unit = {},
    onDownloadAndInstall: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(1) } // 0: Recientes, 1: Todo, 2: Favoritos
    var expandedPlaylistId by remember { mutableStateOf<String?>(null) }
    var sharingItem by remember { mutableStateOf<TvPlaylistItem?>(null) }

    val activePlaylists = remember(playlists) {
        if (playlists.isEmpty()) {
            listOf(
                TvPlaylist("from_website", "FromWebSite", emptyList(), false, System.currentTimeMillis()),
                TvPlaylist("favoritos", "⭐ Favoritos", emptyList(), true, System.currentTimeMillis())
            )
        } else {
            playlists
        }
    }

    val displayCount = when (selectedTab) {
        0 -> history.size
        1 -> activePlaylists.size
        2 -> activePlaylists.filter { it.isDefault || it.name.contains("Favorito", ignoreCase = true) }.size
        else -> 0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13101E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp, start = 36.dp, end = 36.dp, bottom = 80.dp)
        ) {
            TvCleanTopBar(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                updateState = updateState,
                onCheckUpdate = onCheckUpdate,
                onDownloadAndInstall = onDownloadAndInstall
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "$displayCount elemento${if (displayCount != 1) "s" else ""}",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    0 -> {
                        if (history.isEmpty()) {
                            TvEmptyState("No hay videos reproducidos recientemente.")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(history, key = { it.id }) { item ->
                                    TvRecentItemRow(item = item, onClick = { onPlayHistory(item) })
                                }
                            }
                        }
                    }
                    1 -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(activePlaylists, key = { it.id }) { pl ->
                                TvFolderRow(
                                    name = pl.name,
                                    date = formatFolderDate(pl.createdAt),
                                    itemCount = pl.items.size,
                                    isExpanded = expandedPlaylistId == pl.id,
                                    items = pl.items,
                                    onClick = {
                                        expandedPlaylistId = if (expandedPlaylistId == pl.id) null else pl.id
                                    },
                                    onPlayItem = onPlayPlaylist,
                                    onShareItem = { sharingItem = it }
                                )
                            }
                        }
                    }
                    2 -> {
                        val favs = activePlaylists.filter { it.isDefault || it.name.contains("Favorito", ignoreCase = true) }
                        if (favs.isEmpty() || favs.all { it.items.isEmpty() }) {
                            TvEmptyState("No tienes carpetas ni videos en Favoritos.")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(favs, key = { it.id }) { pl ->
                                    TvFolderRow(
                                        name = pl.name,
                                        date = formatFolderDate(pl.createdAt),
                                        itemCount = pl.items.size,
                                        isExpanded = expandedPlaylistId == pl.id,
                                        items = pl.items,
                                        onClick = {
                                            expandedPlaylistId = if (expandedPlaylistId == pl.id) null else pl.id
                                        },
                                        onPlayItem = onPlayPlaylist,
                                        onShareItem = { sharingItem = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Surface(
                    color = Color(0xFF251C35),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        "¿No encuentras tus archivos? Actualiza o envía desde tu celular",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                    )
                }
            }
        }

        TvBottomConnectionDock(
            ipAddress = ipAddress,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        sharingItem?.let { item ->
            TvShareDialog(item = item, onDismiss = { sharingItem = null })
        }
    }
}

@Composable
private fun TvEmptyState(msg: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(msg, color = Color(0xFF64748B), fontSize = 14.sp)
    }
}
