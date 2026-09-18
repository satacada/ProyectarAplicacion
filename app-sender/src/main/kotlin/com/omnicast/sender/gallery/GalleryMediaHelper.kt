package com.omnicast.sender.gallery

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Consulta y decodificación ultrarrápida de fotos de la galería local vía MediaStore.
 */
object GalleryMediaHelper {

    private val thumbnailCache = object : LruCache<Long, Bitmap>(60) {}

    fun hasGalleryPermission(context: Context): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    suspend fun loadGalleryPhotos(context: Context): List<GalleryPhotoItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<GalleryPhotoItem>()
        if (!hasGalleryPermission(context)) return@withContext result

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val dateCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "Foto" else "Foto"
                    val bucket = if (bucketCol >= 0) cursor.getString(bucketCol) ?: "General" else "General"
                    val date = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    result.add(GalleryPhotoItem(id, uri, name, bucket, date))
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("GalleryMediaHelper", "Error consultando MediaStore", e)
        }
        result
    }

    suspend fun getThumbnail(context: Context, item: GalleryPhotoItem, targetSize: Int = 240): Bitmap? = withContext(Dispatchers.IO) {
        thumbnailCache.get(item.id)?.let { return@withContext it }

        val bmp = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(item.uri, Size(targetSize, targetSize), null)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Thumbnails.getThumbnail(
                    context.contentResolver,
                    item.id,
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    null
                ) ?: decodeSampledBitmapFromUri(context, item.uri, targetSize, targetSize)
            }
        } catch (_: Throwable) {
            decodeSampledBitmapFromUri(context, item.uri, targetSize, targetSize)
        }

        if (bmp != null) thumbnailCache.put(item.id, bmp)
        bmp
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqW: Int, reqH: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            var inSampleSize = 1
            if (options.outHeight > reqH || options.outWidth > reqW) {
                val halfH = options.outHeight / 2
                val halfW = options.outWidth / 2
                while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                    inSampleSize *= 2
                }
            }
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
        } catch (_: Throwable) { null }
    }
}
