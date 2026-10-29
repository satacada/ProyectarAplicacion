package com.omnicast.sender.overlay

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
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
import androidx.lifecycle.lifecycleScope
import com.omnicast.core.protocol.SignalingMessage
import com.omnicast.sender.gallery.GalleryMediaHelper
import com.omnicast.sender.gallery.GalleryPhotoItem
import com.omnicast.sender.gallery.GallerySessionHolder
import com.omnicast.sender.media.LocalImageServer
import com.omnicast.sender.network.QuickSignalingHelper
import com.omnicast.sender.ui.SenderMainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class VideoCastPlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_VIDEO_URI = "EXTRA_VIDEO_URI"
        const val EXTRA_VIDEO_TITLE = "EXTRA_VIDEO_TITLE"
        const val RESULT_BACK_TO_GALLERY = 201
    }

    private var videoView: VideoView? = null
    private var mediaPlayer: MediaPlayer? = null
    private val videoList = mutableStateListOf<GalleryPhotoItem>()
    private val folderNameState = mutableStateOf("")
    private val currentUriState = mutableStateOf<Uri?>(null)
    private val currentTitleState = mutableStateOf("Video de Galería")
    private val currentIndexState = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FloatingIndicatorService.setGalleryActive(true)
        com.omnicast.sender.service.CastSessionState.isGalleryCasting = true

        val uriString = intent.getStringExtra(EXTRA_VIDEO_URI)
        val title = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Video de Galería"
        val initialUri = uriString?.let { Uri.parse(it) }

        if (initialUri == null && GallerySessionHolder.activeVideos.isEmpty()) {
            finish(); return
        }

        val sessionVideos = GallerySessionHolder.activeVideos
        if (sessionVideos.isNotEmpty()) {
            videoList.clear(); videoList.addAll(sessionVideos)
            val idx = GallerySessionHolder.activeVideoIndex.coerceIn(0, sessionVideos.lastIndex)
            currentIndexState.intValue = idx
            currentUriState.value = initialUri ?: sessionVideos[idx].uri
            currentTitleState.value = sessionVideos[idx].name.ifBlank { title }
            folderNameState.value = GallerySessionHolder.activeVideoFolderName
        } else if (initialUri != null) {
            currentUriState.value = initialUri
            currentTitleState.value = title
            videoList.add(GalleryPhotoItem(0L, initialUri, title, "Videos", System.currentTimeMillis(), isVideo = true, mimeType = "video/mp4"))
        }

        val targetVideoUri = currentUriState.value
        if (targetVideoUri != null && videoList.size <= 1 && GalleryMediaHelper.hasGalleryPermission(this, "video/*")) {
            lifecycleScope.launch(Dispatchers.IO) {
                val (fPath, ctxVideos) = GalleryMediaHelper.loadFolderContextForUri(this@VideoCastPlayerActivity, targetVideoUri, isVideo = true)
                if (ctxVideos.size > 1) {
                    withContext(Dispatchers.Main) {
                        val targetId = targetVideoUri.lastPathSegment?.substringAfterLast(':')?.filter { it.isDigit() } ?: ""
                        val mIdx = ctxVideos.indexOfFirst { it.uri == targetVideoUri || (targetId.isNotBlank() && it.id.toString() == targetId) }.coerceAtLeast(0)
                        val fLabel = fPath.substringAfterLast('/').ifBlank { folderNameState.value.ifBlank { "Videos" } }
                        GallerySessionHolder.setVideos(ctxVideos, mIdx, fLabel)
                        videoList.clear(); videoList.addAll(ctxVideos)
                        currentIndexState.intValue = mIdx; folderNameState.value = fLabel
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { returnToFolder() }
        })

        setContent {
            val activeUri = currentUriState.value
            if (activeUri != null) {
                val totalVideos = videoList.size.coerceAtLeast(1)
                val folderName = folderNameState.value.ifBlank { "Videos" }
                val sub = "📁 $folderName • ${currentIndexState.intValue + 1} / $totalVideos"

                VideoPlayerScreen(
                    title = currentTitleState.value,
                    subtitle = sub,
                    uri = activeUri,
                    hasMultipleVideos = totalVideos >= 1,
                    onPrevVideo = { changeVideo(-1) },
                    onNextVideo = { changeVideo(1) },
                    onBack = { returnToFolder() },
                    onStop = { returnToFolder() },
                    onSendControl = { act, pos ->
                        QuickSignalingHelper.sendQuickMessage(this, SignalingMessage.PlayControl(act, pos))
                    },
                    onSendTransform = { scale, normPanX, normPanY ->
                        val mode = if (scale <= 1.02f && abs(normPanX) < 0.01f && abs(normPanY) < 0.01f) "FIT" else "ZOOM"
                        QuickSignalingHelper.sendQuickMessage(this, SignalingMessage.SetDisplayMode(mode, scale, normPanY, normPanX))
                    }
                )
            }
        }
    }

    private fun changeVideo(delta: Int) {
        if (videoList.isEmpty()) return
        val nextIdx = (currentIndexState.intValue + delta).let {
            if (it < 0) videoList.lastIndex else if (it >= videoList.size) 0 else it
        }
        currentIndexState.intValue = nextIdx
        GallerySessionHolder.activeVideoIndex = nextIdx
        val nextItem = videoList[nextIdx]
        currentUriState.value = nextItem.uri
        currentTitleState.value = nextItem.name
        QuickSignalingHelper.sendQuickMessage(this, SignalingMessage.SetDisplayMode("FIT", 1.0f, 0f, 0f))
        val mediaUrl = LocalImageServer.serveMedia(nextItem.uri, nextItem.mimeType, this)
        SenderMainActivity.sendDirectStreamStatic(this, mediaUrl, nextItem.name)
    }

    private fun returnToFolder() {
        videoView?.stopPlayback()
        com.omnicast.sender.service.CastSessionState.isGalleryCasting = false
        com.omnicast.sender.stream.ActiveStreamHolder.stopCasting()
        com.omnicast.sender.service.CastMediaNotificationService.stop(this)
        QuickSignalingHelper.sendQuickMessage(this, SignalingMessage.PlayControl("STOP"))
        val resIntent = Intent().apply { data = currentUriState.value }
        setResult(RESULT_BACK_TO_GALLERY, resIntent)
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    @Composable
    private fun VideoPlayerScreen(
        title: String, subtitle: String, uri: Uri, hasMultipleVideos: Boolean,
        onPrevVideo: () -> Unit, onNextVideo: () -> Unit, onBack: () -> Unit,
        onStop: () -> Unit, onSendControl: (String, Long) -> Unit,
        onSendTransform: (Float, Float, Float) -> Unit
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
                videoView?.let { v ->
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
                        videoView = this
                    }
                },
                update = { view ->
                    if (view.tag != uri) {
                        view.tag = uri
                        view.setVideoURI(uri)
                        view.setOnPreparedListener { mp ->
                            mediaPlayer = mp
                            mp.setVolume(if (phoneMuted) 0f else 1f, if (phoneMuted) 0f else 1f)
                            durationMs = mp.duration.toLong().coerceAtLeast(1L)
                            mp.start(); isPlaying = true
                        }
                        view.setOnCompletionListener {
                            if (videoList.isNotEmpty()) onNextVideo() else { isPlaying = false; currentMs = durationMs }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = panX; translationY = panY
                }
            )

            AnimatedVisibility(visible = showGestureHint, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter).padding(top = 104.dp)) {
                Surface(color = Color(0xE60D1322), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7))) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🤏", fontSize = 15.sp)
                        Text("Pellizca y mueve con el dedo para enfocar en la TV", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFBAE6FD))
                    }
                }
            }

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
                        onRewind = { val target = (currentMs - 10000L).coerceAtLeast(0L); videoView?.seekTo(target.toInt()); currentMs = target; onSendControl("SEEK", target) },
                        onPlayPause = {
                            videoView?.let { v ->
                                if (v.isPlaying) { v.pause(); isPlaying = false; onSendControl("PAUSE", currentMs) }
                                else { if (isNearEnd) { v.seekTo(0); currentMs = 0L; onSendControl("SEEK", 0L) }; v.start(); isPlaying = true; onSendControl("PLAY", if (isNearEnd) 0L else currentMs) }
                            }
                        },
                        onForward = { val target = (currentMs + 10000L).coerceAtMost(durationMs); videoView?.seekTo(target.toInt()); currentMs = target; onSendControl("SEEK", target) },
                        onNextVideo = onNextVideo,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    VideoPlayerBottomBar(
                        currentMs = currentMs, durationMs = durationMs, phoneMuted = phoneMuted,
                        onSeek = { newMs -> currentMs = newMs; videoView?.seekTo(newMs.toInt()); onSendControl("SEEK", newMs) },
                        onToggleMute = { phoneMuted = !phoneMuted; val vol = if (phoneMuted) 0f else 1f; mediaPlayer?.setVolume(vol, vol) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.omnicast.sender.service.CastSessionState.isGalleryCasting = false
        videoView?.stopPlayback(); videoView = null; mediaPlayer = null
        if (!com.omnicast.sender.service.CastSessionState.isGalleryCasting &&
            !com.omnicast.sender.stream.ActiveStreamHolder.isCasting.value) {
            LocalImageServer.stop()
        }
    }
}
