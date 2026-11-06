package com.omnicast.sender.overlay

import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
internal fun VideoCastGestureHint(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier.padding(top = 104.dp)) {
        Surface(color = Color(0xE60D1322), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7))) {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🤏", fontSize = 15.sp)
                Text("Pellizca y mueve con el dedo para enfocar en la TV", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFBAE6FD))
            }
        }
    }
}

@Composable
internal fun VideoCastPlayerScreen(
    title: String,
    subtitle: String,
    uri: Uri,
    hasMultipleVideos: Boolean,
    onPrevVideo: () -> Unit,
    onNextVideo: () -> Unit,
    onBack: () -> Unit,
    onStop: () -> Unit,
    onSendControl: (String, Long) -> Unit,
    onSendTransform: (Float, Float, Float) -> Unit,
    onBindVideoView: (VideoView?) -> Unit,
    onBindMediaPlayer: (MediaPlayer?) -> Unit,
    onVideoCompleted: () -> Unit = {}
) {
    var isPlaying by remember(uri) { mutableStateOf(true) }
    var currentMs by remember(uri) { mutableLongStateOf(0L) }
    var durationMs by remember(uri) { mutableLongStateOf(1L) }
    var showControls by remember { mutableStateOf(true) }
    var phoneMuted by remember { mutableStateOf(true) }
    var scale by remember(uri) { mutableFloatStateOf(1f) }
    var panX by remember(uri) { mutableFloatStateOf(0f) }
    var panY by remember(uri) { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize(1080, 1920)) }
    var showGestureHint by remember { mutableStateOf(true) }
    var activeVideoView by remember { mutableStateOf<VideoView?>(null) }
    var activeMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun dispatchTransform(s: Float, px: Float, py: Float) {
        val w = containerSize.width.coerceAtLeast(1).toFloat()
        val h = containerSize.height.coerceAtLeast(1).toFloat()
        onSendTransform(s, px / w, py / h)
    }

    fun resetVideoZoom() {
        scale = 1f; panX = 0f; panY = 0f
        dispatchTransform(1f, 0f, 0f)
    }

    LaunchedEffect(Unit) { delay(4500); showGestureHint = false }

    LaunchedEffect(isPlaying, uri) {
        while (true) {
            activeVideoView?.let { v ->
                if (v.isPlaying) {
                    currentMs = v.currentPosition.toLong()
                    val dur = v.duration.toLong()
                    if (dur > 0L) durationMs = dur
                }
            }
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F17))
            .onSizeChanged { if (it.width > 0 && it.height > 0) containerSize = it }
            .pointerInput(uri) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = {
                        showGestureHint = false
                        if (scale > 1.05f) resetVideoZoom() else {
                            scale = 2.0f; panX = 0f; panY = 0f
                            dispatchTransform(2.0f, 0f, 0f)
                        }
                    }
                )
            }
            .pointerInput(uri) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var lastSendTs = 0L
                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.count { it.pressed }
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val isPinch = abs(zoom - 1f) > 0.005f || pointerCount > 1
                        if (isPinch || scale > 1.02f) {
                            showGestureHint = false
                            if (isPinch) scale = (scale * zoom).coerceIn(0.60f, 4.5f)
                            val maxPanX = (containerSize.width * (scale - 1f).coerceAtLeast(0.15f)) * 0.65f
                            val maxPanY = (containerSize.height * (scale - 1f).coerceAtLeast(0.15f)) * 0.65f
                            panX = (panX + pan.x).coerceIn(-maxPanX, maxPanX)
                            panY = (panY + pan.y).coerceIn(-maxPanY, maxPanY)
                            val now = System.currentTimeMillis()
                            if (now - lastSendTs >= 24L) {
                                dispatchTransform(scale, panX, panY)
                                lastSendTs = now
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    activeVideoView = this
                    onBindVideoView(this)
                }
            },
            update = { view ->
                if (view.tag != uri) {
                    view.tag = uri
                    view.setVideoURI(uri)
                    view.setOnPreparedListener { mp ->
                        activeMediaPlayer = mp
                        onBindMediaPlayer(mp)
                        mp.setVolume(if (phoneMuted) 0f else 1f, if (phoneMuted) 0f else 1f)
                        durationMs = mp.duration.toLong().coerceAtLeast(1L)
                        mp.start(); isPlaying = true
                    }
                    view.setOnCompletionListener {
                        if (hasMultipleVideos) onNextVideo() else {
                            isPlaying = false
                            currentMs = durationMs
                            onVideoCompleted()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale; scaleY = scale; translationX = panX; translationY = panY
            }
        )

        VideoCastGestureHint(visible = showGestureHint, modifier = Modifier.align(Alignment.TopCenter))

        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0x77000000))) {
                VideoPlayerTopBar(
                    title = title, subtitle = subtitle, currentZoom = scale,
                    onResetZoom = { resetVideoZoom() },
                    onBack = onBack, onStop = onStop, modifier = Modifier.align(Alignment.TopCenter)
                )

                val isNearEnd = durationMs > 2000L && currentMs >= (durationMs - 1500L)
                VideoPlayerCenterControls(
                    isPlaying = isPlaying, isNearEnd = isNearEnd, hasMultipleVideos = hasMultipleVideos,
                    onPrevVideo = onPrevVideo,
                    onRewind = {
                        val target = (currentMs - 10000L).coerceAtLeast(0L)
                        activeVideoView?.seekTo(target.toInt()); currentMs = target; onSendControl("SEEK", target)
                    },
                    onPlayPause = {
                        activeVideoView?.let { v ->
                            if (v.isPlaying) {
                                v.pause(); isPlaying = false; onSendControl("PAUSE", currentMs)
                            } else {
                                if (isNearEnd) { v.seekTo(0); currentMs = 0L; onSendControl("SEEK", 0L) }
                                v.start(); isPlaying = true; onSendControl("PLAY", if (isNearEnd) 0L else currentMs)
                            }
                        }
                    },
                    onForward = {
                        val target = (currentMs + 10000L).coerceAtMost(durationMs)
                        activeVideoView?.seekTo(target.toInt()); currentMs = target; onSendControl("SEEK", target)
                    },
                    onNextVideo = onNextVideo,
                    modifier = Modifier.align(Alignment.Center)
                )

                VideoPlayerBottomBar(
                    currentMs = currentMs, durationMs = durationMs, phoneMuted = phoneMuted,
                    onSeek = { newMs ->
                        currentMs = newMs; activeVideoView?.seekTo(newMs.toInt()); onSendControl("SEEK", newMs)
                    },
                    onToggleMute = {
                        phoneMuted = !phoneMuted
                        val vol = if (phoneMuted) 0f else 1f
                        activeMediaPlayer?.setVolume(vol, vol)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
