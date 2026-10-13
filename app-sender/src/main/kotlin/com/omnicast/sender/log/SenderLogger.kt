package com.omnicast.sender.log

import android.content.Context
import android.os.Build
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread
import org.json.JSONObject

/**
 * Telemetría remota y caja negra para el Emisor (Celular Android).
 * Reporta eventos críticos a Supabase para soporte remoto de amigos/testers.
 */
object SenderLogger {
    private const val TAG = "SenderLogger"
    private const val SUPABASE_URL = "https://pfhjrplnfuupftgzdnop.supabase.co/rest/v1/omnicast_system_logs"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBmaGpycGxuZnV1cGZ0Z3pkbm9wIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg5NTc4NDEsImV4cCI6MjEwNDUzMzg0MX0.u2ysI-hTvT7LczA7QL2YS2mudW8pSI644Ig8gyvTs7o"

    private val logBuffer = ConcurrentLinkedQueue<String>()
    private const val MAX_LOGS = 500
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private var appVersion: String = "v3.32.30"
    private var deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    private var androidVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        runCatching {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            appVersion = "v${pInfo.versionName ?: "3.32.28"}"
        }
        i("SENDER_INIT", "🚀 OmniCast Sender $appVersion iniciado en $deviceModel ($androidVersion)")
    }

    fun i(tag: String, message: String) = log("INFO", tag, message, null)
    fun w(tag: String, message: String, throwable: Throwable? = null) = log("WARN", tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log("ERROR", tag, message, throwable)

    fun log(level: String, tag: String, message: String, throwable: Throwable? = null) {
        val time = dateFormat.format(Date())
        val errStr = throwable?.let { "\n" + Log.getStackTraceString(it) } ?: ""
        val line = "[$time][$level][$tag] $message$errStr"

        when (level) {
            "ERROR" -> Log.e(tag, message, throwable)
            "WARN" -> Log.w(tag, message, throwable)
            else -> Log.i(tag, message, throwable)
        }

        logBuffer.add(line)
        while (logBuffer.size > MAX_LOGS) { logBuffer.poll() }

        // Enviar a Supabase solo eventos clave (inicio de sesion o fallos warn/error)
        val shouldDispatchCloud = level == "ERROR" || level == "WARN" || tag == "SENDER_INIT" || tag == "PROJECTION_START"
        if (shouldDispatchCloud) {
            dispatchToCloud(level, tag, message, errStr)
        }
    }

    private fun dispatchToCloud(level: String, tag: String, message: String, errStr: String) {
        thread(isDaemon = true, name = "SenderCloudLog") {
            runCatching {
                val detailsObj = JSONObject().apply {
                    if (errStr.isNotBlank()) put("stack_trace", errStr.take(1500))
                }
                val payload = JSONObject().apply {
                    put("role", "phone")
                    put("app_version", appVersion)
                    put("device_model", deviceModel)
                    put("android_version", androidVersion)
                    put("level", level.lowercase(Locale.US))
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
    }
}
