package com.omnicast.sender.playlist

import android.content.Context
import com.omnicast.sender.history.HistoryItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Gestor persistente de listas de reproducción personalizadas y carpetas de marcadores.
 * Permite organizar videos detectados en la web en listas temáticas creadas por el usuario.
 */
class PlaylistManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "omnicast_playlist_prefs"
        private const val KEY_PLAYLISTS = "playlists_json"
        const val DEFAULT_FAVORITES_ID = "default_favorites"
        const val DEFAULT_WATCH_LATER_ID = "default_watch_later"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        ensureDefaultPlaylists()
    }

    private fun ensureDefaultPlaylists() {
        val existing = getPlaylistsRaw()
        if (existing.isEmpty()) {
            val defaults = listOf(
                Playlist(
                    id = DEFAULT_FAVORITES_ID,
                    name = "⭐ Favoritos",
                    isDefault = true
                ),
                Playlist(
                    id = DEFAULT_WATCH_LATER_ID,
                    name = "🕒 Ver más tarde",
                    isDefault = true
                )
            )
            savePlaylists(defaults)
        }
    }

    private fun getPlaylistsRaw(): List<Playlist> {
        val raw = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    var onPlaylistsChanged: ((String) -> Unit)? = null

    private fun savePlaylists(list: List<Playlist>) {
        try {
            val encoded = json.encodeToString(list)
            prefs.edit().putString(KEY_PLAYLISTS, encoded).apply()
            onPlaylistsChanged?.invoke(encoded)
        } catch (_: Throwable) {}
    }

    fun getRawJson(): String {
        return json.encodeToString(getPlaylists())
    }

    fun getPlaylists(): List<Playlist> {
        val lists = getPlaylistsRaw()
        return if (lists.isEmpty()) {
            ensureDefaultPlaylists()
            getPlaylistsRaw()
        } else {
            lists.sortedWith(compareByDescending<Playlist> { it.isDefault }.thenBy { it.createdAt })
        }
    }

    fun createPlaylist(name: String): Playlist? {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return null
        val current = getPlaylists().toMutableList()
        if (current.any { it.name.equals(trimmed, ignoreCase = true) }) return null

        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            isDefault = false
        )
        current.add(newPlaylist)
        savePlaylists(current)
        return newPlaylist
    }

    fun deletePlaylist(playlistId: String): Boolean {
        val current = getPlaylists().toMutableList()
        val target = current.firstOrNull { it.id == playlistId } ?: return false
        if (target.isDefault) return false
        current.remove(target)
        savePlaylists(current)
        return true
    }

    fun addItemToPlaylist(playlistId: String, item: HistoryItem) {
        if (item.url.isBlank()) return
        val current = getPlaylists().map { pl ->
            if (pl.id == playlistId) {
                val updatedItems = pl.items.filterNot { it.url == item.url }.toMutableList()
                updatedItems.add(0, item)
                pl.copy(items = updatedItems)
            } else {
                pl
            }
        }
        savePlaylists(current)
    }

    fun removeItemFromPlaylist(playlistId: String, url: String) {
        val current = getPlaylists().map { pl ->
            if (pl.id == playlistId) {
                pl.copy(items = pl.items.filterNot { it.url == url })
            } else {
                pl
            }
        }
        savePlaylists(current)
    }

    fun isVideoInPlaylist(playlistId: String, url: String): Boolean {
        val target = getPlaylists().firstOrNull { it.id == playlistId } ?: return false
        return target.items.any { it.url == url }
    }

    fun toggleVideoInPlaylist(playlistId: String, item: HistoryItem): Boolean {
        val inList = isVideoInPlaylist(playlistId, item.url)
        if (inList) {
            removeItemFromPlaylist(playlistId, item.url)
            return false
        } else {
            addItemToPlaylist(playlistId, item)
            return true
        }
    }
}
