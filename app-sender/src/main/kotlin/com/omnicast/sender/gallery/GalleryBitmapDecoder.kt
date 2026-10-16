package com.omnicast.sender.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Decodificador optimizado de imágenes de galería para proyección HD en TV.
 * Corrige automáticamente la rotación EXIF de la cámara y genera Base64 optimizado.
 */
object GalleryBitmapDecoder {

    private const val TAG = "GalleryBitmapDecoder"
    private const val DEFAULT_MAX_DIM = 1920

    suspend fun decodeBitmap(
        context: Context,
        uri: Uri,
        maxDim: Int = DEFAULT_MAX_DIM
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }

            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return@withContext null

            var inSample = 1
            val maxSide = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
            if (maxSide > maxDim) {
                inSample = (maxSide / maxDim).coerceAtLeast(1)
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = inSample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBmp = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext null

            val rotationDegrees = getExifRotation(context, uri)
            if (rotationDegrees != 0f) {
                rotateBitmap(decodedBmp, rotationDegrees)
            } else {
                decodedBmp
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error decodificando imagen ($uri): ${e.message}")
            null
        }
    }

    fun toBase64Jpeg(bitmap: Bitmap, quality: Int = 85): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$b64"
    }

    private fun getExifRotation(context: Context, uri: Uri): Float {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (_: Throwable) {
            0f
        }
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated != source) {
            source.recycle()
        }
        return rotated
    }
}
