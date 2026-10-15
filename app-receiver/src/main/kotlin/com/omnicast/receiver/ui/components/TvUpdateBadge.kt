package com.omnicast.receiver.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.receiver.update.TvUpdateState

@Composable
fun TvUpdateStatusBadge(
    updateState: TvUpdateState,
    currentVersion: String,
    onCheckUpdate: () -> Unit,
    onDownloadAndInstall: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    // Re-assert focus across state transitions (e.g. Checking -> UpdateAvailable -> Downloading -> ReadyToInstall)
    // only if the user has not actively navigated away with D-Pad arrow keys.
    LaunchedEffect(updateState) {
        if (isFocused) {
            try {
                focusRequester.requestFocus()
            } catch (_: Throwable) {}
        }
    }

    val isActionable = when (updateState) {
        is TvUpdateState.Checking, is TvUpdateState.Downloading -> false
        else -> true
    }

    val surfaceColor = when (updateState) {
        is TvUpdateState.Idle -> if (isFocused) Color(0xFF0284C7) else Color(0xFF1E293B)
        is TvUpdateState.Checking -> if (isFocused) Color(0xFF075985) else Color(0xFF1E293B)
        is TvUpdateState.UpdateAvailable -> if (isFocused) Color(0xFF059669) else Color(0xFF10B981)
        is TvUpdateState.Downloading -> if (isFocused) Color(0xFF065F46) else Color(0xFF1E293B)
        is TvUpdateState.ReadyToInstall -> if (isFocused) Color(0xFF047857) else Color(0xFF059669)
        is TvUpdateState.UpToDate -> if (isFocused) Color(0xFF0284C7) else Color(0xFF1E293B)
        is TvUpdateState.Error -> if (isFocused) Color(0xFFDC2626) else Color(0xFF1E293B)
    }

    val borderColor = when {
        isFocused -> Color.White
        updateState is TvUpdateState.Idle -> Color(0xFF334155)
        updateState is TvUpdateState.Checking -> Color(0xFF38BDF8)
        updateState is TvUpdateState.UpdateAvailable -> Color(0xFF34D399)
        updateState is TvUpdateState.Downloading -> Color(0xFF10B981)
        updateState is TvUpdateState.ReadyToInstall -> Color(0xFF34D399)
        updateState is TvUpdateState.UpToDate -> Color(0xFF10B981)
        updateState is TvUpdateState.Error -> Color(0xFFEF4444)
        else -> Color(0xFF334155)
    }

    val borderWidth = if (isFocused || updateState is TvUpdateState.UpdateAvailable || updateState is TvUpdateState.ReadyToInstall) 2.dp else 1.dp

    Surface(
        color = surfaceColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable {
                focusRequester.requestFocus()
                if (isActionable) {
                    when (updateState) {
                        is TvUpdateState.Idle, is TvUpdateState.UpToDate, is TvUpdateState.Error -> onCheckUpdate()
                        is TvUpdateState.UpdateAvailable -> onDownloadAndInstall(updateState.downloadUrl)
                        is TvUpdateState.ReadyToInstall -> onDownloadAndInstall("")
                        else -> {}
                    }
                }
            }
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
    ) {
        when (updateState) {
            is TvUpdateState.Idle -> {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(currentVersion, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Text("•", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text("🔄 Actualizar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF38BDF8))
                }
            }
            is TvUpdateState.Checking -> {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF38BDF8)
                    )
                    Text("Buscando...", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF38BDF8))
                }
            }
            is TvUpdateState.UpdateAvailable -> {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("⬇️ Actualizar a v${updateState.versionName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            is TvUpdateState.Downloading -> {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF10B981)
                    )
                    Text("Descargando ${updateState.progressPercent}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
            }
            is TvUpdateState.ReadyToInstall -> {
                Text(
                    "📦 Instalar Ahora",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            is TvUpdateState.UpToDate -> {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(currentVersion, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("•", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text("✅ Al día", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF10B981))
                }
            }
            is TvUpdateState.Error -> {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(currentVersion, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Text("•", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text("⚠️ Reintentar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFEF4444))
                }
            }
        }
    }
}
