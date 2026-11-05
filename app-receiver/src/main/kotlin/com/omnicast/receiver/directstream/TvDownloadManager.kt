package com.omnicast.receiver.directstream

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale

data class TvStorageBridgeInfo(
    val freeBytes: Long,
    val totalBytes: Long,
    val movieBytes: Long,
    val isExactMovieSize: Boolean,
    val hasUsbDrive: Boolean,
    val usbLabel: String,
    val bridgeFolderBytes: Long,
    val bridgeDirPath: String
) {
    val remainingBytesAfterDownload: Long get() = (freeBytes - movieBytes).coerceAtLeast(0L)
    val willExceedSafeSpace: Boolean get() = movieBytes >= (freeBytes * 0.88)
}

data class TvBridgeDownloadProgress(
    val isDownloading: Boolean = false,
    val isCompleted: Boolean = false,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val percent: Int = 0,
    val savedFilePath: String = "",
    val errorMsg: String? = null
)

object TvDownloadManager {

    @Volatile
    var activeJob: Job? = null

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format(Locale.US, "%.2f GB", mb / 1024.0)
        } else {
            String.format(Locale.US, "%.0f MB", mb)
        }
    }

    fun getBridgeDir(context: Context, useUsb: Boolean): File {
        if (useUsb) {
            val extDirs = context.getExternalFilesDirs(null).filterNotNull()
            if (extDirs.size > 1) {
                val usbDir = File(extDirs[1], "Pelis_descargas")
                if (usbDir.exists() || usbDir.mkdirs()) return usbDir
            }
        }
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val pelisDir = File(publicDownloads, "Pelis_descargas")
        if (pelisDir.exists() || pelisDir.mkdirs()) return pelisDir
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        return File(base, "Pelis_descargas").apply { mkdirs() }
    }

    suspend fun inspectStorageAndMovie(
        context: Context,
        url: String,
        headers: Map<String, String>,
        durationMs: Long = 0L
    ): TvStorageBridgeInfo = withContext(Dispatchers.IO) {
        val stat = StatFs(Environment.getDataDirectory().path)
        val freeBytes = stat.availableBytes
        val totalBytes = stat.totalBytes

        val extDirs = context.getExternalFilesDirs(null).filterNotNull()
        val hasUsb = extDirs.size > 1
        val usbLabel = if (hasUsb) extDirs[1].absolutePath.substringBefore("/Android") else ""

        val bridgeDir = getBridgeDir(context, false)
        val bridgeUsed = bridgeDir.listFiles()?.sumOf { it.length() } ?: 0L

        val (probedSize, isExact) = probeStreamSize(url, headers, durationMs)
        TvStorageBridgeInfo(
            freeBytes = freeBytes,
            totalBytes = totalBytes,
            movieBytes = probedSize,
            isExactMovieSize = isExact,
            hasUsbDrive = hasUsb,
            usbLabel = usbLabel,
            bridgeFolderBytes = bridgeUsed,
            bridgeDirPath = bridgeDir.absolutePath
        )
    }

    private fun probeStreamSize(url: String, headers: Map<String, String>, durationMs: Long): Pair<Long, Boolean> {
        val lower = url.lowercase()
        val isHls = lower.contains(".m3u8") || lower.contains("urlset") || lower.contains("master.txt")
        if (!isHls) {
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    setRequestProperty("Range", "bytes=0-1")
                    headers.forEach { (k, v) -> setRequestProperty(k, v) }
                }
                val rangeHeader = conn.getHeaderField("Content-Range") ?: ""
                val slashIdx = rangeHeader.lastIndexOf('/')
                if (slashIdx >= 0) {
                    val total = rangeHeader.substring(slashIdx + 1).trim().toLongOrNull()
                    if (total != null && total > 1024L * 1024L) {
                        conn.disconnect()
                        return total to true
                    }
                }
                val len = conn.contentLengthLong
                conn.disconnect()
                if (len > 1024L * 1024L) return len to true
            } catch (_: Throwable) {}
        }
        val estimatedSeconds = if (durationMs > 60_000L) (durationMs / 1000L) else 5400L // ~90 min por defecto
        val estimatedBytes = estimatedSeconds * 165_000L // ~1.3 Mbps promedio (~850 MB por película)
        return estimatedBytes to false
    }

    suspend fun downloadMovieInBridgeMode(
        context: Context,
        title: String,
        url: String,
        headers: Map<String, String>,
        expectedBytes: Long,
        useUsb: Boolean,
        targetDir: File = getBridgeDir(context, useUsb),
        targetHeight: Int = 1080,
        onProgress: (TvBridgeDownloadProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        val safeName = title.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim().replace(' ', '_').ifBlank { "pelicula_omnicast" }
        val lower = url.lowercase()
        val isHls = lower.contains(".m3u8") || lower.contains("urlset") || lower.contains("master.txt") || lower.contains(".txt")
        val isFmp4 = lower.contains("dailymotion.com") || lower.contains("dmcdn.net")
        val ext = if (isHls && !isFmp4) "ts" else "mp4"
        val outFile = File(targetDir, "${safeName}_${targetHeight}p.$ext")

        try {
            onProgress(TvBridgeDownloadProgress(isDownloading = true, totalBytes = expectedBytes, savedFilePath = outFile.absolutePath))
            if (isHls) {
                downloadHlsStream(url, headers, outFile, expectedBytes, targetHeight, onProgress)
            } else {
                downloadDirectStream(url, headers, outFile, expectedBytes, onProgress)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            if (outFile.exists() && outFile.length() == 0L) runCatching { outFile.delete() }
            val len = if (outFile.exists()) outFile.length() else 0L
            onProgress(TvBridgeDownloadProgress(isDownloading = false, isCompleted = len > 0L, downloadedBytes = len, totalBytes = expectedBytes, savedFilePath = outFile.absolutePath))
        } catch (e: Throwable) {
            if (outFile.exists() && outFile.length() == 0L) runCatching { outFile.delete() }
            onProgress(TvBridgeDownloadProgress(isDownloading = false, errorMsg = "Error en descarga: ${e.message ?: "Red interrumpida"}"))
        }
    }

    private suspend fun downloadDirectStream(
        url: String,
        headers: Map<String, String>,
        outFile: File,
        expectedBytes: Long,
        onProgress: (TvBridgeDownloadProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        val conn = PvrNetworkHelper.openConnection(url, headers, 10000, 15000)
        val total = conn.contentLengthLong.takeIf { it > 0L } ?: expectedBytes
        var downloaded = 0L
        val buf = ByteArray(64 * 1024)
        var fos: FileOutputStream? = null
        try {
            conn.inputStream.use { input ->
                while (isActive) {
                    val read = input.read(buf)
                    if (read == -1) break
                    val out = fos ?: FileOutputStream(outFile).also { fos = it }
                    out.write(buf, 0, read)
                    downloaded += read
                    val pct = if (total > 0L) ((downloaded * 100L) / total).toInt().coerceIn(1, 99) else 50
                    onProgress(TvBridgeDownloadProgress(true, false, downloaded, total, pct, outFile.absolutePath))
                }
            }
        } finally {
            runCatching { fos?.flush() }; runCatching { fos?.close() }
        }
        onProgress(TvBridgeDownloadProgress(false, true, downloaded, downloaded, 100, outFile.absolutePath))
    }

    private suspend fun downloadHlsStream(
        manifestUrl: String,
        headers: Map<String, String>,
        outFile: File,
        expectedBytes: Long,
        targetHeight: Int,
        onProgress: (TvBridgeDownloadProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        // Variante 1 (TvWebStreamScreen activo): delegar al puente Chromium para evitar bloqueo TLS 403
        if (TvWebPvrBridge.canHandleWebStream(manifestUrl)) {
            val ok = TvWebPvrBridge.downloadViaChromium(outFile, expectedBytes, onProgress)
            if (ok) return@withContext
            throw java.io.IOException("Flujo web protegido interrumpido o rechazado por CDN")
        }

        val text = PvrNetworkHelper.fetchText(manifestUrl, headers)
        val master = PvrHlsMasterParser.parseMaster(text, manifestUrl)
        val chosenVariant = PvrHlsMasterParser.selectVariant(master.variants, targetHeight)
        val finalManifestUrl = chosenVariant?.url ?: manifestUrl
        val finalText = if (chosenVariant != null) PvrNetworkHelper.fetchText(finalManifestUrl, headers) else text
        val segments = PvrHlsMasterParser.parseSegments(finalText, finalManifestUrl)

        if (segments.isEmpty()) {
            downloadDirectStream(manifestUrl, headers, outFile, expectedBytes, onProgress)
            return@withContext
        }

        var downloaded = 0L
        val buf = ByteArray(64 * 1024)
        var fos: FileOutputStream? = null
        try {
            for ((idx, segUrl) in segments.withIndex()) {
                if (!isActive) break
                val conn = PvrNetworkHelper.openConnection(segUrl, headers, 8000, 12000)
                conn.inputStream.use { input ->
                    while (isActive) {
                        val r = input.read(buf)
                        if (r == -1) break
                        val out = fos ?: FileOutputStream(outFile).also { fos = it }
                        out.write(buf, 0, r)
                        downloaded += r
                    }
                }
                val pct = (((idx + 1) * 100) / segments.size).coerceIn(1, 99)
                onProgress(TvBridgeDownloadProgress(true, false, downloaded, expectedBytes, pct, outFile.absolutePath))
            }
        } finally {
            runCatching { fos?.flush() }; runCatching { fos?.close() }
        }
        onProgress(TvBridgeDownloadProgress(false, true, downloaded, downloaded, 100, outFile.absolutePath))
    }

    fun findDownloadedMovie(context: Context, title: String): File? {
        val safeName = title.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim().replace(' ', '_').ifBlank { "pelicula_omnicast" }
        listOf(false, true).forEach { useUsb ->
            val dir = getBridgeDir(context, useUsb)
            listOf("mp4", "ts", "mkv", "webm").forEach { ext ->
                val f = File(dir, "$safeName.$ext")
                if (f.exists() && f.length() > 0L) return f
            }
        }
        return null
    }

    fun deleteMovieDownload(context: Context, title: String): Long {
        val f = findDownloadedMovie(context, title) ?: return 0L
        val size = f.length()
        return if (f.delete()) size else 0L
    }

    fun clearBridgeDownloads(context: Context): Long {
        var freed = 0L
        listOf(false, true).forEach { useUsb ->
            val dir = getBridgeDir(context, useUsb)
            dir.listFiles()?.forEach { file ->
                freed += file.length()
                runCatching { file.delete() }
            }
        }
        return freed
    }
}
