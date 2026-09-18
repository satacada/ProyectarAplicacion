package com.omnicast.sender.media

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Servidor HTTP local embebido ultraligero para transmitir fotos y videos locales a la TV Box.
 * 100% offline, cero compresión, resolución nativa.
 */
object LocalImageServer {
    private const val TAG = "LocalImageServer"
    const val PORT = 8090
    private var serverSocket: ServerSocket? = null
    private var currentUri: Uri? = null
    private var currentFile: File? = null
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
                    val file = currentFile
                    val uri = currentUri
                    val mime = currentMime
                    val ctx = appContext
                    thread(isDaemon = true) {
                        LocalMediaStreamHandler.handleClient(client, file, uri, mime, ctx)
                    }
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
        val ctx = context ?: appContext

        if (ctx != null && !mime.startsWith("video/")) {
            try {
                val ext = "jpg"
                val cacheFile = File(ctx.cacheDir, "cast_media.$ext")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    cacheFile.outputStream().use { output -> input.copyTo(output) }
                }
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    currentFile = cacheFile
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Error en cache rápida de foto: ${e.message}")
            }
        } else {
            currentFile = null
        }

        val tvIp = ctx?.getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)?.getString("saved_tv_ip", null)
        val subnet = tvIp?.substringBeforeLast(".")?.let { "$it." }
        val ip = getLocalIpAddress(subnet)
        val ext = if (mime.startsWith("video/")) "mp4" else "jpg"
        return "http://$ip:$PORT/media_${System.currentTimeMillis()}.$ext"
    }

    fun serveImage(uri: Uri, context: Context? = null): String = serveMedia(uri, "image/jpeg", context)

    fun getLocalIpAddress(preferredSubnet: String? = null): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return "127.0.0.1"
            if (!preferredSubnet.isNullOrBlank()) {
                for (intf in interfaces) {
                    if (intf.isLoopback || !intf.isUp) continue
                    for (addr in intf.inetAddresses) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val ip = addr.hostAddress ?: continue
                            if (ip.startsWith(preferredSubnet)) return ip
                        }
                    }
                }
            }
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        if (ip.startsWith("192.168.")) return ip
                    }
                }
            }
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        if (!ip.startsWith("127.")) return ip
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
        currentFile = null
    }
}
