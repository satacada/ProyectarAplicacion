package com.omnicast.sender.gallery

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Consulta y decodificación de fotos, videos y jerarquía de carpetas vía MediaStore.
 */
object GalleryMediaHelper {

    private val thumbnailCache = object : LruCache<String, Bitmap>(80) {}

    fun hasGalleryPermission(context: Context, mimeFilter: String = "*/*"): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasImg = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasVid = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            when { mimeFilter.startsWith("image") -> hasImg; mimeFilter.startsWith("video") -> hasVid; else -> hasImg || hasVid }
        } else ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    fun hasAllMediaPermissions(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    fun getRequiredPermissions(mimeFilter: String = "*/*"): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                mimeFilter.startsWith("image") -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                mimeFilter.startsWith("video") -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                else -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun isGooglePhotosCloudUri(uri: Uri): Boolean =
        uri.toString().lowercase().let { it.contains("com.google.android.apps.photos") || it.contains("cloudpicker") }

    fun extractNormalizedFolderPath(relativePath: String?, dataPath: String?, bucketName: String): String {
        if (!relativePath.isNullOrBlank()) {
            val cleaned = relativePath.replace('\\', '/').trim('/')
            if (cleaned.isNotBlank()) return cleaned
        }
        if (!dataPath.isNullOrBlank()) {
            var norm = dataPath.replace('\\', '/')
            for (m in listOf("/storage/emulated/0/", "/storage/self/primary/", "/sdcard/")) {
                val idx = norm.indexOf(m, ignoreCase = true)
                if (idx >= 0) { norm = norm.substring(idx + m.length); break }
            }
            if (norm.startsWith("/storage/")) {
                val after = norm.removePrefix("/storage/")
                val sIdx = after.indexOf('/')
                if (sIdx >= 0) norm = after.substring(sIdx + 1)
            }
            val parent = norm.substringBeforeLast('/', "").trim('/')
            if (parent.isNotBlank()) return parent
        }
        return bucketName.trim('/').ifBlank { "General" }
    }

    suspend fun loadGalleryPhotos(context: Context): List<GalleryPhotoItem> = loadAllMedia(context, "image/*")

    suspend fun loadAllMedia(context: Context, mimeFilter: String = "*/*"): List<GalleryPhotoItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<GalleryPhotoItem>()
        if (!mimeFilter.startsWith("video/") && hasGalleryPermission(context, "image/*")) {
            queryCollection(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, result)
        }
        if (!mimeFilter.startsWith("image/") && hasGalleryPermission(context, "video/*")) {
            queryCollection(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, result)
        }
        result.sortByDescending { it.dateAdded }
        if (mimeFilter == "*/*") {
            GallerySessionHolder.cachedAllItems = result
        }
        result
    }

    @Suppress("DEPRECATION")
    private fun queryCollection(context: Context, collectionUri: Uri, isVideo: Boolean, out: MutableList<GalleryPhotoItem>) {
        val cols = mutableListOf(
            MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME, MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.DATA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cols.add(MediaStore.MediaColumns.RELATIVE_PATH)
            cols.add(MediaStore.MediaColumns.DURATION)
        }
        try {
            context.contentResolver.query(collectionUri, cols.toTypedArray(), null, null, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val bucketCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val dateCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val relCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH) else -1
                val durCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) cursor.getColumnIndex(MediaStore.MediaColumns.DURATION) else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val defName = if (isVideo) "Video" else "Foto"
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: defName else defName
                    val bucket = if (bucketCol >= 0) cursor.getString(bucketCol) ?: "General" else "General"
                    val date = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                    val defMime = if (isVideo) "video/mp4" else "image/jpeg"
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: defMime else defMime
                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val relPath = if (relCol >= 0) cursor.getString(relCol) else null
                    val duration = if (durCol >= 0) cursor.getLong(durCol) else 0L
                    val folderPath = extractNormalizedFolderPath(relPath, dataPath, bucket)
                    val uri = ContentUris.withAppendedId(collectionUri, id)
                    out.add(GalleryPhotoItem(id, uri, name, folderPath.substringAfterLast('/').ifBlank { bucket }, date, isVideo, mime, folderPath, duration))
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("GalleryMediaHelper", "Error consultando MediaStore ($collectionUri)", e)
        }
    }

    @Suppress("DEPRECATION")
    fun resolveFolderPathFromUri(context: Context, uri: Uri): String? {
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val auth = uri.authority ?: ""
                if (auth == "com.android.externalstorage.documents" && docId.contains(':')) {
                    val folder = docId.substringAfter(':').trim('/').substringBeforeLast('/', "").trim('/')
                    if (folder.isNotBlank()) return folder
                }
            }
            queryFolderPathFromContentUri(context, uri)?.let { return it }
            val pathSeg = uri.path ?: ""
            if (pathSeg.contains("/storage/")) return extractNormalizedFolderPath(null, pathSeg, "")
        } catch (_: Throwable) {}
        return null
    }

    @Suppress("DEPRECATION")
    private fun queryFolderPathFromContentUri(context: Context, uri: Uri): String? {
        val cols = mutableListOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) cols.add(MediaStore.MediaColumns.RELATIVE_PATH)
        context.contentResolver.query(uri, cols.toTypedArray(), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val dataPath = c.getColumnIndex(MediaStore.MediaColumns.DATA).takeIf { it >= 0 }?.let { c.getString(it) }
                val bucket = c.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME).takeIf { it >= 0 }?.let { c.getString(it) } ?: ""
                val relPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH).takeIf { it >= 0 }?.let { c.getString(it) } else null
                val resolved = extractNormalizedFolderPath(relPath, dataPath, bucket)
                if (resolved.isNotBlank() && resolved != "General") return resolved
            }
        }
        return null
    }

    private fun queryDisplayNameFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx) else null
                } else null
            }
        } catch (_: Throwable) { null }
    }

    suspend fun loadFolderContextForUri(context: Context, targetUri: Uri, isVideo: Boolean): Pair<String, List<GalleryPhotoItem>> = withContext(Dispatchers.IO) {
        val all = loadAllMedia(context, if (isVideo) "video/*" else "image/*")
        val decodedUri = Uri.decode(targetUri.toString()).replace('\\', '/')
        val rawSeg = targetUri.lastPathSegment ?: ""
        val targetId = rawSeg.substringAfterLast(':').substringAfterLast('/').filter { it.isDigit() }
        val queriedName = queryDisplayNameFromUri(context, targetUri)?.trim() ?: ""

        val matchedItem = all.firstOrNull { item ->
            item.uri == targetUri ||
                (targetId.isNotBlank() && item.id.toString() == targetId) ||
                (queriedName.isNotBlank() && item.name.equals(queriedName, ignoreCase = true)) ||
                (item.folderPath.isNotBlank() && decodedUri.endsWith("${item.folderPath}/${item.name}", ignoreCase = true))
        }
        val folderPath = resolveFolderPathFromUri(context, targetUri) ?: matchedItem?.folderPath ?: ""
        val rawFolderItems = if (folderPath.isNotBlank()) {
            all.filter { it.folderPath.equals(folderPath, ignoreCase = true) && it.isVideo == isVideo }
        } else {
            listOfNotNull(matchedItem)
        }

        // Si proviene de una colección de Google Fotos (ej: Personas y mascotas) y apunta a DCIM/Camera (>20 fotos),
        // acotar al grupo de fotos de esa misma sesión/fecha para no mezclar las 100 fotos de la cámara.
        val contextItems = if (matchedItem != null && isGooglePhotosCloudUri(targetUri) && rawFolderItems.size > 20) {
            val sameDayWindowSec = 36L * 3600L
            val sessionGroup = rawFolderItems.filter { kotlin.math.abs(it.dateAdded - matchedItem.dateAdded) <= sameDayWindowSec }
            if (sessionGroup.size in 2..25) {
                sessionGroup
            } else {
                val idx = rawFolderItems.indexOfFirst { it.id == matchedItem.id }.coerceAtLeast(0)
                val from = (idx - 4).coerceAtLeast(0)
                val to = (from + 10).coerceAtMost(rawFolderItems.size)
                rawFolderItems.subList(from, to)
            }
        } else {
            rawFolderItems
        }
        folderPath to contextItems
    }

    fun buildFolderViewData(allItems: List<GalleryPhotoItem>, currentPath: String): FolderViewData {
        val cleanCurrent = currentPath.trim('/')
        val directItems = if (cleanCurrent.isEmpty()) emptyList() else allItems.filter { it.folderPath.equals(cleanCurrent, ignoreCase = true) }
        val childGrouping = LinkedHashMap<String, MutableList<GalleryPhotoItem>>()
        val prefix = if (cleanCurrent.isEmpty()) "" else "$cleanCurrent/"

        for (item in allItems) {
            val fp = item.folderPath.trim('/')
            if (cleanCurrent.isEmpty()) {
                val topSeg = fp.substringBefore('/')
                if (topSeg.isNotBlank()) childGrouping.getOrPut(topSeg) { mutableListOf() }.add(item)
            } else if (fp.startsWith(prefix, ignoreCase = true) && fp.length > prefix.length) {
                val nextSeg = fp.substring(prefix.length).substringBefore('/')
                if (nextSeg.isNotBlank()) childGrouping.getOrPut("$cleanCurrent/$nextSeg") { mutableListOf() }.add(item)
            }
        }

        val subfolders = childGrouping.map { (path, items) ->
            GalleryFolderInfo(path, path.substringAfterLast('/'), items.count { !it.isVideo }, items.count { it.isVideo }, items.firstOrNull(), false)
        }
        val directAlbumMap = LinkedHashMap<String, MutableList<GalleryPhotoItem>>()
        for (item in allItems) {
            val fp = item.folderPath.trim('/')
            if (fp.isNotBlank()) directAlbumMap.getOrPut(fp) { mutableListOf() }.add(item)
        }
        val allFolders = directAlbumMap.map { (path, items) ->
            GalleryFolderInfo(path, path.substringAfterLast('/'), items.count { !it.isVideo }, items.count { it.isVideo }, items.firstOrNull(), true)
        }
        return FolderViewData(subfolders, directItems, allFolders)
    }

    suspend fun getThumbnail(context: Context, item: GalleryPhotoItem, targetSize: Int = 240): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${item.isVideo}_${item.id}_${item.uri}"
        thumbnailCache.get(cacheKey)?.let { return@withContext it }
        val bmp = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(item.uri, Size(targetSize, targetSize), null)
            } else if (item.isVideo) {
                @Suppress("DEPRECATION")
                MediaStore.Video.Thumbnails.getThumbnail(context.contentResolver, item.id, MediaStore.Video.Thumbnails.MINI_KIND, null)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Thumbnails.getThumbnail(context.contentResolver, item.id, MediaStore.Images.Thumbnails.MINI_KIND, null)
            }
        } catch (_: Throwable) { null } ?: if (item.isVideo) decodeVideoFrame(context, item.uri) else decodeSampledBitmapFromUri(context, item.uri, targetSize, targetSize)

        if (bmp != null) thumbnailCache.put(cacheKey, bmp)
        bmp
    }

    private fun decodeVideoFrame(context: Context, uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Throwable) { null } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqW: Int, reqH: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            var inSampleSize = 1
            if (options.outHeight > reqH || options.outWidth > reqW) {
                val halfH = options.outHeight / 2; val halfW = options.outWidth / 2
                while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) inSampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize; inPreferredConfig = Bitmap.Config.RGB_565 }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
        } catch (_: Throwable) { null }
    }
}
