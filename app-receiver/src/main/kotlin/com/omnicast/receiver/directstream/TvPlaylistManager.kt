package com.omnicast.receiver.directstream

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class TvPlaylistItem(
    val url: String,
    val title: String,
    val pageUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val timestamp: Long = 0L,
    val lastPositionMs: Long = 0L,
    val isFavorite: Boolean = false
)

@Serializable
data class TvPlaylist(
    val id: String,
    val name: String,
    val items: List<TvPlaylistItem> = emptyList(),
    val isDefault: Boolean = false,
    val createdAt: Long = 0L
)

class TvPlaylistManager(private val context: Context) {

    companion object {
        private const val TAG = "TvPlaylistManager"
        private const val FILE_NAME = "tv_playlists_sync.json"
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file by lazy { File(context.filesDir, FILE_NAME) }

    private val _playlists = MutableStateFlow<List<TvPlaylist>>(emptyList())
    val playlists: StateFlow<List<TvPlaylist>> = _playlists.asStateFlow()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        try {
            if (file.exists()) {
                val text = file.readText()
                if (text.isNotBlank()) {
                    _playlists.value = json.decodeFromString(text)
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error cargando listas sincronizadas: ${e.message}")
        }
    }

    suspend fun syncPlaylistsJson(jsonString: String) = withContext(Dispatchers.IO) {
        try {
            val list = json.decodeFromString<List<TvPlaylist>>(jsonString)
            _playlists.value = list
            file.writeText(jsonString)
            Log.i(TAG, "Listas sincronizadas desde móvil: ${list.size} listas recibidas")
        } catch (e: Throwable) {
            Log.e(TAG, "Error procesando sincronización de listas: ${e.message}", e)
        }
    }
}
