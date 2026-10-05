package com.omnicast.sender.stream

import android.content.Context

/**
 * Gestor ligero para persistir y reanudar la última posición de reproducción de videos.
 */
object StreamResumeManager {

    private const val PREFS_NAME = "omnicast_playback_resume"
    private const val MIN_RESUME_MS = 5000L

    fun savePosition(context: Context, url: String?, posMs: Long) {
        if (url.isNullOrBlank() || posMs < MIN_RESUME_MS) return
        val key = cleanKey(url)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(key, posMs)
            .apply()
    }

    fun getPosition(context: Context, url: String?): Long {
        if (url.isNullOrBlank()) return 0L
        val key = cleanKey(url)
        val pos = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(key, 0L)
        return if (pos >= MIN_RESUME_MS) pos else 0L
    }

    fun clearPosition(context: Context, url: String?) {
        if (url.isNullOrBlank()) return
        val key = cleanKey(url)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(key)
            .apply()
    }

    fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return "%02d:%02d".format(m, s)
    }

    private fun cleanKey(url: String): String {
        val uri = try { android.net.Uri.parse(url) } catch (_: Throwable) { null }
        val videoId = uri?.getQueryParameter("id") ?: uri?.getQueryParameter("v") ?: uri?.getQueryParameter("mid")
        val base = if (!videoId.isNullOrBlank() && uri != null) {
            "${uri.scheme}://${uri.host}${uri.path}?id=$videoId"
        } else {
            url.substringBefore('?')
        }
        return "pos_" + base.hashCode()
    }
}
