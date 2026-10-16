package com.omnicast.sender.ui.browser

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gestor dinámico modular para la sincronización de reglas de bloqueo vivas
 * (EasyList y palabras clave de telemetría/anuncios) sin necesidad de recompilar la app.
 */
object AdBlockRuleSyncManager {
    private const val TAG = "AdBlockRuleSync"
    private const val PREFS_NAME = "omnicast_adblock_sync"
    private const val KEY_LAST_SYNC = "last_sync_timestamp"
    private const val SYNC_INTERVAL_MS = 3 * 24 * 60 * 60 * 1000L // 3 días

    // Fuentes abiertas oficiales y espejos de alta disponibilidad
    private const val EASYLIST_PRIMARY_URL =
        "https://raw.githubusercontent.com/easylist/easylist/master/easylist/easylist_general_block.txt"
    private const val EASYLIST_FALLBACK_URL =
        "https://easylist.to/easylist/easylist.txt"
    private const val DYNAMIC_RULES_URL =
        "https://raw.githubusercontent.com/satacada/ProyectarAplicacion/main/app-sender/src/main/assets/adblock/default_ad_rules.json"

    private const val SUPABASE_LOGS_URL = "https://pfhjrplnfuupftgzdnop.supabase.co/rest/v1/omnicast_system_logs"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBmaGpycGxuZnV1cGZ0Z3pkbm9wIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg5NTc4NDEsImV4cCI6MjEwNDUzMzg0MX0.u2ysI-hTvT7LczA7QL2YS2mudW8pSI644Ig8gyvTs7o"
    private const val KEY_LAST_CROWD_SYNC = "last_crowd_sync_timestamp"
    private const val CROWD_SYNC_INTERVAL_MS = 2 * 60 * 60 * 1000L // 2 horas

    private val scope = CoroutineScope(Dispatchers.IO)
    private val dynamicAdKeywords = mutableSetOf<String>()
    private val dynamicTelemetryKeywords = mutableSetOf<String>()

    @Volatile
    private var isSyncing = false

    fun getAdKeywords(): Set<String> = synchronized(dynamicAdKeywords) { dynamicAdKeywords.toSet() }
    fun getTelemetryKeywords(): Set<String> = synchronized(dynamicTelemetryKeywords) { dynamicTelemetryKeywords.toSet() }

    fun syncIfNeeded(context: Context) {
        val appContext = context.applicationContext
        AdBlockDatabase.init(appContext)
        loadLocalCustomKeywords(appContext)
        syncCrowdLearnedDomains(appContext)

        if (isSyncing) return
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
        val now = System.currentTimeMillis()

        val rulesFile = File(appContext.filesDir, "adblock/easylist.txt")
        val needsSync = (now - lastSync > SYNC_INTERVAL_MS) || !rulesFile.exists() || rulesFile.length() == 0L

        if (!needsSync) return

        isSyncing = true
        scope.launch {
            try {
                Log.i(TAG, "Iniciando sincronización de reglas vivas...")
                val downloaded = downloadRuleFile(EASYLIST_PRIMARY_URL) ?: downloadRuleFile(EASYLIST_FALLBACK_URL)
                if (downloaded != null && downloaded.isNotEmpty()) {
                    val adblockDir = File(appContext.filesDir, "adblock")
                    if (!adblockDir.exists()) adblockDir.mkdirs()

                    val tempFile = File(adblockDir, "easylist.tmp")
                    tempFile.writeBytes(downloaded)
                    if (tempFile.renameTo(rulesFile) || (rulesFile.delete() && tempFile.renameTo(rulesFile))) {
                        prefs.edit().putLong(KEY_LAST_SYNC, now).apply()
                        Log.i(TAG, "Lista viva EasyList actualizada con éxito (${downloaded.size} bytes). Recargando motor C++...")
                        NativeAdBlockEngine.reload(appContext)
                    }
                }
                val dynamicBytes = downloadRuleFile(DYNAMIC_RULES_URL)
                if (dynamicBytes != null && dynamicBytes.isNotEmpty()) {
                    val root = org.json.JSONObject(String(dynamicBytes, Charsets.UTF_8))
                    val dList = mutableListOf<String>()
                    val kList = mutableListOf<String>()
                    root.optJSONArray("domains")?.let { arr ->
                        for (i in 0 until arr.length()) dList.add(arr.optString(i))
                    }
                    root.optJSONArray("keywords")?.let { arr ->
                        for (j in 0 until arr.length()) kList.add(arr.optString(j))
                    }
                    AdBlockDatabase.get().bulkInsert(dList, kList)
                    Log.i(TAG, "Sincronizados ${dList.size} dominios y ${kList.size} palabras clave en SQLite AdBlock.")
                }
            } catch (e: Throwable) {
                Log.w(TAG, "No se pudo actualizar la lista viva remota (usando reglas locales/cacheadas): ${e.message}")
            } finally {
                isSyncing = false
            }
        }
    }

