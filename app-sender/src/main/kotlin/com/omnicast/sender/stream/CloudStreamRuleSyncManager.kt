package com.omnicast.sender.stream

import android.content.Context
import android.util.Log
import com.omnicast.core.domain.config.SupabaseCloudConfig
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

    private val scope = CoroutineScope(Dispatchers.IO)
    @Volatile private var isSyncing = false

    fun initAndSync(context: Context) {
        val appContext = context.applicationContext
        StreamProfileDatabase.init(appContext)
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
        if (System.currentTimeMillis() - lastSync > SYNC_INTERVAL_MS) syncFromCloud(appContext)
    }

    fun syncFromCloud(context: Context) {
        if (isSyncing) return
        isSyncing = true
        scope.launch {
            try {
                var loaded = parseRulesEndpoint("${SupabaseCloudConfig.RULES_URL}?select=host,branch,bypass_proxy,headers&order=last_verified_at.desc&limit=100")
                if (!loaded) loaded = parseLogsEndpoint("${SupabaseCloudConfig.LOGS_URL}?tag=eq.STREAM_PROFILE_VERIFIED&order=created_at.desc&limit=50")
                if (loaded) {
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Sync comunitario streaming: ${e.message}")
            } finally { isSyncing = false }
        }
    }

    private fun parseRulesEndpoint(urlStr: String): Boolean {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 8000; readTimeout = 8000
            setRequestProperty("apikey", SupabaseCloudConfig.ANON_KEY); setRequestProperty("Authorization", "Bearer ${SupabaseCloudConfig.ANON_KEY}")
        }
        if (conn.responseCode !in 200..299) { conn.disconnect(); return false }
        val array = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
        val db = StreamProfileDatabase.get()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val host = item.optString("host", "").trim().lowercase()
            val branchStr = item.optString("branch", "VOD_HLS_MP4")
            val bypass = item.optBoolean("bypass_proxy", true)
            val hdrsMap = mutableMapOf<String, String>()
            val hdrsObj = item.optJSONObject("headers")
            if (hdrsObj != null) {
                val keys = hdrsObj.keys()
                while (keys.hasNext()) { val k = keys.next(); hdrsMap[k] = hdrsObj.optString(k, "") }
            }
            if (host.isNotBlank()) {
                val branch = MediaBranch.entries.firstOrNull { it.name == branchStr } ?: MediaBranch.VOD_HLS_MP4
                db.recordSuccess(host, hdrsMap, bypass, branch)
            }
        }
        conn.disconnect()
        Log.i(TAG, "Sincronizadas ${array.length()} reglas desde omnicast_stream_rules")
        return true
    }

    private fun parseLogsEndpoint(urlStr: String): Boolean {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 8000; readTimeout = 8000
            setRequestProperty("apikey", SupabaseCloudConfig.ANON_KEY); setRequestProperty("Authorization", "Bearer ${SupabaseCloudConfig.ANON_KEY}")
        }
        if (conn.responseCode !in 200..299) { conn.disconnect(); return false }
        val array = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
        val db = StreamProfileDatabase.get()
        for (i in 0 until array.length()) {
            val meta = JSONObject(array.getJSONObject(i).optString("metadata", "{}"))
            val host = meta.optString("host", "").trim().lowercase()
            val branchStr = meta.optString("branch", "VOD_HLS_MP4")
            val bypass = meta.optBoolean("bypass_proxy", true)
            val hdrsMap = mutableMapOf<String, String>()
            val hdrsObj = meta.optJSONObject("headers")
            if (hdrsObj != null) {
                val keys = hdrsObj.keys()
                while (keys.hasNext()) { val k = keys.next(); hdrsMap[k] = hdrsObj.optString(k, "") }
            }
            if (host.isNotBlank()) {
                val branch = MediaBranch.entries.firstOrNull { it.name == branchStr } ?: MediaBranch.VOD_HLS_MP4
                db.recordSuccess(host, hdrsMap, bypass, branch)
            }
        }
        conn.disconnect()
        return true
    }

    fun reportVerifiedSuccess(context: Context, host: String, branch: MediaBranch, bypassProxy: Boolean) {
        if (host.isBlank()) return
        scope.launch {
            try {
                val rulePayload = JSONObject().apply {
                    put("host", host.trim().lowercase())
                    put("branch", branch.name)
                    put("bypass_proxy", bypassProxy)
                    put("last_verified_at", "NOW()")
                }
                val conn = (URL(SupabaseCloudConfig.RULES_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 6000; readTimeout = 6000; doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", SupabaseCloudConfig.ANON_KEY); setRequestProperty("Authorization", "Bearer ${SupabaseCloudConfig.ANON_KEY}")
                    setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
                }
                conn.outputStream.use { it.write(rulePayload.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode; conn.disconnect()
                if (code in 200..299) {
                    Log.i(TAG, "Regla de streaming registrada en omnicast_stream_rules: $host")
                } else fallbackReportLogs(host, branch, bypassProxy)
            } catch (_: Throwable) { fallbackReportLogs(host, branch, bypassProxy) }
        }
    }

    private fun fallbackReportLogs(host: String, branch: MediaBranch, bypassProxy: Boolean) {
        try {
            val payload = JSONObject().apply {
                put("tag", "STREAM_PROFILE_VERIFIED"); put("level", "INFO"); put("message", "Perfil: $host")
                put("metadata", JSONObject().apply { put("host", host); put("branch", branch.name); put("bypass_proxy", bypassProxy) }.toString())
            }
            val conn = (URL(SupabaseCloudConfig.LOGS_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 5000; readTimeout = 5000; doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", SupabaseCloudConfig.ANON_KEY); setRequestProperty("Authorization", "Bearer ${SupabaseCloudConfig.ANON_KEY}")
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            conn.responseCode; conn.disconnect()
        } catch (_: Throwable) {}
    }
}
