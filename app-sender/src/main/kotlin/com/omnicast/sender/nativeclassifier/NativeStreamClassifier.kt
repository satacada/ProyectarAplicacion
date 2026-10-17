package com.omnicast.sender.nativeclassifier

import android.util.Log
import com.omnicast.core.domain.models.MediaBranch

/**
 * Enlace JNI de ultra-baja latencia (<0.1 ms) con el motor C++20 libstream_classifier.so.
 * Cumple con los requerimientos de HU-009 y ADR-006.
 */
object NativeStreamClassifier {
    private const val TAG = "NativeClassifier"
    private var isLoaded = false

    init {
        try {
            System.loadLibrary("stream_classifier")
            isLoaded = true
            Log.i(TAG, "Motor nativo C++20 libstream_classifier.so cargado con éxito")
        } catch (e: Throwable) {
            Log.e(TAG, "No se pudo cargar libstream_classifier.so nativo: ${e.message}")
            isLoaded = false
        }
    }

    external fun classifyUrl(url: String, docUrl: String?): Int
    external fun isDiscardCandidate(url: String, durationSec: Float): Boolean
    external fun isTelemetryOrBeacon(url: String): Boolean

    fun classify(url: String, docUrl: String? = null): MediaBranch {
        if (!isLoaded || url.isBlank()) return fallbackClassify(url)
        return try {
            val code = classifyUrl(url, docUrl)
            MediaBranch.fromCode(code)
        } catch (e: Throwable) {
            fallbackClassify(url)
        }
    }

    fun shouldDiscard(url: String, durationSec: Float = 0.0f): Boolean {
        if (!isLoaded || url.isBlank()) return fallbackShouldDiscard(url, durationSec)
        return try {
            isDiscardCandidate(url, durationSec)
        } catch (e: Throwable) {
            fallbackShouldDiscard(url, durationSec)
        }
    }

    fun isTelemetry(url: String): Boolean {
        if (!isLoaded || url.isBlank()) return false
        return try {
            isTelemetryOrBeacon(url)
        } catch (_: Throwable) {
            false
        }
    }

    private fun fallbackClassify(url: String): MediaBranch {
        val lower = url.lowercase()
        return when {
            lower.contains(":8089") -> MediaBranch.LOCAL_MEDIA
            lower.contains(".mpd") || lower.contains("/dash/") -> MediaBranch.DASH_MPD
            lower.contains(".mp3") || lower.contains(".aac") || lower.contains(".ogg") -> MediaBranch.AUDIO_ONLY
            lower.contains("playlist.m3u") || lower.contains("/iptv") -> MediaBranch.IPTV_STREAM
            lower.contains("live") && lower.contains(".m3u8") -> MediaBranch.LIVE_HLS
            lower.contains(".m3u8") || lower.contains(".mp4") || lower.contains(".webm") -> MediaBranch.VOD_HLS_MP4
            else -> MediaBranch.DISCARD
        }
    }

    private fun fallbackShouldDiscard(url: String, durationSec: Float): Boolean {
        if (url.isBlank() || url.startsWith("blob:") || url.startsWith("data:")) return true
        val lower = url.lowercase()
        val staticExts = listOf(".html", ".css", ".js", ".json", ".png", ".jpg", ".webp", ".svg", ".ico", ".woff", ".woff2")
        val path = lower.substringBefore('?')
        if (staticExts.any { path.endsWith(it) }) return true
        if (lower.contains("telemetry") || lower.contains("analytics") || lower.contains("beacon")) return true
        if (durationSec in 0.1f..44.9f && (lower.contains("preroll") || lower.contains("vast"))) return true
        if (lower.contains(".ts?") || (path.endsWith(".ts") && !lower.contains(".m3u8"))) return true
        return false
    }
}
