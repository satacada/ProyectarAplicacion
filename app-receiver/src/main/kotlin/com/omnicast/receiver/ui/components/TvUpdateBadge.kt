package com.omnicast.receiver.ui.components

import androidx.compose.foundation.background
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
    var isFocused by remember { mutableStateOf(false) }

    when (updateState) {
        is TvUpdateState.Idle -> {
            Surface(
                color = if (isFocused) Color(0xFF0284C7) else Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .clickable { onCheckUpdate() }
                    .border(1.dp, if (isFocused) Color.White else Color(0xFF334155), RoundedCornerShape(8.dp))
            ) {
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
        }
        is TvUpdateState.Checking -> {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
            ) {
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
        }
        is TvUpdateState.UpdateAvailable -> {
            Surface(
                color = if (isFocused) Color(0xFF059669) else Color(0xFF10B981),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .clickable { onDownloadAndInstall(updateState.downloadUrl) }
                    .border(2.dp, if (isFocused) Color.White else Color(0xFF34D399), RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("⬇️ Actualizar a v${updateState.versionName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        is TvUpdateState.Downloading -> {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
            ) {
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
        }
        is TvUpdateState.ReadyToInstall -> {
            Surface(
                color = Color(0xFF059669),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .clickable { onDownloadAndInstall("") }
                    .border(2.dp, Color.White, RoundedCornerShape(8.dp))
            ) {
                Text(
                    "📦 Instalar Ahora",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        is TvUpdateState.UpToDate -> {
            Surface(
                color = if (isFocused) Color(0xFF0284C7) else Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .clickable { onCheckUpdate() }
                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
            ) {
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
        }
        is TvUpdateState.Error -> {
            Surface(
                color = if (isFocused) Color(0xFFDC2626) else Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .clickable { onCheckUpdate() }
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
            ) {
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
