package com.omnicast.sender.history

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class NavigationStep(
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Gestor local de rutas de navegación (breadcrumbs) en el dispositivo móvil.
 * Registra el historial de navegación hacia películas 100% offline para ahorrar cuota en Supabase.
 */
object NavigationTrackManager {
    private const val PREFS_NAME = "omnicast_nav_tracks_prefs"
    private const val KEY_TRACKS = "nav_tracks_json"
    private const val MAX_STEPS = 40
    private val json = Json { ignoreUnknownKeys = true }
    private val sessionSteps = mutableListOf<NavigationStep>()

    fun recordStep(url: String, title: String) {
        if (url.isBlank() || url.startsWith("about:") || url.contains("favicon")) return
        synchronized(sessionSteps) {
            val last = sessionSteps.lastOrNull()
            if (last?.url == url) return
            val cleanTitle = if (title.isNotBlank() && title != "Cargando..." && title != "Transmisión") title else url
            sessionSteps.add(NavigationStep(url, cleanTitle))
            if (sessionSteps.size > MAX_STEPS) sessionSteps.removeAt(0)
        }
    }

    fun saveTrackLocally(context: Context, movieUrl: String, movieTitle: String) {
        try {
            val stepsCopy: List<NavigationStep>
            synchronized(sessionSteps) { stepsCopy = sessionSteps.toList() }
            if (stepsCopy.isEmpty()) return
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_TRACKS, null)
            val currentTracks = if (!raw.isNullOrBlank()) {
                runCatching { json.decodeFromString<MutableMap<String, List<NavigationStep>>>(raw) }.getOrDefault(mutableMapOf())
            } else mutableMapOf()
            val key = movieTitle.ifBlank { movieUrl }
            currentTracks[key] = stepsCopy
            prefs.edit().putString(KEY_TRACKS, json.encodeToString(currentTracks)).apply()
            android.util.Log.d("NavigationTrack", "📍 Ruta guardada localmente para '$key' (${stepsCopy.size} pasos)")
        } catch (_: Throwable) {}
    }

    private const val KEY_PERSISTENT_STACK = "browser_persistent_stack"
    private const val KEY_STACK_INDEX = "browser_stack_index"

    fun pushBrowserHistory(context: Context, url: String, title: String) {
        if (url.isBlank() || url.startsWith("about:") || url.contains("favicon")) return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_PERSISTENT_STACK, null)
            val stack = if (!raw.isNullOrBlank()) {
                runCatching { json.decodeFromString<MutableList<NavigationStep>>(raw) }.getOrDefault(mutableListOf())
            } else mutableListOf()
            val curIdx = prefs.getInt(KEY_STACK_INDEX, -1)
            val newStack = if (curIdx in 0 until stack.size - 1) {
                stack.subList(0, curIdx + 1).toMutableList()
            } else stack

            if (newStack.lastOrNull()?.url != url) {
                val cleanTitle = if (title.isNotBlank() && title != "Cargando..." && title != "Transmisión") title else url
                newStack.add(NavigationStep(url, cleanTitle))
                if (newStack.size > MAX_STEPS) newStack.removeAt(0)
            }
            prefs.edit()
                .putString(KEY_PERSISTENT_STACK, json.encodeToString(newStack))
                .putInt(KEY_STACK_INDEX, newStack.size - 1)
                .apply()
        } catch (_: Throwable) {}
    }

    fun popPreviousBrowserUrl(context: Context): String? {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_PERSISTENT_STACK, null) ?: return null
            val stack = runCatching { json.decodeFromString<List<NavigationStep>>(raw) }.getOrNull() ?: return null
            val curIdx = prefs.getInt(KEY_STACK_INDEX, stack.size - 1)
            if (curIdx > 0 && curIdx - 1 < stack.size) {
                val newIdx = curIdx - 1
                prefs.edit().putInt(KEY_STACK_INDEX, newIdx).apply()
                return stack[newIdx].url
            }
        } catch (_: Throwable) {}
        return null
    }

    fun canGoBackPersistent(context: Context): Boolean {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val curIdx = prefs.getInt(KEY_STACK_INDEX, -1)
            return curIdx > 0
        } catch (_: Throwable) { return false }
    }

    fun getTrackFor(context: Context, key: String): List<NavigationStep> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_TRACKS, null) ?: return emptyList()
            val tracks = json.decodeFromString<Map<String, List<NavigationStep>>>(raw)
            tracks[key] ?: emptyList()
        } catch (_: Throwable) { emptyList() }
    }
}
