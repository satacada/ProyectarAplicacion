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

    private var lastAppPackage: String? = null

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

    private fun getInstalledGalleryPackage(): String? {
        val candidates = listOf(
            "com.miui.gallery",
            "com.google.android.apps.photos",
            "com.sec.android.gallery3d",
            "com.coloros.gallery3d",
            "com.huawei.photos"
        )
        for (pkg in candidates) {
            try {
                packageManager.getPackageInfo(pkg, 0)
                return pkg
            } catch (_: Throwable) {}
        }
        return null
    }

    private fun resolveAppPackage(uri: Uri): String {
        val auth = uri.authority ?: ""
        return when {
            auth.contains("miui.gallery") -> "com.miui.gallery"
            auth.contains("photos") -> "com.google.android.apps.photos"
            auth.contains("gallery") -> auth.substringBefore(".open").substringBefore(".provider")
            else -> getInstalledGalleryPackage() ?: "com.miui.gallery"
        }
    }

    private val appPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null) {
            handleMediaSelected(uri)
        } else {
            // Usuario dio ATRÁS dentro de la Galería -> Retrocede al Explorador Nativo (Paso 1)
            lastAppPackage = null
            launchExplorer()
        }
    }

    private val viewerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == PhotoCastViewerActivity.RESULT_EXIT_TO_MAIN) {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        } else {
            // Usuario dio ATRÁS en foto a pantalla completa -> Retrocede a la Galería de origen (Paso 2)
            val pkg = lastAppPackage ?: getInstalledGalleryPackage()
            if (pkg != null) {
                launchAppPicker(pkg)
            } else {
                launchExplorer()
            }
        }
    }

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            handleMediaSelected(uri)
        } else {
            // Usuario dio ATRÁS en el Explorador Nativo -> Retrocede a OmniCast (Paso 0)
            finish()
        }
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun launchAppPicker(pkg: String) {
        lastAppPackage = pkg
        try {
            val intent = Intent(Intent.ACTION_PICK).apply {
                setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/* video/*")
                setPackage(pkg)
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            appPickerLauncher.launch(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        } catch (_: Throwable) {
            launchExplorer()
        }
    }

    private fun launchExplorer() {
        try {
            picker.launch("*/*")
        } catch (_: Throwable) {
            try {
                picker.launch("image/*")
            } catch (_: Throwable) {
                Toast.makeText(this, "No se encontró selector de archivos", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun handleMediaSelected(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Throwable) {}

        val pkg = resolveAppPackage(uri)
        lastAppPackage = pkg

        val mime = resolveMime(uri)
        if (mime.startsWith("video/")) {
            val mediaUrl = LocalImageServer.serveMedia(uri, mime, this)
            SenderMainActivity.sendDirectStreamStatic(this, mediaUrl, "Video de Galería")
            Toast.makeText(this, "🎬 Reproduciendo video en TV Box", Toast.LENGTH_SHORT).show()
            // Permanece en la Galería para seguir seleccionando videos/fotos sin cerrar la app
            launchAppPicker(pkg)
        } else {
            val photoItems = listOf(com.omnicast.sender.gallery.GalleryPhotoItem(0L, uri, "Foto", "Selección", System.currentTimeMillis()))
            com.omnicast.sender.gallery.GallerySessionHolder.setPhotos(photoItems, 0)
            val viewerIntent = Intent(this, PhotoCastViewerActivity::class.java).apply {
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra("EXTRA_START_INDEX", 0)
                putExtra("IMAGE_URI", uri.toString())
            }
            viewerLauncher.launch(viewerIntent)
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
}
