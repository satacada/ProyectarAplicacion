package com.omnicast.sender.ui.browser

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.sender.history.HistoryItem
import com.omnicast.sender.playlist.PlaylistManager

/**
 * Diálogo modal para guardar el video actual en listas de reproducción temáticas.
 * Permite seleccionar listas existentes o crear una nueva lista con nombre personalizado al instante.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveToPlaylistSheet(
    videoItem: HistoryItem,
    playlistManager: PlaylistManager,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    var playlists by remember { mutableStateOf(playlistManager.getPlaylists()) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "💾 Guardar en Lista",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { isCreatingNew = !isCreatingNew }) {
                    Text(
                        text = if (isCreatingNew) "Cancelar" else "+ Nueva Lista",
                        color = Color(0xFF38BDF8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = videoItem.title.ifBlank { "Video actual" },
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (isCreatingNew) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        placeholder = { Text("Nombre de la lista (ej: Películas 2026)", fontSize = 12.sp, color = Color(0xFF64748B)) },
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
                                playlistManager.addItemToPlaylist(created.id, videoItem)
                                onSaved(created.name)
                                onDismiss()
                            }
                        },
                        enabled = newListName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Crear", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                items(playlists, key = { it.id }) { pl ->
                    val isChecked = playlistManager.isVideoInPlaylist(pl.id, videoItem)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) Color(0xFF1E293B) else Color(0xFF131D31)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                val added = playlistManager.toggleVideoInPlaylist(pl.id, videoItem)
                                playlists = playlistManager.getPlaylists()
                                if (added) {
                                    onSaved(pl.name)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pl.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${pl.items.size} videos guardados",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            }
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    val added = playlistManager.toggleVideoInPlaylist(pl.id, videoItem)
                                    playlists = playlistManager.getPlaylists()
                                    if (added) {
                                        onSaved(pl.name)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF10B981),
                                    checkmarkColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
