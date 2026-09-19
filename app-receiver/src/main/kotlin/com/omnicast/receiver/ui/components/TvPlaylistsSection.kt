package com.omnicast.receiver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.receiver.directstream.TvPlaylist
import com.omnicast.receiver.directstream.TvPlaylistItem

@Composable
fun TvPlaylistsSection(
    playlists: List<TvPlaylist>,
    onPlayItem: (TvPlaylistItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePlaylists = remember(playlists) { playlists.filter { it.items.isNotEmpty() } }
    if (activePlaylists.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📁 Listas y Carpetas del Móvil", color = Color(0xFF38BDF8), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("• Sincronizadas", color = Color(0xFF94A3B8), fontSize = 12.sp)
        }

        activePlaylists.forEach { playlist ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${playlist.name} (${playlist.items.size})",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(playlist.items, key = { it.url + it.title }) { item ->
                        TvPlaylistVideoCard(item = item, onClick = { onPlayItem(item) })
                    }
                }
            }
        }
    }
}

@Composable
fun TvPlaylistVideoCard(
    item: TvPlaylistItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF1E3A8A) else Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .width(180.dp)
            .height(78.dp)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color(0xFF38BDF8) else Color(0xFF334155),
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(if (item.isFavorite) "⭐" else "🎬", fontSize = 12.sp)
                Text(
                    text = item.title.ifBlank { "Video Web" },
                    color = if (isFocused) Color.White else Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isFocused) "▶ Presiona OK" else "Guardado",
                    color = if (isFocused) Color(0xFF7DD3FC) else Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
