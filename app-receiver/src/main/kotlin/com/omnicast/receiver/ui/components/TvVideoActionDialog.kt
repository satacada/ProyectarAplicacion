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
    onRenew: () -> Unit = {},
    onDownload: () -> Unit = {},
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E182A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(420.dp).wrapContentHeight().border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("🎬 Opciones de la Película", color = Color(0xFF38BDF8), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = item.title.ifBlank { "Película guardada" }, color = Color.White, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                TvDialogButton(text = "▶️ Reproducir película", isPrimary = true, onClick = { onPlay(); onDismiss() })
                TvDialogButton(text = "⬇️ Descargar película (Modo Puente / Aviso Disco)", isPrimary = false, onClick = { onDownload(); onDismiss() })
                TvDialogButton(text = "🔄 Renovar enlace (QR móvil)", isPrimary = false, onClick = { onRenew(); onDismiss() })
                if (TvShareHelper.canShare(item)) {
                    TvDialogButton(text = "🔗 Compartir enlace", isPrimary = false, onClick = { onShare(); onDismiss() })
                }
                TvDialogButton(text = "🗑️ Eliminar de guardados", isDanger = true, onClick = { onDelete(); onDismiss() })
                TvDialogButton(text = "✕ Cancelar", isPrimary = false, onClick = onDismiss)
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
            .border(if (isFocused) 2.dp else 1.dp, if (isFocused) Color.White else Color(0xFF3E3255), RoundedCornerShape(8.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = text, color = if (isFocused && !isDanger) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TvSaveToFolderDialog(
    movieTitle: String,
    onSaveToFavorites: () -> Unit,
    onSaveToWatchLater: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E182A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(400.dp).wrapContentHeight().border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🔖 Guardar Película", color = Color(0xFF38BDF8), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(movieTitle.ifBlank { "Película" }, color = Color.White, fontSize = 13.sp, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                TvDialogButton(text = "⭐ Guardar en Favoritos", isPrimary = true, onClick = { onSaveToFavorites(); onDismiss() })
                TvDialogButton(text = "🕒 Guardar en Ver más tarde", isPrimary = false, onClick = { onSaveToWatchLater(); onDismiss() })
                TvDialogButton(text = "✕ Cancelar", isPrimary = false, onClick = onDismiss)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvPlaylistItemRow(
    item: TvPlaylistItem,
    isPhoneConnected: Boolean = false,
    onPlay: () -> Unit,
    onMenu: () -> Unit,
    onSaveToFolder: (() -> Unit)? = null,
    onRemoveSaved: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    isSelected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val webName = remember(item) { com.omnicast.receiver.directstream.TvPlaylistManager.extractCleanWebName(item.pageUrl, item.headers, item.url) }
    val hasEphemeralToken = remember(item.url) { com.omnicast.core.domain.usecases.StreamManifestProbe.hasSignedSecurityToken(item.url) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFF3B1D28) else (if (isFocused) Color(0xFF2E2442) else Color(0xFF1E182A))
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = modifier
                .weight(1f)
                .border(
                    width = if (isFocused || isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color(0xFFEF4444) else (if (isFocused) Color(0xFF38BDF8) else Color(0xFF2D223F)),
                    shape = RoundedCornerShape(10.dp)
                )
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .combinedClickable(onClick = onPlay, onLongClick = onMenu)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("▶", color = if (isFocused) Color(0xFF38BDF8) else Color(0xFF10B981), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(if (item.isFavorite) "⭐" else "🎬", fontSize = 17.sp)
                Column(modifier = Modifier.weight(1f).padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = item.title.ifBlank { "Película guardada" },
                        color = if (isFocused) Color.White else Color(0xFFE2E8F0),
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    val minLeft = if (item.lastPositionMs > 0L) "⏱ Quedó en ${formatDuration(item.lastPositionMs)}" else "⏱ Desde el inicio (0:00)"
                    val actionLabel = when {
                        !hasEphemeralToken -> "▶ Continuar viendo"
                        isPhoneConnected -> "📱 Proyectar directo desde celular"
                        else -> "🔒 Enlace temporal (Conecta celular)"
                    }
                    Text(
                        text = "$minLeft • 🌐 $webName • $actionLabel",
                        color = if (isFocused) Color(0xFF7DD3FC) else if (!hasEphemeralToken) Color(0xFF34D399) else if (isPhoneConnected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (onSaveToFolder != null) {
            var isSaveFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.size(38.dp).onFocusChanged { isSaveFocused = it.isFocused }
                    .background(if (isSaveFocused) Color(0xFF10B981) else Color(0xFF064E3B), RoundedCornerShape(8.dp))
                    .border(if (isSaveFocused) 2.dp else 1.dp, if (isSaveFocused) Color.White else Color(0xFF10B981), RoundedCornerShape(8.dp))
                    .focusable().clickable(onClick = onSaveToFolder),
                contentAlignment = Alignment.Center
            ) {
                TvBookmarkOutlineIcon(modifier = Modifier.size(18.dp), color = if (isSaveFocused) Color.Black else Color(0xFF10B981), filled = true)
            }
        }

        if (onRemoveSaved != null) {
            var isUnsaveFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.size(38.dp).onFocusChanged { isUnsaveFocused = it.isFocused }
                    .background(if (isUnsaveFocused) Color(0xFFEF4444) else Color(0xFF2B1924), RoundedCornerShape(8.dp))
                    .border(if (isUnsaveFocused) 2.dp else 1.dp, if (isUnsaveFocused) Color.White else Color(0xFFF87171), RoundedCornerShape(8.dp))
                    .focusable().clickable(onClick = onRemoveSaved),
                contentAlignment = Alignment.Center
            ) {
                TvBookmarkRemoveIcon(modifier = Modifier.size(18.dp), color = if (isUnsaveFocused) Color.White else Color(0xFFF87171))
            }
        }

        if (onDownload != null) {
            var isDlFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.size(38.dp).onFocusChanged { isDlFocused = it.isFocused }
                    .border(if (isDlFocused) 2.dp else 1.dp, if (isDlFocused) Color.White else Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                    .background(if (isDlFocused) Color(0xFFF59E0B) else Color(0xFF332510), RoundedCornerShape(8.dp))
                    .focusable().clickable(onClick = onDownload),
                contentAlignment = Alignment.Center
            ) {
                Text("⬇️", fontSize = 14.sp)
            }
        }

        if (onToggleSelect != null) {
            var isCheckFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.size(38.dp).onFocusChanged { isCheckFocused = it.isFocused }
                    .border(if (isCheckFocused) 2.dp else 1.dp, if (isCheckFocused) Color.White else (if (isSelected) Color(0xFFEF4444) else Color(0xFF475569)), RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFFEF4444) else (if (isCheckFocused) Color(0xFF38BDF8).copy(alpha = 0.4f) else Color(0xFF2D243F)), RoundedCornerShape(8.dp))
                    .focusable().clickable { onToggleSelect() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (isSelected) "✓" else "☐", fontSize = 15.sp, color = if (isSelected || isCheckFocused) Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
            }
        }

        var isMenuFocused by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier.size(38.dp).onFocusChanged { isMenuFocused = it.isFocused }
                .border(if (isMenuFocused) 2.dp else 1.dp, if (isMenuFocused) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                .background(if (isMenuFocused) Color(0xFF38BDF8) else Color(0xFF2D243F), RoundedCornerShape(8.dp))
                .focusable().clickable(onClick = onMenu),
            contentAlignment = Alignment.Center
        ) {
            Text("⚙", fontSize = 16.sp, color = if (isMenuFocused) Color.Black else Color.White)
        }
    }
}
