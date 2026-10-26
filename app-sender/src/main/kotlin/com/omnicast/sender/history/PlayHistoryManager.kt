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
    val isFavorite: Boolean = false,
    val requiresProxy: Boolean = false
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
        val raw = prefs.getString(KEY_HISTORY, null)
        val saved = if (!raw.isNullOrBlank()) {
            try { json.decodeFromString<List<HistoryItem>>(raw) } catch (_: Throwable) { emptyList() }
        } else emptyList()
        val defaults = getPreloadedCatalog()
        val combined = (saved + defaults.filterNot { d -> saved.any { s -> (s.pageUrl != null && s.pageUrl == d.pageUrl) || s.url == d.url } })
        return combined.sortedWith(
            compareByDescending<HistoryItem> { it.isFavorite }
                .thenByDescending { it.timestamp }
        )
    }

    private fun getPreloadedCatalog(): List<HistoryItem> {
        val now = System.currentTimeMillis()
        return listOf(
            HistoryItem(
                url = "https://pelisplus.hair/peliculas/8551-mad-max-salvajes-de-la-autopista-ver-online-hd.html",
                title = "Mad Max: Salvajes de la autopista (1979 - PelisPlus)",
                pageUrl = "https://pelisplus.hair/peliculas/8551-mad-max-salvajes-de-la-autopista-ver-online-hd.html",
                headers = mapOf("Referer" to "https://pelisplus.hair/"),
                timestamp = now + 120_000_000L,
                isFavorite = true
            ),
            HistoryItem(
                url = "https://pelisplus.hair/peliculas/8536-mad-max-2-el-guerrero-de-la-carretera-ver-online-hd.html",
                title = "Mad Max 2: El guerrero de la carretera (PelisPlus)",
                pageUrl = "https://pelisplus.hair/peliculas/8536-mad-max-2-el-guerrero-de-la-carretera-ver-online-hd.html",
                headers = mapOf("Referer" to "https://pelisplus.hair/"),
                timestamp = now + 100_000_000L,
                isFavorite = true
            ),
            HistoryItem(
                url = "https://pelisgo.online/pelicula/spider-man-un-nuevo-dia",
                title = "Spider-Man: Un nuevo día (PelisGO)",
                pageUrl = "https://pelisgo.online/pelicula/spider-man-un-nuevo-dia",
                headers = mapOf("Referer" to "https://pelisgo.online/"),
                timestamp = now + 80_000_000L,
                isFavorite = true
            ),
            HistoryItem(
                url = "https://reypelis.net/pelicula/bichos-una-aventura-en-miniatura/",
                title = "Bichos: Una Aventura en Miniatura (Reypelis)",
                pageUrl = "https://reypelis.net/pelicula/bichos-una-aventura-en-miniatura/",
                headers = mapOf("Referer" to "https://reypelis.net/"),
                timestamp = now + 70_000_000L
            ),
            HistoryItem(
                url = "https://reypelis.net/pelicula/mad-max-furia-en-la-carretera/",
                title = "Mad Max: Furia en la carretera (Reypelis)",
                pageUrl = "https://reypelis.net/pelicula/mad-max-furia-en-la-carretera/",
                headers = mapOf("Referer" to "https://reypelis.net/"),
                timestamp = now + 60_000_000L
            ),
            HistoryItem(
                url = "https://reypelis.net/pelicula/ms1-maxima-seguridad/",
                title = "MS1: Máxima Seguridad (Reypelis)",
                pageUrl = "https://reypelis.net/pelicula/ms1-maxima-seguridad/",
                headers = mapOf("Referer" to "https://reypelis.net/"),
                timestamp = now + 50_000_000L
            ),
            HistoryItem(
                url = "https://pelis182.net/sisu-camino-a-la-venganza/",
                title = "Sisu: Camino a la venganza (Pelis182)",
                pageUrl = "https://pelis182.net/sisu-camino-a-la-venganza/",
                headers = mapOf("Referer" to "https://pelis182.net/"),
                timestamp = now + 40_000_000L
            ),
            HistoryItem(
                url = "https://pelismart.mov/pelicula/the-senior-class",
                title = "The Senior Class (Pelismart)",
                pageUrl = "https://pelismart.mov/pelicula/the-senior-class",
                headers = mapOf("Referer" to "https://pelismart.mov/"),
                timestamp = now + 30_000_000L
            ),
            HistoryItem(
                url = "https://wcm.cuevana3.nu/ver-el-episodio/118048/episodio-3-de-amia-el-fin-de-la-verdad-temporada-1/",
                title = "AMIA: El fin de la verdad (Cuevana3)",
                pageUrl = "https://wcm.cuevana3.nu/ver-el-episodio/118048/episodio-3-de-amia-el-fin-de-la-verdad-temporada-1/",
                headers = mapOf("Referer" to "https://wcm.cuevana3.nu/"),
                timestamp = now + 25_000_000L
            ),
            HistoryItem(
                url = "https://repelisplus.casa/vod-movie/v1/el-senor-de-los-anillos-las-dos-torres.mp4",
                title = "El Señor de los Anillos: Las Dos Torres (MP4)",
                pageUrl = "https://repelisplus.casa/",
                headers = mapOf("Referer" to "https://repelisplus.casa/"),
                timestamp = now + 20_000_000L
            ),
            HistoryItem(
                url = "https://www.canela.tv/movies",
                title = "Canela.tv Películas (Streaming)",
                pageUrl = "https://www.canela.tv/movies",
                headers = mapOf("Referer" to "https://www.canela.tv/"),
                timestamp = now + 15_000_000L
            ),
            HistoryItem(
                url = "https://www.808ball15.com/football/2962655-banfield-vs-rosario-central.html",
                title = "Banfield vs Rosario Central (808ball Fútbol)",
                pageUrl = "https://www.808ball15.com/football/2962655-banfield-vs-rosario-central.html",
                headers = mapOf("Referer" to "https://www.808ball15.com/"),
                timestamp = now + 10_000_000L
            ),
            HistoryItem(
                url = "https://www.808ball15.com/football/3011883-depindependiente-medellin-vs-independiente-santa-fe.html",
                title = "DIM vs Santa Fe (808ball Fútbol)",
                pageUrl = "https://www.808ball15.com/football/3011883-depindependiente-medellin-vs-independiente-santa-fe.html",
                headers = mapOf("Referer" to "https://www.808ball15.com/"),
                timestamp = now + 8_000_000L
            ),
            HistoryItem(
                url = "https://ok.ru/video/7330074135206",
                title = "Película en OK.ru",
                pageUrl = "https://ok.ru/video/7330074135206",
                headers = mapOf("Referer" to "https://ok.ru/"),
                timestamp = now + 5_000_000L
            ),
            HistoryItem(
                url = "https://www.dailymotion.com/video/x5ui2hp",
                title = "Video en Dailymotion",
                pageUrl = "https://www.dailymotion.com/video/x5ui2hp",
                headers = mapOf("Referer" to "https://www.dailymotion.com/"),
                timestamp = now + 3_000_000L
            ),
            HistoryItem(
                url = "https://www.youtube.com/watch?v=fJ9rUzIMcZQ",
                title = "Trailer Oficial en YouTube",
                pageUrl = "https://www.youtube.com/watch?v=fJ9rUzIMcZQ",
                headers = mapOf("Referer" to "https://www.youtube.com/"),
                timestamp = now + 1_000_000L
            )
        )
    }

    fun addOrUpdate(url: String, title: String, pageUrl: String? = null, headers: Map<String, String> = emptyMap(), positionMs: Long = 0L, durationMs: Long = 0L, requiresProxy: Boolean = false) {
        if (url.isBlank() || url.contains("stub.mp4") || url.contains("creative")) return
        val current = getHistory().toMutableList()
        val cleanTitle = title.ifBlank { "Película / Video" }
        val existing = current.firstOrNull { it.url == url || (cleanTitle != "Película / Video" && cleanTitle != "Dailymotion" && it.title == cleanTitle) }
        val isFav = existing?.isFavorite ?: false
        val isProx = requiresProxy || (existing?.requiresProxy ?: false) || url.contains(":8092")
        val finalPos = if (positionMs > 0L) positionMs else (existing?.lastPositionMs ?: 0L)
        val finalDur = if (durationMs > 0L) durationMs else (existing?.durationMs ?: 0L)
        val effectivePageUrl = pageUrl?.takeIf { it.isNotBlank() } ?: existing?.pageUrl ?: headers["Referer"]?.takeIf { it.isNotBlank() }
        current.add(0, HistoryItem(url, cleanTitle, effectivePageUrl, if (headers.isNotEmpty()) headers else (existing?.headers ?: emptyMap()), System.currentTimeMillis(), finalPos, finalDur, isFavorite = isFav, requiresProxy = isProx))
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
