package com.omnicast.receiver.directstream

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class TvHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val mediaUrl: String,
    val title: String,
    val headers: Map<String, String> = emptyMap(),
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

class TvPlaybackHistoryManager(private val context: Context) {

    companion object {
        private const val TAG = "TvPlaybackHistory"
        private const val FILE_NAME = "tv_playback_history.json"
        private const val MAX_ITEMS = 50
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val historyFile by lazy { File(context.filesDir, FILE_NAME) }

    private val _history = MutableStateFlow<List<TvHistoryItem>>(emptyList())
    val history: StateFlow<List<TvHistoryItem>> = _history.asStateFlow()
    private var lastDiskSaveTime = 0L
    private var lastDiskSavePosition = 0L

    init {
        loadHistory()
    }

    private fun loadHistory() {
        try {
            if (historyFile.exists()) {
                val text = historyFile.readText()
                if (text.isNotBlank()) {
                    val list = json.decodeFromString<List<TvHistoryItem>>(text)
                    _history.value = list.distinctBy {
                        if (it.title.isNotBlank() && it.title != "Película / Video Web") it.title else it.mediaUrl
                    }.sortedByDescending { it.updatedAt }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error cargando historial TV: ${e.message}")
        }
    }

    suspend fun addOrUpdate(
        url: String,
        title: String,
        headers: Map<String, String> = emptyMap(),
        positionMs: Long = 0L,
        durationMs: Long = 0L
    ) = withContext(Dispatchers.IO) {
        try {
            val current = _history.value.toMutableList()
            val cleanTitle = title.ifBlank { "Película / Video Web" }
            val existingIdx = current.indexOfFirst {
                it.mediaUrl == url || (cleanTitle != "Película / Video Web" && cleanTitle != "Dailymotion" && it.title == cleanTitle)
            }
            val item = if (existingIdx >= 0) {
                val old = current.removeAt(existingIdx)
                old.copy(
                    mediaUrl = url,
                    title = if (cleanTitle.isNotBlank() && cleanTitle != "Transmisión Web" && cleanTitle != "Dailymotion") cleanTitle else old.title,
                    headers = if (headers.isNotEmpty()) headers else old.headers,
                    lastPositionMs = if (positionMs > 0L) positionMs else old.lastPositionMs,
                    durationMs = if (durationMs > 0L) durationMs else old.durationMs,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                TvHistoryItem(
                    mediaUrl = url,
                    title = cleanTitle,
                    headers = headers,
                    lastPositionMs = positionMs,
                    durationMs = durationMs
                )
            }
            current.add(0, item)
            val trimmed = current.distinctBy {
                if (it.title.isNotBlank() && it.title != "Película / Video Web") it.title else it.mediaUrl
            }.take(MAX_ITEMS)
            _history.value = trimmed
            historyFile.writeText(json.encodeToString(trimmed))
            Log.i(TAG, "Guardado en historial TV: ${item.title} ($url)")
        } catch (e: Throwable) {
            Log.e(TAG, "Error guardando historial TV: ${e.message}", e)
        }
    }

    suspend fun updateProgress(url: String, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        if (positionMs <= 0L && durationMs <= 0L) return@withContext
        val current = _history.value.toMutableList()
        val idx = current.indexOfFirst { it.mediaUrl == url }
        if (idx >= 0) {
            val old = current[idx]
            current[idx] = old.copy(lastPositionMs = positionMs, durationMs = durationMs, updatedAt = System.currentTimeMillis())
            _history.value = current
            val now = System.currentTimeMillis()
            val posDelta = kotlin.math.abs(positionMs - lastDiskSavePosition)
            if (posDelta >= 15_000L || now - lastDiskSaveTime >= 30_000L) {
                lastDiskSaveTime = now
                lastDiskSavePosition = positionMs
                try { historyFile.writeText(json.encodeToString(current)) } catch (_: Throwable) {}
            }
        }
    }

    suspend fun deleteItem(id: String) = withContext(Dispatchers.IO) {
        val filtered = _history.value.filterNot { it.id == id }
        _history.value = filtered
        try { historyFile.writeText(json.encodeToString(filtered)) } catch (_: Throwable) {}
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        _history.value = emptyList()
        try { if (historyFile.exists()) historyFile.delete() } catch (_: Throwable) {}
    }
}
