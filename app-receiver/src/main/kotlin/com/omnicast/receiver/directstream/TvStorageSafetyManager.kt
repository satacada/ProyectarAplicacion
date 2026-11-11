package com.omnicast.receiver.directstream

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

data class TvStorageUnit(
    val id: String, val name: String, val rootPath: String, val isUsb: Boolean,
    val totalBytes: Long, val freeBytes: Long, val reserveBytes: Long,
    val usableBytes: Long, val canRecord: Boolean, val warningMessage: String? = null
)

data class TvPvrRecordingFile(
    val name: String, val path: String, val sizeBytes: Long,
    val formattedSize: String, val lastModified: Long, val isUsb: Boolean
)

enum class VideoBitratePreset(val label: String, val mbPerMin: Double, val kbps: Int) {
    FULL_HD_1080P("1080p Full HD (~3.5 Mbps)", 26.2, 3500),
    HD_720P("720p HD (~2.0 Mbps)", 15.0, 2000),
    SD_480P("480p SD (~1.0 Mbps)", 7.5, 1000),
    LOW_360P("360p Ahorro (~0.6 Mbps)", 4.5, 600)
}

/**
 * Gestor de Blindaje de Almacenamiento Seguro para PVR y Descargas en Android TV.
 * Cumple estrictamente con la Reserva Sagrada de 2.0 GB en Memoria Interna y 500 MB en USB.
 */
object TvStorageSafetyManager {

    const val INTERNAL_SAFETY_RESERVE_BYTES: Long = 2L * 1024L * 1024L * 1024L // 2.0 GB
    const val EXTERNAL_USB_SAFETY_RESERVE_BYTES: Long = 500L * 1024L * 1024L // 500 MB
    const val SAFE_RESERVE_USB_BYTES: Long = EXTERNAL_USB_SAFETY_RESERVE_BYTES

    @Volatile
    private var watchdogJob: Job? = null

    fun calculateReserveBytes(isUsb: Boolean): Long = if (isUsb) EXTERNAL_USB_SAFETY_RESERVE_BYTES else INTERNAL_SAFETY_RESERVE_BYTES
    fun calculateUsableBytes(freeBytes: Long, isUsb: Boolean): Long = (freeBytes - calculateReserveBytes(isUsb)).coerceAtLeast(0L)
    fun isStorageSafeForRecording(freeBytes: Long, isUsb: Boolean): Boolean = freeBytes > calculateReserveBytes(isUsb)
    fun isUsbStorageAvailable(context: Context): Boolean = context.getExternalFilesDirs(null).filterNotNull().size > 1
    fun getAvailableBytes(path: String): Long = runCatching { StatFs(path).availableBytes }.getOrDefault(0L)

