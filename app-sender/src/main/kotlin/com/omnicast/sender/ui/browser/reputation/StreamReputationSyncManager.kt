package com.omnicast.sender.ui.browser.reputation

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gestor de sincronización de reputación e inteligencia colectiva en la nube (HU-014).
 * Descarga y consolida periódicamente puntajes comunitarios desde Supabase sin afectar el rendimiento local.
 */
object StreamReputationSyncManager {
    private const val TAG = "StreamReputationSync"
    private const val PREFS_NAME = "omnicast_reputation_prefs"
    private const val KEY_LAST_SYNC = "last_stream_reputation_sync"
    private const val SYNC_INTERVAL_MS = 4 * 3600 * 1000L // 4 horas

    private const val SUPABASE_LOGS_URL = "https://pfhjrplnfuupftgzdnop.supabase.co/rest/v1/omnicast_system_logs"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBmaGpycGxuZnV1cGZ0Z3pkbm9wIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg5NTc4NDEsImV4cCI6MjEwNDUzMzg0MX0.u2ysI-hTvT7LczA7QL2YS2mudW8pSI644Ig8gyvTs7o"

    private val scope = CoroutineScope(Dispatchers.IO)
    @Volatile private var isSyncing = false

    fun initAndSync(context: Context) {
        val appContext = context.applicationContext
        StreamReputationDatabase.init(appContext)
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
        if (System.currentTimeMillis() - lastSync > SYNC_INTERVAL_MS) {
            syncFromCloud(appContext)
        }
    }

    fun syncFromCloud(context: Context) {
        if (isSyncing) return
        isSyncing = true
        scope.launch {
            try {
                val queryUrl = "$SUPABASE_LOGS_URL?select=message&event=eq.STREAM_REPUTATION&order=created_at.desc&limit=300"
                val conn = (URL(queryUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"; connectTimeout = 6000; readTimeout = 6000
                    setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                }
                if (conn.responseCode in 200..299) {
                    val raw = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    val array = JSONArray(raw)
                    val aggregated = mutableMapOf<String, Pair<Int, Int>>() // key -> (success, failure)

                    for (i in 0 until array.length()) {
                        val row = array.getJSONObject(i)
                        val msg = row.optString("message", "{}")
                        val json = try { JSONObject(msg) } catch (_: Throwable) { null } ?: continue
                        val site = StreamReputationDatabase.cleanDomain(json.optString("site", "*"))
                        val host = StreamReputationDatabase.cleanDomain(json.optString("host", ""))
                        val result = json.optString("result", "")
                        if (host.isNotBlank() && (result == "SUCCESS" || result == "FAILURE")) {
                            val key = "$site|$host"
                            val cur = aggregated[key] ?: (0 to 0)
                            if (result == "SUCCESS") aggregated[key] = (cur.first + 1) to cur.second
                            else aggregated[key] = cur.first to (cur.second + 1)
                        }
                    }

                    if (aggregated.isNotEmpty()) {
                        val items = aggregated.map { (key, counts) ->
                            val site = key.substringBefore("|")
                            val host = key.substringAfter("|")
                            val total = counts.first + counts.second
                            val score = if (total == 0) 50 else ((counts.first.toFloat() / total) * 100).toInt().coerceIn(0, 100)
                            StreamReputation(site, host, counts.first, counts.second, score, System.currentTimeMillis())
                        }
                        StreamReputationDatabase.get().saveBatchFromCloud(items)
                        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
                        Log.i(TAG, "🧠 Sincronizados ${items.size} puntajes comunitarios desde Supabase.")
                    }
                } else {
                    conn.disconnect()
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Sync reputacion comunitario diferido: ${e.message}")
            } finally {
                isSyncing = false
            }
        }
    }

    fun reportStreamResult(siteUrl: String?, streamUrl: String?, isSuccess: Boolean) {
        val site = StreamReputationDatabase.cleanDomain(siteUrl).ifBlank { "*" }
        val host = StreamReputationDatabase.cleanDomain(streamUrl)
        if (host.isBlank()) return

        // 1. Registro local inmediato en SQLite + memoria RAM
        if (isSuccess) StreamReputationDatabase.get().recordSuccess(site, host)
        else StreamReputationDatabase.get().recordFailure(site, host)

        // 2. Reporte asíncrono y silencioso a Supabase
        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("event", "STREAM_REPUTATION")
                    put("tag", "REPUTATION_TELEMETRY")
                    put("level", if (isSuccess) "INFO" else "WARN")
                    put("message", JSONObject().apply {
                        put("site", site)
                        put("host", host)
                        put("result", if (isSuccess) "SUCCESS" else "FAILURE")
                        put("timestamp", System.currentTimeMillis())
                    }.toString())
                }
                val conn = (URL(SUPABASE_LOGS_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; doOutput = true; connectTimeout = 4000; readTimeout = 4000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {}
        }
    }
}
