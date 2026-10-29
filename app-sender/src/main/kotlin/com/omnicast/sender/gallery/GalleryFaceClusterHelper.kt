package com.omnicast.sender.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PointF
import android.media.FaceDetector
import android.net.Uri
import android.os.Build
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Indexador y agrupador inteligente de rostros para el flujo de Colecciones -> Personas y mascotas.
 * Usa el detector nativo android.media.FaceDetector y descriptores faciales normalizados con caché
 * persistente para agrupar únicamente las fotos de la misma persona sin mezclar carpetas masivas.
 */
object GalleryFaceClusterHelper {

    private const val PREFS_NAME = "omnicast_face_cluster_cache_v1"
    private const val FEATURE_DIM = 36
    private const val SIMILARITY_THRESHOLD = 0.885f
    private const val MAX_SCAN_NEW_ITEMS = 140

    private val memoryCache = ConcurrentHashMap<Long, FloatArray>()
    private val noFaceSet = ConcurrentHashMap.newKeySet<Long>()
    @Volatile
    private var cacheLoaded = false

    private fun ensureCacheLoaded(context: Context) {
        if (cacheLoaded) return
        synchronized(this) {
            if (cacheLoaded) return
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            for ((key, value) in prefs.all) {
                val id = key.toLongOrNull() ?: continue
                val str = value as? String ?: continue
                if (str == "NO_FACE") {
                    noFaceSet.add(id)
                } else {
                    val parts = str.split(',')
                    if (parts.size == FEATURE_DIM) {
                        val arr = FloatArray(FEATURE_DIM) { i -> parts[i].toFloatOrNull() ?: 0f }
                        memoryCache[id] = arr
                    }
                }
            }
            cacheLoaded = true
        }
    }

    private fun saveToCache(context: Context, id: Long, features: FloatArray?) {
        if (id <= 0L) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (features == null) {
            noFaceSet.add(id)
            prefs.edit().putString(id.toString(), "NO_FACE").apply()
        } else {
            memoryCache[id] = features
            val serialized = features.joinToString(",") { String.format(java.util.Locale.US, "%.4f", it) }
            prefs.edit().putString(id.toString(), serialized).apply()
        }
    }

    suspend fun findPersonClusterForUri(
        context: Context,
        targetUri: Uri,
        allPhotos: List<GalleryPhotoItem>,
        onProgressUpdate: ((List<GalleryPhotoItem>, Int) -> Unit)? = null
    ): Pair<String, List<GalleryPhotoItem>> = withContext(Dispatchers.Default) {
        ensureCacheLoaded(context)
        val photosOnly = allPhotos.filter { !it.isVideo }
        val targetId = extractMediaId(targetUri)
        val matchedTargetItem = photosOnly.firstOrNull {
            it.uri == targetUri || (targetId != null && it.id == targetId)
        } ?: GalleryPhotoItem(
            id = targetId ?: 0L,
            uri = targetUri,
            name = "Foto seleccionada",
            albumName = "Personas y mascotas",
            dateAdded = System.currentTimeMillis() / 1000L,
            isVideo = false,
            mimeType = "image/jpeg",
            folderPath = "__collections_person__"
        )

        val targetFeatures = extractFaceFeaturesFromUri(context, targetUri, matchedTargetItem.id)
        if (targetFeatures == null) {
            // Si la imagen elegida en Colecciones no es un rostro (ej. paisaje/objeto),
            // agrupar fotos de la misma sesión temporal cercana (±48h) sin cargar las 357 capturas
            val refDate = matchedTargetItem.dateAdded
            val sessionCluster = photosOnly.filter {
                kotlin.math.abs(it.dateAdded - refDate) <= 172_800L &&
                    it.folderPath.equals(matchedTargetItem.folderPath, ignoreCase = true)
            }.take(25).toMutableList()
            if (sessionCluster.none { it.uri == matchedTargetItem.uri || (matchedTargetItem.id > 0 && it.id == matchedTargetItem.id) }) {
                sessionCluster.add(0, matchedTargetItem)
            }
            return@withContext "Colección (${sessionCluster.size})" to sessionCluster
        }

        val cluster = mutableListOf<GalleryPhotoItem>()
        cluster.add(matchedTargetItem)

        // Ordenar candidatos priorizando cercanía temporal y carpetas de cámara/retratos
        val sortedCandidates = photosOnly
            .filter { it.id != matchedTargetItem.id && it.uri != targetUri }
            .sortedBy { kotlin.math.abs(it.dateAdded - matchedTargetItem.dateAdded) }

        var newlyDecoded = 0
        for (candidate in sortedCandidates) {
            if (noFaceSet.contains(candidate.id)) continue
            val candFeatures = memoryCache[candidate.id] ?: if (newlyDecoded < MAX_SCAN_NEW_ITEMS) {
                newlyDecoded++
                extractFaceFeaturesFromUri(context, candidate.uri, candidate.id)
            } else null

            if (candFeatures != null) {
                val sim = computeSimilarity(targetFeatures, candFeatures)
                if (sim >= SIMILARITY_THRESHOLD) {
                    cluster.add(candidate)
                    if (cluster.size >= 3 && cluster.size % 2 == 1 && onProgressUpdate != null) {
                        val sortedNow = cluster.sortedByDescending { it.dateAdded }
                        val idxNow = sortedNow.indexOfFirst { it.id == matchedTargetItem.id || it.uri == targetUri }.coerceAtLeast(0)
                        onProgressUpdate.invoke(sortedNow, idxNow)
                    }
                }
            }
        }

        val finalSorted = cluster.sortedByDescending { it.dateAdded }
        "Personas y mascotas" to finalSorted
    }

