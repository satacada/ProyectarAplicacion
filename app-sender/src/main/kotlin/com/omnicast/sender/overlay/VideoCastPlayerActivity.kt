package com.omnicast.sender.overlay

import android.content.Context
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
import com.omnicast.sender.network.QuickSignalingHelper
import kotlinx.coroutines.delay

class VideoCastPlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_VIDEO_URI = "EXTRA_VIDEO_URI"
        const val EXTRA_VIDEO_TITLE = "EXTRA_VIDEO_TITLE"
        const val RESULT_BACK_TO_GALLERY = 201
    }

    private var videoView: VideoView? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isMuted = true
    private var videoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.omnicast.sender.service.CastSessionState.isGalleryCasting = true

        val uriString = intent.getStringExtra(EXTRA_VIDEO_URI)
        val title = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Video de Galería"
        videoUri = uriString?.let { Uri.parse(it) }

        if (videoUri == null) {
            finish()
            return
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitToGallery()
            }
        })

        setContent {
            VideoPlayerScreen(
                title = title,
                uri = videoUri!!,
                onBack = { exitToGallery() },
                onStop = { stopAndExitToMain() },
                onSendControl = { act, pos ->
                    QuickSignalingHelper.sendQuickMessage(this, SignalingMessage.PlayControl(act, pos))
                }
            )
        }
    }

    private fun exitToGallery() {
        videoView?.stopPlayback()
        val resIntent = Intent().apply { data = videoUri }
        setResult(RESULT_BACK_TO_GALLERY, resIntent)
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun stopAndExitToMain() {
        videoView?.stopPlayback()
        com.omnicast.sender.service.CastSessionState.isGalleryCasting = false
        com.omnicast.sender.stream.ActiveStreamHolder.stopCasting()
        com.omnicast.sender.service.CastMediaNotificationService.stop(this)
        QuickSignalingHelper.sendQuickMessage(this, SignalingMessage.PlayControl("STOP"))
        setResult(PhotoCastViewerActivity.RESULT_EXIT_TO_MAIN)
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    @Composable
    private fun VideoPlayerScreen(
        title: String,
        uri: Uri,
        onBack: () -> Unit,
        onStop: () -> Unit,
        onSendControl: (String, Long) -> Unit
    ) {
        var isPlaying by remember { mutableStateOf(true) }
        var currentMs by remember { mutableLongStateOf(0L) }
        var durationMs by remember { mutableLongStateOf(1L) }
        var showControls by remember { mutableStateOf(true) }
        var phoneMuted by remember { mutableStateOf(true) }

        LaunchedEffect(isPlaying) {
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
                        setVideoURI(uri)
                        setOnPreparedListener { mp ->
                            mediaPlayer = mp
                            mp.setVolume(if (phoneMuted) 0f else 1f, if (phoneMuted) 0f else 1f)
                            durationMs = mp.duration.toLong().coerceAtLeast(1L)
                            mp.start()
                            isPlaying = true
                        }
                        setOnCompletionListener { isPlaying = false; currentMs = durationMs }
                        videoView = this
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
                        onBack = onBack,
                        onStop = onStop,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    val isNearEnd = durationMs > 2000L && currentMs >= (durationMs - 1500L)
                    VideoPlayerCenterControls(
                        isPlaying = isPlaying,
                        isNearEnd = isNearEnd,
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
            com.omnicast.sender.media.LocalImageServer.stop()
        }
    }
}
