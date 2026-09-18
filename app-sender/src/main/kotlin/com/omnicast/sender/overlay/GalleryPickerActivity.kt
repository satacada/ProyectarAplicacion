package com.omnicast.sender.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.omnicast.core.protocol.SignalingMessage
import com.omnicast.sender.media.LocalImageServer
import com.omnicast.sender.network.SignalingClient
import com.omnicast.sender.ui.SenderMainActivity

class GalleryPickerActivity : ComponentActivity() {

    private fun resolveMime(uri: Uri): String {
        val type = contentResolver.getType(uri)
        if (!type.isNullOrBlank() && type != "application/octet-stream") return type
        val path = uri.toString().lowercase()
        return when {
            path.endsWith(".mp4") || path.contains(".mp4?") -> "video/mp4"
            path.endsWith(".mkv") || path.contains(".mkv?") -> "video/x-matroska"
            path.endsWith(".mov") || path.contains(".mov?") -> "video/quicktime"
            path.endsWith(".webm") || path.contains(".webm?") -> "video/webm"
            path.endsWith(".avi") || path.contains(".avi?") -> "video/x-msvideo"
            path.endsWith(".png") || path.contains(".png?") -> "image/png"
            path.endsWith(".webp") || path.contains(".webp?") -> "image/webp"
            path.endsWith(".gif") || path.contains(".gif?") -> "image/gif"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            else -> type ?: "image/jpeg"
        }
    }

    private val picker = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (!uris.isNullOrEmpty()) {
            val firstUri = uris.first()
            val mime = resolveMime(firstUri)
            val mediaUrl = LocalImageServer.serveMedia(firstUri, mime, this)
            if (mime.startsWith("video/")) {
                SenderMainActivity.sendDirectStreamStatic(this, mediaUrl, "Video de Galería")
                Toast.makeText(this, "🎬 Video enviado a la TV Box", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "🖼️ ${uris.size} foto(s) cargada(s). Sesión activa.", Toast.LENGTH_SHORT).show()
                val viewerIntent = Intent(this, PhotoCastViewerActivity::class.java).apply {
                    putStringArrayListExtra("EXTRA_IMAGE_URIS", ArrayList(uris.map { it.toString() }))
                    putExtra("IMAGE_URI", firstUri.toString())
                    putExtra("MEDIA_URL", mediaUrl)
                }
                startActivity(viewerIntent)
                finish()
            }
        } else {
            finish()
        }
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        picker.launch("*/*")
    }
}
