package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.sender.history.HistoryItem

enum class VideoSortOrder(val label: String, val icon: ImageVector) {
    NEWEST("Recientes", Icons.Rounded.CalendarToday),
    NAME_AZ("A - Z", Icons.Rounded.SortByAlpha),
    IN_PROGRESS("En progreso", Icons.Rounded.PlayArrow),
    OLDEST("Antiguos", Icons.Rounded.Schedule)
}

@Composable
fun VideoSortFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedSort: VideoSortOrder,
    onSortChange: (VideoSortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Campo de búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buscar película o video...", fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Limpiar", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF334155)
            )
        )

        // Chips de ordenamiento
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ordenar:", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            VideoSortOrder.values().forEach { sort ->
                val isSelected = sort == selectedSort
                Surface(
                    color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable { onSortChange(sort) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = sort.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = sort.label,
                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

fun filterAndSortHistoryItems(
    items: List<HistoryItem>,
    query: String,
    sort: VideoSortOrder
): List<HistoryItem> {
    val q = query.trim().lowercase()
    val filtered = if (q.isBlank()) items else items.filter {
        it.title.lowercase().contains(q) || it.url.lowercase().contains(q)
    }
    return when (sort) {
        VideoSortOrder.NEWEST -> filtered.sortedByDescending { it.timestamp }
        VideoSortOrder.OLDEST -> filtered.sortedBy { it.timestamp }
        VideoSortOrder.NAME_AZ -> filtered.sortedBy { it.title.lowercase() }
        VideoSortOrder.IN_PROGRESS -> filtered.sortedWith(
            compareByDescending<HistoryItem> { it.lastPositionMs >= 5000L }
                .thenByDescending { it.timestamp }
        )
    }
}
