package com.omnicast.sender.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
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

    private var signalingClient: SignalingClient? = null
    private val photoUris = mutableStateListOf<Uri>()
    private var currentIndexState = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

            // Background discovery: cargar secuencia de galería completa si se abrió con una sola foto
            lifecycleScope.launch(Dispatchers.IO) {
                val fullList = GalleryMediaHelper.loadGalleryPhotos(this@PhotoCastViewerActivity)
                if (fullList.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        GallerySessionHolder.setPhotos(fullList, 0)
                        val currentTargetUri = photoUris.getOrNull(currentIndexState.intValue)
                        val matchIdx = fullList.indexOfFirst { it.uri == currentTargetUri }
                        photoUris.clear()
                        photoUris.addAll(fullList.map { it.uri })
                        currentIndexState.intValue = if (matchIdx >= 0) matchIdx else 0
                    }
                }
            }
        }

        val prefs = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
        val ip = prefs.getString("saved_tv_ip", "192.168.0.136") ?: "192.168.0.136"
        val port = prefs.getInt("saved_tv_port", 8088)

        lifecycleScope.launch(Dispatchers.IO) {
            signalingClient = SignalingClientHolder.get()
            if (signalingClient?.isConnected != true) {
                signalingClient?.connect(ip, port, { transmitCurrentPhoto() }, {})
            } else {
                transmitCurrentPhoto()
            }
            signalingClient?.messages?.collect { msg ->
                if (msg is SignalingMessage.PlayControl) {
                    withContext(Dispatchers.Main) {
                        when (msg.action) {
                            "NEXT" -> changePhoto(1)
                            "PREV" -> changePhoto(-1)
                            "STOP" -> finish()
                        }
                    }
                }
            }
        }

        setContent {
            val addPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { newUris ->
                if (newUris.isNotEmpty()) {
                    val startIndex = photoUris.size
                    photoUris.addAll(newUris)
                    currentIndexState.intValue = startIndex
                    transmitCurrentPhoto()
                    Toast.makeText(this, "➕ ${newUris.size} foto(s) añadidas", Toast.LENGTH_SHORT).show()
                }
            }

            PhotoViewerContent(
                uris = photoUris,
                currentIndex = currentIndexState.intValue,
                onIndexChange = { newIdx ->
                    if (newIdx in photoUris.indices) {
                        currentIndexState.intValue = newIdx
                        transmitCurrentPhoto()
                    }
                },
                onAddMore = { addPicker.launch("image/*") },
                onTransform = { scale, px, py ->
                    signalingClient?.sendMessage(SignalingMessage.ImageTransform(scale, px, py))
                },
                onClose = { finish() }
            )
        }
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
            val dataUrl = try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val original = BitmapFactory.decodeStream(stream)
                    if (original != null) {
                        val maxDim = 1920
                        val maxSide = maxOf(original.width, original.height)
                        val factor = if (maxSide > maxDim) maxDim.toFloat() / maxSide else 1f
                        val scaled = if (factor < 1f) {
                            Bitmap.createScaledBitmap(original, (original.width * factor).toInt(), (original.height * factor).toInt(), true)
                        } else original
                        val baos = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                        "data:image/jpeg;base64,$b64"
                    } else null
                }
            } catch (_: Throwable) { null }

            val photoUrl = dataUrl ?: LocalImageServer.serveMedia(uri, "image/jpeg", this@PhotoCastViewerActivity)
            signalingClient?.sendMessage(SignalingMessage.ShowImage(photoUrl, "Foto (${idx + 1}/$count)"))
            signalingClient?.sendMessage(SignalingMessage.ImageTransform(1f, 0f, 0f))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        signalingClient?.sendMessage(SignalingMessage.PlayControl("STOP"))
        signalingClient = null
    }
}
