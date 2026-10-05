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

    private val scope = CoroutineScope(Dispatchers.IO)
    private val dynamicAdKeywords = mutableSetOf<String>()
    private val dynamicTelemetryKeywords = mutableSetOf<String>()

    @Volatile
    private var isSyncing = false

    fun getAdKeywords(): Set<String> = synchronized(dynamicAdKeywords) { dynamicAdKeywords.toSet() }
    fun getTelemetryKeywords(): Set<String> = synchronized(dynamicTelemetryKeywords) { dynamicTelemetryKeywords.toSet() }

    fun syncIfNeeded(context: Context) {
        val appContext = context.applicationContext
        loadLocalCustomKeywords(appContext)

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
                Log.i(TAG, "Iniciando sincronización de reglas vivas de EasyList...")
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
            } catch (e: Throwable) {
                Log.w(TAG, "No se pudo actualizar la lista viva remota (usando reglas locales/cacheadas): ${e.message}")
            } finally {
                isSyncing = false
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
