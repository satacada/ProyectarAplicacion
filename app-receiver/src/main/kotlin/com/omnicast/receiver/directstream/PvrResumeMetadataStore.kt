package com.omnicast.receiver.directstream

import org.json.JSONObject
import java.io.File

/**
 * Almacenamiento persistente de metadatos de reanudación VOD/PVR (HU-019 / Actividad 3.2).
 * Guarda puntos de control por archivo (.meta.json) ante desconexiones o micro-cortes.
 */
data class PvrResumeInfo(
    val url: String,
    val filePath: String,
    val lastSegmentIndex: Int,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val timestamp: Long = System.currentTimeMillis()
)

object PvrResumeMetadataStore {

    fun getMetadataFile(mediaFile: File): File =
        File(mediaFile.parentFile, "${mediaFile.name}.meta.json")

    fun saveProgress(info: PvrResumeInfo) {
        runCatching {
            val mediaFile = File(info.filePath)
            val metaFile = getMetadataFile(mediaFile)
            val json = JSONObject().apply {
                put("url", info.url)
                put("filePath", info.filePath)
                put("lastSegmentIndex", info.lastSegmentIndex)
                put("bytesDownloaded", info.bytesDownloaded)
                put("totalBytes", info.totalBytes)
                put("timestamp", info.timestamp)
            }
            metaFile.writeText(json.toString())
        }
    }

    fun loadResumeInfo(mediaFile: File): PvrResumeInfo? {
        val metaFile = getMetadataFile(mediaFile)
        if (!metaFile.exists() || !mediaFile.exists()) return null
        return runCatching {
            val json = JSONObject(metaFile.readText())
            PvrResumeInfo(
                url = json.getString("url"),
                filePath = json.getString("filePath"),
                lastSegmentIndex = json.getInt("lastSegmentIndex"),
                bytesDownloaded = json.getLong("bytesDownloaded"),
                totalBytes = json.getLong("totalBytes"),
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        }.getOrNull()
    }

    fun deleteMetadata(mediaFile: File) {
        runCatching {
            val metaFile = getMetadataFile(mediaFile)
            if (metaFile.exists()) metaFile.delete()
        }
    }
}
