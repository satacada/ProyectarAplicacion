package com.omnicast.receiver.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.omnicast.receiver.webrtc.WebRtcReceiverManager

/**
 * Gestor del estado de visualización del overlay de telemetría HUD en Android TV.
 */
object TvTelemetryManager {
    var isVisible by mutableStateOf(false)
    var activeWebRtc by mutableStateOf<WebRtcReceiverManager?>(null)

    fun toggle() { isVisible = !isVisible }
}

val WebRtcReceiverManager.Companion.effectiveFrameW: Int
    get() = TvTelemetryManager.activeWebRtc?.effectiveFrameW ?: 0

val WebRtcReceiverManager.Companion.effectiveFrameH: Int
    get() = TvTelemetryManager.activeWebRtc?.effectiveFrameH ?: 0

val WebRtcReceiverManager.Companion.currentMode: String
    get() = TvTelemetryManager.activeWebRtc?.currentMode ?: "FIT"

val WebRtcReceiverManager.Companion.currentZoom: Float
    get() = TvTelemetryManager.activeWebRtc?.currentZoom ?: 1.0f

val WebRtcReceiverManager.Companion.isAppProjectionMode: Boolean
    get() = TvTelemetryManager.activeWebRtc?.isAppProjectionMode ?: false

val WebRtcReceiverManager.Companion.isLandscape: Boolean
    get() = TvTelemetryManager.activeWebRtc?.isLandscape ?: true

/**
 * Overlay de telemetría y métricas en tiempo real (HUD) para Android TV.
 * Permite supervisar resolución, escala, proyección, RAM y estado de sesión.
 */
@Composable
fun BoxScope.TvTelemetryOverlay(modifier: Modifier = Modifier) {
    if (!TvTelemetryManager.isVisible) return

    val shape = RoundedCornerShape(8.dp)
    val bgColor = Color(0xDD0F172A)
    val greenColor = Color(0xFF22C55E)
    val borderColor = greenColor.copy(alpha = 0.4f)

    val runtime = Runtime.getRuntime()
    val totalMemMb = runtime.totalMemory() / (1024 * 1024)
    val freeMemMb = runtime.freeMemory() / (1024 * 1024)
    val usedMemMb = totalMemMb - freeMemMb
    val maxMemMb = runtime.maxMemory() / (1024 * 1024)

    val frameW = WebRtcReceiverManager.effectiveFrameW
    val frameH = WebRtcReceiverManager.effectiveFrameH
    val orientation = if (frameW >= frameH) "Horizontal" else "Vertical"
    val resStr = if (frameW > 0 && frameH > 0) "${frameW}x${frameH} ($orientation)" else "Sin flujo activo"

    val modeStr = WebRtcReceiverManager.currentMode
    val zoomStr = "${(WebRtcReceiverManager.currentZoom * 100).toInt()}%"
    val projStr = if (WebRtcReceiverManager.isAppProjectionMode) "Por App" else "Completa"

    Box(
        modifier = modifier
            .align(Alignment.TopStart)
            .padding(24.dp)
            .zIndex(100f)
            .background(color = bgColor, shape = shape)
            .border(border = BorderStroke(1.dp, borderColor), shape = shape)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.widthIn(min = 280.dp, max = 420.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📡 OMNICAST HUD",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "● EN VIVO",
                    color = greenColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            TelemetryRow(
                label = "Resolución:",
                value = resStr
            )

            TelemetryRow(
                label = "Modo / Zoom:",
                value = "$modeStr | Zoom: $zoomStr | Proy: $projStr"
            )

            TelemetryRow(
                label = "Memoria RAM:",
                value = "${usedMemMb} MB / ${totalMemMb} MB (Máx: ${maxMemMb} MB)"
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Alternar: Tecla INFO / 1 en control",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = Color(0xFFF1F5F9),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal
        )
    }
}
