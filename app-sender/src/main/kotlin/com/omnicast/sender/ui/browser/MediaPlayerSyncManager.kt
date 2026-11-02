package com.omnicast.sender.ui.browser

import android.content.Context
import android.util.Log
import com.omnicast.sender.log.SenderLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestor dinámico modular para la sincronización remota y auto-aprendizaje
 * de dominios de reproductores multimedia (Embeds y CDNs de video).
 */
object MediaPlayerSyncManager {
    private const val TAG = "MediaPlayerSync"
    private const val PREFS_NAME = "omnicast_media_players_prefs"
    private const val KEY_LAST_SYNC = "last_sync_timestamp"
    private const val KEY_LAST_SYNC_STATUS = "last_sync_status"
    private const val KEY_LAST_SYNC_ERROR_TIME = "last_sync_error_time"
    private const val KEY_LAST_ERROR_DETAILS = "last_error_details"
    private const val SYNC_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 horas

    private const val REMOTE_PLAYERS_URL =
        "https://raw.githubusercontent.com/satacada/ProyectarAplicacion/main/app-sender/src/main/assets/adblock/known_media_players.json"

    private val scope = CoroutineScope(Dispatchers.IO)
    private val cachedPlayerHosts = ConcurrentHashMap.newKeySet<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile private var isInitialized = false
    @Volatile private var isSyncing = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        val appContext = context.applicationContext
        loadBaselineFromAssets(appContext)
        loadCachedFromLocalFile(appContext)
        syncIfNeeded(appContext)
    }

    fun isKnownPlayerHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val lower = host.lowercase().trim()
        if (cachedPlayerHosts.contains(lower)) return true
        for (known in cachedPlayerHosts) {
            if (lower.endsWith(".$known") || (known.length > 5 && lower.contains(known.substringBefore(".")))) {
                return true
            }
        }
        return lower.contains("embed") || lower.contains("player") || lower.contains("stream") || lower.contains("cdn")
    }

    fun learnAndSavePlayer(host: String?, context: Context? = null) {
        if (host.isNullOrBlank() || host.length < 4 || !host.contains(".")) return
        val cleanHost = host.lowercase().trim()
        if (cachedPlayerHosts.add(cleanHost)) {
            Log.i(TAG, "🧠 [Auto-Aprendizaje] Nuevo reproductor aprendido: $cleanHost")
            SenderLogger.i("PLAYER_LEARNED", "Nuevo reproductor registrado en caliente: $cleanHost")
            context?.let { ctx ->
                scope.launch { persistLocalFile(ctx.applicationContext) }
            }
        }
    }

    fun syncIfNeeded(context: Context, force: Boolean = false) {
        if (isSyncing) return
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
        val now = System.currentTimeMillis()

        if (!force && (now - lastSync < SYNC_INTERVAL_MS)) return

        isSyncing = true
        scope.launch {
            try {
                Log.i(TAG, "Iniciando descarga de actualización de reproductores desde GitHub...")
                val conn = (URL(REMOTE_PLAYERS_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    useCaches = false
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    val count = parseAndApplyJson(body)
                    persistLocalFile(appContext)
                    prefs.edit()
                        .putLong(KEY_LAST_SYNC, now)
                        .putString(KEY_LAST_SYNC_STATUS, "SUCCESS (HTTP $code - $count hosts)")
                        .remove(KEY_LAST_ERROR_DETAILS)
                        .apply()
                    Log.i(TAG, "✅ Actualización de reproductores exitosa ($count hosts sincronizados).")
                    SenderLogger.i("PLAYER_SYNC_SUCCESS", "Actualización completada exitosamente: $count hosts cargados.")
                } else {
                    conn.disconnect()
                    val errMsg = "HTTP $code al consultar $REMOTE_PLAYERS_URL"
                    recordSyncError(prefs, errMsg, null)
                }
            } catch (t: Throwable) {
                recordSyncError(prefs, t.message ?: "Excepción de red desconocida", t)
            } finally {
                isSyncing = false
            }
        }
    }

    private fun recordSyncError(prefs: android.content.SharedPreferences, reason: String, throwable: Throwable?) {
        val timeStr = dateFormat.format(Date())
        val details = throwable?.let { Log.getStackTraceString(it) } ?: reason
        prefs.edit()
            .putString(KEY_LAST_SYNC_STATUS, "FAILED: $reason")
            .putString(KEY_LAST_SYNC_ERROR_TIME, timeStr)
            .putString(KEY_LAST_ERROR_DETAILS, details)
            .apply()

        Log.e(TAG, "❌ [PLAYER_SYNC_ERROR] $timeStr - Fallo al descargar base actualizada: $reason")
        SenderLogger.e("PLAYER_SYNC_ERROR", "Fallo al descargar base actualizada de reproductores: $reason", throwable)
    }

    private fun loadBaselineFromAssets(context: Context) {
        try {
            context.assets.open("adblock/known_media_players.json").use { stream ->
                val json = stream.bufferedReader().use { it.readText() }
                parseAndApplyJson(json)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo leer semilla de assets: ${t.message}")
        }
    }

    private fun loadCachedFromLocalFile(context: Context) {
        try {
            val file = File(context.filesDir, "adblock/known_media_players.json")
            if (file.exists() && file.length() > 0) {
                val json = file.readText(Charsets.UTF_8)
                parseAndApplyJson(json)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo leer cache local de reproductores: ${t.message}")
        }
    }

    private fun parseAndApplyJson(jsonStr: String): Int {
        val root = JSONObject(jsonStr)
        val arr = root.optJSONArray("media_players") ?: return 0
        var added = 0
        for (i in 0 until arr.length()) {
            val host = arr.optString(i).trim().lowercase()
            if (host.isNotBlank() && cachedPlayerHosts.add(host)) {
                added++
            }
        }
        return cachedPlayerHosts.size
    }

    private fun persistLocalFile(context: Context) {
        try {
            val dir = File(context.filesDir, "adblock")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "known_media_players.json")
            val root = JSONObject()
            root.put("version", 1)
            root.put("updated_at", dateFormat.format(Date()))
            val arr = org.json.JSONArray()
            for (h in cachedPlayerHosts) arr.put(h)
            root.put("media_players", arr)
            file.writeText(root.toString(2), Charsets.UTF_8)
        } catch (t: Throwable) {
            Log.w(TAG, "Error persistiendo cache local de reproductores: ${t.message}")
        }
    }
}
