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
import com.omnicast.receiver.directstream.TvHistoryItem

@Composable
fun TvHistorySection(
    history: List<TvHistoryItem>,
    onPlayItem: (TvHistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "🕒 Continuar Viendo (Historial en TV)",
                color = Color(0xFF38BDF8),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${history.size} videos guardados",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(history) { item ->
                TvHistoryCard(item = item, onPlay = { onPlayItem(item) })
            }
        }
    }
}

@Composable
fun TvHistoryCard(
    item: TvHistoryItem,
    onPlay: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF0369A1) else Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(220.dp)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color(0xFF38BDF8) else Color(0xFF334155),
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onPlay() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val posSec = item.lastPositionMs / 1000
            val durSec = item.durationMs / 1000
            val timeText = if (durSec > 0) {
                "${posSec / 60}:${(posSec % 60).toString().padStart(2, '0')} / ${durSec / 60}:${(durSec % 60).toString().padStart(2, '0')}"
            } else if (posSec > 0) {
                "${posSec / 60}:${(posSec % 60).toString().padStart(2, '0')}"
            } else {
                "Inicio"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "▶ $timeText",
                    color = if (isFocused) Color(0xFFBAE6FD) else Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "OK para ver",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
        }
    }
}
