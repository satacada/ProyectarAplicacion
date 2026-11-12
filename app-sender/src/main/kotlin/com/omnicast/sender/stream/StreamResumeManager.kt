package com.omnicast.sender.stream

import android.content.Context

/**
 * Gestor ligero para persistir y reanudar la última posición de reproducción de videos.
 */
object StreamResumeManager {

    private const val PREFS_NAME = "omnicast_playback_resume"
    private const val MIN_RESUME_MS = 5000L

    fun savePosition(context: Context, url: String?, posMs: Long, durationMs: Long = 0L, pageUrl: String? = null) {
        if (url.isNullOrBlank()) return
        if (durationMs > 0L && (posMs >= durationMs - 45_000L || (posMs.toDouble() / durationMs) >= 0.95)) {
            clearPosition(context, url, pageUrl)
            return
        }
        if (posMs < MIN_RESUME_MS) return
        val key = cleanKey(url, pageUrl)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(key, posMs)
            .apply()
    }

    fun getPosition(context: Context, url: String?, pageUrl: String? = null): Long {
        if (url.isNullOrBlank()) return 0L
        val key = cleanKey(url, pageUrl)
        val pos = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(key, 0L)
        return if (pos >= MIN_RESUME_MS) pos else 0L
    }

    fun clearPosition(context: Context, url: String?, pageUrl: String? = null) {
        if (url.isNullOrBlank()) return
        val key = cleanKey(url, pageUrl)
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

    private fun cleanKey(url: String, pageUrl: String? = null): String {
        val uri = try { android.net.Uri.parse(url) } catch (_: Throwable) { null }
        val videoId = uri?.getQueryParameter("id") ?: uri?.getQueryParameter("v") ?: uri?.getQueryParameter("mid")
        var base = if (!videoId.isNullOrBlank() && uri != null) {
            "${uri.scheme}://${uri.host}${uri.path}?id=$videoId"
        } else {
            url.substringBefore('?')
        }
        if (!pageUrl.isNullOrBlank()) {
            val pagePath = try { android.net.Uri.parse(pageUrl).path?.lowercase() } catch (_: Throwable) { null }
            if (pagePath != null && (pagePath.contains("episod") || pagePath.contains("capitul") || pagePath.contains("season") || pagePath.contains("temporad") || pagePath.contains("serie"))) {
                base += "@$pagePath"
            }
        }
        return "pos_" + base.hashCode()
    }
}
