package com.omnicast.sender.stream

import android.content.Context
import android.util.Log
import com.omnicast.core.domain.models.MediaBranch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gestor de sincronización de reglas e inteligencia colectiva en la nube (ADR-006 / HU-009).
 * Sincroniza stream_rules.json y reporta servidores ganadores anónimamente.
 */
object CloudStreamRuleSyncManager {
    private const val TAG = "CloudStreamSync"
    private const val PREFS_NAME = "omnicast_stream_rules_prefs"
    private const val KEY_LAST_SYNC = "last_stream_rules_sync"
    private const val SYNC_INTERVAL_MS = 3 * 3600 * 1000L // 3 horas

    private const val SUPABASE_LOGS_URL = "https://pfhjrplnfuupftgzdnop.supabase.co/rest/v1/omnicast_system_logs"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBmaGpycGxuZnV1cGZ0Z3pkbm9wIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg5NTc4NDEsImV4cCI6MjEwNDUzMzg0MX0.u2ysI-hTvT7LczA7QL2YS2mudW8pSI644Ig8gyvTs7o"

    private val scope = CoroutineScope(Dispatchers.IO)
    @Volatile private var isSyncing = false

    fun initAndSync(context: Context) {
        val appContext = context.applicationContext
        StreamProfileDatabase.init(appContext)

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
        val now = System.currentTimeMillis()

        if (now - lastSync > SYNC_INTERVAL_MS) {
            syncFromCloud(appContext)
        }
    }

    fun syncFromCloud(context: Context) {
        if (isSyncing) return
        isSyncing = true
        scope.launch {
            try {
                val urlStr = "$SUPABASE_LOGS_URL?tag=eq.STREAM_PROFILE_VERIFIED&order=created_at.desc&limit=50"
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                conn.setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")

                if (conn.responseCode in 200..299) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val array = JSONArray(response)
                    val db = StreamProfileDatabase.get()
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val metaStr = item.optString("metadata", "{}")
                        val meta = JSONObject(metaStr)
                        val host = meta.optString("host", "").trim().lowercase()
                        val branchStr = meta.optString("branch", "VOD_HLS_MP4")
                        val bypass = meta.optBoolean("bypass_proxy", true)
                        if (host.isNotBlank()) {
                            val branch = MediaBranch.entries.firstOrNull { it.name == branchStr } ?: MediaBranch.VOD_HLS_MP4
                            db.recordSuccess(host, emptyMap(), bypass, branch)
                        }
                    }
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
                    Log.i(TAG, "Sincronización comunitaria completada: ${array.length()} perfiles de streaming descargados")
                }
                conn.disconnect()
            } catch (e: Throwable) {
                Log.w(TAG, "Fallo menor en sync comunitario de streaming (continuando local): ${e.message}")
            } finally {
                isSyncing = false
            }
        }
    }

    fun reportVerifiedSuccess(context: Context, host: String, branch: MediaBranch, bypassProxy: Boolean) {
        if (host.isBlank()) return
        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("tag", "STREAM_PROFILE_VERIFIED")
                    put("level", "INFO")
                    put("message", "Perfil de servidor verificado: $host")
                    val meta = JSONObject().apply {
                        put("host", host)
                        put("branch", branch.name)
                        put("bypass_proxy", bypassProxy)
                    }
                    put("metadata", meta.toString())
                }

                val conn = URL(SUPABASE_LOGS_URL).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY)
                conn.setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                conn.setRequestProperty("Prefer", "return=minimal")

                conn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(Charsets.UTF_8))
                }
                val code = conn.responseCode
                conn.disconnect()
                if (code in 200..299) {
                    Log.i(TAG, "🚀 Perfil de servidor compartido anónimamente con éxito: $host ($branch)")
                }
            } catch (e: Throwable) {
                Log.w(TAG, "No se pudo reportar perfil a la nube: ${e.message}")
            }
        }
    }
}
