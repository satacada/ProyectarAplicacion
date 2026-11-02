package com.omnicast.receiver.ui.components

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import kotlinx.coroutines.delay

/**
 * Superposición OSD unificada con controles D-Pad nativos para reproductores web (HTML5 y YouTube).
 * Provee barra de transporte [⏪ -10s], [▶ / ⏸], [⏩ +10s], [⚙️ Ajustes], línea de tiempo interactiva
 * y resaltado de foco de alto contraste para Android TV.
 */
@Composable
fun TvWebUnifiedOverlay(
    title: String,
    badgeText: String,
    state: TvWebPlayerState,
    onTogglePlay: () -> Unit,
    onSeekRelative: (Long) -> Unit,
    onToggleSettings: () -> Unit,
    onShowControls: () -> Unit,
    onHideControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state.isControlsVisible) {
        if (state.isControlsVisible) {
            runCatching { playFocusRequester.requestFocus() }
            delay(4500L)
            onHideControls()
        }
    }

    AnimatedVisibility(
        visible = state.isControlsVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0xCC000000),
                        0.25f to Color.Transparent,
                        0.65f to Color.Transparent,
                        1.0f to Color(0xEE000000)
                    )
                )
        ) {
            // Cabecera superior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = badgeText,
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = title.ifBlank { "Reproducción Web" },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Panel inferior de controles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 24.dp)
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Barra de progreso y tiempos
                TvWebTimelineBar(
                    currentMs = state.currentPositionMs,
                    durationMs = state.durationMs,
                    onSeekDelta = { delta -> onSeekRelative(delta); onShowControls() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Fila de botones de transporte D-Pad
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvWebDpadButton(
                        label = "⏪ -10s",
                        icon = "⏪",
                        onClick = { onSeekRelative(-10_000L); onShowControls() }
                    )

                    TvWebDpadButton(
                        label = if (state.isPlaying) "Pausar" else "Reproducir",
                        icon = if (state.isPlaying) "⏸" else "▶",
                        isPrimary = true,
                        focusRequester = playFocusRequester,
                        onClick = { onTogglePlay(); onShowControls() }
                    )

                    TvWebDpadButton(
                        label = "⏩ +10s",
                        icon = "⏩",
                        onClick = { onSeekRelative(10_000L); onShowControls() }
                    )

                    TvWebDpadButton(
                        label = "⚙️ Ajustes",
                        icon = "⚙️",
                        onClick = { onToggleSettings() }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvWebTimelineBar(
    currentMs: Long,
    durationMs: Long,
    onSeekDelta: (Long) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val progress = if (durationMs > 0L) (currentMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> { onSeekDelta(-15_000L); true }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> { onSeekDelta(15_000L); true }
                        else -> false
                    }
                } else false
            }
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color(0x3338BDF8) else Color.Transparent)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color(0xFF38BDF8) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentMs),
                color = if (isFocused) Color(0xFF38BDF8) else Color(0xFFE2E8F0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatDuration(durationMs),
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Barra de progreso visual
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFocused) 8.dp else 5.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x44FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(if (isFocused) Color(0xFF38BDF8) else Color(0xFF0284C7))
            )
        }
    }
}

@Composable
private fun TvWebDpadButton(
    label: String,
    icon: String,
    isPrimary: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val size = if (isPrimary) 58.dp else 46.dp
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .size(size)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable(interactionSource = interactionSource)
            .clickable { onClick() }
            .clip(shape)
            .background(
                when {
                    isFocused -> Color(0xFF0284C7)
                    isPrimary -> Color(0x880284C7)
                    else -> Color(0x660F172A)
                }
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) Color(0xFF38BDF8) else Color(0x33FFFFFF),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = if (isPrimary) 22.sp else 18.sp,
            color = if (isFocused) Color.White else Color(0xFFE2E8F0)
        )
    }
}
