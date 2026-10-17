package com.omnicast.sender.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayHistorySheet(
    historyList: List<HistoryItem>,
    onSelectAndTransmit: (HistoryItem) -> Unit,
    onToggleFavorite: (String) -> Unit = {},
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    Text("Historial de Películas", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                if (historyList.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Limpiar (conserva favoritos)", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            }

            if (historyList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No hay películas en el historial aún.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    items(historyList, key = { it.url }) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelectAndTransmit(item) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (item.isFavorite) {
                                            Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFFDE047), modifier = Modifier.size(14.dp))
                                        }
                                        Text(
                                            item.title,
                                            color = if (item.isFavorite) Color(0xFFFDE047) else Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (item.requiresProxy) {
                                            Icon(Icons.Rounded.Lock, contentDescription = "Proxy", tint = Color(0xFFF97316), modifier = Modifier.size(12.dp))
                                        }
                                    }
                                    val subtitle = if (item.requiresProxy) "Requiere emisor en WiFi • " + item.url else item.url
                                    Text(subtitle, color = if (item.requiresProxy) Color(0xFFF97316) else Color(0xFF64748B), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onToggleFavorite(item.url) }, modifier = Modifier.size(32.dp)) {
                                        Icon(if (item.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, contentDescription = "Favorito", tint = if (item.isFavorite) Color(0xFFF59E0B) else Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { onDelete(item.url) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Eliminar", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
