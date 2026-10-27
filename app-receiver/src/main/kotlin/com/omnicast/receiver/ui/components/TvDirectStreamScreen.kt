package com.omnicast.receiver.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.omnicast.receiver.directstream.ExoPlayerTvManager
import kotlinx.coroutines.delay

@Composable
fun TvDirectStreamScreen(
    exoPlayerManager: ExoPlayerTvManager,
    title: String,
    streamUrl: String,
    isSaved: Boolean = false,
    onSaveToWatchLater: () -> Unit = {},
    onCloseClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isGearFocused by remember { mutableStateOf(false) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var userInteractionCount by remember { mutableIntStateOf(0) }
    var currentQuality by remember { mutableStateOf(exoPlayerManager.getCurrentVideoQuality()) }
    var subsEnabled by remember { mutableStateOf(exoPlayerManager.areSubtitlesEnabled) }
    var currentResizeLabel by remember { mutableStateOf(exoPlayerManager.aspectRatioHelper.currentMode.value.label) }

    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(exoPlayerManager.isPlaying) }

    LaunchedEffect(Unit) {
        exoPlayerManager.isControlsShowing = true
        while (true) {
            currentPosMs = exoPlayerManager.currentPositionMs
            durationMs = exoPlayerManager.durationMs
            isPlaying = exoPlayerManager.isPlaying
            delay(500)
        }
    }

    LaunchedEffect(userInteractionCount, isPlaying) {
        if (isControlsVisible && isPlaying && !showSettingsDialog) {
            delay(5000)
            isControlsVisible = false
            exoPlayerManager.isControlsShowing = false
        }
    }

    DisposableEffect(exoPlayerManager) {
        exoPlayerManager.onWakeControls = {
            isControlsVisible = exoPlayerManager.isControlsShowing
            userInteractionCount++
        }
        onDispose {
            exoPlayerManager.onWakeControls = null
            exoPlayerManager.playerView = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        val player = exoPlayerManager.player
        val currentResizeMode = remember { mutableIntStateOf(exoPlayerManager.resizeMode) }
        val currentZoom = remember { mutableFloatStateOf(exoPlayerManager.currentZoom) }

        DisposableEffect(exoPlayerManager) {
            exoPlayerManager.onResizeModeChanged = { mode -> currentResizeMode.intValue = mode }
            exoPlayerManager.onZoomChanged = { z -> currentZoom.floatValue = z }
            onDispose {
                exoPlayerManager.onResizeModeChanged = null
                exoPlayerManager.onZoomChanged = null
            }
        }

        if (player != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        resizeMode = currentResizeMode.intValue
                        subtitleView?.visibility = android.view.View.VISIBLE
                        setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        exoPlayerManager.playerView = this
                    }
                },
                update = { pv ->
                    if (pv.player != player) pv.player = player
                    pv.resizeMode = currentResizeMode.intValue
                    val z = currentZoom.floatValue
                    pv.scaleX = if (z > 0f) z else 1.0f
                    pv.scaleY = if (z > 0f) z else 1.0f
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Tuerca superior izquierda
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 24.dp)
                .size(38.dp)
                .background(if (isGearFocused) Color(0xFF38BDF8) else Color(0x990F172A), RoundedCornerShape(10.dp))
                .border(if (isGearFocused) 2.dp else 1.dp, if (isGearFocused) Color.White else Color(0x6638BDF8), RoundedCornerShape(10.dp))
                .onFocusChanged { isGearFocused = it.isFocused; if (it.isFocused) { isControlsVisible = true; userInteractionCount++ } }
                .focusable()
                .clickable { showSettingsDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Text("⚙", fontSize = 18.sp, color = if (isGearFocused) Color.Black else Color.White)
        }

        // Barra inferior interactiva para TV (Navegable con control remoto)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF2070D18))))
                    .padding(horizontal = 28.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Título y estado
                Text(
                    text = title.ifBlank { "Transmisión en TV Box" },
                    color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )

                // Línea de tiempo
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val progress = if (durationMs > 0L) (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = Color(0xFFFF7A00),
                        trackColor = Color(0xFF334155)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatTvTime(currentPosMs), color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(formatTvTime(durationMs), color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Fila de botones de control nativo para TV
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvPlayerButton(label = "⏪ -10s", onClick = {
                        exoPlayerManager.seekTo((currentPosMs - 10_000L).coerceAtLeast(0L))
                        userInteractionCount++
                    })
                    TvPlayerButton(label = if (isPlaying) "⏸ Pausa" else "▶ Play", isPrimary = true, onClick = {
                        if (isPlaying) exoPlayerManager.pause() else exoPlayerManager.resume()
                        userInteractionCount++
                    })
                    TvPlayerButton(label = "⏩ +10s", onClick = {
                        exoPlayerManager.seekTo((currentPosMs + 10_000L).coerceAtMost(durationMs))
                        userInteractionCount++
                    })
                    TvPlayerButton(label = "📺 $currentQuality", onClick = {
                        currentQuality = exoPlayerManager.cycleVideoQuality()
                        Toast.makeText(context, "📺 Calidad: $currentQuality", Toast.LENGTH_SHORT).show()
                        userInteractionCount++
                    })
                    TvPlayerButton(label = if (subsEnabled) "💬 Sub: Sí" else "💬 Sub: No", onClick = {
                        subsEnabled = exoPlayerManager.toggleSubtitles()
                        Toast.makeText(context, if (subsEnabled) "💬 Subtítulos Activados" else "💬 Subtítulos Desactivados", Toast.LENGTH_SHORT).show()
                        userInteractionCount++
                    })
                    TvPlayerButton(label = "🔲 $currentResizeLabel", onClick = {
                        exoPlayerManager.toggleResizeMode()
                        currentResizeLabel = exoPlayerManager.aspectRatioHelper.currentMode.value.label
                        userInteractionCount++
                    })
                    TvPlayerButton(label = "⚙ Ajustes", onClick = {
                        showSettingsDialog = true
                        userInteractionCount++
                    })
                    TvPlayerButton(label = "🚪 Salir", isDanger = true, onClick = {
                        exoPlayerManager.stop("Botón Salir TV")
                        onCloseClick()
                    })
                }
            }
        }

        if (showSettingsDialog) {
            TvPlayerSettingsDialog(
                title = title,
                isSaved = isSaved,
                currentQuality = currentQuality,
                onCycleQuality = {
                    val q = exoPlayerManager.cycleVideoQuality()
                    currentQuality = q
                    q
                },
                subtitlesEnabled = subsEnabled,
                onToggleSubtitles = {
                    val st = exoPlayerManager.toggleSubtitles()
                    subsEnabled = st
                    st
                },
                onSaveToWatchLater = onSaveToWatchLater,
                onExitPlayback = onCloseClick,
                onDismiss = { showSettingsDialog = false; userInteractionCount++ }
            )
        }
    }
}

@Composable
private fun TvPlayerButton(
    label: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
    isDanger: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isFocused -> if (isDanger) Color(0xFFEF4444) else if (isPrimary) Color(0xFFFF9500) else Color(0xFF38BDF8)
        isDanger -> Color(0x992D1515)
        isPrimary -> Color(0xD9E65100)
        else -> Color(0xB31E293B)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .height(38.dp)
            .scale(if (isFocused) 1.1f else 1.0f)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
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

private fun formatTvTime(ms: Long): String {
    val totalSec = ms / 1000
    val sec = totalSec % 60
    val min = (totalSec / 60) % 60
    val hrs = totalSec / 3600
    return if (hrs > 0) String.format("%02d:%02d:%02d", hrs, min, sec) else String.format("%02d:%02d", min, sec)
}
