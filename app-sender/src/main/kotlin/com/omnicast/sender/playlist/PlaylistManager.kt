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

    fun isSameVideo(a: HistoryItem, b: HistoryItem): Boolean {
        if (a.url.isNotBlank() && b.url.isNotBlank()) {
            if (a.url == b.url) return true
            val cleanA = a.url.substringBefore('?')
            val cleanB = b.url.substringBefore('?')
            if (cleanA.length > 15 && cleanA == cleanB) return true
        }
        val tA = cleanTitleForMatch(a.title)
        val tB = cleanTitleForMatch(b.title)
        if (tA.isNotBlank() && tB.isNotBlank() && isNonGenericTitle(tA) && isNonGenericTitle(tB)) {
            if (tA == tB) return true
            if (calculateTitleSimilarity(tA, tB) >= 0.85) return true
        }
        return false
    }

    private fun cleanTitleForMatch(t: String): String =
        t.lowercase().replace(Regex("[^a-z0-9áéíóúüñ\\s]"), " ").trim().replace(Regex("\\s+"), " ")

    private fun isNonGenericTitle(t: String): Boolean =
        t.length >= 4 && t !in setOf("video", "pelicula", "transmision", "cargando", "dailymotion", "video web", "stream")

    private fun calculateTitleSimilarity(s1: String, s2: String): Double {
        val w1 = s1.split(" ").filter { it.length >= 2 }.toSet()
        val w2 = s2.split(" ").filter { it.length >= 2 }.toSet()
        if (w1.isEmpty() || w2.isEmpty()) return 0.0
        val intersection = w1.intersect(w2).size
        val union = w1.union(w2).size
        return if (union == 0) 0.0 else intersection.toDouble() / union.toDouble()
    }

    fun addItemToPlaylist(playlistId: String, item: HistoryItem) {
        if (item.url.isBlank()) return
        val current = getPlaylists().map { pl ->
            if (pl.id == playlistId) {
                val updatedItems = pl.items.filterNot { isSameVideo(it, item) }.toMutableList()
                updatedItems.add(0, item)
                pl.copy(items = updatedItems)
            } else {
                pl
            }
        }
        savePlaylists(current)
    }

    fun removeItemFromPlaylist(playlistId: String, item: HistoryItem) {
        val current = getPlaylists().map { pl ->
            if (pl.id == playlistId) {
                pl.copy(items = pl.items.filterNot { isSameVideo(it, item) })
            } else {
                pl
            }
        }
        savePlaylists(current)
    }

    fun removeItemFromPlaylist(playlistId: String, url: String) {
        val current = getPlaylists().map { pl ->
            if (pl.id == playlistId) {
                pl.copy(items = pl.items.filterNot { it.url == url || it.url.substringBefore('?') == url.substringBefore('?') })
            } else {
                pl
            }
        }
        savePlaylists(current)
    }

    fun isVideoInPlaylist(playlistId: String, item: HistoryItem): Boolean {
        val target = getPlaylists().firstOrNull { it.id == playlistId } ?: return false
        return target.items.any { isSameVideo(it, item) }
    }

    fun isVideoInPlaylist(playlistId: String, url: String): Boolean {
        val target = getPlaylists().firstOrNull { it.id == playlistId } ?: return false
        return target.items.any { it.url == url || it.url.substringBefore('?') == url.substringBefore('?') }
    }

    fun toggleVideoInPlaylist(playlistId: String, item: HistoryItem): Boolean {
        val inList = isVideoInPlaylist(playlistId, item)
        if (inList) {
            removeItemFromPlaylist(playlistId, item)
            return false
        } else {
            addItemToPlaylist(playlistId, item)
            return true
        }
    }

    fun isVideoSaved(url: String, title: String = ""): Boolean {
        if (url.isBlank() && title.isBlank()) return false
        val item = HistoryItem(url = url, title = title)
        return getPlaylists().any { pl ->
            pl.items.any { isSameVideo(it, item) }
        }
    }

    fun updateProgress(url: String, positionMs: Long, durationMs: Long) {
        if (url.isBlank() || positionMs < 5000L) return
        val current = getPlaylists()
        var changed = false
        val cleanUrl = url.substringBefore('?')
        val updated = current.map { pl ->
            val idx = pl.items.indexOfFirst { it.url == url || it.url.substringBefore('?') == cleanUrl }
            if (idx >= 0) {
                changed = true
                val list = pl.items.toMutableList()
                val old = list[idx]
                list[idx] = old.copy(lastPositionMs = positionMs, durationMs = if (durationMs > 0L) durationMs else old.durationMs)
                pl.copy(items = list)
            } else pl
        }
        if (changed) savePlaylists(updated)
    }

    fun getLastPosition(url: String): Long {
        val cleanUrl = url.substringBefore('?')
        for (pl in getPlaylists()) {
            val found = pl.items.firstOrNull { it.url == url || it.url.substringBefore('?') == cleanUrl }
            if (found != null && found.lastPositionMs >= 5000L) return found.lastPositionMs
        }
        return 0L
    }

    fun syncFromTv(jsonStr: String): Boolean {
        if (jsonStr.isBlank()) return false
        return try {
            val incoming = json.decodeFromString<List<Playlist>>(jsonStr)
            if (incoming.isEmpty()) return false
            val current = getPlaylists().toMutableList()
            var changed = false
            for (inPl in incoming) {
                val targetIdx = current.indexOfFirst { it.id == inPl.id || (it.isDefault && inPl.isDefault) || it.name.equals(inPl.name, ignoreCase = true) }
                if (targetIdx >= 0) {
                    val pl = current[targetIdx]
                    val updatedItems = pl.items.toMutableList()
                    for (item in inPl.items) {
                        val existIdx = updatedItems.indexOfFirst { isSameVideo(it, item) }
                        if (existIdx >= 0) {
                            if (item.lastPositionMs > updatedItems[existIdx].lastPositionMs || item.timestamp > updatedItems[existIdx].timestamp) {
                                updatedItems[existIdx] = updatedItems[existIdx].copy(
                                    url = item.url,
                                    lastPositionMs = if (item.lastPositionMs > 0L) item.lastPositionMs else updatedItems[existIdx].lastPositionMs,
                                    pageUrl = item.pageUrl ?: updatedItems[existIdx].pageUrl
                                )
                                changed = true
                            }
                        } else {
                            updatedItems.add(0, item)
                            changed = true
                        }
                    }
                    current[targetIdx] = pl.copy(items = updatedItems)
                } else {
                    current.add(inPl)
                    changed = true
                }
            }
            if (changed) {
                val encoded = json.encodeToString(current)
                prefs.edit().putString(KEY_PLAYLISTS, encoded).apply()
            }
            changed
        } catch (_: Throwable) { false }
    }
}
