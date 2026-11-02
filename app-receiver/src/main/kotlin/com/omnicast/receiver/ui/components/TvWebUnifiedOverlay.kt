package com.omnicast.receiver.ui.components

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Superposición OSD unificada con controles D-Pad nativos para reproductores web (HTML5 y YouTube).
 * Provee avance progresivo acumulativo (+10s, +20s, +30s...), foco D-Pad estable sin saltos forzados a Play,
 * barra de tiempo interactiva con reporte en tiempo real y ocultamiento selectivo de botones inoperativos.
 */
@Composable
fun TvWebUnifiedOverlay(
    title: String,
    badgeText: String,
    state: TvWebPlayerState,
    hasSettings: Boolean = true,
    onTogglePlay: () -> Unit,
    onSeekRelative: (Long) -> Unit,
    onToggleSettings: () -> Unit,
    onShowControls: () -> Unit,
    onHideControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playFocusRequester = remember { FocusRequester() }
    var hideTimerKey by remember { mutableStateOf(0) }
    var accumulatedSeekDelta by remember { mutableStateOf(0L) }
    var seekNoticeText by remember { mutableStateOf("") }
    var seekDebounceJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun triggerSeek(deltaMs: Long) {
        hideTimerKey++
        accumulatedSeekDelta = if ((accumulatedSeekDelta > 0 && deltaMs < 0) || (accumulatedSeekDelta < 0 && deltaMs > 0)) deltaMs else accumulatedSeekDelta + deltaMs
        val sign = if (accumulatedSeekDelta > 0) "+" else ""
        seekNoticeText = "$sign${accumulatedSeekDelta / 1000}s"
        seekDebounceJob?.cancel()
        seekDebounceJob = coroutineScope.launch {
            delay(500L); onSeekRelative(accumulatedSeekDelta); accumulatedSeekDelta = 0L; delay(500L); seekNoticeText = ""
        }
    }

    LaunchedEffect(state.isControlsVisible, hideTimerKey) {
        if (state.isControlsVisible) {
            if (hideTimerKey == 0) runCatching { playFocusRequester.requestFocus() }
            delay(4500L); hideTimerKey = 0; onHideControls()
        } else { hideTimerKey = 0 }
    }

    AnimatedVisibility(visible = state.isControlsVisible, enter = fadeIn(), exit = fadeOut(), modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(0.0f to Color(0xCC000000), 0.25f to Color.Transparent, 0.60f to Color.Transparent, 1.0f to Color(0xEE000000)))) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp).align(Alignment.TopStart), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = badgeText, color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = title.ifBlank { "Reproducción Web" }, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 24.dp).align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                if (seekNoticeText.isNotBlank()) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xD90284C7)).padding(horizontal = 14.dp, vertical = 4.dp)) {
                        Text(text = "⏩ $seekNoticeText", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                TvWebTimelineBar(currentMs = state.currentPositionMs, durationMs = state.durationMs, onSeekDelta = { delta -> triggerSeek(delta) })
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    TvWebDpadButton(label = "⏪", icon = "⏪", onClick = { triggerSeek(-10_000L) })
                    TvWebDpadButton(label = if (state.isPlaying) "Pausar" else "Reproducir", icon = if (state.isPlaying) "⏸" else "▶", isPrimary = true, focusRequester = playFocusRequester, onClick = { onTogglePlay(); hideTimerKey++ })
                    TvWebDpadButton(label = "⏩", icon = "⏩", onClick = { triggerSeek(10_000L) })
                    if (hasSettings) {
                        TvWebDpadButton(label = "⚙️", icon = "⚙️", onClick = { onToggleSettings(); hideTimerKey++ })
                    }
                }
            }
        }
    }
}

@Composable
private fun TvWebTimelineBar(currentMs: Long, durationMs: Long, onSeekDelta: (Long) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val progress = if (durationMs > 0L) (currentMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier.fillMaxWidth().focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> { onSeekDelta(-10_000L); true }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> { onSeekDelta(10_000L); true }
                        else -> false
                    }
                } else false
            }
            .clip(RoundedCornerShape(8.dp)).background(if (isFocused) Color(0x3338BDF8) else Color.Transparent)
            .border(if (isFocused) 2.dp else 0.dp, if (isFocused) Color(0xFF38BDF8) else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formatDuration(currentMs), color = if (isFocused) Color(0xFF38BDF8) else Color(0xFFE2E8F0), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = if (durationMs > 0L) formatDuration(durationMs) else "--:--", color = Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(if (isFocused) 8.dp else 5.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x44FFFFFF))) {
            Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(if (isFocused) Color(0xFF38BDF8) else Color(0xFF0284C7)))
        }
    }
}

@Composable
private fun TvWebDpadButton(label: String, icon: String, isPrimary: Boolean = false, focusRequester: FocusRequester? = null, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val size = if (isPrimary) 58.dp else 46.dp
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier.size(size)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable(interactionSource = interactionSource).clickable { onClick() }.clip(shape)
            .background(when { isFocused -> Color(0xFF0284C7); isPrimary -> Color(0x880284C7); else -> Color(0x660F172A) })
            .border(if (isFocused) 2.5.dp else 1.dp, if (isFocused) Color(0xFF38BDF8) else Color(0x33FFFFFF), shape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = icon, fontSize = if (isPrimary) 22.sp else 18.sp, color = if (isFocused) Color.White else Color(0xFFE2E8F0))
    }
}
