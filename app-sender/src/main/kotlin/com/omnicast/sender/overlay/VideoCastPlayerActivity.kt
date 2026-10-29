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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.omnicast.core.protocol.SignalingMessage
import com.omnicast.sender.gallery.GallerySessionHolder
import com.omnicast.sender.media.LocalImageServer
import com.omnicast.sender.network.QuickSignalingHelper
import com.omnicast.sender.ui.SenderMainActivity
import kotlinx.coroutines.delay

class VideoCastPlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_VIDEO_URI = "EXTRA_VIDEO_URI"
        const val EXTRA_VIDEO_TITLE = "EXTRA_VIDEO_TITLE"
        const val RESULT_BACK_TO_GALLERY = 201
    }

    private var videoView: VideoView? = null
    private var mediaPlayer: MediaPlayer? = null
    private val currentUriState = mutableStateOf<Uri?>(null)
    private val currentTitleState = mutableStateOf("Video de Galería")
    private val currentIndexState = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.omnicast.sender.service.CastSessionState.isGalleryCasting = true

        val uriString = intent.getStringExtra(EXTRA_VIDEO_URI)
        val title = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Video de Galería"
        val initialUri = uriString?.let { Uri.parse(it) }

        if (initialUri == null && GallerySessionHolder.activeVideos.isEmpty()) {
            finish()
            return
        }

        val sessionVideos = GallerySessionHolder.activeVideos
        if (sessionVideos.isNotEmpty()) {
            val idx = GallerySessionHolder.activeVideoIndex.coerceIn(0, sessionVideos.lastIndex)
            currentIndexState.intValue = idx
            currentUriState.value = initialUri ?: sessionVideos[idx].uri
            currentTitleState.value = sessionVideos[idx].name.ifBlank { title }
        } else {
            currentUriState.value = initialUri
            currentTitleState.value = title
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToFolder()
            }
        })

        setContent {
            val activeUri = currentUriState.value
            if (activeUri != null) {
                val totalVideos = GallerySessionHolder.activeVideos.size
                val folderName = GallerySessionHolder.activeVideoFolderName
                val sub = if (totalVideos > 0 && folderName.isNotBlank()) {
                    "📁 $folderName (${currentIndexState.intValue + 1}/$totalVideos)"
                } else if (folderName.isNotBlank()) {
                    "📁 $folderName"
                } else {
                    "TV Box Conectada"
                }

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
                    }
                )
            }
        }
    }

    private fun changeVideo(delta: Int) {
        val list = GallerySessionHolder.activeVideos
        if (list.isEmpty()) return
        val nextIdx = (currentIndexState.intValue + delta).let {
            if (it < 0) list.lastIndex else if (it >= list.size) 0 else it
        }
        currentIndexState.intValue = nextIdx
        GallerySessionHolder.activeVideoIndex = nextIdx
        val nextItem = list[nextIdx]
        currentUriState.value = nextItem.uri
        currentTitleState.value = nextItem.name
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
        title: String,
        subtitle: String,
        uri: Uri,
        hasMultipleVideos: Boolean,
        onPrevVideo: () -> Unit,
        onNextVideo: () -> Unit,
        onBack: () -> Unit,
        onStop: () -> Unit,
        onSendControl: (String, Long) -> Unit
    ) {
        var isPlaying by remember(uri) { mutableStateOf(true) }
        var currentMs by remember(uri) { mutableLongStateOf(0L) }
        var durationMs by remember(uri) { mutableLongStateOf(1L) }
        var showControls by remember { mutableStateOf(true) }
        var phoneMuted by remember { mutableStateOf(true) }

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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showControls = !showControls }
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
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
                            mp.start()
                            isPlaying = true
                        }
                        view.setOnCompletionListener {
                            if (GallerySessionHolder.activeVideos.isNotEmpty()) {
                                onNextVideo()
                            } else {
                                isPlaying = false
                                currentMs = durationMs
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x88000000))) {
                    VideoPlayerTopBar(
                        title = title,
                        subtitle = subtitle,
                        onBack = onBack,
                        onStop = onStop,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    val isNearEnd = durationMs > 2000L && currentMs >= (durationMs - 1500L)
                    VideoPlayerCenterControls(
                        isPlaying = isPlaying,
                        isNearEnd = isNearEnd,
                        hasMultipleVideos = hasMultipleVideos,
                        onPrevVideo = onPrevVideo,
                        onRewind = {
                            val target = (currentMs - 10000L).coerceAtLeast(0L)
                            videoView?.seekTo(target.toInt())
                            currentMs = target
                            onSendControl("SEEK", target)
                        },
                        onPlayPause = {
                            videoView?.let { v ->
                                if (v.isPlaying) {
                                    v.pause()
                                    isPlaying = false
                                    onSendControl("PAUSE", currentMs)
                                } else {
                                    if (isNearEnd) {
                                        v.seekTo(0)
                                        currentMs = 0L
                                        onSendControl("SEEK", 0L)
                                    }
                                    v.start()
                                    isPlaying = true
                                    onSendControl("PLAY", if (isNearEnd) 0L else currentMs)
                                }
                            }
                        },
                        onForward = {
                            val target = (currentMs + 10000L).coerceAtMost(durationMs)
                            videoView?.seekTo(target.toInt())
                            currentMs = target
                            onSendControl("SEEK", target)
                        },
                        onNextVideo = onNextVideo,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    VideoPlayerBottomBar(
                        currentMs = currentMs,
                        durationMs = durationMs,
                        phoneMuted = phoneMuted,
                        onSeek = { newMs ->
                            currentMs = newMs
                            videoView?.seekTo(newMs.toInt())
                            onSendControl("SEEK", newMs)
                        },
                        onToggleMute = {
                            phoneMuted = !phoneMuted
                            val vol = if (phoneMuted) 0f else 1f
                            mediaPlayer?.setVolume(vol, vol)
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.omnicast.sender.service.CastSessionState.isGalleryCasting = false
        videoView?.stopPlayback()
        videoView = null
        mediaPlayer = null
        if (!com.omnicast.sender.service.CastSessionState.isGalleryCasting &&
            !com.omnicast.sender.stream.ActiveStreamHolder.isCasting.value) {
            LocalImageServer.stop()
        }
    }
}
