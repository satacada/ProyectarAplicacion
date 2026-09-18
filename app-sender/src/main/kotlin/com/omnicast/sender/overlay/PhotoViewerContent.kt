package com.omnicast.sender.overlay

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PhotoViewerContent(
    uris: List<Uri>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onAddMore: () -> Unit,
    onTransform: (Float, Float, Float) -> Unit,
    onClose: () -> Unit
) {
    var scale by remember(currentIndex) { mutableStateOf(1f) }
    var panX by remember(currentIndex) { mutableStateOf(0f) }
    var panY by remember(currentIndex) { mutableStateOf(0f) }
    var bitmap by remember(currentIndex) { mutableStateOf<ImageBitmap?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUri = uris.getOrNull(currentIndex)

    LaunchedEffect(currentUri) {
        if (currentUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(currentUri)?.use {
                        bitmap = BitmapFactory.decodeStream(it)?.asImageBitmap()
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    fun applyZoomStep(delta: Float) {
        val newScale = (scale + delta).coerceIn(0.40f, 5.0f)
        scale = newScale
        onTransform(scale, panX, panY)
    }

    fun resetZoom() {
        scale = 1.0f
        panX = 0f
        panY = 0f
        onTransform(1.0f, 0f, 0f)
    }

    val onNext = {
        if (uris.isNotEmpty()) onIndexChange((currentIndex + 1) % uris.size)
    }
    val onPrev = {
        if (uris.isNotEmpty()) onIndexChange(if (currentIndex > 0) currentIndex - 1 else uris.size - 1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(currentIndex, scale) {
                if (scale > 1.05f) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(0.40f, 5.0f)
                        scale = newScale
                        panX += pan.x
                        panY += pan.y
                        onTransform(scale, panX, panY)
                    }
                } else {
                    var dragAccumulator = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulator += dragAmount
                        },
                        onDragEnd = {
                            if (dragAccumulator < -60f) onNext()
                            else if (dragAccumulator > 60f) onPrev()
                            dragAccumulator = 0f
                        }
                    )
                }
            }
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp, contentDescription = "Foto Actual",
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = panX; translationY = panY
                },
                contentScale = ContentScale.Fit
            )
        }

        val zoomPct = ((scale - 1f) * 100).roundToInt()
        val zoomLabel = when {
            abs(scale - 1f) < 0.02f -> "Zoom: 1:1"
            zoomPct > 0 -> "Zoom: +$zoomPct% (${String.format(java.util.Locale.US, "%.1fx", scale)})"
            else -> "Zoom: $zoomPct% (${String.format(java.util.Locale.US, "%.1fx", scale)})"
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp, start = 12.dp, end = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(zoomLabel, color = Color.White, fontSize = 12.sp)
                FilledTonalButton(onClick = { applyZoomStep(-0.15f) }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("-15%", fontSize = 10.sp) }
                FilledTonalButton(onClick = { resetZoom() }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("1:1", fontSize = 10.sp) }
                FilledTonalButton(onClick = { applyZoomStep(0.25f) }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("+25%", fontSize = 10.sp) }
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Cerrar", fontSize = 10.sp, color = Color.White)
                }
            }
        }

        PhotoViewerBottomBar(
            currentIndex = currentIndex,
            totalCount = uris.size,
            onPrev = onPrev,
            onNext = onNext,
            onAddMore = onAddMore,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
