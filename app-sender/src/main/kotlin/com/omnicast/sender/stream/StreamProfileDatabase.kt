package com.omnicast.sender.stream

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.omnicast.core.domain.models.MediaBranch
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Entrada de perfil exitoso de streaming aprendido en SQLite y nube.
 */
data class StreamProfileEntry(
    val host: String,
    val headers: Map<String, String>,
    val bypassProxy: Boolean,
    val branch: MediaBranch,
    val successCount: Int = 1,
    val lastSuccessTs: Long = System.currentTimeMillis()
)

/**
 * Base de datos SQLite local de refuerzo e inteligencia colectiva (ADR-006 / HU-009).
 * Preserva las cabeceras ganadoras y políticas de red por servidor edge.
 */
class StreamProfileDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val TAG = "StreamProfileDb"
        private const val DB_NAME = "omnicast_stream_profiles.db"
        private const val DB_VERSION = 1

        private const val TABLE = "stream_profiles"
        private const val COL_HOST = "host"
        private const val COL_HEADERS = "headers_json"
        private const val COL_BYPASS_PROXY = "bypass_proxy"
        private const val COL_BRANCH = "branch"
        private const val COL_SUCCESS_COUNT = "success_count"
        private const val COL_LAST_SUCCESS = "last_success_ts"

        @Volatile
        private var instance: StreamProfileDatabase? = null
        private val cache = ConcurrentHashMap<String, StreamProfileEntry>()

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        val db = StreamProfileDatabase(context)
                        instance = db
                        db.loadCache()
                    }
                }
            }
        }

        fun get(): StreamProfileDatabase =
            instance ?: throw IllegalStateException("StreamProfileDatabase no inicializado")

        fun extractHost(url: String): String {
            return try {
                val uri = URI(url)
                val h = uri.host?.lowercase() ?: ""
                if (h.startsWith("www.")) h.substring(4) else h
            } catch (_: Throwable) { "" }
        }

        fun getProfileForHost(host: String): StreamProfileEntry? {
            if (host.isBlank()) return null
            val direct = cache[host.lowercase()]
            if (direct != null) return direct
            val cleanHost = host.lowercase()
            for ((key, profile) in cache) {
                if (cleanHost == key || cleanHost.endsWith(".$key")) return profile
            }
            return null
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE (
                $COL_HOST TEXT PRIMARY KEY,
                $COL_HEADERS TEXT NOT NULL,
                $COL_BYPASS_PROXY INTEGER NOT NULL DEFAULT 1,
                $COL_BRANCH TEXT NOT NULL DEFAULT 'VOD_HLS_MP4',
                $COL_SUCCESS_COUNT INTEGER NOT NULL DEFAULT 1,
                $COL_LAST_SUCCESS INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    private fun loadCache() {
        try {
            val db = readableDatabase
            val cursor = db.query(TABLE, null, null, null, null, null, null)
            cursor.use { c ->
                while (c.moveToNext()) {
                    val host = c.getString(c.getColumnIndexOrThrow(COL_HOST))
                    val hdrsJson = c.getString(c.getColumnIndexOrThrow(COL_HEADERS))
                    val bypass = c.getInt(c.getColumnIndexOrThrow(COL_BYPASS_PROXY)) == 1
                    val brStr = c.getString(c.getColumnIndexOrThrow(COL_BRANCH))
                    val count = c.getInt(c.getColumnIndexOrThrow(COL_SUCCESS_COUNT))
                    val ts = c.getLong(c.getColumnIndexOrThrow(COL_LAST_SUCCESS))

                    val headersMap = parseHeaders(hdrsJson)
                    val branch = MediaBranch.entries.firstOrNull { it.name == brStr } ?: MediaBranch.VOD_HLS_MP4
                    cache[host] = StreamProfileEntry(host, headersMap, bypass, branch, count, ts)
                }
            }
            Log.i(TAG, "Caché de perfiles de servidores cargada (${cache.size} hosts)")
        } catch (e: Throwable) {
            Log.e(TAG, "Error al cargar caché de perfiles: ${e.message}", e)
        }
    }

    fun recordSuccess(host: String, headers: Map<String, String>, bypassProxy: Boolean, branch: MediaBranch) {
        if (host.isBlank()) return
        val cleanHost = host.lowercase()
        val existing = cache[cleanHost]
        val newCount = (existing?.successCount ?: 0) + 1
        val now = System.currentTimeMillis()

        val safeHeaders = headers.filterKeys {
            it.equals("Referer", true) || it.equals("Origin", true) || it.equals("User-Agent", true) || it.equals("Authorization", true)
        }

        val entry = StreamProfileEntry(cleanHost, safeHeaders, bypassProxy, branch, newCount, now)
        cache[cleanHost] = entry

        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_HOST, cleanHost)
                put(COL_HEADERS, serializeHeaders(safeHeaders))
                put(COL_BYPASS_PROXY, if (bypassProxy) 1 else 0)
                put(COL_BRANCH, branch.name)
                put(COL_SUCCESS_COUNT, newCount)
                put(COL_LAST_SUCCESS, now)
            }
            db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            Log.i(TAG, "✅ Perfil exitoso guardado en SQLite para host: $cleanHost (branch=${branch.name}, count=$newCount)")
        } catch (e: Throwable) {
            Log.e(TAG, "Error al persistir perfil de streaming: ${e.message}", e)
        }
    }

    private fun serializeHeaders(headers: Map<String, String>): String {
        val json = JSONObject()
        headers.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }

    private fun parseHeaders(jsonStr: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.optString(key)
            }
        } catch (_: Throwable) {}
        return map
    }
}
