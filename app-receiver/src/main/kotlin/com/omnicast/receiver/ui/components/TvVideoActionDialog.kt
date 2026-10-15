package com.omnicast.receiver.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omnicast.receiver.directstream.TvPlaylistItem

@Composable
fun TvVideoActionDialog(
    item: TvPlaylistItem,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E182A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(420.dp)
                .wrapContentHeight()
                .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🎬 Opciones de la Película",
                    color = Color(0xFF38BDF8),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = item.title.ifBlank { "Película guardada" },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                TvDialogButton(
                    text = "▶️ Reproducir película",
                    isPrimary = true,
                    onClick = { onPlay(); onDismiss() }
                )

                if (TvShareHelper.canShare(item)) {
                    TvDialogButton(
                        text = "🔗 Compartir enlace",
                        isPrimary = false,
                        onClick = { onShare(); onDismiss() }
                    )
                }

                TvDialogButton(
                    text = "🗑️ Eliminar de guardados",
                    isDanger = true,
                    onClick = { onDelete(); onDismiss() }
                )

                TvDialogButton(
                    text = "✕ Cancelar",
                    isPrimary = false,
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun TvDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isDanger: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }

    val bgColor = when {
        isFocused -> if (isDanger) Color(0xFFEF4444) else Color(0xFF38BDF8)
        isDanger -> Color(0xFF7F1D1D)
        isPrimary -> Color(0xFF2563EB)
        else -> Color(0xFF2D243F)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else Color(0xFF3E3255),
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (isFocused && !isDanger) Color.Black else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvPlaylistItemRow(
    item: TvPlaylistItem,
    onPlay: () -> Unit,
    onMenu: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF2E2442) else Color(0xFF1E182A)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color(0xFF38BDF8) else Color(0xFF2D223F),
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onMenu
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("↳", color = if (isFocused) Color(0xFF38BDF8) else Color(0xFF64748B), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(if (item.isFavorite) "⭐" else "🎬", fontSize = 17.sp)
                Column(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.title.ifBlank { "Película guardada" },
                        color = if (isFocused) Color.White else Color(0xFFE2E8F0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val posLabel = if (item.lastPositionMs >= 5000L) {
                        val m = (item.lastPositionMs / 1000) / 60
                        val s = (item.lastPositionMs / 1000) % 60
                        "%02d:%02d".format(m, s)
                    } else null
                    Text(
                        text = if (isFocused) (if (posLabel != null) "▶ Reanudar en $posLabel • Mantén presionado OK para menú" else "▶ Presiona OK para reproducir • Mantén OK para menú")
                               else (if (posLabel != null) "▶ En $posLabel" else "Guardada"),
                        color = if (isFocused) Color(0xFF7DD3FC) else (if (posLabel != null) Color(0xFF10B981) else Color(0xFF94A3B8)),
                        fontSize = 11.sp,
                        fontWeight = if (isFocused || posLabel != null) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(if (isFocused) Color(0xFF38BDF8) else Color(0xFF2D243F), RoundedCornerShape(6.dp))
                    .clickable(onClick = onMenu),
                contentAlignment = Alignment.Center
            ) {
                Text("⚙", fontSize = 14.sp, color = if (isFocused) Color.Black else Color.White)
            }
        }
    }
}
