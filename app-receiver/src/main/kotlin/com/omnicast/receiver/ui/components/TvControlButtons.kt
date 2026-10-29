package com.omnicast.receiver.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TvCircleButton(
    text: String = "",
    icon: ImageVector? = null,
    size: Dp,
    fontSize: TextUnit = 14.sp,
    onClick: () -> Unit,
    isPrimary: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isFocused -> if (isPrimary) Color(0xFFFF7A00) else Color(0xFF38BDF8)
        isPrimary -> Color(0xD9E65100)
        else -> Color(0xCC0F172A)
    }
    val contentColor = if (isFocused) Color.Black else Color.White

    Box(
        modifier = Modifier
            .size(size)
            .scale(if (isFocused) 1.18f else 1.0f)
            .background(bgColor, CircleShape)
            .border(
                width = if (isFocused) 3.dp else 1.5.dp,
                color = if (isFocused) Color.White else Color(0xFF475569),
                shape = CircleShape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text.ifBlank { null },
                tint = contentColor,
                modifier = Modifier.size(size * 0.54f)
            )
        } else {
            Text(
                text = text,
                color = contentColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TvInteractiveTimeline(
    currentPosMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    onUserInteraction: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var scrubPosMs by remember(currentPosMs, isFocused) { mutableLongStateOf(currentPosMs) }
    val activePosMs = if (isFocused) scrubPosMs else currentPosMs
    val progress = if (durationMs > 0L) (activePosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    // Paso dinámico: 15s base o 1% de la película (mínimo 10s, máximo 60s por toque)
    val stepMs = remember(durationMs) {
        if (durationMs > 0L) (durationMs / 100L).coerceIn(10_000L, 60_000L) else 15_000L
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color(0x401E293B) else Color.Transparent)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color(0xFF38BDF8) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = if (isFocused) 10.dp else 0.dp, vertical = if (isFocused) 6.dp else 2.dp)
            .onFocusChanged { focused ->
                isFocused = focused.isFocused
                if (focused.isFocused) {
                    scrubPosMs = currentPosMs
                    onUserInteraction()
                }
            }
            .onPreviewKeyEvent { event ->
                if (!isFocused || durationMs <= 0L) return@onPreviewKeyEvent false
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val repeat = event.nativeKeyEvent.repeatCount
                    val accel = if (repeat > 8) 4L else if (repeat > 3) 2L else 1L
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            val next = (scrubPosMs - stepMs * accel).coerceAtLeast(0L)
                            scrubPosMs = next
                            onSeekTo(next)
                            onUserInteraction()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            val next = (scrubPosMs + stepMs * accel).coerceAtMost(durationMs)
                            scrubPosMs = next
                            onSeekTo(next)
                            onUserInteraction()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            onSeekTo(scrubPosMs)
                            onUserInteraction()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(if (isFocused) 16.dp else 8.dp), contentAlignment = Alignment.CenterStart) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isFocused) 8.dp else 5.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isFocused) Color(0xFF38BDF8) else Color(0xFFFF7A00),
                trackColor = Color(0xFF334155)
            )
            if (isFocused) {
                val thumbOffset = (maxWidth - 14.dp) * progress
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset)
                        .size(14.dp)
                        .background(Color.White, CircleShape)
                        .border(2.5.dp, Color(0xFFFF7A00), CircleShape)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatTvTime(activePosMs),
                color = if (isFocused) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                fontSize = if (isFocused) 12.sp else 11.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
            )
            if (isFocused) {
                Text(
                    text = "◀ ▶ Usa las flechas para avanzar o retroceder",
                    color = Color(0xFFFFB74D),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = formatTvTime(durationMs),
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TvPlayerButton(
    label: String,
    onClick: () -> Unit,
    isDanger: Boolean = false,
    isSettings: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isFocused -> if (isDanger) Color(0xFFEF4444) else if (isSettings) Color(0xFF0284C7) else Color(0xFF38BDF8)
        isDanger -> Color(0x992D1515)
        isSettings -> Color(0xCC1E293B)
        else -> Color(0xB31E293B)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .height(36.dp)
            .scale(if (isFocused) 1.12f else 1.0f)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) Color.White else Color(0xFF475569),
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (isFocused && !isDanger) Color.Black else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun formatTvTime(ms: Long): String {
    val totalSec = ms / 1000
    val sec = totalSec % 60
    val min = (totalSec / 60) % 60
    val hrs = totalSec / 3600
    return if (hrs > 0) String.format("%02d:%02d:%02d", hrs, min, sec) else String.format("%02d:%02d", min, sec)
}

