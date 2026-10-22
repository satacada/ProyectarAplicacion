package com.omnicast.receiver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun TvConnectedStandbyScreen(
    clientAddress: String,
    onReturnToStandby: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF064E3B)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val ver = androidx.compose.runtime.remember {
                try { "v" + ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName } catch (_: Throwable) { "v3.32" }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("OmniCast Pro TV", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White)
                androidx.compose.material3.Surface(color = Color(0xFF059669), shape = RoundedCornerShape(8.dp)) {
                    Text(ver, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.size(16.dp).background(Color(0xFF10B981), RoundedCornerShape(8.dp)))
                Text("🟢 Celular Conectado", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA7F3D0))
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF047857)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Dispositivo Móvil", color = Color(0xFFA7F3D0), fontSize = 16.sp)
                    Text(clientAddress, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Esperando transmisión de pantalla o flujo de video...", color = Color(0xFFD1FAE5), fontSize = 16.sp)
                    Text("💡 Usa el Asistente Flotante en tu celular para enviar tu partido o video.", color = Color(0xFFE0F2FE), fontSize = 14.sp)
                }
            }
            androidx.compose.material3.Button(
                onClick = onReturnToStandby,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("⮌", color = Color(0xFFA7F3D0), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Volver a la Pantalla Principal (OmniCast)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TvMirroringActiveScreen(
    webRtcReceiver: com.omnicast.receiver.webrtc.WebRtcReceiverManager,
    resolution: String,
    fps: Int,
    onCloseClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val renderer = webRtcReceiver.surfaceViewRenderer
        if (renderer != null) {
            val isVertical = webRtcReceiver.isVertical(resolution)
            val streamAspect = webRtcReceiver.streamAspect(resolution)
            val videoModifier = if (isVertical) {
                Modifier.fillMaxHeight().aspectRatio(streamAspect)
            } else {
                Modifier.fillMaxSize()
            }
            androidx.compose.ui.viewinterop.AndroidView(
                factory = {
                    try {
                        (renderer.parent as? android.view.ViewGroup)?.removeView(renderer)
                    } catch (_: Throwable) {}
                    val lp = if (isVertical) {
                        android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    } else {
                        android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    renderer.apply {
                        layoutParams = lp
                    }
                },
                update = { r ->
                    val targetW = if (isVertical) android.view.ViewGroup.LayoutParams.WRAP_CONTENT else android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    if (r.layoutParams?.width != targetW) {
                        r.layoutParams = android.view.ViewGroup.LayoutParams(targetW, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                    }
                },
                modifier = videoModifier
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Proyección de Pantalla Activa", color = Color.White, fontSize = 28.sp)
                Text("$resolution @ ${fps}fps (Decodificación Hardware)", color = Color(0xFF10B981), fontSize = 18.sp)
            }
        }
        var isExitFocused by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 24.dp)
                .size(38.dp)
                .background(if (isExitFocused) Color(0xFFEF4444) else Color(0x990F172A), RoundedCornerShape(10.dp))
                .border(
                    width = if (isExitFocused) 2.dp else 1.dp,
                    color = if (isExitFocused) Color.White else Color(0x66EF4444),
                    shape = RoundedCornerShape(10.dp)
                )
                .onFocusChanged { isExitFocused = it.isFocused }
                .focusable()
                .clickable { onCloseClick() },
            contentAlignment = Alignment.Center
        ) {
            ModernExitIcon()
        }
    }
}

@Composable
fun TvDirectStreamScreen(
    exoPlayerManager: com.omnicast.receiver.directstream.ExoPlayerTvManager,
    title: String,
    streamUrl: String,
    isSaved: Boolean = false,
    onSaveToWatchLater: () -> Unit = {},
    onCloseClick: () -> Unit = {}
) {
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isGearFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Tuerca pequeña en la parte superior izquierda
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 24.dp)
                .size(38.dp)
                .background(if (isGearFocused) Color(0xFF38BDF8) else Color(0x990F172A), RoundedCornerShape(10.dp))
                .border(
                    width = if (isGearFocused) 2.dp else 1.dp,
                    color = if (isGearFocused) Color.White else Color(0x6638BDF8),
                    shape = RoundedCornerShape(10.dp)
                )
                .onFocusChanged { isGearFocused = it.isFocused }
                .focusable()
                .clickable { showSettingsDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Text("⚙", fontSize = 18.sp, color = if (isGearFocused) Color.Black else Color.White)
        }

        if (showSettingsDialog) {
            TvPlayerSettingsDialog(
                title = title,
                isSaved = isSaved,
                onSaveToWatchLater = onSaveToWatchLater,
                onExitPlayback = onCloseClick,
                onDismiss = { showSettingsDialog = false }
            )
        }
        val player = exoPlayerManager.player
        val currentResizeMode = remember { mutableStateOf(exoPlayerManager.resizeMode) }
        val currentZoom = remember { mutableStateOf(exoPlayerManager.currentZoom) }
        DisposableEffect(exoPlayerManager) {
            exoPlayerManager.onResizeModeChanged = { mode -> currentResizeMode.value = mode }
            exoPlayerManager.onZoomChanged = { z -> currentZoom.value = z }
            onDispose {
                exoPlayerManager.onResizeModeChanged = null
                exoPlayerManager.onZoomChanged = null
                exoPlayerManager.playerView = null
            }
        }
        if (player != null) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    androidx.media3.ui.PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        controllerShowTimeoutMs = 5000
                        controllerAutoShow = true
                        resizeMode = currentResizeMode.value
                        subtitleView?.visibility = android.view.View.VISIBLE
                        setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_ALWAYS)
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        exoPlayerManager.playerView = this
                        isFocusable = true
                        isFocusableInTouchMode = true
                        TvMediaControlsStyler.applyTvFocusHighlight(this)
                    }
                },
                update = { pv ->
                    if (pv.player != player) pv.player = player
                    pv.resizeMode = currentResizeMode.value
                    val z = currentZoom.value
                    pv.scaleX = if (z > 0f) z else 1.0f
                    pv.scaleY = if (z > 0f) z else 1.0f
                },
                modifier = Modifier.fillMaxSize()
            )
            val showBanner = remember(title, streamUrl) { mutableStateOf(true) }
            LaunchedEffect(title, streamUrl) {
                kotlinx.coroutines.delay(4000)
                showBanner.value = false
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = showBanner.value,
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 24.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎬", fontSize = 14.sp)
                        Column {
                            Text(text = title.ifBlank { "Reproducción Directa" }, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text("🟢 ExoPlayer Nativo", color = Color(0xFF38BDF8), fontSize = 10.sp)
                        }
                    }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Reproduciendo Video Nativo", color = Color(0xFF38BDF8), fontSize = 28.sp)
                Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("URL: $streamUrl", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}
