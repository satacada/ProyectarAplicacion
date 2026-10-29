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

    fun hasGalleryPermission(context: Context, mimeFilter: String = "*/*"): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasImg = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasVid = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            when {
                mimeFilter.startsWith("image") -> hasImg
                mimeFilter.startsWith("video") -> hasVid
                else -> hasImg || hasVid
            }
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getRequiredPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE

    fun getRequiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

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
        if (!hasGalleryPermission(context, mimeFilter)) return@withContext result
        if (!mimeFilter.startsWith("video/")) queryCollection(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, result)
        if (!mimeFilter.startsWith("image/")) queryCollection(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, result)
        result.sortByDescending { it.dateAdded }
        GallerySessionHolder.cachedAllItems = result
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
                } else if (auth == "com.android.providers.media.documents" && docId.contains(':')) {
                    val type = docId.substringBefore(':')
                    val mediaId = docId.substringAfter(':').toLongOrNull()
                    if (mediaId != null) {
                        val base = if (type == "video") MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        return queryFolderPathFromContentUri(context, ContentUris.withAppendedId(base, mediaId))
                    }
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

    suspend fun loadFolderContextForUri(context: Context, targetUri: Uri, isVideo: Boolean): Pair<String, List<GalleryPhotoItem>> = withContext(Dispatchers.IO) {
        val all = loadAllMedia(context, if (isVideo) "video/*" else "image/*")
        val decodedUri = Uri.decode(targetUri.toString()).replace('\\', '/')
        val targetId = targetUri.lastPathSegment?.substringAfterLast(':') ?: ""
        val matchedItem = all.firstOrNull { item ->
            item.uri == targetUri ||
                (targetId.isNotBlank() && item.id.toString() == targetId) ||
                (item.folderPath.isNotBlank() && decodedUri.endsWith("${item.folderPath}/${item.name}", ignoreCase = true))
        }
        val folderPath = matchedItem?.folderPath ?: resolveFolderPathFromUri(context, targetUri) ?: ""
        val contextItems = if (folderPath.isNotBlank()) {
            all.filter { it.folderPath.equals(folderPath, ignoreCase = true) && it.isVideo == isVideo }
        } else {
            listOfNotNull(matchedItem)
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