    fun reportLearnedDomainToCloud(domain: String, sourceHost: String?) {
        if (domain.isBlank() || domain.length < 4 || !domain.contains(".")) return
        kotlin.concurrent.thread(isDaemon = true, name = "ReportAdCloud") {
            try {
                val details = org.json.JSONObject().apply {
                    put("domain", domain)
                    if (!sourceHost.isNullOrBlank()) put("source_host", sourceHost)
                }
                val payload = org.json.JSONObject().apply {
                    put("role", "phone")
                    put("level", "info")
                    put("event", "ADBLOCK_LEARNED_DOMAIN")
                    put("message", domain)
                    put("details", details)
                }
                val conn = (URL(SUPABASE_LOGS_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 4000
                    readTimeout = 4000
                    doOutput = true
                    setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Prefer", "return=minimal")
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                conn.disconnect()
                if (code in 200..299) {
                    Log.i(TAG, "☁️ Dominio publicitario '$domain' reportado a Supabase para inmunidad colectiva.")
                }
            } catch (e: Throwable) {
                Log.w(TAG, "No se pudo reportar dominio a Supabase: ${e.message}")
            }
        }
    }

    fun syncCrowdLearnedDomains(context: Context) {
        scope.launch {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val lastCrowdSync = prefs.getLong(KEY_LAST_CROWD_SYNC, 0L)
                val now = System.currentTimeMillis()
                if (now - lastCrowdSync < CROWD_SYNC_INTERVAL_MS) return@launch

                val queryUrl = "$SUPABASE_LOGS_URL?select=message&event=eq.ADBLOCK_LEARNED_DOMAIN&order=created_at.desc&limit=500"
                val conn = (URL(queryUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 6000
                    setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                }
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    val arr = org.json.JSONArray(body)
                    var count = 0
                    for (i in 0 until arr.length()) {
                        val d = arr.optJSONObject(i)?.optString("message")?.trim()?.lowercase() ?: continue
                        if (d.length >= 4 && d.contains(".") && !AdBlockDatabase.isAdDomain(d)) {
                            if (AdBlockDatabase.get().addDomain(d, "crowd_learned")) {
                                count++
                            }
                        }
                    }
                    prefs.edit().putLong(KEY_LAST_CROWD_SYNC, now).apply()
                    if (count > 0) {
                        Log.i(TAG, "🧠 [Crowd-AdBlock] Sincronizados $count dominios publicitarios colectivos desde Supabase.")
                    }
                } else {
                    conn.disconnect()
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Error sincronizando dominios colectivos de Supabase: ${e.message}")
            }
        }
    }

    private fun loadLocalCustomKeywords(context: Context) {
        try {
            val kwFile = File(context.filesDir, "adblock/dynamic_keywords.txt")
            if (kwFile.exists() && kwFile.length() > 0) {
                val lines = kwFile.readLines().map { it.trim().lowercase() }.filter { it.isNotBlank() && !it.startsWith("#") }
                synchronized(dynamicAdKeywords) {
                    dynamicAdKeywords.addAll(lines)
                }
            }
        } catch (_: Throwable) {}
    }

    private fun downloadRuleFile(urlString: String): ByteArray? {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.use { it.readBytes() }
            } else {
                null
            }
        } catch (e: Throwable) {
            null
        }
    }
}
