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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                onStop = {
                    QuickSignalingHelper.sendQuickMessage(this, SignalingMessage.PlayControl("STOP"))
                    exitToGallery()
                },
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
                .background(Color.Black)
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
                            durationMs = mp.duration.toLong().coerceAtLeast(1L); mp.start(); isPlaying = true
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
                Box(modifier = Modifier.fillMaxSize().background(Color(0x77000000))) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onBack) { Text("<", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Surface(color = Color(0xFF0284C7), shape = RoundedCornerShape(12.dp)) {
                                Text("TV Box Conectada", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }
                        Button(onClick = onStop, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("Detener", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val target = (currentMs - 10000L).coerceAtLeast(0L)
                                videoView?.seekTo(target.toInt())
                                currentMs = target
                                onSendControl("SEEK", target)
                            },
                            modifier = Modifier.size(52.dp).background(Color(0x66000000), CircleShape)
                        ) {
                            Text("<<", fontSize = 18.sp, color = Color.White)
                        }

                        val isNearEnd = durationMs > 2000L && currentMs >= (durationMs - 1500L)
                        IconButton(
                            onClick = {
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
                            modifier = Modifier.size(68.dp).background(Color(0xFF0284C7), CircleShape)
                        ) {
                            Text(if (isPlaying) "⏸" else if (isNearEnd) "↺" else "▶", fontSize = 26.sp, color = Color.White)
                        }

                        IconButton(
                            onClick = {
                                val target = (currentMs + 10000L).coerceAtMost(durationMs)
                                videoView?.seekTo(target.toInt())
                                currentMs = target
                                onSendControl("SEEK", target)
                            },
                            modifier = Modifier.size(52.dp).background(Color(0x66000000), CircleShape)
                        ) {
                            Text(">>", fontSize = 18.sp, color = Color.White)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Slider(
                            value = currentMs.toFloat(),
                            onValueChange = { newPos ->
                                currentMs = newPos.toLong()
                                videoView?.seekTo(newPos.toInt())
                            },
                            onValueChangeFinished = {
                                onSendControl("SEEK", currentMs)
                            },
                            valueRange = 0f..durationMs.toFloat(),
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${formatTime(currentMs)} / ${formatTime(durationMs)}",
                                color = Color(0xFFE2E8F0),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Button(
                                onClick = {
                                    phoneMuted = !phoneMuted
                                    val vol = if (phoneMuted) 0f else 1f
                                    mediaPlayer?.setVolume(vol, vol)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (phoneMuted) Color(0xFF334155) else Color(0xFF10B981)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text(if (phoneMuted) "Celular Mudo" else "Celular Audio", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return "%02d:%02d".format(m, s)
    }

    override fun onDestroy() {
        super.onDestroy()
        videoView?.stopPlayback()
        videoView = null
        mediaPlayer = null
    }
}
