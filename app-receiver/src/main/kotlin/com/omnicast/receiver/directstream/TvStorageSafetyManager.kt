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
    val id: String,
    val name: String,
    val rootPath: String,
    val isUsb: Boolean,
    val totalBytes: Long,
    val freeBytes: Long,
    val reserveBytes: Long,
    val usableBytes: Long,
    val canRecord: Boolean,
    val warningMessage: String? = null
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

    @Volatile
    private var watchdogJob: Job? = null

    fun calculateReserveBytes(isUsb: Boolean): Long =
        if (isUsb) EXTERNAL_USB_SAFETY_RESERVE_BYTES else INTERNAL_SAFETY_RESERVE_BYTES

    fun calculateUsableBytes(freeBytes: Long, isUsb: Boolean): Long {
        val reserve = calculateReserveBytes(isUsb)
        return (freeBytes - reserve).coerceAtLeast(0L)
    }

    fun isStorageSafeForRecording(freeBytes: Long, isUsb: Boolean): Boolean {
        val reserve = calculateReserveBytes(isUsb)
        return freeBytes > reserve
    }

    fun calculateMaxSafeMinutes(usableBytes: Long, preset: VideoBitratePreset): Long {
        if (usableBytes <= 0L || preset.mbPerMin <= 0.0) return 0L
        val mbAvailable = usableBytes.toDouble() / (1024.0 * 1024.0)
        return (mbAvailable / preset.mbPerMin).toLong()
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format(Locale.US, "%.2f GB", mb / 1024.0)
        } else {
            String.format(Locale.US, "%.0f MB", mb)
        }
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
                    id = "internal",
                    name = "Memoria Interna",
                    rootPath = internalPath,
                    isUsb = false,
                    totalBytes = totalBytes,
                    freeBytes = freeBytes,
                    reserveBytes = reserve,
                    usableBytes = usable,
                    canRecord = safe,
                    warningMessage = warn
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
                            id = "usb_$i",
                            name = "USB ($label)",
                            rootPath = usbDir.path,
                            isUsb = true,
                            totalBytes = totalBytes,
                            freeBytes = freeBytes,
                            reserveBytes = reserve,
                            usableBytes = usable,
                            canRecord = safe,
                            warningMessage = warn
                        )
                    )
                }
            }
        }

        return units
    }

    fun getPvrRecordingsDir(context: Context, useUsb: Boolean): File {
        if (useUsb) {
            val extDirs = context.getExternalFilesDirs(null).filterNotNull()
            if (extDirs.size > 1) {
                val usbDir = File(extDirs[1], "PVR_Grabaciones")
                if (usbDir.exists() || usbDir.mkdirs()) return usbDir
            }
        }
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val pvrDir = File(publicDownloads, "PVR_Grabaciones")
        if (pvrDir.exists() || pvrDir.mkdirs()) return pvrDir
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        return File(base, "PVR_Grabaciones").apply { mkdirs() }
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