    fun calculateMaxSafeMinutes(usableBytes: Long, preset: VideoBitratePreset): Long =
        if (usableBytes <= 0L || preset.mbPerMin <= 0.0) 0L else ((usableBytes.toDouble() / (1024.0 * 1024.0)) / preset.mbPerMin).toLong()

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return if (mb >= 1024.0) String.format(Locale.US, "%.2f GB", mb / 1024.0) else String.format(Locale.US, "%.0f MB", mb)
    }

    fun detectAvailableStorageUnits(context: Context): List<TvStorageUnit> {
        val units = mutableListOf<TvStorageUnit>()

        // 1. Memoria Interna (Primary)
        runCatching {
            val internalPath = Environment.getDataDirectory().path
            val stat = StatFs(internalPath)
            val freeBytes = stat.availableBytes
            val totalBytes = stat.totalBytes
            val reserve = INTERNAL_SAFETY_RESERVE_BYTES
            val usable = calculateUsableBytes(freeBytes, false)
            val safe = freeBytes > reserve

            val warn = if (!safe) {
                "Espacio insuficiente: La memoria interna tiene menos de 2.0 GB libres. Por seguridad del sistema, conecta una memoria USB para grabar."
            } else null

            units.add(
                TvStorageUnit(
                    id = "internal", name = "Memoria Interna", rootPath = internalPath, isUsb = false,
                    totalBytes = totalBytes, freeBytes = freeBytes, reserveBytes = reserve,
                    usableBytes = usable, canRecord = safe, warningMessage = warn
                )
            )
        }

        // 2. Unidades Externas USB (Secondary)
        runCatching {
            val extDirs = context.getExternalFilesDirs(null).filterNotNull()
            if (extDirs.size > 1) {
                for (i in 1 until extDirs.size) {
                    val usbDir = extDirs[i]
                    val stat = StatFs(usbDir.path)
                    val freeBytes = stat.availableBytes
                    val totalBytes = stat.totalBytes
                    val reserve = EXTERNAL_USB_SAFETY_RESERVE_BYTES
                    val usable = calculateUsableBytes(freeBytes, true)
                    val safe = freeBytes > reserve
                    val label = usbDir.absolutePath.substringBefore("/Android").substringAfterLast('/')
                        .ifBlank { "Unidad USB $i" }

                    val warn = if (!safe) {
                        "Espacio insuficiente: La memoria USB ($label) tiene menos de 500 MB libres."
                    } else null

                    units.add(
                        TvStorageUnit(
                            id = "usb_$i", name = "USB ($label)", rootPath = usbDir.path, isUsb = true,
                            totalBytes = totalBytes, freeBytes = freeBytes, reserveBytes = reserve,
                            usableBytes = usable, canRecord = safe, warningMessage = warn
                        )
                    )
                }
            }
        }

        return units
    }

    fun getAvailableFolders(context: Context, useUsb: Boolean): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        if (useUsb) {
            val extDirs = context.getExternalFilesDirs(null).filterNotNull()
            if (extDirs.size > 1) {
                val usbRoot = extDirs[1]
                list.add("USB > PVR_Grabaciones" to File(usbRoot, "PVR_Grabaciones").apply { mkdirs() }.absolutePath)
                list.add("USB > Pelis_descargas" to File(usbRoot, "Pelis_descargas").apply { mkdirs() }.absolutePath)
                list.add("USB > Movies" to File(usbRoot, "Movies").apply { mkdirs() }.absolutePath)
                return list
            }
        }
        val pubDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val pubMovies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        list.add("Download > PVR_Grabaciones" to File(pubDownloads, "PVR_Grabaciones").apply { mkdirs() }.absolutePath)
        list.add("Download > Pelis_descargas" to File(pubDownloads, "Pelis_descargas").apply { mkdirs() }.absolutePath)
        list.add("Movies > OmniCast" to File(pubMovies, "OmniCast").apply { mkdirs() }.absolutePath)
        return list
    }

    fun getPvrRecordingsDir(context: Context, useUsb: Boolean, subFolder: String = "PVR_Grabaciones"): File {
        if (useUsb) {
            val extDirs = context.getExternalFilesDirs(null).filterNotNull()
            if (extDirs.size > 1) {
                val usbDir = File(extDirs[1], subFolder)
                if (usbDir.exists() || usbDir.mkdirs()) return usbDir
            }
        }
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val pvrDir = File(publicDownloads, subFolder)
        if (pvrDir.exists() || pvrDir.mkdirs()) return pvrDir
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        return File(base, subFolder).apply { mkdirs() }
    }

    fun getTargetDirectory(context: Context, useUsb: Boolean, customPath: String? = null, defaultFolder: String = "PVR_Grabaciones"): File {
        if (!customPath.isNullOrBlank()) {
            val f = File(customPath)
            if (f.exists() || f.mkdirs()) return f
        }
        return getPvrRecordingsDir(context, useUsb, defaultFolder)
    }

    fun getTargetDirectory(context: Context, isMovieDownload: Boolean, useUsb: Boolean): File? {
        if (useUsb && !isUsbStorageAvailable(context)) return null
        val sub = if (isMovieDownload) "Pelis_descargas" else "PVR_Grabaciones"
        return getPvrRecordingsDir(context, useUsb, sub)
    }

    fun purgeOrphanTempFiles(context: Context) {
        runCatching {
            val dirs = listOfNotNull(
                getTargetDirectory(context, isMovieDownload = false, useUsb = false),
                getTargetDirectory(context, isMovieDownload = true, useUsb = false),
                getTargetDirectory(context, isMovieDownload = false, useUsb = true),
                getTargetDirectory(context, isMovieDownload = true, useUsb = true)
            ).distinct()
            dirs.forEach { dir ->
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.forEach { file ->
                        val name = file.name.lowercase()
                        if (name.endsWith(".tmp") || name.endsWith(".part") || (file.isFile && file.length() == 0L)) {
                            // Purga solo si tiene más de 3 minutos de antigüedad para no borrar grabaciones en curso
                            if (System.currentTimeMillis() - file.lastModified() > 180_000L) {
                                file.delete()
                            }
                        }
                    }
                }
            }
        }
    }

    fun getAutoPreferredDirectory(context: Context, isMovieDownload: Boolean): Pair<File, Boolean> {
        val usbDir = getTargetDirectory(context, isMovieDownload, useUsb = true)
        if (usbDir != null && isUsbStorageAvailable(context)) {
            val freeBytes = getAvailableBytes(usbDir.absolutePath)
            if (freeBytes > (SAFE_RESERVE_USB_BYTES + 1_000_000_000L)) { // >= 1.5 GB
                return Pair(usbDir, true)
            }
        }
        val internalDir = getTargetDirectory(context, isMovieDownload, useUsb = false)
            ?: context.filesDir
        return Pair(internalDir, false)
    }

    fun scanPvrRecordings(context: Context): List<TvPvrRecordingFile> {
        val results = mutableListOf<TvPvrRecordingFile>()
        val scanned = mutableSetOf<String>()
        val subDirs = listOf("PVR_Grabaciones", "Pelis_descargas", "Movies/PVR")

        // 1. Almacenamiento Interno
        runCatching {
            val pubDown = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val pubMov = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val appExt = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            listOf(pubDown, pubMov, appExt).forEach { root ->
                subDirs.forEach { sub ->
                    val dir = File(root, sub)
                    if (scanned.add(dir.absolutePath)) results.addAll(scanDirectoryForRecordings(dir, false))
                }
            }
            val movPvr = File(pubMov, "PVR")
            if (scanned.add(movPvr.absolutePath)) results.addAll(scanDirectoryForRecordings(movPvr, false))
        }

        // 2. Unidades USB detectadas
        runCatching {
            detectAvailableStorageUnits(context).filter { it.isUsb }.forEach { unit ->
                val uRoot = File(unit.rootPath)
                val uParent = File(unit.rootPath.substringBefore("/Android"))
                listOf(uRoot, uParent).forEach { root ->
                    subDirs.forEach { sub ->
                        val dir = File(root, sub)
                        if (scanned.add(dir.absolutePath)) results.addAll(scanDirectoryForRecordings(dir, true))
                        val dlDir = File(root, "Download/$sub")
                        if (scanned.add(dlDir.absolutePath)) results.addAll(scanDirectoryForRecordings(dlDir, true))
                    }
                }
            }
        }
        return results.sortedByDescending { it.lastModified }
    }

    fun scanDirectoryForRecordings(dir: File, isUsb: Boolean): List<TvPvrRecordingFile> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return (dir.listFiles() ?: emptyArray())
            .filter { it.isFile && it.length() > 0L && (it.extension.equals("ts", true) || it.extension.equals("mp4", true)) }
            .map { f ->
                TvPvrRecordingFile(f.name, f.absolutePath, f.length(), formatBytes(f.length()), f.lastModified(), isUsb)
            }
            .sortedByDescending { it.lastModified }
    }

    fun startWatchdog(
        targetPath: String,
        isUsb: Boolean,
        scope: CoroutineScope,
        intervalMs: Long = 5000L,
        onThresholdReached: (String) -> Unit
    ): Job {
        stopWatchdog()
        val reserve = calculateReserveBytes(isUsb)
        val job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(intervalMs)
                try {
                    val stat = StatFs(targetPath)
                    val available = stat.availableBytes
                    if (available <= reserve) {
                        val limitLabel = if (isUsb) "500 MB" else "2.0 GB"
                        val msg = "⚠️ Grabación detenida por seguridad: Se preservaron $limitLabel libres para proteger la TV Box. Tu video se guardó íntegro hasta el minuto grabado."
                        onThresholdReached(msg)
                        break
                    }
                } catch (_: Throwable) {
                    // Si el dispositivo fue desconectado
                    onThresholdReached("⚠️ Grabación interrumpida: Unidad de almacenamiento inaccesible.")
                    break
                }
            }
        }
        watchdogJob = job
        return job
    }

    fun stopWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = null
    }
}
