package com.omnicast.receiver.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.receiver.directstream.TvHistoryItem
import com.omnicast.receiver.directstream.TvPlaylistItem
import com.omnicast.receiver.directstream.TvDownloadedVideo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TvCastToTvVectorIcon(modifier: Modifier = Modifier, tvColor: Color = Color(0xFFA78BFA), castWaveColor: Color = Color(0xFF38BDF8)) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; val stroke = 1.7.dp.toPx()
        drawRoundRect(color = tvColor, topLeft = Offset(w * 0.10f, h * 0.14f), size = Size(w * 0.80f, h * 0.60f), cornerRadius = CornerRadius(w * 0.09f, w * 0.09f), style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color = tvColor, start = Offset(w * 0.36f, h * 0.86f), end = Offset(w * 0.64f, h * 0.86f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = tvColor, start = Offset(w * 0.50f, h * 0.74f), end = Offset(w * 0.50f, h * 0.86f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawCircle(color = castWaveColor, radius = w * 0.045f, center = Offset(w * 0.24f, h * 0.58f))
        drawArc(color = castWaveColor, startAngle = -90f, sweepAngle = 90f, useCenter = false, topLeft = Offset(w * 0.10f, h * 0.44f), size = Size(w * 0.28f, h * 0.28f), style = Stroke(width = stroke * 0.85f, cap = StrokeCap.Round))
        drawArc(color = castWaveColor, startAngle = -90f, sweepAngle = 90f, useCenter = false, topLeft = Offset(w * 0.01f, h * 0.35f), size = Size(w * 0.46f, h * 0.46f), style = Stroke(width = stroke * 0.85f, cap = StrokeCap.Round))
    }
}

@Composable
fun TvCleanTopBar(
    selectedTab: Int, onSelectTab: (Int) -> Unit,
    updateState: com.omnicast.receiver.update.TvUpdateState = com.omnicast.receiver.update.TvUpdateState.Idle,
    onCheckUpdate: () -> Unit = {}, onDownloadAndInstall: (String) -> Unit = {},
    onOpenLanguage: () -> Unit = {}
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val tvLang by com.omnicast.receiver.language.TvLanguageManager.currentLanguageFlow.collectAsState()
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TvCastToTvVectorIcon(modifier = Modifier.size(32.dp))
            Text("Castera TV", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            TvTopTab(label = com.omnicast.receiver.language.TvLanguageManager.t(ctx, "recent_watched", tvLang), isSelected = selectedTab == 0, onClick = { onSelectTab(0) })
            TvTopTab(label = com.omnicast.receiver.language.TvLanguageManager.t(ctx, "saved_title", tvLang), isSelected = selectedTab == 1, onClick = { onSelectTab(1) })
            TvTopTab(label = "📥 Descargas", isSelected = selectedTab == 2, onClick = { onSelectTab(2) })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var isLangFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.onFocusChanged { isLangFocused = it.isFocused }.focusable().clickable { onOpenLanguage() }
                    .background(if (isLangFocused) Color(0xFF38BDF8) else Color(0xFF1E182A), RoundedCornerShape(8.dp))
                    .border(1.dp, if (isLangFocused) Color.White else Color(0xFF2D223F), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center
            ) { Text("🌐", fontSize = 14.sp) }
            val currentVersion = remember { try { "v" + ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName } catch (_: Throwable) { "v3.5.42" } }
            TvUpdateStatusBadge(updateState = updateState, currentVersion = currentVersion, onCheckUpdate = onCheckUpdate, onDownloadAndInstall = onDownloadAndInstall)
        }
    }
}

@Composable
fun TvTopTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Box(modifier = Modifier.onFocusChanged { isFocused = it.isFocused }.focusable().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = if (isSelected || isFocused) Color.White else Color(0xFF94A3B8), fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            if (isSelected) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(modifier = Modifier.width(24.dp).height(2.dp).background(Color(0xFF38BDF8), RoundedCornerShape(1.dp)))
            }
        }
    }
}

