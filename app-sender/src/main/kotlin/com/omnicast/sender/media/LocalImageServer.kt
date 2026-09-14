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

    fun serveMedia(uri: Uri, mime: String): String {
        currentUri = uri
        currentMime = mime
        val ip = getLocalIpAddress()
        val ext = if (mime.startsWith("video/")) "mp4" else "jpg"
        return "http://$ip:$PORT/media_${System.currentTimeMillis()}.$ext"
    }

    fun serveImage(uri: Uri): String = serveMedia(uri, "image/jpeg")

    private fun handleClient(client: Socket) {
        try {
            client.use { s ->
                val input = s.getInputStream().bufferedReader()
                val reqLine = input.readLine() ?: return
                var line = input.readLine()
                while (!line.isNullOrEmpty()) { line = input.readLine() }

                val uri = currentUri
                val ctx = appContext
                if (uri != null && ctx != null) {
                    val stream: InputStream? = ctx.contentResolver.openInputStream(uri)
                    if (stream != null) {
                        val out = s.getOutputStream()
                        val mime = currentMime.ifBlank { "application/octet-stream" }
                        val size = try {
                            ctx.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
                        } catch (_: Throwable) { -1L }
                        val lenHeader = if (size > 0) "Content-Length: $size\r\n" else ""
                        val header = "HTTP/1.1 200 OK\r\nContent-Type: $mime\r\n${lenHeader}Accept-Ranges: bytes\r\nAccess-Control-Allow-Origin: *\r\nConnection: close\r\n\r\n"
                        out.write(header.toByteArray())
                        val buffer = ByteArray(64 * 1024)
                        stream.use { ins ->
                            var read: Int
                            while (ins.read(buffer).also { read = it } != -1) {
                                out.write(buffer, 0, read)
                            }
                        }
                        out.flush()
                        Log.i(TAG, "Media enviado con éxito ($mime) a ${s.inetAddress}")
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
