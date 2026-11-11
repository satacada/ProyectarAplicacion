package com.omnicast.receiver.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Gestor del Puntero Virtual en Android TV para control tipo Air-Mouse / Touchpad.
 */
object TvVirtualPointerManager {
    var isPointerVisible by mutableStateOf(false)
    var pointerXPct by mutableFloatStateOf(0.5f)
    var pointerYPct by mutableFloatStateOf(0.5f)
    var isClicking by mutableStateOf(false)
    private var hideJob: Job? = null

    fun updatePointer(
        xPct: Float,
        yPct: Float,
        isClick: Boolean,
        isDown: Boolean,
        isVisible: Boolean = true,
        scope: CoroutineScope
    ) {
        pointerXPct = xPct.coerceIn(0f, 1f)
        pointerYPct = yPct.coerceIn(0f, 1f)
        isClicking = isClick || isDown
        isPointerVisible = isVisible

        hideJob?.cancel()
        if (isVisible) {
            hideJob = scope.launch {
                delay(4000L)
                isPointerVisible = false
                isClicking = false
            }
        }
    }
}

/**
 * Overlay de Puntero Virtual Flotante (Air-Mouse / Touchpad remoto) para Android TV.
 */
@Composable
fun BoxScope.TvVirtualPointerOverlay() {
    if (!TvVirtualPointerManager.isPointerVisible) return

    val pointerScale by animateFloatAsState(
        targetValue = if (TvVirtualPointerManager.isClicking) 0.8f else 1.0f,
        label = "pointerScale"
    )
    val pointerColor = if (TvVirtualPointerManager.isClicking) Color(0xCC22C55E) else Color(0xAA0EA5E9)

    BoxWithConstraints(modifier = Modifier.fillMaxSize().zIndex(110f)) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (maxWidth * TvVirtualPointerManager.pointerXPct).roundToPx(),
                        (maxHeight * TvVirtualPointerManager.pointerYPct).roundToPx()
                    )
                }
                .size(24.dp)
                .graphicsLayer {
                    scaleX = pointerScale
                    scaleY = pointerScale
                }
                .background(pointerColor, CircleShape)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}
