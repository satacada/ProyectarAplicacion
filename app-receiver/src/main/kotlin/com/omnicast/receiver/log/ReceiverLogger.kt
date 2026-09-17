package com.omnicast.receiver.log

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.net.ServerSocket
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

/**
 * Sistema de telemetría y diagnóstico en vivo para la TV Box.
 * 1. Mantiene buffer circular de logs en memoria.
 * 2. Guarda archivo en /sdcard/Download/omnicast_receiver.log para volcado a USB.
 * 3. Expone servidor HTTP en puerto 8087 (GET http://<tv_ip>:8087/logs) para consulta remota desde PC.
 */
object ReceiverLogger {
    private const val TAG = "ReceiverLogger"
    const val HTTP_PORT = 8087
    private val logBuffer = ConcurrentLinkedQueue<String>()
    private const val MAX_LOGS = 1000
    private var logFile: File? = null
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        if (isRunning) return
        isRunning = true
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            logFile = if (downloadDir.exists() || downloadDir.mkdirs()) {
                File(downloadDir, "omnicast_receiver.log")
            } else {
                File(context.filesDir, "omnicast_receiver.log")
            }
            log("INIT", "=== OmniCast TV Box Receiver v3.19 Iniciado ===")
        } catch (e: Throwable) {
            Log.e(TAG, "Error inicializando archivo de log", e)
        }
        startHttpServer()
    }

    fun log(tag: String, message: String, throwable: Throwable? = null) {
        val time = dateFormat.format(Date())
        val errStr = throwable?.let { "\n" + Log.getStackTraceString(it) } ?: ""
        val line = "[$time][$tag] $message$errStr"
        Log.i(tag, message, throwable)

        logBuffer.add(line)
        while (logBuffer.size > MAX_LOGS) { logBuffer.poll() }

        thread(isDaemon = true) {
            try {
                logFile?.appendText("$line\n")
                val url = java.net.URL("http://192.168.0.3:8000/api/tv_log")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 1000
                conn.readTimeout = 1000
                conn.doOutput = true
                conn.outputStream.use { it.write(line.toByteArray(Charsets.UTF_8)) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {}
        }
    }

    private fun startHttpServer() {
        thread(isDaemon = true, name = "ReceiverLogServer") {
            try {
                serverSocket = ServerSocket(HTTP_PORT).apply { reuseAddress = true }
                Log.i(TAG, "Servidor de logs activo en http://0.0.0.0:$HTTP_PORT/logs")
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    thread(isDaemon = true) {
                        try {
                            client.use { s ->
                                val reader = java.io.BufferedReader(java.io.InputStreamReader(s.getInputStream()))
                                val reqLine = reader.readLine()
                                var headerLine: String? = reqLine
                                while (!headerLine.isNullOrBlank()) {
                                    headerLine = reader.readLine()
                                }
                                val out = s.getOutputStream()
                                val logsText = logBuffer.joinToString("\n")
                                val bytes = logsText.toByteArray(Charsets.UTF_8)
                                val response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nAccess-Control-Allow-Origin: *\r\nConnection: close\r\n\r\n"
                                out.write(response.toByteArray())
                                out.write(bytes)
                                out.flush()
                            }
                        } catch (_: Throwable) {}
                    }
                }
            } catch (e: Throwable) {
                if (isRunning) Log.e(TAG, "Error en servidor HTTP de logs", e)
            }
        }
    }

    fun stop() {
        isRunning = false
        try { serverSocket?.close() } catch (_: Throwable) {}
        serverSocket = null
    }
}
