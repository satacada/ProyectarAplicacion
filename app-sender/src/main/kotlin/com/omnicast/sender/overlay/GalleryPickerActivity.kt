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
import com.omnicast.sender.network.QuickSignalingHelper
import com.omnicast.sender.ui.SenderMainActivity

class GalleryPickerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MIME_FILTER = "EXTRA_MIME_FILTER"
    }

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

    private val viewerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == PhotoCastViewerActivity.RESULT_EXIT_TO_MAIN) {
            QuickSignalingHelper.sendQuickMessage(this, SignalingMessage.PlayControl("STOP"))
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        } else {
            launchExplorer()
        }
    }

    private val videoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == PhotoCastViewerActivity.RESULT_EXIT_TO_MAIN) {
            QuickSignalingHelper.sendQuickMessage(this, SignalingMessage.PlayControl("STOP"))
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        } else {
            launchExplorer()
        }
    }

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            handleMediaSelected(uri)
        } else {
            QuickSignalingHelper.sendQuickMessage(this, SignalingMessage.PlayControl("STOP"))
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun launchExplorer() {
        val filter = intent.getStringExtra(EXTRA_MIME_FILTER) ?: "*/*"
        try {
            picker.launch(filter)
        } catch (_: Throwable) {
            try {
                picker.launch("*/*")
            } catch (_: Throwable) {
                Toast.makeText(this, "No se encontró explorador de archivos", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun handleMediaSelected(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Throwable) {}

        val mime = resolveMime(uri)
        if (mime.startsWith("video/")) {
            val mediaUrl = LocalImageServer.serveMedia(uri, mime, this)
            SenderMainActivity.sendDirectStreamStatic(this, mediaUrl, "Video de Galería")
            Toast.makeText(this, "🎬 Proyectando video en TV Box", Toast.LENGTH_SHORT).show()
            val videoIntent = Intent(this, VideoCastPlayerActivity::class.java).apply {
                putExtra(VideoCastPlayerActivity.EXTRA_VIDEO_URI, uri.toString())
                putExtra(VideoCastPlayerActivity.EXTRA_VIDEO_TITLE, "Video de Galería")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            videoLauncher.launch(videoIntent)
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        } else if (mime.startsWith("image/")) {
            val photoItems = listOf(com.omnicast.sender.gallery.GalleryPhotoItem(0L, uri, "Foto", "Selección", System.currentTimeMillis()))
            com.omnicast.sender.gallery.GallerySessionHolder.setPhotos(photoItems, 0)
            val viewerIntent = Intent(this, PhotoCastViewerActivity::class.java).apply {
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra("EXTRA_START_INDEX", 0)
                putExtra("IMAGE_URI", uri.toString())
            }
            viewerLauncher.launch(viewerIntent)
        } else {
            Toast.makeText(this, "Formato no compatible. Por favor selecciona una foto o un video.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchExplorer()
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && !com.omnicast.sender.service.CastSessionState.isGalleryCasting && !com.omnicast.sender.stream.ActiveStreamHolder.isCasting.value) {
            QuickSignalingHelper.sendQuickMessage(this, SignalingMessage.PlayControl("STOP"))
        }
    }
}