@Composable
fun TvFolderRow(name: String, date: String, itemCount: Int, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF2E2442) else Color(0xFF1E182A)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }.focusable().clickable(onClick = onClick)
            .border(if (isFocused) 2.dp else 1.dp, if (isFocused) Color(0xFF38BDF8) else Color(0xFF2D223F), RoundedCornerShape(12.dp))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.size(38.dp).background(if (isFocused) Color(0xFFD97706) else Color(0xFFF59E0B), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text("📁", fontSize = 20.sp)
                }
                Column {
                    Text(text = name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (itemCount > 0) "$itemCount película${if (itemCount != 1) "s" else ""} guardada${if (itemCount != 1) "s" else ""}" else "Sin películas", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = date, color = Color(0xFF64748B), fontSize = 11.sp)
                Text("▶", color = if (isFocused) Color(0xFF38BDF8) else Color(0xFF64748B), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun TvBookmarkOutlineIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF38BDF8), filled: Boolean = false) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.22f, h * 0.12f); lineTo(w * 0.78f, h * 0.12f); lineTo(w * 0.78f, h * 0.88f); lineTo(w * 0.50f, h * 0.68f); lineTo(w * 0.22f, h * 0.88f); close()
        }
        if (filled) drawPath(path = path, color = color)
        else drawPath(path = path, color = color, style = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun TvRecentItemRow(
    item: TvHistoryItem,
    isSaved: Boolean = false,
    downloadedVideo: TvDownloadedVideo? = null,
    isPhoneConnected: Boolean = false,
    onClick: () -> Unit,
    onToggleSave: () -> Unit = {},
    onDownload: () -> Unit = {},
    onLocalAction: () -> Unit = {}
) {
    var isPlayFocused by remember { mutableStateOf(false) }
    var isSaveFocused by remember { mutableStateOf(false) }
    var isActionFocused by remember { mutableStateOf(false) }
    val webName = remember(item) { com.omnicast.receiver.directstream.TvPlaylistManager.extractCleanWebName(item.pageUrl, item.headers, item.mediaUrl) }
    val hasEphemeralToken = remember(item.mediaUrl) { com.omnicast.core.domain.usecases.StreamManifestProbe.hasSignedSecurityToken(item.mediaUrl) }
    val rowHighlighted = isPlayFocused || isSaveFocused || isActionFocused

    Card(
        colors = CardDefaults.cardColors(containerColor = if (rowHighlighted) Color(0xFF281F3B) else Color(0xFF1E182A)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(if (rowHighlighted) 2.dp else 1.dp, if (rowHighlighted) Color(0xFF38BDF8) else Color(0xFF2D223F), RoundedCornerShape(12.dp))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).onFocusChanged { isPlayFocused = it.isFocused }.focusable().clickable(onClick = onClick).padding(vertical = 2.dp)
            ) {
                Text("▶", color = if (isPlayFocused) Color(0xFF38BDF8) else Color(0xFF10B981), fontSize = 16.sp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val isLive = item.durationMs <= 0L || item.mediaUrl.contains("linear", true) || item.mediaUrl.contains("live", true)
                    val minLeft = if (isLive) "🔴 EN VIVO" else if (item.lastPositionMs > 0L) "⏱ Quedó en ${formatDuration(item.lastPositionMs)}" else "⏱ Desde el inicio (0:00)"
                    val actionLabel = when {
                        !hasEphemeralToken -> "▶ Continuar viendo"
                        isPhoneConnected -> "📱 Proyectar directo desde celular"
                        else -> "🔒 Enlace temporal (Conecta celular)"
                    }
                    val diskBadge = if (downloadedVideo != null) " • 💾 En disco" else ""
                    Text(
                        text = "$minLeft • 🌐 $webName • $actionLabel$diskBadge",
                        color = if (downloadedVideo != null) Color(0xFF34D399) else if (!hasEphemeralToken) Color(0xFF34D399) else if (isPhoneConnected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier.size(38.dp).onFocusChanged { isSaveFocused = it.isFocused }
                    .background(if (isSaveFocused) Color(0xFFF59E0B) else if (isSaved) Color(0xFF3B2E15) else Color(0xFF251C38), RoundedCornerShape(8.dp))
                    .border(if (isSaveFocused) 2.dp else 1.dp, if (isSaveFocused) Color.White else if (isSaved) Color(0xFFF59E0B) else Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                    .focusable().clickable { onToggleSave() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (isSaved) "⭐" else "☆", fontSize = 16.sp, color = if (isSaveFocused) Color.Black else if (isSaved) Color(0xFFF59E0B) else Color(0xFF94A3B8))
            }

            if (downloadedVideo != null) {
                Box(
                    modifier = Modifier.height(38.dp).onFocusChanged { isActionFocused = it.isFocused }
                        .background(if (isActionFocused) Color(0xFF10B981) else Color(0xFF064E3B), RoundedCornerShape(8.dp))
                        .border(if (isActionFocused) 2.dp else 1.dp, if (isActionFocused) Color.White else Color(0xFF10B981), RoundedCornerShape(8.dp))
                        .focusable().clickable { onLocalAction() }.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💾 En disco", color = if (isActionFocused) Color.Black else Color(0xFF6EE7B7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier.size(38.dp).onFocusChanged { isActionFocused = it.isFocused }
                        .background(if (isActionFocused) Color(0xFFF59E0B) else Color(0xFF332510), RoundedCornerShape(8.dp))
                        .border(if (isActionFocused) 2.dp else 1.dp, if (isActionFocused) Color.White else Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                        .focusable().clickable { onDownload() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⬇️", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun TvBottomConnectionDock(
    ipAddress: String,
    connectedClientIp: String? = null,
    modifier: Modifier = Modifier
) {
    val isConnected = !connectedClientIp.isNullOrBlank()
    Surface(
        color = if (isConnected) Color(0xFF047857) else Color(0xEE0D0B14),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(if (isConnected) 2.dp else 1.dp, if (isConnected) Color(0xFF6EE7B7) else Color(0xFF2D223F), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(12.dp).background(if (isConnected) Color.White else Color(0xFFF59E0B), CircleShape))
                Text(
                    if (isConnected) "🟢 Celular Conectado ($connectedClientIp)" else "Listo para vincular",
                    fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (isConnected) Color.White else Color(0xFFCBD5E1)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (isConnected) "Enlace activo:" else "Dirección Local:", color = if (isConnected) Color(0xFFECFDF5) else Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = if (isConnected) FontWeight.SemiBold else FontWeight.Normal)
                Text(if (isConnected) "Listo para proyectar" else "$ipAddress:8088", color = if (isConnected) Color.White else Color(0xFF38BDF8), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            }

            Text(
                if (isConnected) "Envía desde tu celular o elige una película arriba" else "Abre OmniCast en tu celular para conectar",
                color = if (isConnected) Color.White else Color(0xFF64748B), fontSize = 12.sp, fontWeight = if (isConnected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

fun formatFolderDate(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
        sdf.format(Date(timestamp))
    } catch (_: Throwable) {
        "2026/06/21 15:12"
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return if (min >= 60) String.format(Locale.US, "%d:%02d:%02d", min / 60, min % 60, sec) else String.format(Locale.US, "%d:%02d", min, sec)
}

@Composable
fun TvFolderDetailHeader(folderName: String, itemCount: Int, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var isBackFocused by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = if (isBackFocused) Color(0xFF38BDF8) else Color(0xFF251C35)),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier.onFocusChanged { isBackFocused = it.isFocused }.focusable().clickable(onClick = onBack)
                .border(1.dp, if (isBackFocused) Color.White else Color(0xFF38BDF8), RoundedCornerShape(8.dp))
        ) {
            Text("◀ Volver a carpetas", color = if (isBackFocused) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
        }
        Text("📂 $folderName ($itemCount película${if (itemCount != 1) "s" else ""})", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TvEmptyState(msg: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg, color = Color(0xFF64748B), fontSize = 14.sp)
    }
}
