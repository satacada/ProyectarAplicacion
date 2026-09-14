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
    val lastPositionMs: Long = 0L
)

class PlayHistoryManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "omnicast_history_prefs"
        private const val KEY_HISTORY = "history_json"
        private const val MAX_ITEMS = 30
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHistory(): List<HistoryItem> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<HistoryItem>>(raw).sortedByDescending { it.timestamp }
        } catch (_: Throwable) { emptyList() }
    }

    fun addOrUpdate(url: String, title: String, pageUrl: String? = null, headers: Map<String, String> = emptyMap(), positionMs: Long = 0L) {
        if (url.isBlank() || url.contains("stub.mp4")) return
        val current = getHistory().toMutableList()
        current.removeAll { it.url == url }
        val cleanTitle = title.ifBlank { "Pelicula / Video" }
        current.add(0, HistoryItem(url, cleanTitle, pageUrl, headers, System.currentTimeMillis(), positionMs))
        val trimmed = current.take(MAX_ITEMS)
        try {
            prefs.edit().putString(KEY_HISTORY, json.encodeToString(trimmed)).apply()
        } catch (_: Throwable) {}
    }

    fun remove(url: String) {
        val current = getHistory().filterNot { it.url == url }
        try {
            prefs.edit().putString(KEY_HISTORY, json.encodeToString(current)).apply()
        } catch (_: Throwable) {}
    }

    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}
