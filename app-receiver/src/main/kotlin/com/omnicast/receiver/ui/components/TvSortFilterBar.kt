package com.omnicast.receiver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
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

enum class TvVideoSortOrder(val label: String, val icon: String) {
    NEWEST("Recientes", "📅"),
    NAME_AZ("A - Z", "🔤"),
    IN_PROGRESS("En progreso", "▶"),
    OLDEST("Antiguos", "⏳")
}

@Composable
fun TvSortFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedSort: TvVideoSortOrder,
    onSortChange: (TvVideoSortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchFocused by remember { mutableStateOf(false) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(isSearchFocused) {
        if (isSearchFocused) keyboardController?.show()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Campo de búsqueda D-Pad sin cortes de texto
        BasicTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF38BDF8)),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .onFocusChanged { isSearchFocused = it.isFocused }
                .background(Color(0xFF1E182A), RoundedCornerShape(10.dp))
                .border(
                    width = if (isSearchFocused) 2.dp else 1.dp,
                    color = if (isSearchFocused) Color(0xFF38BDF8) else Color(0xFF2D223F),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 12.dp),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔍 ", fontSize = 13.sp)
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Buscar en TV...",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        innerTextField()
                    }
                    if (searchQuery.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onSearchQueryChange("") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                }
            }
        )

        // Botones de orden para control remoto D-Pad
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvVideoSortOrder.values().forEach { sort ->
                TvSortButton(
                    order = sort,
                    isSelected = sort == selectedSort,
                    onClick = { onSortChange(sort) }
                )
            }
        }
    }
}

@Composable
private fun TvSortButton(
    order: TvVideoSortOrder,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        color = when {
            isFocused -> Color(0xFF38BDF8)
            isSelected -> Color(0xFF2E2442)
            else -> Color(0xFF1E182A)
        },
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected || isFocused) 1.5.dp else 1.dp,
                color = when {
                    isFocused -> Color(0xFF38BDF8)
                    isSelected -> Color(0xFF818CF8)
                    else -> Color(0xFF2D223F)
                },
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(order.icon, fontSize = 12.sp)
            Text(
                text = order.label,
                color = if (isFocused) Color(0xFF0D0B14) else (if (isSelected) Color(0xFF38BDF8) else Color(0xFFE2E8F0)),
                fontSize = 12.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

fun filterAndSortTvPlaylists(
    playlists: List<TvPlaylist>,
    query: String,
    sort: TvVideoSortOrder
): List<TvPlaylist> {
    val q = query.trim().lowercase()
    return playlists.mapNotNull { pl ->
        val filteredItems = if (q.isBlank()) pl.items else pl.items.filter {
            it.title.lowercase().contains(q) || it.url.lowercase().contains(q)
        }
        val sortedItems = when (sort) {
            TvVideoSortOrder.NEWEST -> filteredItems.sortedByDescending { it.timestamp }
            TvVideoSortOrder.OLDEST -> filteredItems.sortedBy { it.timestamp }
            TvVideoSortOrder.NAME_AZ -> filteredItems.sortedBy { it.title.lowercase() }
            TvVideoSortOrder.IN_PROGRESS -> filteredItems.sortedWith(
                compareByDescending<TvPlaylistItem> { it.lastPositionMs >= 5000L }
                    .thenByDescending { it.timestamp }
            )
        }
        if (q.isNotBlank()) {
            if (sortedItems.isNotEmpty() || pl.name.lowercase().contains(q)) pl.copy(items = sortedItems) else null
        } else {
            pl.copy(items = sortedItems)
        }
    }
}

fun filterAndSortTvHistory(
    history: List<TvHistoryItem>,
    query: String,
    sort: TvVideoSortOrder
): List<TvHistoryItem> {
    val q = query.trim().lowercase()
    val filtered = if (q.isBlank()) history else history.filter {
        it.title.lowercase().contains(q) || it.mediaUrl.lowercase().contains(q)
    }
    return when (sort) {
        TvVideoSortOrder.NEWEST -> filtered.sortedByDescending { it.updatedAt }
        TvVideoSortOrder.OLDEST -> filtered.sortedBy { it.updatedAt }
        TvVideoSortOrder.NAME_AZ -> filtered.sortedBy { it.title.lowercase() }
        TvVideoSortOrder.IN_PROGRESS -> filtered.sortedWith(
            compareByDescending<TvHistoryItem> { it.lastPositionMs >= 5000L }
                .thenByDescending { it.updatedAt }
        )
    }
}

fun filterAndSortTvPlaylistItems(
    items: List<TvPlaylistItem>,
    query: String,
    sort: TvVideoSortOrder
): List<TvPlaylistItem> {
    val q = query.trim().lowercase()
    val filtered = if (q.isBlank()) items else items.filter {
        it.title.lowercase().contains(q) || it.url.lowercase().contains(q)
    }
    return when (sort) {
        TvVideoSortOrder.NEWEST -> filtered.sortedByDescending { it.timestamp }
        TvVideoSortOrder.OLDEST -> filtered.sortedBy { it.timestamp }
        TvVideoSortOrder.NAME_AZ -> filtered.sortedBy { it.title.lowercase() }
        TvVideoSortOrder.IN_PROGRESS -> filtered.sortedWith(
            compareByDescending<TvPlaylistItem> { it.lastPositionMs >= 5000L }
                .thenByDescending { it.timestamp }
        )
    }
}
