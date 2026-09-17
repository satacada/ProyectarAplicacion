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

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val mime = contentResolver.getType(uri) ?: "image/jpeg"
            val mediaUrl = LocalImageServer.serveMedia(uri, mime, this)
            if (mime.startsWith("video/")) {
                SenderMainActivity.sendDirectStreamStatic(this, mediaUrl, "Video de Galería")
                Toast.makeText(this, "🎬 Video enviado a la TV Box", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "🖼️ Foto enviada. Usa tus dedos para zoom en TV.", Toast.LENGTH_SHORT).show()
                val viewerIntent = Intent(this, PhotoCastViewerActivity::class.java).apply {
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
