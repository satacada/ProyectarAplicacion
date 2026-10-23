package com.omnicast.sender.ui.browser.reputation

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

data class StreamReputation(
    val siteDomain: String,
    val streamHost: String,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val score: Int = 50,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Base de datos SQLite local con caché en RAM (Zero-Lag) para reputación y puntajes de streams (HU-014).
 * Almacena el historial de éxitos y fallos por sitio web y servidor de video para clasificar enlaces.
 */
class StreamReputationDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "omnicast_stream_reputation.db"
        private const val DB_VERSION = 1

        private const val TABLE = "stream_reputation"
        private const val COL_KEY = "lookup_key"
        private const val COL_SITE = "site_domain"
        private const val COL_HOST = "stream_host"
        private const val COL_SUCCESS = "success_count"
        private const val COL_FAILURE = "failure_count"
        private const val COL_SCORE = "score"
        private const val COL_UPDATED = "last_updated"

        @Volatile private var instance: StreamReputationDatabase? = null
        private val cache = ConcurrentHashMap<String, StreamReputation>()

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        val db = StreamReputationDatabase(context)
                        instance = db
                        db.loadCache()
                    }
                }
            }
        }

        fun get(): StreamReputationDatabase =
            instance ?: throw IllegalStateException("StreamReputationDatabase no inicializado")

        fun cleanDomain(rawUrlOrHost: String?): String {
            if (rawUrlOrHost.isNullOrBlank()) return ""
            return try {
                val host = if (rawUrlOrHost.contains("://")) URI(rawUrlOrHost).host ?: rawUrlOrHost else rawUrlOrHost
                host.lowercase().removePrefix("www.").trim()
            } catch (_: Throwable) {
                rawUrlOrHost.lowercase().removePrefix("www.").trim()
            }
        }

        fun getScore(siteUrl: String?, streamUrl: String?): Int {
            val site = cleanDomain(siteUrl)
            val host = cleanDomain(streamUrl)
            if (host.isBlank()) return 50
            cache["$site|$host"]?.let { return it.score }
            cache["*|$host"]?.let { return it.score }
            for ((k, v) in cache) {
                if (k.endsWith("|$host") || (host.endsWith(k.substringAfter("|")))) return v.score
            }
            return 50
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE (
                $COL_KEY TEXT PRIMARY KEY,
                $COL_SITE TEXT NOT NULL,
                $COL_HOST TEXT NOT NULL,
                $COL_SUCCESS INTEGER NOT NULL DEFAULT 0,
                $COL_FAILURE INTEGER NOT NULL DEFAULT 0,
                $COL_SCORE INTEGER NOT NULL DEFAULT 50,
                $COL_UPDATED INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    private fun loadCache() {
        try {
            val db = readableDatabase
            db.query(TABLE, null, null, null, null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val k = cursor.getString(cursor.getColumnIndexOrThrow(COL_KEY))
                    val site = cursor.getString(cursor.getColumnIndexOrThrow(COL_SITE))
                    val host = cursor.getString(cursor.getColumnIndexOrThrow(COL_HOST))
                    val sCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SUCCESS))
                    val fCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_FAILURE))
                    val score = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SCORE))
                    val updated = cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED))
                    cache[k] = StreamReputation(site, host, sCount, fCount, score, updated)
                }
            }
        } catch (_: Throwable) {}
    }

    fun recordSuccess(siteUrl: String?, streamUrl: String?) {
        val site = cleanDomain(siteUrl).ifBlank { "*" }
        val host = cleanDomain(streamUrl)
        if (host.isBlank()) return
        updateReputation(site, host, successDelta = 1, failureDelta = 0)
    }

    fun recordFailure(siteUrl: String?, streamUrl: String?) {
        val site = cleanDomain(siteUrl).ifBlank { "*" }
        val host = cleanDomain(streamUrl)
        if (host.isBlank()) return
        updateReputation(site, host, successDelta = 0, failureDelta = 1)
    }

    private fun updateReputation(site: String, host: String, successDelta: Int, failureDelta: Int) {
        val key = "$site|$host"
        val existing = cache[key] ?: StreamReputation(site, host, 0, 0, 50)
        val newSuccess = existing.successCount + successDelta
        val newFailure = existing.failureCount + failureDelta
        val total = newSuccess + newFailure
        val newScore = if (total == 0) 50 else ((newSuccess.toFloat() / total) * 100).toInt().coerceIn(0, 100)
        val updated = StreamReputation(site, host, newSuccess, newFailure, newScore, System.currentTimeMillis())
        cache[key] = updated

        try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put(COL_KEY, key); put(COL_SITE, site); put(COL_HOST, host)
                put(COL_SUCCESS, newSuccess); put(COL_FAILURE, newFailure)
                put(COL_SCORE, newScore); put(COL_UPDATED, updated.lastUpdated)
            }
            db.insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (_: Throwable) {}
    }

    fun saveBatchFromCloud(items: List<StreamReputation>) {
        if (items.isEmpty()) return
        try {
            val db = writableDatabase
            db.beginTransaction()
            try {
                for (item in items) {
                    val key = "${item.siteDomain}|${item.streamHost}"
                    val cv = ContentValues().apply {
                        put(COL_KEY, key); put(COL_SITE, item.siteDomain); put(COL_HOST, item.streamHost)
                        put(COL_SUCCESS, item.successCount); put(COL_FAILURE, item.failureCount)
                        put(COL_SCORE, item.score); put(COL_UPDATED, item.lastUpdated)
                    }
                    db.insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    cache[key] = item
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (_: Throwable) {}
    }
}
