package com.omnicast.sender.overlay

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.omnicast.core.protocol.SignalingMessage
import com.omnicast.sender.network.SignalingClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoCastViewerActivity : ComponentActivity() {

    private var signalingClient: SignalingClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imageUriStr = intent.getStringExtra("IMAGE_URI")
        val imageUri = imageUriStr?.let { Uri.parse(it) }

        val prefs = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
        val ip = prefs.getString("saved_tv_ip", "192.168.0.136") ?: "192.168.0.136"
        val port = prefs.getInt("saved_tv_port", 8088)
        signalingClient = SignalingClient().apply { connect(ip, port, {}, {}) }

        setContent {
            PhotoViewerContent(imageUri = imageUri, onTransform = { scale, px, py ->
                signalingClient?.sendMessage(SignalingMessage.ImageTransform(scale, px, py))
            }, onClose = { finish() })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        signalingClient?.disconnect()
        signalingClient = null
    }
}

@Composable
private fun PhotoViewerContent(
    imageUri: Uri?,
    onTransform: (Float, Float, Float) -> Unit,
    onClose: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(imageUri)?.use {
                        bitmap = BitmapFactory.decodeStream(it)?.asImageBitmap()
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                val newScale = (scale * zoom).coerceIn(1f, 5f)
                scale = newScale
                if (scale > 1.05f) { panX += pan.x; panY += pan.y } else { panX = 0f; panY = 0f }
                onTransform(scale, panX, panY)
            }
        }
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp, contentDescription = "Foto",
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = panX; translationY = panY
                },
                contentScale = ContentScale.Fit
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🖼️ Zoom Táctil TV: ${String.format("%.1fx", scale)}", color = Color.White, fontSize = 12.sp)
                FilledTonalButton(onClick = { scale = 1f; panX = 0f; panY = 0f; onTransform(1f, 0f, 0f) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("1:1", fontSize = 11.sp)
                }
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)), contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text("Cerrar", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}
