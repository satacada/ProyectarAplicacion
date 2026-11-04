package com.omnicast.receiver.directstream

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TvDownloadedVideo(
    val title: String,
    val filePath: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val dateModifiedMs: Long,
    val formattedDate: String,
    val isUsb: Boolean
)

object TvDownloadsManager {

    private val MEDIA_EXTENSIONS = setOf("mp4", "ts", "mkv", "webm")

    fun getDownloadedVideos(context: Context): List<TvDownloadedVideo> {
        val result = mutableListOf<TvDownloadedVideo>()
        val scannedDirs = mutableSetOf<String>()

        // 1. Almacenamiento Interno
        val internalDownload = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val internalFolders = listOf(
            File(internalDownload, "PVR_Grabaciones"),
            File(internalDownload, "Pelis_descargas"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "PVR_Grabaciones")
        )
        for (f in internalFolders) {
            if (f.exists() && f.isDirectory && scannedDirs.add(f.absolutePath)) {
                scanFolder(f, isUsb = false, result)
            }
        }

        // 2. Dispositivos USB
        try {
            val storageUnits = TvStorageSafetyManager.detectAvailableStorageUnits(context)
            for (unit in storageUnits) {
                if (unit.isUsb) {
                    val usbDir = File(unit.rootPath)
                    val usbRoot = File(unit.rootPath.substringBefore("/Android"))
                    val usbFolders = listOf(
                        File(usbDir, "PVR_Grabaciones"),
                        File(usbDir, "Pelis_descargas"),
                        File(usbRoot, "Download/PVR_Grabaciones"),
                        File(usbRoot, "Download/Pelis_descargas")
                    )
                    for (uf in usbFolders) {
                        if (uf.exists() && uf.isDirectory && scannedDirs.add(uf.absolutePath)) {
                            scanFolder(uf, isUsb = true, result)
                        }
                    }
                }
            }
        } catch (_: Throwable) {}

        return result.sortedByDescending { it.dateModifiedMs }
    }

    private fun scanFolder(folder: File, isUsb: Boolean, outList: MutableList<TvDownloadedVideo>) {
        val files = folder.listFiles() ?: return
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        for (f in files) {
            if (f.isFile && f.length() > 0L) {
                val ext = f.extension.lowercase()
                if (ext in MEDIA_EXTENSIONS) {
                    val rawName = f.nameWithoutExtension.replace(Regex("_\\d+p(_\\d+)?$"), "").replace('_', ' ')
                    outList.add(
                        TvDownloadedVideo(
                            title = rawName.ifBlank { f.name },
                            filePath = f.absolutePath,
                            sizeBytes = f.length(),
                            formattedSize = formatBytes(f.length()),
                            dateModifiedMs = f.lastModified(),
                            formattedDate = dateFormat.format(Date(f.lastModified())),
                            isUsb = isUsb
                        )
                    )
                }
            }
        }
    }

    fun findDownloadedVideo(title: String, list: List<TvDownloadedVideo>): TvDownloadedVideo? {
        val clean = cleanTitle(title)
        if (clean.isBlank()) return null
        return list.firstOrNull {
            val itemClean = cleanTitle(it.title)
            itemClean == clean || (itemClean.length >= 4 && (itemClean in clean || clean in itemClean))
        }
    }

    fun deleteDownloadedVideo(filePath: String): Boolean {
        return try {
            val f = File(filePath)
            f.exists() && f.delete()
        } catch (_: Throwable) {
            false
        }
    }

    private fun cleanTitle(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9]"), "").trim()

    private fun formatBytes(bytes: Long): String {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return if (mb >= 1024.0) String.format(Locale.US, "%.2f GB", mb / 1024.0)
        else String.format(Locale.US, "%.0f MB", mb)
    }
}
