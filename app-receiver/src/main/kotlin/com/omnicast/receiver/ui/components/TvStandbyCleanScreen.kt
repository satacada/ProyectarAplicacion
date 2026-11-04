package com.omnicast.receiver.ui.components

import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
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
import com.omnicast.receiver.directstream.TvDownloadedVideo
import com.omnicast.receiver.directstream.TvDownloadsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TvStandbyCleanScreen(
    ipAddress: String,
    connectedClientIp: String? = null,
    history: List<TvHistoryItem> = emptyList(),
    playlists: List<TvPlaylist> = emptyList(),
    onPlayHistory: (TvHistoryItem) -> Unit = {},
    onPlayLocalVideo: (String, String) -> Unit = { _, _ -> },
    onSaveMovieToFolder: (String, String, Long, Long, String?, Map<String, String>, String) -> Unit = { _, _, _, _, _, _, _ -> },
    isMovieSaved: (String, String) -> Boolean = { _, _ -> false },
    onPlayPlaylist: (TvPlaylistItem) -> Unit = {},
    onDeletePlaylist: (TvPlaylistItem) -> Unit = {},
    onDeleteMultiple: (Set<String>) -> Unit = {},
    updateState: com.omnicast.receiver.update.TvUpdateState = com.omnicast.receiver.update.TvUpdateState.Idle,
    onCheckUpdate: () -> Unit = {},
    onDownloadAndInstall: (String) -> Unit = {},
    expiredItemForRenew: TvPlaylistItem? = null,
    onClearExpiredRenew: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(1) } // 0: Recientes, 1: Guardados, 2: Descargas
    var enteredPlaylistId by remember { mutableStateOf<String?>(null) }
    var menuItem by remember { mutableStateOf<TvPlaylistItem?>(null) }
    var sharingItem by remember { mutableStateOf<TvPlaylistItem?>(null) }
    var renewItem by remember { mutableStateOf<TvPlaylistItem?>(null) }
    var downloadingItem by remember { mutableStateOf<TvPlaylistItem?>(null) }
    var savingDialogItem by remember { mutableStateOf<TvPlaylistItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showTvLangDialog by remember { mutableStateOf(false) }
    var selectedSort by remember { mutableStateOf(TvVideoSortOrder.NEWEST) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val firstItemFocusRequester = remember { FocusRequester() }
    val backButtonFocusRequester = remember { FocusRequester() }
    val selectedUrls = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    val isPhoneConnected = !connectedClientIp.isNullOrBlank()
    val context = androidx.compose.ui.platform.LocalContext.current
    var downloadedVideos by remember { mutableStateOf<List<TvDownloadedVideo>>(emptyList()) }
    var localActionVideo by remember { mutableStateOf<TvDownloadedVideo?>(null) }
    var refreshDownloadsTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab, refreshDownloadsTrigger) {
        withContext(Dispatchers.IO) {
            val list = TvDownloadsManager.getDownloadedVideos(context)
            withContext(Dispatchers.Main) { downloadedVideos = list }
        }
    }

    LaunchedEffect(enteredPlaylistId) {
        if (enteredPlaylistId != null) {
            kotlinx.coroutines.delay(120)
            runCatching { firstItemFocusRequester.requestFocus() }
        }
    }

    BackHandler(enabled = enteredPlaylistId != null) {
        enteredPlaylistId = null; searchQuery = ""; selectedUrls.clear()
    }

    LaunchedEffect(expiredItemForRenew) {
        if (expiredItemForRenew != null) {
            renewItem = expiredItemForRenew
        }
    }

    val activePlaylists = remember(playlists) {
        if (playlists.isEmpty()) listOf(TvPlaylist("from_website", "Ver más tarde", emptyList(), false, System.currentTimeMillis()), TvPlaylist("favoritos", "⭐ Favoritos", emptyList(), true, System.currentTimeMillis()))
        else playlists.map { if (it.name == "FromWebSite") it.copy(name = "Ver más tarde") else it }
    }
    val currentPlaylist = remember(activePlaylists, enteredPlaylistId) { activePlaylists.find { it.id == enteredPlaylistId } }
    val playlistFilteredItems = remember(currentPlaylist, searchQuery, selectedSort) { currentPlaylist?.let { filterAndSortTvPlaylistItems(it.items, searchQuery, selectedSort) } ?: emptyList() }
    val filteredHistory = remember(history, searchQuery, selectedSort) { filterAndSortTvHistory(history, searchQuery, selectedSort) }
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
                    onDownloadAndInstall = onDownloadAndInstall,
                    onOpenLanguage = { showTvLangDialog = true }
                )
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                TvFolderDetailHeader(
                    folderName = currentPlaylist?.name ?: "Carpeta",
                    itemCount = currentPlaylist?.items?.size ?: 0,
                    onBack = { enteredPlaylistId = null; searchQuery = ""; selectedUrls.clear() },
                    modifier = Modifier.focusRequester(backButtonFocusRequester)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvSortFilterBar(
                        searchQuery = searchQuery, onSearchQueryChange = { searchQuery = it },
                        selectedSort = selectedSort, onSortChange = { selectedSort = it },
                        modifier = Modifier.weight(1f)
                    )
                    var isTrashFocused by remember { mutableStateOf(false) }
                    val hasSel = selectedUrls.isNotEmpty()
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isTrashFocused) (if (hasSel) Color(0xFFEF4444) else Color(0xFF38BDF8)) else (if (hasSel) Color(0xFF7F1D1D) else Color(0xFF251C35))),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.onFocusChanged { isTrashFocused = it.isFocused }.focusable()
                            .clickable {
                                if (hasSel) {
                                    val count = selectedUrls.size; onDeleteMultiple(selectedUrls.toSet()); selectedUrls.clear()
                                    scope.launch {
                                        kotlinx.coroutines.delay(120)
                                        if ((currentPlaylist?.items?.size ?: 0) > count) runCatching { firstItemFocusRequester.requestFocus() } else runCatching { backButtonFocusRequester.requestFocus() }
                                    }
                                }
                            }
                            .border(1.dp, if (isTrashFocused) Color.White else (if (hasSel) Color(0xFFEF4444) else Color(0xFF3E3255)), RoundedCornerShape(8.dp))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🗑", fontSize = 15.sp)
                            if (hasSel) Text("(${selectedUrls.size})", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    if (enteredPlaylistId != null) {
                        if (playlistFilteredItems.isEmpty()) {
                            TvEmptyState(if (searchQuery.isNotBlank()) "No se encontraron videos para \"$searchQuery\"" else "Esta carpeta no contiene películas.")
                        } else {
                            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(playlistFilteredItems, key = { _, it -> it.url }) { index, item ->
                                    val isSel = selectedUrls.contains(item.url)
                                    val dl = remember(item.title, downloadedVideos) { TvDownloadsManager.findDownloadedVideo(item.title, downloadedVideos) }
                                    TvPlaylistItemRow(
                                        item = item, isPhoneConnected = isPhoneConnected, downloadedVideo = dl,
                                        onPlay = { onPlayPlaylist(item) }, onMenu = { menuItem = item },
                                        onSaveToFolder = { savingDialogItem = item },
                                        onRemoveSaved = { onDeletePlaylist(item) },
                                        onDownload = { downloadingItem = item },
                                        onLocalAction = { localActionVideo = dl },
                                        isSelected = isSel, onToggleSelect = { if (isSel) selectedUrls.remove(item.url) else selectedUrls.add(item.url) },
                                        modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier
                                    )
                                }
                            }
                        }
                    } else {
                        when (selectedTab) {
                            0 -> {
                                if (filteredHistory.isEmpty()) {
                                    TvEmptyState("No hay videos reproducidos recientemente en el historial.")
                                } else {
                                    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                                        items(filteredHistory, key = { it.id }) { item ->
                                            val saved = isMovieSaved(item.mediaUrl, item.title)
                                            val asPlItem = TvPlaylistItem(
                                                url = item.mediaUrl, title = item.title,
                                                pageUrl = item.pageUrl ?: item.headers["X-Source-Page-Url"] ?: item.headers["Referer"],
                                                headers = item.headers, lastPositionMs = item.lastPositionMs, durationMs = item.durationMs
                                            )
                                            val dl = remember(item.title, downloadedVideos) { TvDownloadsManager.findDownloadedVideo(item.title, downloadedVideos) }
                                            TvRecentItemRow(
                                                item = item, isSaved = saved, downloadedVideo = dl, isPhoneConnected = isPhoneConnected,
                                                onClick = { onPlayHistory(item) },
                                                onToggleSave = { if (saved) onDeletePlaylist(asPlItem) else savingDialogItem = asPlItem },
                                                onDownload = { downloadingItem = asPlItem },
                                                onLocalAction = { localActionVideo = dl }
                                            )
                                        }
                                    }
                                }
                            }
                            1 -> {
                                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                                    items(activePlaylists, key = { it.id }) { pl ->
                                        TvFolderRow(name = pl.name, date = formatFolderDate(pl.createdAt), itemCount = pl.items.size, onClick = { enteredPlaylistId = pl.id; searchQuery = ""; selectedUrls.clear() })
                                    }
                                }
                            }
                            2 -> {
                                if (downloadedVideos.isEmpty()) {
                                    TvEmptyState("No hay videos descargados en el TV Box ni en la memoria USB.")
                                } else {
                                    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                                        items(downloadedVideos, key = { it.filePath }) { video ->
                                            TvDownloadItemRow(
                                                video = video,
                                                onPlay = { onPlayLocalVideo(video.filePath, video.title) },
                                                onDelete = { localActionVideo = video }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                TvVerticalScrollbar(lazyListState = listState)
            }

            Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Surface(color = Color(0xFF251C35), shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        if (enteredPlaylistId != null) "Pulsa [Atrás] en el control para volver a las carpetas" else if (selectedTab == 2) "Películas descargadas localmente • Disponibles 100% offline" else "¿No encuentras tus archivos? Revisa Recientes (Historial) o proyecta desde tu celular",
                        color = Color(0xFF38BDF8), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                    )
                }
            }
        }

        TvBottomConnectionDock(ipAddress = ipAddress, connectedClientIp = connectedClientIp, modifier = Modifier.align(Alignment.BottomCenter))

        menuItem?.let { item ->
            TvVideoActionDialog(
                item = item, onPlay = { onPlayPlaylist(item) }, onShare = { sharingItem = item }, onRenew = { renewItem = item },
                onDownload = { downloadingItem = item },
                onDelete = {
                    onDeletePlaylist(item); menuItem = null
                    scope.launch {
                        kotlinx.coroutines.delay(120)
                        if ((currentPlaylist?.items?.size ?: 0) > 1) runCatching { firstItemFocusRequester.requestFocus() } else runCatching { backButtonFocusRequester.requestFocus() }
                    }
                },
                onDismiss = { menuItem = null }
            )
        }

        savingDialogItem?.let { item ->
            TvSaveToFolderDialog(
                movieTitle = item.title,
                onSaveToFavorites = { onSaveMovieToFolder(item.title, item.url, item.lastPositionMs, item.durationMs, item.pageUrl, item.headers, "favoritos") },
                onSaveToWatchLater = { onSaveMovieToFolder(item.title, item.url, item.lastPositionMs, item.durationMs, item.pageUrl, item.headers, "from_website") },
                onDismiss = { savingDialogItem = null }
            )
        }
        sharingItem?.let { item -> TvShareDialog(item = item, onDismiss = { sharingItem = null }) }
        renewItem?.let { item -> TvRenewQrDialog(item = item, onDismiss = { renewItem = null; onClearExpiredRenew() }) }
        downloadingItem?.let { item -> TvDownloadMovieDialog(item = item, onDismiss = { downloadingItem = null }) }
        localActionVideo?.let { video ->
            TvLocalVideoActionDialog(
                video = video,
                onPlay = { v -> onPlayLocalVideo(v.filePath, v.title) },
                onDelete = { v ->
                    if (TvDownloadsManager.deleteDownloadedVideo(v.filePath)) {
                        android.widget.Toast.makeText(context, "🗑️ Video eliminado del disco", android.widget.Toast.LENGTH_SHORT).show()
                        refreshDownloadsTrigger++
                    } else android.widget.Toast.makeText(context, "⚠️ No se pudo eliminar el archivo", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDismiss = { localActionVideo = null }
            )
        }
        if (showTvLangDialog) TvLanguageDialog(onDismiss = { showTvLangDialog = false }, onLanguageSelected = { showTvLangDialog = false })
    }
}
