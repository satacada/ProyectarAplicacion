package com.omnicast.receiver.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
    onDeletePlaylist: (TvPlaylistItem) -> Unit = {},
    updateState: com.omnicast.receiver.update.TvUpdateState = com.omnicast.receiver.update.TvUpdateState.Idle,
    onCheckUpdate: () -> Unit = {},
    onDownloadAndInstall: (String) -> Unit = {},
    expiredItemForRenew: TvPlaylistItem? = null,
    onClearExpiredRenew: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(1) } // 0: Recientes, 1: Guardados, 2: Favoritos
    var enteredPlaylistId by remember { mutableStateOf<String?>(null) }
    var menuItem by remember { mutableStateOf<TvPlaylistItem?>(null) }
    var sharingItem by remember { mutableStateOf<TvPlaylistItem?>(null) }
    var renewItem by remember { mutableStateOf<TvPlaylistItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf(TvVideoSortOrder.NEWEST) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(enteredPlaylistId) {
        if (enteredPlaylistId != null) {
            kotlinx.coroutines.delay(120)
            runCatching { firstItemFocusRequester.requestFocus() }
        }
    }

    // Regreso en 1 solo clic al nivel de carpetas desde el video 60
    BackHandler(enabled = enteredPlaylistId != null) {
        enteredPlaylistId = null
        searchQuery = ""
    }

    LaunchedEffect(expiredItemForRenew) {
        if (expiredItemForRenew != null) {
            renewItem = expiredItemForRenew
        }
    }

    val activePlaylists = remember(playlists) {
        if (playlists.isEmpty()) {
            listOf(
                TvPlaylist("from_website", "Ver más tarde", emptyList(), false, System.currentTimeMillis()),
                TvPlaylist("favoritos", "⭐ Favoritos", emptyList(), true, System.currentTimeMillis())
            )
        } else {
            playlists.map {
                if (it.name == "FromWebSite") it.copy(name = "Ver más tarde") else it
            }
        }
    }

    val currentPlaylist = remember(activePlaylists, enteredPlaylistId) {
        activePlaylists.find { it.id == enteredPlaylistId }
    }

    val playlistFilteredItems = remember(currentPlaylist, searchQuery, selectedSort) {
        currentPlaylist?.let { filterAndSortTvPlaylistItems(it.items, searchQuery, selectedSort) } ?: emptyList()
    }

    val filteredHistory = remember(history, searchQuery, selectedSort) {
        filterAndSortTvHistory(history, searchQuery, selectedSort)
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
            if (enteredPlaylistId == null) {
                TvCleanTopBar(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it; searchQuery = "" },
                    updateState = updateState,
                    onCheckUpdate = onCheckUpdate,
                    onDownloadAndInstall = onDownloadAndInstall
                )
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                // Cabecera interna de la carpeta con botón de retorno y buscador superior exclusivo
                TvFolderDetailHeader(
                    folderName = currentPlaylist?.name ?: "Carpeta",
                    itemCount = currentPlaylist?.items?.size ?: 0,
                    onBack = { enteredPlaylistId = null; searchQuery = "" }
                )
                Spacer(modifier = Modifier.height(10.dp))
                TvSortFilterBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedSort = selectedSort,
                    onSortChange = { selectedSort = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    if (enteredPlaylistId != null) {
                        // Vista detallada de películas dentro de la carpeta
                        if (playlistFilteredItems.isEmpty()) {
                            TvEmptyState(if (searchQuery.isNotBlank()) "No se encontraron videos para \"$searchQuery\"" else "Esta carpeta no contiene películas.")
                        } else {
                            LazyColumn(
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(playlistFilteredItems, key = { _, it -> it.url }) { index, item ->
                                    TvPlaylistItemRow(
                                        item = item,
                                        onPlay = { onPlayPlaylist(item) },
                                        onMenu = { menuItem = item },
                                        modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier
                                    )
                                }
                            }
                        }
                    } else {
                        // Vista raíz: Solo las 2 carpetas principales o historial
                        when (selectedTab) {
                            0 -> {
                                if (filteredHistory.isEmpty()) {
                                    TvEmptyState("No hay videos reproducidos recientemente.")
                                } else {
                                    LazyColumn(
                                        state = listState,
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(filteredHistory, key = { it.id }) { item ->
                                            TvRecentItemRow(item = item, onClick = { onPlayHistory(item) })
                                        }
                                    }
                                }
                            }
                            1 -> {
                                LazyColumn(
                                    state = listState,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(activePlaylists, key = { it.id }) { pl ->
                                        TvFolderRow(
                                            name = pl.name,
                                            date = formatFolderDate(pl.createdAt),
                                            itemCount = pl.items.size,
                                            onClick = { enteredPlaylistId = pl.id; searchQuery = "" }
                                        )
                                    }
                                }
                            }
                            2 -> {
                                val favs = activePlaylists.filter { it.isDefault || it.name.contains("Favorito", ignoreCase = true) }
                                LazyColumn(
                                    state = listState,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(favs, key = { it.id }) { pl ->
                                        TvFolderRow(
                                            name = pl.name,
                                            date = formatFolderDate(pl.createdAt),
                                            itemCount = pl.items.size,
                                            onClick = { enteredPlaylistId = pl.id; searchQuery = "" }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                TvVerticalScrollbar(lazyListState = listState)
            }

            Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Surface(
                    color = Color(0xFF251C35),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        if (enteredPlaylistId != null) "Pulsa [Atrás] en el control para volver a las carpetas" else "¿No encuentras tus archivos? Actualiza o envía desde tu celular",
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

        menuItem?.let { item ->
            TvVideoActionDialog(
                item = item,
                onPlay = { onPlayPlaylist(item) },
                onShare = { sharingItem = item },
                onRenew = { renewItem = item },
                onDelete = { onDeletePlaylist(item) },
                onDismiss = { menuItem = null }
            )
        }

        sharingItem?.let { item ->
            TvShareDialog(item = item, onDismiss = { sharingItem = null })
        }

        renewItem?.let { item ->
            TvRenewQrDialog(
                item = item,
                onDismiss = {
                    renewItem = null
                    onClearExpiredRenew()
                }
            )
        }
    }
}

@Composable
private fun TvFolderDetailHeader(folderName: String, itemCount: Int, onBack: () -> Unit) {
    var isBackFocused by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = if (isBackFocused) Color(0xFF38BDF8) else Color(0xFF251C35)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.onFocusChanged { isBackFocused = it.isFocused }.focusable().clickable(onClick = onBack)
                .border(1.dp, if (isBackFocused) Color.White else Color(0xFF38BDF8), RoundedCornerShape(8.dp))
        ) {
            Text("◀ Volver a carpetas", color = if (isBackFocused) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
        }
        Text("📂 $folderName ($itemCount película${if (itemCount != 1) "s" else ""})", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TvEmptyState(msg: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg, color = Color(0xFF64748B), fontSize = 14.sp)
    }
}
