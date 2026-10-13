package com.omnicast.receiver.log

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread
import org.json.JSONObject

/**
 * Sistema de telemetría y diagnóstico para la TV Box (Caja Negra).
 * 1. Mantiene buffer circular de 1000 logs en memoria.
 * 2. Guarda archivo local en /sdcard/Download/omnicast_receiver.log.
 * 3. Expone servidor HTTP en puerto 8087 (GET http://<tv_ip>:8087/logs) para depuración en LAN.
 * 4. Despacha eventos clave y fallos a Supabase en la nube para soporte remoto a testers.
 */
object ReceiverLogger {
    private const val TAG = "ReceiverLogger"
    const val HTTP_PORT = 8087
    private const val SUPABASE_URL = "https://pfhjrplnfuupftgzdnop.supabase.co/rest/v1/omnicast_system_logs"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBmaGpycGxuZnV1cGZ0Z3pkbm9wIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg5NTc4NDEsImV4cCI6MjEwNDUzMzg0MX0.u2ysI-hTvT7LczA7QL2YS2mudW8pSI644Ig8gyvTs7o"

    private val logBuffer = ConcurrentLinkedQueue<String>()
    private const val MAX_LOGS = 1000
    private var logFile: File? = null
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private var appVersion: String = "v3.32.30"
    private var deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    private var androidVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    fun init(context: Context) {
        if (isRunning) return
        isRunning = true
        runCatching {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            appVersion = "v${pInfo.versionName ?: "3.32.28"}"
        }
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            logFile = if (downloadDir.exists() || downloadDir.mkdirs()) {
                File(downloadDir, "omnicast_receiver.log")
            } else {
                File(context.filesDir, "omnicast_receiver.log")
            }
            log("INIT", "=== OmniCast TV Box Receiver $appVersion Iniciado en $deviceModel ($androidVersion) ===")
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

        val isErrorOrWarning = throwable != null || message.contains("❌") || message.contains("Error", ignoreCase = true) || message.contains("⚠️") || message.contains("warning", ignoreCase = true)
        val isMilestone = tag == "INIT" || tag == "TV_RECEIVER" || tag == "ICE" || tag.contains("WebRtc", ignoreCase = true)

        thread(isDaemon = true) {
            runCatching { logFile?.appendText("$line\n") }
            if (isErrorOrWarning || isMilestone) {
                dispatchToSupabase(tag, message, errStr, if (isErrorOrWarning) "error" else "info")
            }
        }
    }

    private fun dispatchToSupabase(tag: String, message: String, errStr: String, level: String) {
        runCatching {
            val detailsObj = JSONObject().apply {
                if (errStr.isNotBlank()) put("stack_trace", errStr.take(1500))
            }
            val payload = JSONObject().apply {
                put("role", "tv")
                put("app_version", appVersion)
                put("device_model", deviceModel)
                put("android_version", androidVersion)
                put("level", level)
                put("event", tag)
                put("message", message.take(1000))
                put("details", detailsObj)
            }

            val conn = (URL(SUPABASE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3000
                readTimeout = 3000
                doOutput = true
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=minimal")
            }

            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            conn.responseCode
            conn.disconnect()
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
                                var headerLine: String? = reader.readLine()
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
