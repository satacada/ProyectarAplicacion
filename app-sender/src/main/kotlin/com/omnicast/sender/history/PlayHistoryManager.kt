package com.omnicast.sender.history

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class HistoryItem(
    val url: String,
    val title: String,
    val pageUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isFavorite: Boolean = false
)

class PlayHistoryManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "omnicast_history_prefs"
        private const val KEY_HISTORY = "history_json"
        private const val MAX_NON_FAVORITES = 50
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHistory(): List<HistoryItem> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<HistoryItem>>(raw).sortedWith(
                compareByDescending<HistoryItem> { it.isFavorite }
                    .thenByDescending { it.timestamp }
            )
        } catch (_: Throwable) { emptyList() }
    }

    fun addOrUpdate(url: String, title: String, pageUrl: String? = null, headers: Map<String, String> = emptyMap(), positionMs: Long = 0L, durationMs: Long = 0L) {
        if (url.isBlank() || url.contains("stub.mp4") || url.contains("creative")) return
        val current = getHistory().toMutableList()
        val cleanTitle = title.ifBlank { "Película / Video" }
        val existing = current.firstOrNull { it.url == url || (cleanTitle != "Película / Video" && cleanTitle != "Dailymotion" && it.title == cleanTitle) }
        val isFav = existing?.isFavorite ?: false
        val finalPos = if (positionMs > 0L) positionMs else (existing?.lastPositionMs ?: 0L)
        val finalDur = if (durationMs > 0L) durationMs else (existing?.durationMs ?: 0L)
        current.removeAll { it.url == url || (cleanTitle != "Película / Video" && cleanTitle != "Dailymotion" && it.title == cleanTitle) }
        current.add(0, HistoryItem(url, cleanTitle, pageUrl ?: existing?.pageUrl, if (headers.isNotEmpty()) headers else (existing?.headers ?: emptyMap()), System.currentTimeMillis(), finalPos, finalDur, isFavorite = isFav))
        val favorites = current.filter { it.isFavorite }
        val nonFavorites = current.filter { !it.isFavorite }.take(MAX_NON_FAVORITES)
        val combined = (favorites + nonFavorites).distinctBy { it.url }
        try {
            prefs.edit().putString(KEY_HISTORY, json.encodeToString(combined)).apply()
        } catch (_: Throwable) {}
    }

    fun updateProgress(url: String, positionMs: Long, durationMs: Long) {
        if (url.isBlank() || (positionMs <= 0L && durationMs <= 0L)) return
        val current = getHistory().toMutableList()
        val idx = current.indexOfFirst { it.url == url }
        if (idx >= 0) {
            val old = current[idx]
            current[idx] = old.copy(lastPositionMs = positionMs, durationMs = if (durationMs > 0L) durationMs else old.durationMs, timestamp = System.currentTimeMillis())
            try {
                prefs.edit().putString(KEY_HISTORY, json.encodeToString(current)).apply()
            } catch (_: Throwable) {}
        }
    }

    fun toggleFavorite(url: String) {
        val current = getHistory().map {
            if (it.url == url) it.copy(isFavorite = !it.isFavorite) else it
        }
        try {
            prefs.edit().putString(KEY_HISTORY, json.encodeToString(current)).apply()
        } catch (_: Throwable) {}
    }

    fun remove(url: String) {
        val current = getHistory().filterNot { it.url == url }
        try {
            prefs.edit().putString(KEY_HISTORY, json.encodeToString(current)).apply()
        } catch (_: Throwable) {}
    }

    fun clear(keepFavorites: Boolean = true) {
        if (keepFavorites) {
            val favorites = getHistory().filter { it.isFavorite }
            try {
                prefs.edit().putString(KEY_HISTORY, json.encodeToString(favorites)).apply()
            } catch (_: Throwable) {}
        } else {
            prefs.edit().remove(KEY_HISTORY).apply()
        }
    }
}
