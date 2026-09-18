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

    fun addOrUpdate(url: String, title: String, pageUrl: String? = null, headers: Map<String, String> = emptyMap(), positionMs: Long = 0L) {
        if (url.isBlank() || url.contains("stub.mp4") || url.contains("creative")) return
        val current = getHistory().toMutableList()
        val existing = current.firstOrNull { it.url == url }
        val isFav = existing?.isFavorite ?: false
        current.removeAll { it.url == url }
        val cleanTitle = title.ifBlank { "Película / Video" }
        current.add(0, HistoryItem(url, cleanTitle, pageUrl, headers, System.currentTimeMillis(), positionMs, isFavorite = isFav))
        val favorites = current.filter { it.isFavorite }
        val nonFavorites = current.filter { !it.isFavorite }.take(MAX_NON_FAVORITES)
        val combined = (favorites + nonFavorites).distinctBy { it.url }
        try {
            prefs.edit().putString(KEY_HISTORY, json.encodeToString(combined)).apply()
        } catch (_: Throwable) {}
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
