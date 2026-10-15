package com.omnicast.sender.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.sender.history.HistoryItem
import com.omnicast.sender.playlist.Playlist
import com.omnicast.sender.playlist.PlaylistManager
import com.omnicast.sender.ui.components.VerticalScrollbar

/**
 * Pantalla modal para explorar y administrar Listas de Reproducción y Marcadores Guardados.
 * Permite reproducir cualquier video directamente en la TV Box con 1 toque.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPlaylistsSheet(
    playlistManager: PlaylistManager,
    onSelectAndTransmit: (HistoryItem) -> Unit,
    onDismiss: () -> Unit
) {
    var playlists by remember { mutableStateOf(playlistManager.getPlaylists()) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    val activePlaylist = playlists.firstOrNull { it.id == selectedPlaylistId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Cabecera superior
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (activePlaylist != null) {
                    TextButton(onClick = { selectedPlaylistId = null }) {
                        Text("◀ Volver a Listas", color = Color(0xFF38BDF8), fontSize = 13.sp)
                    }
                    Text(
                        text = activePlaylist.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text("📂 Listas Guardadas", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { isCreatingNew = !isCreatingNew }) {
                        Text(
                            text = if (isCreatingNew) "Cancelar" else "+ Nueva Lista",
                            color = Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (isCreatingNew && activePlaylist == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        placeholder = { Text("Nombre de la lista...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )
                    Button(
                        onClick = {
                            val created = playlistManager.createPlaylist(newListName)
                            if (created != null) {
                                playlists = playlistManager.getPlaylists()
                                newListName = ""
                                isCreatingNew = false
                            }
                        },
                        enabled = newListName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Crear", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Vista de Detalle de Lista o Vista de Carpetas
            if (activePlaylist != null) {
                PlaylistDetailView(
                    playlist = activePlaylist,
                    onTransmit = {
                        onSelectAndTransmit(it)
                        onDismiss()
                    },
                    onRemoveItem = { itemUrl ->
                        playlistManager.removeItemFromPlaylist(activePlaylist.id, itemUrl)
                        playlists = playlistManager.getPlaylists()
                    }
                )
            } else {
                PlaylistFolderListView(
                    playlists = playlists,
                    onOpenPlaylist = { selectedPlaylistId = it.id },
                    onDeletePlaylist = { plId ->
                        playlistManager.deletePlaylist(plId)
                        playlists = playlistManager.getPlaylists()
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaylistFolderListView(
    playlists: List<Playlist>,
    onOpenPlaylist: (Playlist) -> Unit,
    onDeletePlaylist: (String) -> Unit
) {
    val folderListState = rememberLazyListState()

    if (playlists.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No hay listas creadas aún.", color = Color(0xFF94A3B8), fontSize = 13.sp)
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            LazyColumn(
                state = folderListState,
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) {
                items(playlists, key = { it.id }) { pl ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onOpenPlaylist(pl) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (pl.isDefault) "📁" else "🎬", fontSize = 22.sp)
                                Column {
                                    Text(pl.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${pl.items.size} película${if (pl.items.size != 1) "s" else ""} guardada${if (pl.items.size != 1) "s" else ""}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (!pl.isDefault) {
                                IconButton(onClick = { onDeletePlaylist(pl.id) }, modifier = Modifier.size(28.dp)) {
                                    Text("🗑️", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
            VerticalScrollbar(lazyListState = folderListState)
        }
    }
}
