package com.omnicast.sender.overlay

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.lifecycleScope
import com.omnicast.core.protocol.SignalingMessage
import com.omnicast.sender.gallery.GalleryMediaHelper
import com.omnicast.sender.gallery.GalleryPhotoItem
import com.omnicast.sender.gallery.GallerySessionHolder
import com.omnicast.sender.media.LocalImageServer
import com.omnicast.sender.network.SignalingClient
import com.omnicast.sender.network.SignalingClientHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class PhotoCastViewerActivity : ComponentActivity() {

    companion object {
        const val RESULT_EXIT_TO_MAIN = 100
        const val RESULT_BACK_TO_GALLERY = 101
    }

    private var signalingClient: SignalingClient? = null
    private val photoUris = mutableStateListOf<Uri>()
    private var currentIndexState = mutableIntStateOf(0)
    private val currentBitmapState = mutableStateOf<ImageBitmap?>(null)

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) loadFullGallery()
    }

    private fun loadFullGallery() {
        lifecycleScope.launch(Dispatchers.IO) {
            val fullList = GalleryMediaHelper.loadGalleryPhotos(this@PhotoCastViewerActivity)
            if (fullList.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    val currentTargetUri = photoUris.getOrNull(currentIndexState.intValue)
                    val targetId = currentTargetUri?.lastPathSegment?.substringAfterLast(":") ?: ""
                    val matchIdx = fullList.indexOfFirst {
                        it.uri == currentTargetUri || (targetId.isNotBlank() && (it.uri.toString().endsWith("/$targetId") || it.id.toString() == targetId))
                    }
                    val finalList = if (matchIdx >= 0) {
                        fullList
                    } else if (currentTargetUri != null) {
                        listOf(GalleryPhotoItem(0L, currentTargetUri, "Foto", "Selección", System.currentTimeMillis())) + fullList
                    } else fullList
                    val finalIndex = if (matchIdx >= 0) matchIdx else 0
                    GallerySessionHolder.setPhotos(finalList, finalIndex)
                    photoUris.clear()
                    photoUris.addAll(finalList.map { it.uri })
                    currentIndexState.intValue = finalIndex
                    transmitCurrentPhoto()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.omnicast.sender.service.CastSessionState.isGalleryCasting = true

        val holderPhotos = GallerySessionHolder.activePhotos
        val startIdxExtra = intent.getIntExtra("EXTRA_START_INDEX", GallerySessionHolder.activeIndex)

        if (holderPhotos.isNotEmpty()) {
            photoUris.addAll(holderPhotos.map { it.uri })
            currentIndexState.intValue = startIdxExtra.coerceIn(0, (photoUris.size - 1).coerceAtLeast(0))
        } else {
            val urisExtra = intent.getStringArrayListExtra("EXTRA_IMAGE_URIS")
            val singleUriExtra = intent.getStringExtra("IMAGE_URI")
            val initialUris = urisExtra?.map { Uri.parse(it) } ?: listOfNotNull(singleUriExtra?.let { Uri.parse(it) })
            photoUris.addAll(initialUris)
            currentIndexState.intValue = startIdxExtra.coerceIn(0, (photoUris.size - 1).coerceAtLeast(0))
        }

        val isCloudOrDoc = photoUris.any {
            val auth = it.authority ?: ""
            auth.contains("docs") || auth.contains("drive") || auth.contains("documents")
        }
        if (!isCloudOrDoc && holderPhotos.size <= 1) {
            if (GalleryMediaHelper.hasGalleryPermission(this)) {
                loadFullGallery()
            } else {
                permLauncher.launch(GalleryMediaHelper.getRequiredPermission())
            }
        }

        val prefs = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
        val ip = prefs.getString("saved_tv_ip", null)?.takeIf { it.isNotBlank() }
        val port = prefs.getInt("saved_tv_port", 8088)

        lifecycleScope.launch(Dispatchers.IO) {
            signalingClient = SignalingClientHolder.get()
            if (signalingClient?.isConnected != true && ip != null) {
                signalingClient?.connect(ip, port, { transmitCurrentPhoto() }, {})
            } else if (signalingClient?.isConnected == true) {
                transmitCurrentPhoto()
            }
            signalingClient?.messages?.collect { msg ->
                if (msg is SignalingMessage.PlayControl) {
                    withContext(Dispatchers.Main) {
                        when (msg.action) {
                            "NEXT" -> changePhoto(1)
                            "PREV" -> changePhoto(-1)
                        }
                    }
                }
            }
        }

        setResult(RESULT_BACK_TO_GALLERY)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                sendSignaling(SignalingMessage.PlayControl("STOP"))
                val currentUri = photoUris.getOrNull(currentIndexState.intValue)
                val resultData = Intent().apply { data = currentUri }
                setResult(RESULT_BACK_TO_GALLERY, resultData)
                finish()
            }
        })

        setContent {
            PhotoViewerContent(
                uris = photoUris,
                currentIndex = currentIndexState.intValue,
                currentBitmap = currentBitmapState.value,
                onIndexChange = { newIdx ->
                    if (newIdx in photoUris.indices) {
                        currentIndexState.intValue = newIdx
                        transmitCurrentPhoto()
                    }
                },
                onTransform = { scale, px, py, rot ->
                    sendSignaling(SignalingMessage.ImageTransform(scale, px, py, rot))
                },
                onClose = {
                    sendSignaling(SignalingMessage.PlayControl("STOP"))
                    setResult(RESULT_EXIT_TO_MAIN)
                    finish()
                }
            )
        }
    }

    private fun sendSignaling(msg: SignalingMessage) {
        val client = signalingClient ?: SignalingClientHolder.get().also { signalingClient = it }
        if (client.isConnected) {
            client.sendMessage(msg)
        } else {
            val prefs = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
            val ip = prefs.getString("saved_tv_ip", null)?.takeIf { it.isNotBlank() } ?: return
            val port = prefs.getInt("saved_tv_port", 8088)
            client.connect(ip, port, { client.sendMessage(msg) }, {})
        }
        com.omnicast.sender.discovery.MultiReceiverManager.broadcastToSecondary(msg, emptyList())
    }

    private fun changePhoto(delta: Int) {
        if (photoUris.isEmpty()) return
        val count = photoUris.size
        val nextIdx = (currentIndexState.intValue + delta).let {
            if (it < 0) count - 1 else if (it >= count) 0 else it
        }
        currentIndexState.intValue = nextIdx
        transmitCurrentPhoto()
    }

    private fun transmitCurrentPhoto() {
        val idx = currentIndexState.intValue
        val uri = photoUris.getOrNull(idx) ?: return
        val count = photoUris.size

        lifecycleScope.launch(Dispatchers.IO) {
            val bmp = com.omnicast.sender.gallery.GalleryBitmapDecoder.decodeBitmap(this@PhotoCastViewerActivity, uri)

            if (bmp != null) {
                withContext(Dispatchers.Main) {
                    currentBitmapState.value = bmp.asImageBitmap()
                }
                val photoUrl = com.omnicast.sender.gallery.GalleryBitmapDecoder.toBase64Jpeg(bmp)
                sendSignaling(SignalingMessage.ShowImage(photoUrl, "Foto (${idx + 1}/$count)"))
                sendSignaling(SignalingMessage.ImageTransform(1f, 0f, 0f, 0f))
            } else {
                val fallbackUrl = try {
                    LocalImageServer.serveMedia(uri, "image/jpeg", this@PhotoCastViewerActivity)
                } catch (_: Throwable) { null }
                if (fallbackUrl != null) {
                    sendSignaling(SignalingMessage.ShowImage(fallbackUrl, "Foto (${idx + 1}/$count)"))
                    sendSignaling(SignalingMessage.ImageTransform(1f, 0f, 0f, 0f))
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        com.omnicast.sender.service.CastSessionState.isGalleryCasting = false
        signalingClient = null
        if (!com.omnicast.sender.stream.ActiveStreamHolder.isCasting.value) {
            LocalImageServer.stop()
        }
    }
}
