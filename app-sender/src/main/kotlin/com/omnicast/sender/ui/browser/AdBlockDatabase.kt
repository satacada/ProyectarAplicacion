package com.omnicast.sender.ui.browser

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Base de datos SQLite dinámica para dominios y patrones de AdBlock.
 * Permite inserción, actualización, borrado y sincronización remota sin valores estáticos en código.
 */
class AdBlockDatabase private constructor(private val appContext: Context) :
    SQLiteOpenHelper(appContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "AdBlockDatabase"
        private const val DATABASE_NAME = "omnicast_adblock.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_DOMAINS = "ad_domains"
        private const val TABLE_KEYWORDS = "ad_keywords"

        private const val COL_ID = "id"
        private const val COL_VALUE = "value"
        private const val COL_CATEGORY = "category"
        private const val COL_ENABLED = "enabled"
        private const val COL_UPDATED_AT = "updated_at"

        @Volatile
        private var instance: AdBlockDatabase? = null

        private val cachedDomains = ConcurrentHashMap.newKeySet<String>()
        private val cachedKeywords = CopyOnWriteArrayList<String>()

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        val db = AdBlockDatabase(context.applicationContext)
                        instance = db
                        db.loadCacheFromDb()
                    }
                }
            }
        }

        fun get(): AdBlockDatabase =
            instance ?: throw IllegalStateException("AdBlockDatabase not initialized. Call init(context) first.")

        fun isAdDomain(host: String?): Boolean {
            if (host.isNullOrBlank()) return false
            val h = host.lowercase()
            for (d in cachedDomains) {
                if (h == d || h.endsWith(".$d")) return true
            }
            return false
        }

        fun matchesKeyword(url: String): Boolean {
            val lower = url.lowercase()
            for (kw in cachedKeywords) {
                if (lower.contains(kw)) return true
            }
            return false
        }

        fun containsDomain(domain: String): Boolean {
            return cachedDomains.contains(domain.lowercase().trim())
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_DOMAINS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_VALUE TEXT UNIQUE NOT NULL,
                $COL_CATEGORY TEXT DEFAULT 'ad',
                $COL_ENABLED INTEGER DEFAULT 1,
                $COL_UPDATED_AT INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_KEYWORDS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_VALUE TEXT UNIQUE NOT NULL,
                $COL_CATEGORY TEXT DEFAULT 'ad',
                $COL_ENABLED INTEGER DEFAULT 1,
                $COL_UPDATED_AT INTEGER
            )
            """.trimIndent()
        )
        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_DOMAINS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_KEYWORDS")
            onCreate(db)
            Log.i(TAG, "Base de datos AdBlock restablecida a v2 limpia.")
        }
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        try {
            val stream = appContext.assets.open("adblock/default_ad_rules.json")
            val jsonStr = stream.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)

            db.beginTransaction()
            try {
                val domains = root.optJSONArray("domains")
                if (domains != null) {
                    for (i in 0 until domains.length()) {
                        val d = domains.optString(i).trim().lowercase()
                        if (d.isNotBlank()) insertItem(db, TABLE_DOMAINS, d, "ad")
                    }
                }
                val keywords = root.optJSONArray("keywords")
                if (keywords != null) {
                    for (j in 0 until keywords.length()) {
                        val k = keywords.optString(j).trim().lowercase()
                        if (k.isNotBlank()) insertItem(db, TABLE_KEYWORDS, k, "ad")
                    }
                }
                db.setTransactionSuccessful()
                Log.i(TAG, "Base de datos SQLite AdBlock inicializada con reglas base.")
            } finally {
                db.endTransaction()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error poblando reglas iniciales: ${e.message}", e)
        }
    }

    private fun insertItem(db: SQLiteDatabase, table: String, value: String, category: String) {
        val cv = ContentValues().apply {
            put(COL_VALUE, value)
            put(COL_CATEGORY, category)
            put(COL_ENABLED, 1)
            put(COL_UPDATED_AT, System.currentTimeMillis())
        }
        db.insertWithOnConflict(table, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun loadCacheFromDb() {
        try {
            val db = readableDatabase
            val domainsSet = mutableSetOf<String>()
            val keywordsList = mutableListOf<String>()

            db.rawQuery("SELECT $COL_VALUE FROM $TABLE_DOMAINS WHERE $COL_ENABLED = 1", null).use { cursor ->
                val colIdx = cursor.getColumnIndex(COL_VALUE)
                while (cursor.moveToNext()) {
                    domainsSet.add(cursor.getString(colIdx).lowercase())
                }
            }

            db.rawQuery("SELECT $COL_VALUE FROM $TABLE_KEYWORDS WHERE $COL_ENABLED = 1", null).use { cursor ->
                val colIdx = cursor.getColumnIndex(COL_VALUE)
                while (cursor.moveToNext()) {
                    keywordsList.add(cursor.getString(colIdx).lowercase())
                }
            }

            val falsePositives = listOf("reypelis.net", "tioplus.app")
            for (fp in falsePositives) {
                domainsSet.remove(fp)
                try { writableDatabase.delete(TABLE_DOMAINS, "$COL_VALUE = ?", arrayOf(fp)) } catch (_: Throwable) {}
            }

            cachedDomains.clear()
            cachedDomains.addAll(domainsSet)
            cachedKeywords.clear()
            cachedKeywords.addAll(keywordsList)

            Log.i(TAG, "Caché AdBlock recargado desde SQLite: ${cachedDomains.size} dominios, ${cachedKeywords.size} palabras clave.")
        } catch (e: Throwable) {
            Log.e(TAG, "Error cargando caché desde base de datos: ${e.message}", e)
        }
    }

    fun addDomain(domain: String, category: String = "ad"): Boolean {
        val clean = domain.trim().lowercase()
        if (clean.isBlank()) return false
        return try {
            val db = writableDatabase
            insertItem(db, TABLE_DOMAINS, clean, category)
            cachedDomains.add(clean)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Error agregando dominio $clean: ${e.message}", e)
            false
        }
    }

    fun addKeyword(keyword: String, category: String = "ad"): Boolean {
        val clean = keyword.trim().lowercase()
        if (clean.isBlank()) return false
        return try {
            val db = writableDatabase
            insertItem(db, TABLE_KEYWORDS, clean, category)
            cachedKeywords.add(clean)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Error agregando palabra clave $clean: ${e.message}", e)
            false
        }
    }

    fun removeDomain(domain: String): Boolean {
        val clean = domain.trim().lowercase()
        return try {
            val db = writableDatabase
            db.delete(TABLE_DOMAINS, "$COL_VALUE = ?", arrayOf(clean))
            cachedDomains.remove(clean)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Error eliminando dominio $clean: ${e.message}", e)
            false
        }
    }

    fun bulkInsert(domains: Collection<String>, keywords: Collection<String>) {
        if (domains.isEmpty() && keywords.isEmpty()) return
        try {
            val db = writableDatabase
            db.beginTransaction()
            try {
                for (d in domains) {
                    val clean = d.trim().lowercase()
                    if (clean.isNotBlank()) insertItem(db, TABLE_DOMAINS, clean, "dynamic_sync")
                }
                for (k in keywords) {
                    val clean = k.trim().lowercase()
                    if (clean.isNotBlank()) insertItem(db, TABLE_KEYWORDS, clean, "dynamic_sync")
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            loadCacheFromDb()
        } catch (e: Throwable) {
            Log.e(TAG, "Error en inserción masiva: ${e.message}", e)
        }
    }

    fun getAllDomains(): Set<String> = cachedDomains.toSet()
    fun getAllKeywords(): List<String> = cachedKeywords.toList()
}
