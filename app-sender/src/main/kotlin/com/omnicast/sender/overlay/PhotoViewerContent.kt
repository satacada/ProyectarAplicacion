package com.omnicast.sender.overlay

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

@Composable
fun PhotoViewerContent(
    uris: List<Uri>,
    currentIndex: Int,
    currentBitmap: ImageBitmap? = null,
    onIndexChange: (Int) -> Unit,
    onTransform: (Float, Float, Float, Float) -> Unit,
    onClose: () -> Unit
) {
    var scale by remember(currentIndex) { mutableStateOf(1f) }
    var panX by remember(currentIndex) { mutableStateOf(0f) }
    var panY by remember(currentIndex) { mutableStateOf(0f) }
    var rotation by remember(currentIndex) { mutableFloatStateOf(0f) }
    val currentUri = uris.getOrNull(currentIndex)
    var localBitmap by remember(currentUri) { mutableStateOf<ImageBitmap?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeBitmap = currentBitmap ?: localBitmap

    LaunchedEffect(currentUri) {
        if (currentUri != null && currentBitmap == null) {
            withContext(Dispatchers.IO) {
                try {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(currentUri)?.use {
                        BitmapFactory.decodeStream(it, null, bounds)
                    }
                    var inSample = 1
                    val maxDim = 1920
                    if (bounds.outWidth > maxDim || bounds.outHeight > maxDim) {
                        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
                        inSample = (maxSide / maxDim).coerceAtLeast(1)
                    }
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = inSample
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    val bmp = context.contentResolver.openInputStream(currentUri)?.use {
                        BitmapFactory.decodeStream(it, null, decodeOpts)
                    }
                    if (bmp != null) {
                        localBitmap = bmp.asImageBitmap()
                    }
                    Unit
                } catch (_: Throwable) {
                    try {
                        context.contentResolver.openInputStream(currentUri)?.use {
                            localBitmap = BitmapFactory.decodeStream(it)?.asImageBitmap()
                        }
                    } catch (_: Throwable) {}
                    Unit
                }
            }
        }
    }

    fun applyZoomStep(delta: Float) {
        val newScale = (scale + delta).coerceIn(0.40f, 5.0f)
        scale = newScale
        onTransform(scale, panX, panY, rotation)
    }

    fun resetZoom() {
        scale = 1.0f
        panX = 0f
        panY = 0f
        onTransform(1.0f, 0f, 0f, rotation)
    }

    fun rotate90() {
        rotation = (rotation + 90f) % 360f
        panX = 0f
        panY = 0f
        onTransform(scale, 0f, 0f, rotation)
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
            .pointerInput(currentIndex) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            resetZoom()
                        } else {
                            scale = 2.0f
                            panX = 0f
                            panY = 0f
                            onTransform(2.0f, 0f, 0f, rotation)
                        }
                    }
                )
            }
            .pointerInput(currentIndex) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var panAccumulator = Offset.Zero
                    var wasZooming = false

                    do {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.filter { it.pressed }
                        val pointerCount = activePointers.size

                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val isPinch = abs(zoom - 1f) > 0.005f || pointerCount > 1

                        if (isPinch) {
                            wasZooming = true
                            scale = (scale * zoom).coerceIn(0.40f, 5.0f)
                            panX += pan.x
                            panY += pan.y
                            onTransform(scale, panX, panY, rotation)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else if (scale > 1.05f) {
                            panX += pan.x
                            panY += pan.y
                            onTransform(scale, panX, panY, rotation)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else {
                            panAccumulator += pan
                            if (abs(panAccumulator.x) > 25f) {
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (!wasZooming && scale <= 1.05f) {
                        if (panAccumulator.x < -40f) {
                            onNext()
                        } else if (panAccumulator.x > 40f) {
                            onPrev()
                        }
                        panX = 0f
                        panY = 0f
                        onTransform(1.0f, 0f, 0f, 0f)
                    }
                }
            }
    ) {
        val bmp = activeBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp, contentDescription = "Foto Actual",
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = panX; translationY = panY; rotationZ = rotation
                },
                contentScale = ContentScale.Fit
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(36.dp))
            }
        }

        val zoomPct = ((scale - 1f) * 100).roundToInt()
        val zoomLabel = when {
            abs(scale - 1f) < 0.02f -> "1:1"
            zoomPct > 0 -> "+$zoomPct%"
            else -> "$zoomPct%"
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp, start = 12.dp, end = 12.dp)
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(zoomLabel, color = Color.White, fontSize = 12.sp)
                FilledTonalButton(onClick = { applyZoomStep(-0.15f) }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("-15%", fontSize = 10.sp) }
                FilledTonalButton(onClick = { resetZoom() }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("1:1", fontSize = 10.sp) }
                FilledTonalButton(onClick = { applyZoomStep(0.25f) }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("+25%", fontSize = 10.sp) }
                FilledTonalButton(onClick = { rotate90() }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("⟳ 90°", fontSize = 10.sp) }
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
            onRotate = { rotate90() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        SmallFloatingActionButton(
            onClick = { rotate90() },
            containerColor = Color(0xFF0284C7),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 90.dp)
        ) {
            Text("⟳", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
