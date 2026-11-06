package com.omnicast.sender.overlay

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.omnicast.core.protocol.SignalingMessage
import com.omnicast.sender.gallery.GalleryMediaHelper
import com.omnicast.sender.gallery.GalleryPhotoItem
import com.omnicast.sender.gallery.GallerySessionHolder
import com.omnicast.sender.media.LocalImageServer
import com.omnicast.sender.network.QuickSignalingHelper
import com.omnicast.sender.ui.SenderMainActivity
import kotlinx.coroutines.Dispatchers
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

                VideoCastPlayerScreen(
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
                    },
                    onBindVideoView = { videoView = it },
                    onBindMediaPlayer = { mediaPlayer = it },
                    onVideoCompleted = { if (videoList.isNotEmpty()) changeVideo(1) }
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
