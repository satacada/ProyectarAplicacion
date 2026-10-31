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
        private const val DB_VERSION = 2

        private const val TABLE = "stream_profiles"
        private const val TABLE_SIGS = "stream_format_signatures"
        private const val COL_SIG = "signature"; private const val COL_TYPE = "rule_type"
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
            val cleanHost = host.lowercase()
            cache[cleanHost]?.let { return it }
            for ((key, profile) in cache) {
                if (cleanHost == key || cleanHost.endsWith(".$key")) return profile
            }
            return null
        }

        fun resolveTargetPlayer(url: String, headers: Map<String, String> = emptyMap()): String {
            headers["X-Target-Player"]?.takeIf { it.isNotBlank() }?.let { return it }
            val streamProfile = getProfileForHost(extractHost(url))
                ?: getProfileForHost(extractHost(headers["Referer"] ?: ""))
            streamProfile?.headers?.get("X-Target-Player")?.takeIf { it.isNotBlank() }?.let { return it }
            return if (com.omnicast.core.domain.usecases.StreamManifestProbe.hasSignedSecurityToken(url)) "WEBVIEW" else "EXOPLAYER"
        }

        fun isSingleUseTokenHost(url: String, headers: Map<String, String> = emptyMap()): Boolean {
            if (headers["X-Single-Use-Token"] == "true") return true
            val streamProfile = getProfileForHost(extractHost(url))
                ?: getProfileForHost(extractHost(headers["Referer"] ?: ""))
            if (streamProfile?.headers?.get("X-Single-Use-Token") != null) {
                return streamProfile.headers["X-Single-Use-Token"] == "true"
            }
            return com.omnicast.core.domain.usecases.StreamManifestProbe.hasSignedSecurityToken(url)
        }

        fun requiresCredentials(url: String, headers: Map<String, String> = emptyMap()): Boolean {
            if (headers["X-With-Credentials"] == "true") return true
            if (headers["X-With-Credentials"] == "false") return false
            if (com.omnicast.core.domain.usecases.StreamManifestProbe.hasSignedSecurityToken(url)) return false
            val streamProfile = getProfileForHost(extractHost(url))
                ?: getProfileForHost(extractHost(headers["Referer"] ?: ""))
            return streamProfile?.headers?.get("X-With-Credentials") == "true"
        }

        fun recordFailure(host: String) {
            runCatching { get().recordFailure(host) }
        }

        fun learnSignature(sig: String, type: String) {
            runCatching { get().learnSignature(sig, type) }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE ($COL_HOST TEXT PRIMARY KEY, $COL_HEADERS TEXT NOT NULL, $COL_BYPASS_PROXY INTEGER NOT NULL DEFAULT 1, $COL_BRANCH TEXT NOT NULL DEFAULT 'VOD_HLS_MP4', $COL_SUCCESS_COUNT INTEGER NOT NULL DEFAULT 1, $COL_LAST_SUCCESS INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_SIGS ($COL_SIG TEXT PRIMARY KEY, $COL_TYPE TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_SIGS ($COL_SIG TEXT PRIMARY KEY, $COL_TYPE TEXT NOT NULL)")
        else { db.execSQL("DROP TABLE IF EXISTS $TABLE"); db.execSQL("DROP TABLE IF EXISTS $TABLE_SIGS"); onCreate(db) }
    }

    private fun loadCache() {
        try {
            val db = readableDatabase
            db.query(TABLE, null, null, null, null, null, null).use { c ->
                while (c.moveToNext()) {
                    val host = c.getString(c.getColumnIndexOrThrow(COL_HOST)); val hdrsJson = c.getString(c.getColumnIndexOrThrow(COL_HEADERS))
                    val bypass = c.getInt(c.getColumnIndexOrThrow(COL_BYPASS_PROXY)) == 1; val brStr = c.getString(c.getColumnIndexOrThrow(COL_BRANCH))
                    val count = c.getInt(c.getColumnIndexOrThrow(COL_SUCCESS_COUNT)); val ts = c.getLong(c.getColumnIndexOrThrow(COL_LAST_SUCCESS))
                    cache[host] = StreamProfileEntry(host, parseHeaders(hdrsJson), bypass, MediaBranch.entries.firstOrNull { it.name == brStr } ?: MediaBranch.VOD_HLS_MP4, count, ts)
                }
            }
            seedInitialProfiles(); loadSignatures(db)
            Log.i(TAG, "Caché de perfiles (${cache.size} hosts) y firmas RFC 8216 sincronizadas")
        } catch (e: Throwable) { Log.e(TAG, "Error al cargar caché de perfiles: ${e.message}", e) }
    }

    private fun loadSignatures(db: SQLiteDatabase) {
        val mTags = mutableSetOf<String>(); val dTags = mutableSetOf<String>(); val sExts = mutableSetOf<String>(); val mNames = mutableSetOf<String>()
        db.query(TABLE_SIGS, null, null, null, null, null, null).use { c ->
            while (c.moveToNext()) {
                val sig = c.getString(c.getColumnIndexOrThrow(COL_SIG)); val type = c.getString(c.getColumnIndexOrThrow(COL_TYPE))
                when (type) { "MASTER" -> mTags.add(sig); "DISQUALIFIER" -> dTags.add(sig); "RAW_EXT" -> sExts.add(sig); "MASTER_NAME" -> mNames.add(sig) }
            }
        }
        val defaultNames = listOf("master.m3u8", "playlist.m3u8", "index.m3u8")
        val wDb = writableDatabase
        defaultNames.filterNot { mNames.contains(it) }.forEach { name ->
            wDb.insertWithOnConflict(TABLE_SIGS, null, ContentValues().apply { put(COL_SIG, name); put(COL_TYPE, "MASTER_NAME") }, SQLiteDatabase.CONFLICT_IGNORE)
            mNames.add(name)
        }
        if (mTags.isEmpty()) {
            val defaults = listOf(
                Triple("#EXT-X-STREAM-INF:", "MASTER", ""), Triple("#EXT-X-MEDIA:", "MASTER", ""), Triple("#EXT-X-I-FRAME-STREAM-INF:", "MASTER", ""),
                Triple("#EXTINF:", "DISQUALIFIER", ""), Triple("#EXT-X-TARGETDURATION:", "DISQUALIFIER", ""), Triple("#EXT-X-MEDIA-SEQUENCE:", "DISQUALIFIER", ""),
                Triple(".aac", "RAW_EXT", ""), Triple(".ts", "RAW_EXT", ""), Triple(".m4s", "RAW_EXT", ""), Triple(".m4a", "RAW_EXT", ""), Triple(".mp3", "RAW_EXT", "")
            )
            defaults.forEach { (sig, t, _) ->
                wDb.insertWithOnConflict(TABLE_SIGS, null, ContentValues().apply { put(COL_SIG, sig); put(COL_TYPE, t) }, SQLiteDatabase.CONFLICT_IGNORE)
                when (t) { "MASTER" -> mTags.add(sig); "DISQUALIFIER" -> dTags.add(sig); "RAW_EXT" -> sExts.add(sig) }
            }
        }
        if (mTags.isNotEmpty()) com.omnicast.core.domain.usecases.HlsMasterResolver.masterTags = mTags
        if (dTags.isNotEmpty()) com.omnicast.core.domain.usecases.HlsMasterResolver.subvariantDisqualifiers = dTags
        if (sExts.isNotEmpty()) com.omnicast.core.domain.usecases.HlsMasterResolver.rawSegmentExts = sExts
        if (mNames.isNotEmpty()) com.omnicast.core.domain.usecases.HlsMasterResolver.masterFileNames = mNames
    }

    private fun seedInitialProfiles() {
        for ((host, cfg) in com.omnicast.core.domain.usecases.StreamManifestProbe.DEFAULT_HOST_PROFILES) {
            if (!host.contains('.')) continue
            val (targetPlayer, singleUse) = cfg
            val existing = cache[host]
            if (existing == null || existing.headers["X-Target-Player"].isNullOrBlank()) {
                val seedHeaders = mapOf("X-Target-Player" to targetPlayer, "X-Single-Use-Token" to singleUse.toString())
                val merged = (existing?.headers ?: emptyMap()) + seedHeaders
                recordSuccess(host, merged, true, MediaBranch.VOD_HLS_MP4)
            }
        }
    }

    fun recordSuccess(host: String, headers: Map<String, String>, bypassProxy: Boolean, branch: MediaBranch) {
        if (host.isBlank()) return
        val cleanHost = host.lowercase()
        val existing = cache[cleanHost]
        val newCount = (existing?.successCount ?: 0) + 1
        val now = System.currentTimeMillis()

        val incomingSafe = headers.filterKeys {
            it.equals("Referer", true) || it.equals("Origin", true) ||
            it.equals("User-Agent", true) || it.equals("Authorization", true) ||
            it.startsWith("X-", true) || it.startsWith("Sec-", true)
        }
        val safeHeaders = (existing?.headers ?: emptyMap()) + incomingSafe

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
            Log.i(TAG, "✅ Perfil exitoso guardado en SQLite para host: $cleanHost (player=${safeHeaders["X-Target-Player"]}, branch=${branch.name})")
        } catch (e: Throwable) {
            Log.e(TAG, "Error al persistir perfil de streaming: ${e.message}", e)
        }
    }

    fun recordFailure(host: String) {
        if (host.isBlank()) return
        val cleanHost = host.lowercase()
        val existing = cache[cleanHost]
        val updatedHeaders = (existing?.headers ?: emptyMap()).toMutableMap().apply {
            put("X-Target-Player", "WEBVIEW")
            put("X-Single-Use-Token", "true")
        }
        val entry = StreamProfileEntry(
            host = cleanHost,
            headers = updatedHeaders,
            bypassProxy = true,
            branch = existing?.branch ?: MediaBranch.VOD_HLS_MP4,
            successCount = existing?.successCount ?: 1,
            lastSuccessTs = System.currentTimeMillis()
        )
        cache[cleanHost] = entry
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_HOST, cleanHost)
                put(COL_HEADERS, serializeHeaders(updatedHeaders))
                put(COL_BYPASS_PROXY, 1)
                put(COL_BRANCH, entry.branch.name)
                put(COL_SUCCESS_COUNT, entry.successCount)
                put(COL_LAST_SUCCESS, entry.lastSuccessTs)
            }
            db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            Log.w(TAG, "⚠️ Host $cleanHost reasignado dinámicamente a WEBVIEW por fallo de reproducción")
        } catch (e: Throwable) {
            Log.e(TAG, "Error al actualizar perfil tras fallo: ${e.message}", e)
        }
    }

    fun learnSignature(sig: String, type: String) {
        if (sig.isBlank() || type.isBlank()) return
        try {
            val db = writableDatabase
            db.insertWithOnConflict(TABLE_SIGS, null, ContentValues().apply { put(COL_SIG, sig); put(COL_TYPE, type) }, SQLiteDatabase.CONFLICT_IGNORE)
            when (type) {
                "MASTER" -> com.omnicast.core.domain.usecases.HlsMasterResolver.masterTags = com.omnicast.core.domain.usecases.HlsMasterResolver.masterTags + sig
                "DISQUALIFIER" -> com.omnicast.core.domain.usecases.HlsMasterResolver.subvariantDisqualifiers = com.omnicast.core.domain.usecases.HlsMasterResolver.subvariantDisqualifiers + sig
                "RAW_EXT" -> com.omnicast.core.domain.usecases.HlsMasterResolver.rawSegmentExts = com.omnicast.core.domain.usecases.HlsMasterResolver.rawSegmentExts + sig
                "MASTER_NAME" -> com.omnicast.core.domain.usecases.HlsMasterResolver.masterFileNames = com.omnicast.core.domain.usecases.HlsMasterResolver.masterFileNames + sig
            }
        } catch (_: Throwable) {}
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
