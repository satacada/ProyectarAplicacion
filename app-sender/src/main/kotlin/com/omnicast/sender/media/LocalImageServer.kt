package com.omnicast.sender.media

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Servidor HTTP local embebido ultraligero para transmitir fotos de la galería local a la TV Box.
 * 100% offline, cero compresión, resolución nativa.
 */
object LocalImageServer {
    private const val TAG = "LocalImageServer"
    const val PORT = 8090
    private var serverSocket: ServerSocket? = null
    private var currentUri: Uri? = null
    private var currentMime: String = "image/jpeg"
    private var appContext: Context? = null
    private var isRunning = false

    fun start(context: Context) {
        if (isRunning) return
        appContext = context.applicationContext
        isRunning = true
        thread(isDaemon = true, name = "LocalImageServerThread") {
            try {
                serverSocket = ServerSocket(PORT).apply { reuseAddress = true }
                Log.i(TAG, "Servidor multimedia local iniciado en puerto $PORT")
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    thread(isDaemon = true) { handleClient(client) }
                }
            } catch (e: Throwable) {
                if (isRunning) Log.e(TAG, "Error en LocalImageServer", e)
            }
        }
    }

    fun serveMedia(uri: Uri, mime: String, context: Context? = null): String {
        context?.let { start(it) }
        currentUri = uri
        currentMime = mime
        val ip = getLocalIpAddress()
        val ext = if (mime.startsWith("video/")) "mp4" else "jpg"
        return "http://$ip:$PORT/media_${System.currentTimeMillis()}.$ext"
    }

    fun serveImage(uri: Uri, context: Context? = null): String = serveMedia(uri, "image/jpeg", context)

    private fun handleClient(client: Socket) {
        try {
            client.use { s ->
                val input = s.getInputStream().bufferedReader()
                val reqLine = input.readLine() ?: return
                var rangeHeader: String? = null
                var line = input.readLine()
                while (!line.isNullOrEmpty()) {
                    if (line.startsWith("Range:", ignoreCase = true)) {
                        rangeHeader = line.substringAfter(":", "").trim()
                    }
                    line = input.readLine()
                }

                val uri = currentUri
                val ctx = appContext
                if (uri != null && ctx != null) {
                    val out = s.getOutputStream()
                    val mime = currentMime.ifBlank { "application/octet-stream" }
                    val pfd = ctx.contentResolver.openFileDescriptor(uri, "r")
                    val size = pfd?.statSize ?: -1L

                    if (pfd != null && size > 0) {
                        var start = 0L
                        var end = size - 1
                        val isRange = rangeHeader != null && rangeHeader.startsWith("bytes=")
                        if (isRange) {
                            val rangeVal = rangeHeader!!.substringAfter("bytes=").trim()
                            val parts = rangeVal.split("-")
                            start = parts.getOrNull(0)?.toLongOrNull() ?: 0L
                            end = parts.getOrNull(1)?.toLongOrNull() ?: (size - 1)
                            if (end >= size) end = size - 1
                        }

                        val contentLength = end - start + 1
                        val statusLine = if (isRange) "HTTP/1.1 206 Partial Content" else "HTTP/1.1 200 OK"
                        val rangeRespHeader = if (isRange) "Content-Range: bytes $start-$end/$size\r\n" else ""
                        val header = "$statusLine\r\nContent-Type: $mime\r\nContent-Length: $contentLength\r\n${rangeRespHeader}Accept-Ranges: bytes\r\nAccess-Control-Allow-Origin: *\r\nConnection: close\r\n\r\n"
                        out.write(header.toByteArray())

                        pfd.use { fd ->
                            java.io.FileInputStream(fd.fileDescriptor).use { fis ->
                                if (start > 0) fis.channel.position(start)
                                val buffer = ByteArray(64 * 1024)
                                var remaining = contentLength
                                while (remaining > 0) {
                                    val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                                    val read = fis.read(buffer, 0, toRead)
                                    if (read == -1) break
                                    out.write(buffer, 0, read)
                                    remaining -= read
                                }
                            }
                        }
                        out.flush()
                        return
                    }
                }
                val notFound = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"
                s.getOutputStream().write(notFound.toByteArray())
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error atendiendo petición de media: ${e.message}")
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return "127.0.0.1"
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Throwable) {}
        return "127.0.0.1"
    }

    fun stop() {
        isRunning = false
        try { serverSocket?.close() } catch (_: Throwable) {}
        serverSocket = null
        currentUri = null
    }
}
