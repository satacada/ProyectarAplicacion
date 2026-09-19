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

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val mime = resolveMime(uri)
            val mediaUrl = LocalImageServer.serveMedia(uri, mime, this)
            if (mime.startsWith("video/")) {
                SenderMainActivity.sendDirectStreamStatic(this, mediaUrl, "Video de Galería")
                Toast.makeText(this, "🎬 Video enviado a la TV Box", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                val photoItems = listOf(com.omnicast.sender.gallery.GalleryPhotoItem(0L, uri, "Foto", "Selección", System.currentTimeMillis()))
                com.omnicast.sender.gallery.GallerySessionHolder.setPhotos(photoItems, 0)
                val viewerIntent = Intent(this, PhotoCastViewerActivity::class.java).apply {
                    putExtra("EXTRA_START_INDEX", 0)
                    putExtra("IMAGE_URI", uri.toString())
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