    private fun extractMediaId(uri: Uri): Long? {
        val decoded = Uri.decode(uri.toString()).replace('\\', '/')
        val regexId = Regex("(?:media/external/(?:images|video|file)/media/|media/picker/\\d+/[^/]+/media/|image:|video:)(\\d+)")
            .find(decoded)?.groupValues?.getOrNull(1)?.toLongOrNull()
        if (regexId != null) return regexId
        return uri.lastPathSegment?.substringAfterLast(':')?.filter { it.isDigit() }?.toLongOrNull()
    }

    private fun extractFaceFeaturesFromUri(context: Context, uri: Uri, mediaId: Long): FloatArray? {
        if (mediaId > 0L) {
            memoryCache[mediaId]?.let { return it }
            if (noFaceSet.contains(mediaId)) return null
        }
        val bmp565 = decodeRgb565ForFaceDetection(context, uri) ?: return null
        return try {
            val faces = arrayOfNulls<FaceDetector.Face>(3)
            val detector = FaceDetector(bmp565.width, bmp565.height, faces.size)
            val found = detector.findFaces(bmp565, faces)
            val bestFace = faces.take(found).filterNotNull().maxByOrNull { it.confidence() * it.eyesDistance() }
            if (bestFace == null || bestFace.confidence() < 0.30f || bestFace.eyesDistance() < 10f) {
                saveToCache(context, mediaId, null)
                null
            } else {
                val features = computeFaceDescriptor(bmp565, bestFace)
                saveToCache(context, mediaId, features)
                features
            }
        } catch (_: Throwable) {
            null
        } finally {
            try { bmp565.recycle() } catch (_: Throwable) {}
        }
    }

    private fun decodeRgb565ForFaceDetection(context: Context, uri: Uri): Bitmap? {
        val raw = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, Size(320, 320), null)
            } else null
        } catch (_: Throwable) { null } ?: try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while ((bounds.outWidth / sample) > 360 || (bounds.outHeight / sample) > 360) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565 }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (_: Throwable) { null } ?: return null

        // FaceDetector requiere ancho PAR y configuración estricta Bitmap.Config.RGB_565
        val evenW = (raw.width and -2).coerceAtLeast(2)
        val evenH = (raw.height and -2).coerceAtLeast(2)
        if (raw.config == Bitmap.Config.RGB_565 && raw.width == evenW && raw.height == evenH) return raw
        val converted = Bitmap.createBitmap(evenW, evenH, Bitmap.Config.RGB_565)
        val canvas = Canvas(converted)
        canvas.drawBitmap(raw, 0f, 0f, null)
        if (converted != raw) {
            try { raw.recycle() } catch (_: Throwable) {}
        }
        return converted
    }

    private fun computeFaceDescriptor(bmp: Bitmap, face: FaceDetector.Face): FloatArray {
        val mid = PointF()
        face.getMidPoint(mid)
        val eyeDist = face.eyesDistance()
        val halfBox = eyeDist * 1.35f
        val left = (mid.x - halfBox).toInt().coerceIn(0, bmp.width - 2)
        val top = (mid.y - halfBox * 0.95f).toInt().coerceIn(0, bmp.height - 2)
        val right = (mid.x + halfBox).toInt().coerceIn(left + 1, bmp.width)
        val bottom = (mid.y + halfBox * 1.25f).toInt().coerceIn(top + 1, bmp.height)

        val grid = 3 // 3x3 celdas x 4 canales (rNorm, gNorm, bNorm, luma) = 36 features
        val out = FloatArray(FEATURE_DIM)
        val boxW = (right - left).coerceAtLeast(1)
        val boxH = (bottom - top).coerceAtLeast(1)

        var idx = 0
        for (gy in 0 until grid) {
            val y0 = top + (gy * boxH) / grid
            val y1 = (top + ((gy + 1) * boxH) / grid).coerceAtLeast(y0 + 1)
            for (gx in 0 until grid) {
                val x0 = left + (gx * boxW) / grid
                val x1 = (left + ((gx + 1) * boxW) / grid).coerceAtLeast(x0 + 1)
                var sumR = 0f; var sumG = 0f; var sumB = 0f; var count = 0
                for (y in y0 until y1 step 2) {
                    for (x in x0 until x1 step 2) {
                        val px = bmp.getPixel(x, y)
                        sumR += ((px shr 16) and 0xFF)
                        sumG += ((px shr 8) and 0xFF)
                        sumB += (px and 0xFF)
                        count++
                    }
                }
                val c = count.coerceAtLeast(1).toFloat()
                val r = sumR / c; val g = sumG / c; val b = sumB / c
                val total = (r + g + b).coerceAtLeast(1f)
                out[idx++] = r / total
                out[idx++] = g / total
                out[idx++] = b / total
                out[idx++] = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            }
        }
        var norm = 0f
        for (v in out) norm += v * v
        val invNorm = 1f / sqrt(norm.coerceAtLeast(1e-6f))
        for (i in out.indices) out[i] *= invNorm
        return out
    }

    private fun computeSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f
        for (i in a.indices) dot += a[i] * b[i]
        return min(1f, max(0f, dot))
    }
}
