package com.omnicast.sender.ui.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

/**
 * Gestor de descargas directas de medios en segundo plano utilizando el DownloadManager nativo de Android.
 */
object MediaDownloadHelper {

    fun downloadMedia(
        context: Context,
        url: String,
        title: String,
        headers: Map<String, String> = emptyMap()
    ) {
        if (url.isBlank()) {
            Toast.makeText(context, "Enlace de video no válido", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (dm == null) {
                Toast.makeText(context, "Servicio de descarga de Android no disponible", Toast.LENGTH_SHORT).show()
                return
            }

            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9._ -]"), "_").trim().ifBlank { "pelicula_omnicast" }
            val ext = when {
                url.contains(".m3u8") -> ".m3u8"
                url.contains(".mpd") -> ".mpd"
                url.contains(".mkv") -> ".mkv"
                else -> ".mp4"
            }
            val fileName = "$sanitizedTitle$ext"

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(title.ifBlank { "Película OmniCast" })
                setDescription("Descargando en segundo plano en Descargas...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                headers.forEach { (k, v) ->
                    if (k.isNotBlank() && v.isNotBlank() && !k.startsWith(":")) {
                        try {
                            addRequestHeader(k, v)
                        } catch (_: Throwable) {}
                    }
                }
            }

            dm.enqueue(request)
            Toast.makeText(context, "⬇️ Descarga iniciada: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Throwable) {
            android.util.Log.e("MediaDownload", "Error iniciando descarga: ${e.message}")
            Toast.makeText(context, "Error al iniciar descarga: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
