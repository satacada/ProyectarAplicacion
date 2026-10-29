package com.omnicast.sender.overlay

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Composable
fun PhotoViewerContent(
    uris: List<Uri>,
    currentIndex: Int,
    currentBitmap: ImageBitmap? = null,
    onIndexChange: (Int) -> Unit,
    onTransform: (Float, Float, Float, Float) -> Unit,
    onClose: () -> Unit
) {
    var scale by remember(currentIndex) { mutableFloatStateOf(1f) }
    var panX by remember(currentIndex) { mutableFloatStateOf(0f) }
    var panY by remember(currentIndex) { mutableFloatStateOf(0f) }
    var rotation by remember(currentIndex) { mutableFloatStateOf(0f) }
    val currentUri = uris.getOrNull(currentIndex)
    var localBitmap by remember(currentUri) { mutableStateOf<ImageBitmap?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeBitmap = currentBitmap ?: localBitmap

    var showGestureHint by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(4500)
        showGestureHint = false
    }

    LaunchedEffect(currentUri) {
        if (currentUri != null && currentBitmap == null) {
            val bmp = com.omnicast.sender.gallery.GalleryBitmapDecoder.decodeBitmap(context, currentUri)
            if (bmp != null) localBitmap = bmp.asImageBitmap()
        }
    }

    fun resetZoom() {
        scale = 1.0f; panX = 0f; panY = 0f
        onTransform(1.0f, 0f, 0f, rotation)
    }

    fun rotateBy(deg: Float) {
        rotation = (rotation + deg) % 360f
        onTransform(scale, panX, panY, rotation)
    }

    val onNext = { if (uris.isNotEmpty()) onIndexChange((currentIndex + 1) % uris.size) }
    val onPrev = { if (uris.isNotEmpty()) onIndexChange(if (currentIndex > 0) currentIndex - 1 else uris.size - 1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F17))
            .pointerInput(currentIndex) {
                detectTapGestures(
                    onDoubleTap = {
                        showGestureHint = false
                        if (scale > 1.05f) resetZoom() else {
                            scale = 2.0f; panX = 0f; panY = 0f
                            onTransform(2.0f, 0f, 0f, rotation)
                        }
                    }
                )
            }
            .pointerInput(currentIndex) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    showGestureHint = false
                    var panAccumulator = Offset.Zero
                    var wasZooming = false
                    var lastTransformTime = 0L
                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.count { it.pressed }
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val isPinch = abs(zoom - 1f) > 0.005f || pointerCount > 1

                        if (isPinch) {
                            wasZooming = true
                            scale = (scale * zoom).coerceIn(0.40f, 5.0f)
                            panX += pan.x; panY += pan.y
                            val now = System.currentTimeMillis()
                            if (now - lastTransformTime >= 16L) {
                                onTransform(scale, panX, panY, rotation)
                                lastTransformTime = now
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else if (scale > 1.05f) {
                            panX += pan.x; panY += pan.y
                            val now = System.currentTimeMillis()
                            if (now - lastTransformTime >= 16L) {
                                onTransform(scale, panX, panY, rotation)
                                lastTransformTime = now
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else {
                            panAccumulator += pan
                            if (abs(panAccumulator.x) > 25f) {
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (wasZooming || scale > 1.05f) {
                        onTransform(scale, panX, panY, rotation)
                    } else {
                        if (panAccumulator.x < -40f) onNext() else if (panAccumulator.x > 40f) onPrev()
                        panX = 0f; panY = 0f
                        onTransform(1.0f, 0f, 0f, rotation)
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

        // Retícula central con punto cian para encuadre en TV
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(64.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = Color(0x6638BDF8), radius = size.width / 2f, style = Stroke(width = 1.2f))
                drawLine(color = Color(0x8838BDF8), start = Offset(c.x, c.y - 12f), end = Offset(c.x, c.y + 12f), strokeWidth = 1.2f)
                drawLine(color = Color(0x8838BDF8), start = Offset(c.x - 12f, c.y), end = Offset(c.x + 12f, c.y), strokeWidth = 1.2f)
                drawCircle(color = Color(0xFF00B4D8), radius = 3.5f)
            }
        }

        // Cápsula Superior: 1:1, -90°, +90° y Salir
        Surface(
            color = Color(0xD91E2433),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 42.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFF283347),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { resetZoom() }
                ) {
                    Text("1:1", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }

                Surface(
                    color = Color(0xFF283347),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { rotateBy(-90f) }
                ) {
                    Text("⟲ -90°", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }

                Surface(
                    color = Color(0xFF283347),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { rotateBy(90f) }
                ) {
                    Text("⟳ +90°", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }

                Surface(
                    color = Color(0xFFDC2626),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { onClose() }
                ) {
                    Text(
                        text = "■ Detener",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Indicador emergente de gestos táctiles (solicitado por usuario)
        AnimatedVisibility(
            visible = showGestureHint,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        ) {
            Surface(
                color = Color(0xE60D1322),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🤏", fontSize = 16.sp)
                    Text(
                        text = "Pellizca para zoom • Desliza para mover en la TV",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFBAE6FD)
                    )
                }
            }
        }

        // Cápsula Inferior: < 1/1 >
        PhotoViewerBottomBar(
            currentIndex = currentIndex,
            totalCount = uris.size,
            onPrev = onPrev,
            onNext = onNext,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
