package com.omnicast.sender.ui.browser

import android.content.Context
import android.util.Log
import com.omnicast.sender.log.SenderLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Gestor dinámico de recetas de reproducción multimedia (Fase 1 de Auto-Recuperación Colectiva).
 * Descarga y sincroniza en caliente reglas remotas sin necesidad de recompilar la app.
 */
object StreamRuleSyncManager {

    private const val TAG = "StreamRuleSyncManager"
    private const val PREFS_NAME = "omnicast_stream_rules"
    private const val KEY_RULES_JSON = "cached_rules_json"
    private const val KEY_LAST_SYNC = "last_sync_timestamp"
    private const val SYNC_INTERVAL_MS = 6 * 3600 * 1000L // 6 horas

    private const val REMOTE_RULES_URL = "https://raw.githubusercontent.com/satacada/ProyectarAplicacion/main/rules/live_stream_rules.json"

    data class StreamRecipeRule(
        val cdnPattern: String,
        val embedOrigin: String,
        val referer: String,
        val skipAdsDuration: Float = 0f,
        val secFetchMode: String = "cors",
        val secFetchDest: String = "empty",
        val secFetchSite: String = "cross-site",
        val enabled: Boolean = true
    )

    private val cachedRules = CopyOnWriteArrayList<StreamRecipeRule>()
    @Volatile private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        val appContext = context.applicationContext
        loadRules(appContext)
    }

    private fun loadRules(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedJson = prefs.getString(KEY_RULES_JSON, null)

        val jsonStr = if (!savedJson.isNullOrBlank()) {
            savedJson
        } else {
            try {
                context.assets.open("rules/default_stream_rules.json").bufferedReader().use { it.readText() }
            } catch (e: Throwable) {
                Log.w(TAG, "No se encontró default_stream_rules.json en assets: ${e.message}")
                "{}"
            }
        }
        parseAndPopulate(jsonStr)
    }

    private fun parseAndPopulate(jsonStr: String) {
        try {
            val root = JSONObject(jsonStr)
            val arr = root.optJSONArray("rules") ?: return
            val list = mutableListOf<StreamRecipeRule>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.optBoolean("enabled", true)) {
                    list.add(
                        StreamRecipeRule(
                            cdnPattern = obj.getString("cdnPattern").lowercase(),
                            embedOrigin = obj.getString("embedOrigin"),
                            referer = obj.getString("referer"),
                            skipAdsDuration = obj.optDouble("skipAdsDuration", 0.0).toFloat(),
                            secFetchMode = obj.optString("secFetchMode", "cors"),
                            secFetchDest = obj.optString("secFetchDest", "empty"),
                            secFetchSite = obj.optString("secFetchSite", "cross-site"),
                            enabled = true
                        )
                    )
                }
            }
            cachedRules.clear()
            cachedRules.addAll(list)
            Log.i(TAG, "🧠 Recetas dinámicas de streaming cargadas: ${cachedRules.size} reglas activas.")
        } catch (e: Throwable) {
            Log.e(TAG, "Error parseando reglas de streaming: ${e.message}", e)
        }
    }

    suspend fun syncIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
        val now = System.currentTimeMillis()
        if (now - lastSync < SYNC_INTERVAL_MS && cachedRules.isNotEmpty()) return@withContext

        try {
            val conn = (URL(REMOTE_RULES_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
            }
            if (conn.responseCode in 200..299) {
                val remoteJson = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                parseAndPopulate(remoteJson)
                prefs.edit()
                    .putString(KEY_RULES_JSON, remoteJson)
                    .putLong(KEY_LAST_SYNC, now)
                    .apply()
                Log.i(TAG, "☁️ Reglas dinámicas actualizadas remotamente desde GitHub.")
            } else {
                conn.disconnect()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Sincronización remota diferida: ${e.message}")
        }
    }

    fun findMatchingRecipe(streamUrl: String): StreamRecipeRule? {
        val lower = streamUrl.lowercase()
        return cachedRules.firstOrNull { rule -> lower.contains(rule.cdnPattern) }
    }

    fun reportStreamFailure(pageUrl: String, streamUrl: String, httpCode: Int, reason: String) {
        val msg = "Fallo de streaming (HTTP $httpCode) | Causa: $reason | Flujo: $streamUrl | Página: $pageUrl"
        Log.w(TAG, "🚨 Sensor colectivo activado: $msg")
        SenderLogger.w("UNRESOLVED_STREAM", msg)
    }
}
