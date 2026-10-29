package com.omnicast.sender.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VideoPlayerTopBar(
    title: String,
    subtitle: String = "",
    currentZoom: Float = 1.0f,
    onResetZoom: () -> Unit = {},
    onBack: () -> Unit,
    onStop: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 36.dp, start = 14.dp, end = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0x801E293B), CircleShape)
                .border(1.dp, Color(0xFF334155), CircleShape)
                .align(Alignment.CenterStart)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Text("←", color = Color(0xFF38BDF8), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 88.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Surface(
                color = Color(0xFF0C2438),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF38BDF8), CircleShape))
                    Text(
                        text = subtitle.ifBlank { "TV Box Conectada" },
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = if (kotlin.math.abs(currentZoom - 1f) > 0.05f) Color(0xD94C1D95) else Color(0xD938142C),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (kotlin.math.abs(currentZoom - 1f) > 0.05f) Color(0xFFA78BFA) else Color(0xFFBE185D)),
                modifier = Modifier.clickable { onResetZoom() }
            ) {
                Text(
                    text = if (kotlin.math.abs(currentZoom - 1f) > 0.05f) "1:1 (${(currentZoom * 100).toInt()}%)" else "1:1",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                )
            }

            Surface(
                color = Color(0xFFDC2626),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFCA5A5)),
                modifier = Modifier.size(36.dp).clickable { onStop() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("■", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun VideoPlayerCenterControls(
    isPlaying: Boolean,
    isNearEnd: Boolean,
    hasMultipleVideos: Boolean = false,
    onPrevVideo: () -> Unit = {},
    onRewind: () -> Unit,
    onPlayPause: () -> Unit,
    onForward: () -> Unit,
    onNextVideo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (hasMultipleVideos) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0x801E293B), CircleShape)
                    .border(1.dp, Color(0xFF334155), CircleShape)
                    .clickable { onPrevVideo() },
                contentAlignment = Alignment.Center
            ) {
                Text("⏮", fontSize = 16.sp, color = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .size(54.dp)
                .background(Color(0x801E293B), CircleShape)
                .border(1.dp, Color(0xFF334155), CircleShape)
                .clickable { onRewind() },
            contentAlignment = Alignment.Center
        ) {
            Text("⏪", fontSize = 20.sp)
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFF00B4D8), CircleShape)
                .clickable { onPlayPause() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPlaying) "⏸" else if (isNearEnd) "↺" else "▶",
                fontSize = 30.sp,
                color = Color.White
            )
        }

        Box(
            modifier = Modifier
                .size(54.dp)
                .background(Color(0x801E293B), CircleShape)
                .border(1.dp, Color(0xFF334155), CircleShape)
                .clickable { onForward() },
            contentAlignment = Alignment.Center
        ) {
            Text("⏩", fontSize = 20.sp)
        }

        if (hasMultipleVideos) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0x801E293B), CircleShape)
                    .border(1.dp, Color(0xFF334155), CircleShape)
                    .clickable { onNextVideo() },
                contentAlignment = Alignment.Center
            ) {
                Text("⏭", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun VideoPlayerBottomBar(
    currentMs: Long,
    durationMs: Long,
    phoneMuted: Boolean,
    onSeek: (Long) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Slider(
            value = currentMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..durationMs.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00B4D8),
                activeTrackColor = Color(0xFF00B4D8),
                inactiveTrackColor = Color(0xFF1E293B)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatTime(currentMs)} / ${formatTime(durationMs)}",
                color = Color(0xFFE2E8F0),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Surface(
                color = Color(0xFF161F2E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF28354E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.clickable { onToggleMute() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(if (phoneMuted) "🔇" else "🔊", fontSize = 12.sp)
                    Text(
                        text = if (phoneMuted) "Celular Mudo" else "Audio Celular",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (phoneMuted) Color(0xFF94A3B8) else Color(0xFF38BDF8)
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}
