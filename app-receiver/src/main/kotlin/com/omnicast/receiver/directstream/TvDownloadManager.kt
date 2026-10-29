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

    private fun getBridgeDir(context: Context, useUsb: Boolean): File {
        val extDirs = context.getExternalFilesDirs(null).filterNotNull()
        if (useUsb && extDirs.size > 1) {
            val usbDir = File(extDirs[1], "OmniCast_Peliculas")
            if (usbDir.exists() || usbDir.mkdirs()) return usbDir
        }
        val base = extDirs.firstOrNull() ?: context.filesDir
        return File(base, "OmniCast_Puente").apply { mkdirs() }
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
        onProgress: (TvBridgeDownloadProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        val targetDir = getBridgeDir(context, useUsb)
        val safeName = title.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim().replace(' ', '_').ifBlank { "pelicula_omnicast" }
        val lower = url.lowercase()
        val isHls = lower.contains(".m3u8") || lower.contains("urlset") || lower.contains("master.txt")
        val ext = if (isHls) "ts" else "mp4"
        val outFile = File(targetDir, "${safeName}.$ext")

        try {
            onProgress(TvBridgeDownloadProgress(isDownloading = true, totalBytes = expectedBytes, savedFilePath = outFile.absolutePath))
            if (isHls) {
                downloadHlsStream(url, headers, outFile, expectedBytes, onProgress)
            } else {
                downloadDirectStream(url, headers, outFile, expectedBytes, onProgress)
            }
        } catch (e: Throwable) {
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
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 15000
            instanceFollowRedirects = true
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        val total = conn.contentLengthLong.takeIf { it > 0L } ?: expectedBytes
        var downloaded = 0L
        val buf = ByteArray(64 * 1024)
        conn.inputStream.use { input ->
            FileOutputStream(outFile).use { output ->
                while (isActive) {
                    val read = input.read(buf)
                    if (read == -1) break
                    output.write(buf, 0, read)
                    downloaded += read
                    val pct = if (total > 0L) ((downloaded * 100L) / total).toInt().coerceIn(1, 99) else 50
                    onProgress(TvBridgeDownloadProgress(true, false, downloaded, total, pct, outFile.absolutePath))
                }
            }
        }
        onProgress(TvBridgeDownloadProgress(false, true, downloaded, downloaded, 100, outFile.absolutePath))
    }

    private suspend fun downloadHlsStream(
        manifestUrl: String,
        headers: Map<String, String>,
        outFile: File,
        expectedBytes: Long,
        onProgress: (TvBridgeDownloadProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        val text = fetchText(manifestUrl, headers)
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val subPlaylistUrl = lines.firstOrNull { !it.startsWith("#") && (it.contains(".m3u8") || it.contains(".txt")) }
            ?.let { resolveRelativeUrl(manifestUrl, it) }
        val finalManifestUrl = subPlaylistUrl ?: manifestUrl
        val finalLines = if (subPlaylistUrl != null) fetchText(subPlaylistUrl, headers).lines().map { it.trim() } else lines
        val segments = finalLines.filter { it.isNotEmpty() && !it.startsWith("#") }.map { resolveRelativeUrl(finalManifestUrl, it) }

        if (segments.isEmpty()) {
            downloadDirectStream(manifestUrl, headers, outFile, expectedBytes, onProgress)
            return@withContext
        }

        var downloaded = 0L
        val buf = ByteArray(64 * 1024)
        FileOutputStream(outFile).use { output ->
            for ((idx, segUrl) in segments.withIndex()) {
                if (!isActive) break
                val conn = (URL(segUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 12000
                    headers.forEach { (k, v) -> setRequestProperty(k, v) }
                }
                conn.inputStream.use { input ->
                    while (isActive) {
                        val r = input.read(buf)
                        if (r == -1) break
                        output.write(buf, 0, r)
                        downloaded += r
                    }
                }
                val pct = (((idx + 1) * 100) / segments.size).coerceIn(1, 99)
                onProgress(TvBridgeDownloadProgress(true, false, downloaded, expectedBytes, pct, outFile.absolutePath))
            }
        }
        onProgress(TvBridgeDownloadProgress(false, true, downloaded, downloaded, 100, outFile.absolutePath))
    }

    private fun fetchText(url: String, headers: Map<String, String>): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun resolveRelativeUrl(baseUrl: String, relative: String): String {
        if (relative.startsWith("http://") || relative.startsWith("https://")) return relative
        return try { URI(baseUrl).resolve(relative).toString() } catch (_: Throwable) { relative }
    }

    fun clearBridgeDownloads(context: Context): Long {
        val dir = getBridgeDir(context, false)
        val freed = dir.listFiles()?.sumOf { it.length() } ?: 0L
        dir.listFiles()?.forEach { runCatching { it.delete() } }
        return freed
    }
}
