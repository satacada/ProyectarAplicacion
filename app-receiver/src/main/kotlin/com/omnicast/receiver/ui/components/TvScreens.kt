package com.omnicast.receiver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TvStandbyScreen(ipAddress: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("OmniCast Pro TV", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
                androidx.compose.material3.Surface(color = Color(0xFF0284C7), shape = RoundedCornerShape(8.dp)) {
                    Text("v3.9", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(14.dp).background(Color(0xFF38BDF8), RoundedCornerShape(7.dp)))
                Text("Listo para conectar", fontSize = 20.sp, color = Color(0xFF94A3B8))
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 36.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Dirección de Red Local", color = Color.LightGray, fontSize = 16.sp)
                    Text("$ipAddress:8088", color = Color(0xFF38BDF8), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                    Text("Abre OmniCast en tu celular para conectar automáticamente", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun TvConnectedStandbyScreen(clientAddress: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF064E3B)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Text("OmniCast Pro TV", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
        }
    }
}

@Composable
fun TvMirroringActiveScreen(
    webRtcReceiver: com.omnicast.receiver.webrtc.WebRtcReceiverManager,
    resolution: String,
    fps: Int
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val renderer = webRtcReceiver.surfaceViewRenderer
        if (renderer != null) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = {
                    try {
                        (renderer.parent as? android.view.ViewGroup)?.removeView(renderer)
                    } catch (_: Throwable) {}
                    renderer.apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Proyección de Pantalla Activa", color = Color.White, fontSize = 28.sp)
                Text("$resolution @ ${fps}fps (Decodificación Hardware)", color = Color(0xFF10B981), fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun TvDirectStreamScreen(
    exoPlayerManager: com.omnicast.receiver.directstream.ExoPlayerTvManager,
    title: String,
    streamUrl: String
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val player = exoPlayerManager.player
        if (player != null) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    androidx.media3.ui.PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        subtitleView?.visibility = android.view.View.VISIBLE
                        setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_ALWAYS)
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { pv -> if (pv.player != player) pv.player = player },
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
