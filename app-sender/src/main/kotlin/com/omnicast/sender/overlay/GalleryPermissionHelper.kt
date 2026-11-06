package com.omnicast.sender.overlay

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.omnicast.sender.gallery.GalleryMediaHelper
import com.omnicast.sender.gallery.GalleryPhotoItem

object GalleryPermissionHelper {

    fun checkGalleryPermission(context: Context, mimeFilter: String): Boolean {
        return GalleryMediaHelper.hasGalleryPermission(context, mimeFilter)
    }

    fun getRequiredPermissions(mimeFilter: String): Array<String> {
        return GalleryMediaHelper.getRequiredPermissions(mimeFilter)
    }

    fun openAppPermissionSettings(context: Context) {
        Toast.makeText(context, "Activa el permiso de Fotos y Videos en Ajustes de la App", Toast.LENGTH_LONG).show()
        try {
            val settingsIntent = Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
            context.startActivity(settingsIntent)
        } catch (_: Throwable) {}
    }

    fun requestOrOpenPermissions(
        activity: Activity,
        mimeFilter: String,
        allItems: List<GalleryPhotoItem>,
        permissionLauncher: ActivityResultLauncher<Array<String>>,
        onRefreshMedia: () -> Unit
    ) {
        if (!checkGalleryPermission(activity, mimeFilter)) {
            permissionLauncher.launch(getRequiredPermissions(mimeFilter))
        } else {
            val isVid = mimeFilter.startsWith("video/")
            val count = if (isVid) allItems.count { it.isVideo } else allItems.count { !it.isVideo }
            if (count == 0) {
                openAppPermissionSettings(activity)
            } else {
                Toast.makeText(activity, "🔄 Actualizando archivos...", Toast.LENGTH_SHORT).show()
                onRefreshMedia()
            }
        }
    }

    fun launchSystemExplorer(
        activity: Activity,
        mimeFilter: String,
        systemPickerLauncher: ActivityResultLauncher<Intent>,
        showToast: Boolean = true
    ) {
        val filter = if (mimeFilter.startsWith("video/")) "video/*" else "image/*"
        if (showToast) {
            Toast.makeText(activity, "💡 En Colecciones puedes elegir varias fotos o tocar un rostro para agruparlo", Toast.LENGTH_SHORT).show()
        }
        try {
            val maxItems = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching { MediaStore.getPickImagesMaxLimit() }.getOrDefault(100).coerceAtLeast(2)
            } else 100
            val pickIntent = Intent("android.provider.action.PICK_IMAGES").apply {
                type = filter
                putExtra("android.provider.extra.PICK_IMAGES_MAX", maxItems)
                putExtra("android.provider.extra.PICK_IMAGES_IN_ORDER", true)
                putExtra("android.provider.extra.PICK_IMAGES_LAUNCH_TAB", 0)
            }
            systemPickerLauncher.launch(pickIntent)
        } catch (_: Throwable) {
            try {
                val fallback = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = filter
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                systemPickerLauncher.launch(fallback)
            } catch (_: Throwable) {
                Toast.makeText(activity, "No se encontró explorador de archivos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun resolveMime(contentResolver: ContentResolver, uri: Uri, fallbackMimeFilter: String = "image/*"): String {
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
            else -> if (fallbackMimeFilter.startsWith("video/")) "video/mp4" else "image/jpeg"
        }
    }
}
