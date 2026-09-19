package com.omnicast.sender.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.sender.ui.components.RemoteControlBar

@Composable
fun BrowserBottomBarContainer(
    isCasting: Boolean,
    castTitle: String,
    isPlaying: Boolean,
    posMs: Long,
    durMs: Long,
    volume: Float = 1.0f,
    detectedCount: Int,
    pageTitle: String,
    onTransmitDirect: () -> Unit,
    onOpenQualities: () -> Unit,
    onPlayControl: (String, Long, Float) -> Unit,
    onStopCasting: () -> Unit,
    onClearStreams: () -> Unit = {},
    onSaveToPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (isCasting && detectedCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF065F46))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🎬 Nuevo flujo detectado ($detectedCount)",
                    color = Color(0xFFA7F3D0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF10B981))
                        .clickable { onTransmitDirect() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("▶ Transmitir a TV", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (isCasting) {
            RemoteControlBar(
                title = castTitle.ifBlank { pageTitle },
                isPlaying = isPlaying, positionMs = posMs, durationMs = durMs, volume = volume,
                onPlayPause = { onPlayControl(if (isPlaying) "PAUSE" else "PLAY", posMs, 1f) },
                onSeekTo = { onPlayControl("SEEK", it, 1f) },
                onSkip = { onPlayControl("SEEK", (posMs + it).coerceAtLeast(0L), 1f) },
                onVolumeChange = { onPlayControl("VOLUME", 0L, it) },
                onStop = onStopCasting,
                onSelectQuality = onOpenQualities,
                onSaveToPlaylist = onSaveToPlaylist
            )
        } else if (detectedCount > 0) {
            DetectedStreamBar(
                streamCount = detectedCount, title = pageTitle,
                onTransmitDirect = onTransmitDirect, onOpenQualities = onOpenQualities,
                onClearStreams = onClearStreams,
                onSaveToPlaylist = onSaveToPlaylist
            )
        }
    }
}
