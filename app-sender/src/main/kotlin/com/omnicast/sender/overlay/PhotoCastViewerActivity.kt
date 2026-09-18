package com.omnicast.sender.overlay

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.omnicast.core.protocol.SignalingMessage
import com.omnicast.sender.media.LocalImageServer
import com.omnicast.sender.network.SignalingClient
import com.omnicast.sender.network.SignalingClientHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoCastViewerActivity : ComponentActivity() {

    private var signalingClient: SignalingClient? = null
    private val photoUris = mutableStateListOf<Uri>()
    private var currentIndexState = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val urisExtra = intent.getStringArrayListExtra("EXTRA_IMAGE_URIS")
        val singleUriExtra = intent.getStringExtra("IMAGE_URI")
        val initialUris = urisExtra?.map { Uri.parse(it) } ?: listOfNotNull(singleUriExtra?.let { Uri.parse(it) })
        photoUris.addAll(initialUris)

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
        val nextIdx = (currentIndexState.intValue + delta).let {
            if (it < 0) photoUris.size - 1 else if (it >= photoUris.size) 0 else it
        }
        currentIndexState.intValue = nextIdx
        transmitCurrentPhoto()
    }

    private fun transmitCurrentPhoto() {
        val idx = currentIndexState.intValue
        val uri = photoUris.getOrNull(idx) ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            val mediaUrl = LocalImageServer.serveMedia(uri, "image/jpeg", this@PhotoCastViewerActivity)
            signalingClient?.sendMessage(SignalingMessage.ShowImage(mediaUrl, "Foto (${idx + 1}/${photoUris.size})"))
            signalingClient?.sendMessage(SignalingMessage.ImageTransform(1f, 0f, 0f))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        signalingClient?.sendMessage(SignalingMessage.PlayControl("STOP"))
        signalingClient = null
    }
}

@Composable
private fun PhotoViewerContent(
    uris: List<Uri>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onAddMore: () -> Unit,
    onTransform: (Float, Float, Float) -> Unit,
    onClose: () -> Unit
) {
    var scale by remember(currentIndex) { mutableStateOf(1f) }
    var panX by remember(currentIndex) { mutableStateOf(0f) }
    var panY by remember(currentIndex) { mutableStateOf(0f) }
    var bitmap by remember(currentIndex) { mutableStateOf<ImageBitmap?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUri = uris.getOrNull(currentIndex)

    LaunchedEffect(currentUri) {
        if (currentUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(currentUri)?.use {
                        bitmap = BitmapFactory.decodeStream(it)?.asImageBitmap()
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).pointerInput(currentIndex) {
            detectTransformGestures { _, pan, zoom, _ ->
                val newScale = (scale * zoom).coerceIn(0.7f, 5f)
                scale = newScale; panX += pan.x; panY += pan.y
                onTransform(scale, panX, panY)
            }
        }
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp, contentDescription = "Foto Actual",
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = panX; translationY = panY
                },
                contentScale = ContentScale.Fit
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp, start = 12.dp, end = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Zoom: ${String.format(java.util.Locale.US, "%.1fx", scale)}", color = Color.White, fontSize = 12.sp)
                FilledTonalButton(onClick = { scale = 0.85f; panX = 0f; panY = 0f; onTransform(0.85f, 0f, 0f) }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("-15%", fontSize = 10.sp) }
                FilledTonalButton(onClick = { scale = 1f; panX = 0f; panY = 0f; onTransform(1f, 0f, 0f) }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("1:1", fontSize = 10.sp) }
                FilledTonalButton(onClick = { scale = 1.25f; panX = 0f; panY = 0f; onTransform(1.25f, 0f, 0f) }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("+25%", fontSize = 10.sp) }
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Cerrar", fontSize = 10.sp, color = Color.White)
                }
            }
        }

        PhotoViewerBottomBar(
            currentIndex = currentIndex,
            totalCount = uris.size,
            onPrev = { onIndexChange(if (currentIndex > 0) currentIndex - 1 else uris.size - 1) },
            onNext = { onIndexChange((currentIndex + 1) % uris.size) },
            onAddMore = onAddMore,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
