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
}
