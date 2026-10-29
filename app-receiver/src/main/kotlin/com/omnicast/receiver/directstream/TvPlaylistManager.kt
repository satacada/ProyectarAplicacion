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

@Serializable
data class TvPlaylistItem(
    val url: String,
    val title: String,
    val pageUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val timestamp: Long = 0L,
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L,
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

        fun extractCleanWebName(pageUrl: String?, headers: Map<String, String>, mediaUrl: String): String {
            val rawCandidate = listOfNotNull(
                pageUrl?.takeIf { it.isNotBlank() },
                headers["X-Source-Page-Url"]?.takeIf { it.isNotBlank() },
                headers["Referer"]?.takeIf { it.isNotBlank() },
                headers["Origin"]?.takeIf { it.isNotBlank() },
                mediaUrl
            ).firstOrNull().orEmpty()

            val host = try {
                java.net.URI(rawCandidate).host?.lowercase()?.removePrefix("www.") ?: ""
            } catch (_: Throwable) {
                rawCandidate.substringAfter("://", "").substringBefore('/').lowercase().removePrefix("www.")
            }

            return when {
                host.contains("pelisplus") || host.contains("pelisplushd") -> "PelisPlus"
                host.contains("cuevana") -> "Cuevana"
                host.contains("canela") -> "Canela.tv"
                host.contains("ok.ru") || host.contains("odnoklassniki") -> "OK.ru"
                host.contains("dailymotion") || host.contains("dai.ly") -> "Dailymotion"
                host.contains("youtube") || host.contains("youtu.be") -> "YouTube"
                host.contains("cinecalidad") -> "CineCalidad"
                host.contains("zoowomaniacos") -> "Zoowomaniacos"
                host.contains("pelisgo") -> "PelisGO"
                host.contains("repelis") -> "Repelis"
                host.contains("gnula") -> "Gnula"
                host.contains("hdfull") -> "HDFull"
                host.contains("tokyvideo") -> "TokyVideo"
                host.contains("archive.org") -> "Internet Archive"
                host.contains("808ball") || host.contains("futbol") -> "Deportes en Vivo"
                host.contains("lulustream") || host.contains("luluvdo") -> "LuluStream Web"
                host.contains("fastream") || host.contains("dropcdn") -> "Fastream Web"
                host.isNotBlank() && !host.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) -> {
                    val base = host.substringBeforeLast('.')
                    base.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
                else -> "Portal Web"
            }
        }
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file by lazy { File(context.filesDir, FILE_NAME) }

    var onPlaylistsChanged: ((String) -> Unit)? = null
    fun getRawJson(): String = try { json.encodeToString(_playlists.value) } catch (_: Throwable) { "[]" }

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

    fun updateProgress(url: String, posMs: Long) {
        if (url.isBlank() || posMs < 5000L) return
        val current = _playlists.value
        var changed = false
        val cleanUrl = url.substringBefore('?')
        val updated = current.map { pl ->
            val idx = pl.items.indexOfFirst { it.url == url || it.url.substringBefore('?') == cleanUrl }
            if (idx >= 0) {
                changed = true
                val list = pl.items.toMutableList()
                list[idx] = list[idx].copy(lastPositionMs = posMs)
                pl.copy(items = list)
            } else pl
        }
        if (changed) {
            _playlists.value = updated
            try { file.writeText(json.encodeToString(updated)) } catch (_: Throwable) {}
        }
    }

    fun getResumePosition(item: TvPlaylistItem): Long {
        if (item.lastPositionMs >= 5000L) return item.lastPositionMs
        val cleanUrl = item.url.substringBefore('?')
        for (pl in _playlists.value) {
            val found = pl.items.firstOrNull { it.url == item.url || it.url.substringBefore('?') == cleanUrl }
            if (found != null && found.lastPositionMs >= 5000L) return found.lastPositionMs
        }
        return 0L
    }
    fun isMovieSaved(url: String, title: String? = null): Boolean {
        if (url.isBlank()) return false
        val cleanUrl = url.substringBefore('?')
        return _playlists.value.any { pl ->
            pl.items.any { it.url == url || it.url.substringBefore('?') == cleanUrl || (!title.isNullOrBlank() && it.title.equals(title, ignoreCase = true)) }
        }
    }

    fun saveMovie(
        title: String,
        url: String,
        posMs: Long = 0L,
        pageUrl: String? = null,
        headers: Map<String, String> = emptyMap(),
        targetFolderId: String = "from_website",
        durationMs: Long = 0L
    ): Boolean {
        if (url.isBlank()) return false
        val current = _playlists.value.toMutableList()
        val cleanUrl = url.substringBefore('?')
        val isFavFolder = targetFolderId == "favoritos"
        val newItem = TvPlaylistItem(
            url = url,
            title = title.ifBlank { "Película guardada" },
            pageUrl = pageUrl,
            headers = headers,
            timestamp = System.currentTimeMillis(),
            lastPositionMs = posMs,
            durationMs = durationMs,
            isFavorite = isFavFolder
        )
        val targetIdx = if (isFavFolder) {
            current.indexOfFirst { it.id == "favoritos" || it.name.contains("Favorito", ignoreCase = true) }
        } else {
            current.indexOfFirst { it.id == "from_website" || it.id == "ver_mas_tarde" || it.name.contains("Ver más tarde", ignoreCase = true) }
        }
        if (targetIdx >= 0) {
            val pl = current[targetIdx]
            val existingItemIdx = pl.items.indexOfFirst { it.url == url || it.url.substringBefore('?') == cleanUrl || it.title.equals(title, ignoreCase = true) }
            val updatedItems = pl.items.toMutableList()
            if (existingItemIdx >= 0) {
                updatedItems[existingItemIdx] = newItem.copy(
                    lastPositionMs = if (posMs > 0L) posMs else updatedItems[existingItemIdx].lastPositionMs,
                    durationMs = if (durationMs > 0L) durationMs else updatedItems[existingItemIdx].durationMs,
                    pageUrl = pageUrl ?: updatedItems[existingItemIdx].pageUrl
                )
            } else {
                updatedItems.add(0, newItem)
            }
            current[targetIdx] = pl.copy(items = updatedItems)
        } else {
            val newPl = TvPlaylist(
                id = if (isFavFolder) "favoritos" else "from_website",
                name = if (isFavFolder) "⭐ Favoritos" else "Ver más tarde",
                items = listOf(newItem),
                isDefault = isFavFolder,
                createdAt = System.currentTimeMillis()
            )
            current.add(0, newPl)
        }
        _playlists.value = current
        val raw = try { json.encodeToString(current) } catch (_: Throwable) { "" }
        if (raw.isNotBlank()) {
            try { file.writeText(raw) } catch (_: Throwable) {}
            onPlaylistsChanged?.invoke(raw)
        }
        return true
    }

    fun replaceExpiredUrl(oldUrl: String, newUrl: String, newHeaders: Map<String, String> = emptyMap()): Boolean {
        if (oldUrl.isBlank() || newUrl.isBlank() || oldUrl == newUrl) return false
        val current = _playlists.value
        var changed = false
        val cleanOldUrl = oldUrl.substringBefore('?')
        val updated = current.map { pl ->
            val idx = pl.items.indexOfFirst { it.url == oldUrl || it.url.substringBefore('?') == cleanOldUrl }
            if (idx >= 0) {
                changed = true
                val list = pl.items.toMutableList()
                list[idx] = list[idx].copy(
                    url = newUrl,
                    headers = if (newHeaders.isNotEmpty()) newHeaders else list[idx].headers,
                    timestamp = System.currentTimeMillis()
                )
                pl.copy(items = list)
            } else pl
        }
        if (changed) {
            _playlists.value = updated
            try { file.writeText(json.encodeToString(updated)) } catch (_: Throwable) {}
        }
        return changed
    }

    fun findItemByUrlOrTitle(url: String, title: String?): TvPlaylistItem? {
        val cleanUrl = url.substringBefore('?')
        for (pl in _playlists.value) {
            val found = pl.items.firstOrNull { 
                it.url == url || 
                it.url.substringBefore('?') == cleanUrl || 
                (!title.isNullOrBlank() && it.title.equals(title, ignoreCase = true))
            }
            if (found != null) return found
        }
        return null
    }

    fun removeItem(url: String) {
        removeItems(setOf(url))
    }

    fun removeItems(urls: Set<String>) {
        if (urls.isEmpty()) return
        val cleanUrls = urls.map { it.substringBefore('?') }.toSet()
        val updated = _playlists.value.map { pl ->
            pl.copy(items = pl.items.filterNot { it.url in urls || it.url.substringBefore('?') in cleanUrls })
        }
        _playlists.value = updated
        val raw = try { json.encodeToString(updated) } catch (_: Throwable) { "" }
        if (raw.isNotBlank()) {
            try { file.writeText(raw) } catch (_: Throwable) {}
            onPlaylistsChanged?.invoke(raw)
        }
    }
}
