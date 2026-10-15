package com.omnicast.sender.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import com.omnicast.sender.ui.utils.ShareUtils

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
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf(VideoSortOrder.NEWEST) }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("📺 Mis Videos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Historial y Guardadas", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
            IconButton(onClick = onClose) { Text("✕", color = Color(0xFF94A3B8), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(10.dp))
        TabRow(selectedTabIndex = selectedTab, containerColor = Color(0xFF1E293B), contentColor = Color(0xFF38BDF8)) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0; historyList = historyManager.getHistory() }, text = { Text("🕒 Historial (" + historyList.size + ")", fontSize = 12.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1; playlists = playlistManager.getPlaylists() }, text = { Text("⭐ Guardadas (" + playlists.sumOf { it.items.size } + " pelis)", fontSize = 11.sp) })
        }
        Spacer(modifier = Modifier.height(8.dp))

        VideoSortFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedSort = selectedSort,
            onSortChange = { selectedSort = it },
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (selectedTab == 0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Vistos recientemente", fontSize = 11.sp, color = Color(0xFF64748B))
                if (historyList.isNotEmpty()) {
                    TextButton(onClick = { historyManager.clear(keepFavorites = false); historyList = emptyList() }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("🗑 Limpiar", fontSize = 11.sp, color = Color(0xFFEF4444))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            DrawerHistoryList(
                historyList = historyList,
                searchQuery = searchQuery,
                selectedSort = selectedSort,
                onPlay = { item -> onPlayOnTv(item.url, item.title, item.headers ?: emptyMap(), item.lastPositionMs); onClose() },
                onOpenWeb = { url -> onOpenInBrowser(url); onClose() },
                onDelete = { url -> historyManager.remove(url); historyList = historyManager.getHistory() }
            )
        } else {
            DrawerSavedPlaylistsList(
                playlists = playlists,
                searchQuery = searchQuery,
                selectedSort = selectedSort,
                onPlay = { url, title, hdrs, pos -> onPlayOnTv(url, title, hdrs, pos); onClose() },
                onOpenWeb = { url -> onOpenInBrowser(url); onClose() },
                onRemoveItem = { plId, u -> playlistManager.removeItemFromPlaylist(plId, u); playlists = playlistManager.getPlaylists() }
            )
        }
    }
}
