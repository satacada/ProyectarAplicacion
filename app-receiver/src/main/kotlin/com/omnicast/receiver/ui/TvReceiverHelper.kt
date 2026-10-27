package com.omnicast.receiver.ui

import android.content.Context
import android.media.AudioManager
import com.omnicast.receiver.directstream.ExoPlayerTvManager
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Utilidades auxiliares para TvReceiverActivity desacopladas para mantener el archivo <= 300 líneas.
 */
object TvReceiverHelper {

    fun getLocalIpAddress(): String = try {
        NetworkInterface.getNetworkInterfaces()?.toList()?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address && it.hostAddress != "127.0.0.1" }?.hostAddress ?: "127.0.0.1"
    } catch (_: Throwable) { "127.0.0.1" }

    fun extractYouTubeId(url: String): String? =
        Regex("(?:watch\\?v=|shorts/|youtu\\.be/|embed/)([a-zA-Z0-9_-]{11})").find(url)?.groupValues?.get(1)

    fun applySystemAndPlayerVolume(context: Context, exoPlayerManager: ExoPlayerTvManager, vol: Float) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = (vol.coerceIn(0f, 1f) * maxVol).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
            }
        } catch (_: Throwable) {}
        exoPlayerManager.setVolume(vol)
    }

    fun shouldUseWebPlayer(url: String, headers: Map<String, String>): Boolean {
        val target = headers["X-Target-Player"]?.uppercase()
        if (target == "WEBVIEW" || target == "CHROMIUM_PURE" || target == "CHROMIUM_ANTI_CORS") return true
        if (target == "EXOPLAYER") return false

        val lowerUrl = url.lowercase()
        val isDash = lowerUrl.contains(".mpd") || lowerUrl.contains("format=dash") ||
                     ((lowerUrl.contains("okcdn.ru") || lowerUrl.contains("mycdn.me")) && (lowerUrl.contains("ct=6") || !lowerUrl.contains(".m3u8")))
        if (isDash) return false

        val ref = headers["Referer"]?.lowercase() ?: ""
        val isProtectedEmbed = ref.contains("dr0pstream.com") || ref.contains("dropcdn.io") ||
                               ref.contains("fastream.to") || ref.contains("luluvdo.com") ||
                               ref.contains("lulustream.com") || ref.contains("pelisgo") ||
                               ref.contains("vk.com") ||
                               lowerUrl.contains("dropcdn.io") || lowerUrl.contains("dr0pstream.com") ||
                               lowerUrl.contains("fastream.to")
        val hasSessionCookies = !headers["Cookie"].isNullOrBlank()
        val hasSignedToken = com.omnicast.core.domain.usecases.StreamManifestProbe.hasSignedSecurityToken(url)

        return isProtectedEmbed || hasSessionCookies || hasSignedToken
    }

    fun shouldUseAntiCorsInterceptor(url: String, headers: Map<String, String>): Boolean {
        val target = headers["X-Target-Player"]?.uppercase()
        if (target == "CHROMIUM_PURE") return false
        if (target == "CHROMIUM_ANTI_CORS") return true

        val lower = url.lowercase()
        val ref = headers["Referer"]?.lowercase() ?: ""
        val isDropOrFast = lower.contains("dropcdn.io") || lower.contains("dr0pstream.com") || lower.contains("fastream.to") ||
                           ref.contains("dr0pstream.com") || ref.contains("dropcdn.io") || ref.contains("fastream.to")
        if (isDropOrFast) return false
        return true
    }
}
