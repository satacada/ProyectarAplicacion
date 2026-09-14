package com.omnicast.receiver.directstream

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

/**
 * Gestor de reproducción multimedia nativa en la TV Box.
 * Configurado con buffer adaptativo de 30-60s (LoadControl) y decodificación por hardware de Web Video Caster.
 */
class ExoPlayerTvManager(private val context: Context) {

    companion object {
        private const val TAG = "ExoPlayerTvManager"
        private const val DEFAULT_UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    var player: ExoPlayer? = null
        private set

    private var currentUrl: String? = null
    private var currentTitle: String? = null
    private var isRetryingClean = false

    @OptIn(UnstableApi::class)
    fun initialize() {
        if (player == null) {
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(30_000, 60_000, 1_500, 3_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(10_000, true)
                .build()

            val renderersFactory = DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                .setEnableDecoderFallback(true)

            val trackSelector = DefaultTrackSelector(context).apply {
                parameters = buildUponParameters().setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true).build()
            }

            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(DEFAULT_UA).setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(20000).setReadTimeoutMs(20000)

            val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(httpFactory)

            player = ExoPlayer.Builder(context, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .build().apply {
                    trackSelectionParameters = trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true).build()
                    playWhenReady = true
                    addListener(object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "❌ Error en ExoPlayer: ${error.errorCodeName} (${error.errorCode}): ${error.message}", error)
                            if (!isRetryingClean && currentUrl != null) {
                                com.omnicast.receiver.log.ReceiverLogger.log(TAG, "🔄 Reintentando con flujo limpio directo sin cabeceras...")
                                isRetryingClean = true
                                playClean(currentUrl!!, currentTitle)
                            }
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            val stateName = when (playbackState) {
                                Player.STATE_READY -> "LISTO / REPRODUCIENDO"
                                Player.STATE_BUFFERING -> "BUFFERING"
                                Player.STATE_ENDED -> "FINALIZADO"
                                Player.STATE_IDLE -> "IDLE"
                                else -> "UNKNOWN($playbackState)"
                            }
                            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "Estado ExoPlayer: $stateName")
                        }
                    })
                }
        }
    }

    @OptIn(UnstableApi::class)
    fun playStream(url: String, headers: Map<String, String> = emptyMap(), title: String? = null) {
        if (url.contains("stub.mp4", ignoreCase = true) || url.contains("dummy.mp4", ignoreCase = true)) {
            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "⚠️ Ignorando video dummy/placeholder: $url")
            return
        }
        initialize()
        currentUrl = url
        currentTitle = title
        isRetryingClean = false

        val safeHeaders = filterSafeHeaders(url, headers)
        val mediaItem = buildMediaItem(url, title)

        if (safeHeaders.isEmpty()) {
            playClean(url, title)
            return
        }

        try {
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(safeHeaders["User-Agent"] ?: DEFAULT_UA)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)
                .setDefaultRequestProperties(safeHeaders)

            val mediaSource = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(httpFactory)
                .createMediaSource(mediaItem)

            player?.apply {
                stop()
                clearMediaItems()
                setMediaSource(mediaSource)
                seekToDefaultPosition()
                prepare()
                play()
            }
            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "Reproduciendo con cabeceras (${safeHeaders.keys.joinToString()}): $url")
        } catch (e: Throwable) {
            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "Error con cabeceras, fallback a limpio", e)
            playClean(url, title)
        }
    }

    fun playClean(url: String, title: String? = null) {
        if (url.contains("stub.mp4", ignoreCase = true) || url.contains("dummy.mp4", ignoreCase = true)) return
        val mediaItem = buildMediaItem(url, title)
        player?.apply {
            stop()
            clearMediaItems()
            setMediaItem(mediaItem)
            seekToDefaultPosition()
            prepare()
            play()
        }
        com.omnicast.receiver.log.ReceiverLogger.log(TAG, "Reproduciendo flujo directo limpio: $url")
    }

    private fun buildMediaItem(url: String, title: String?): MediaItem {
        val metadata = MediaMetadata.Builder().setTitle(title ?: "Transmisión Web").setDisplayTitle(title ?: "Transmisión Web").build()
        val builder = MediaItem.Builder().setUri(url).setMediaMetadata(metadata)
        val lower = url.lowercase()
        when {
            lower.contains(".m3u8") || lower.contains("m3u8") || lower.contains("/hls/") || lower.contains("type=4") -> builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
            lower.contains(".mpd") || lower.contains("dash") -> builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
            lower.contains(".webm") -> builder.setMimeType(androidx.media3.common.MimeTypes.VIDEO_WEBM)
            else -> builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MP4)
        }
        return builder.build()
    }

    private fun filterSafeHeaders(url: String, headers: Map<String, String>): Map<String, String> {
        val allowed = mutableMapOf<String, String>()
        headers.forEach { (k, v) ->
            when {
                k.equals("referer", true) -> allowed["Referer"] = v
                k.equals("origin", true) -> allowed["Origin"] = v
                k.equals("user-agent", true) -> allowed["User-Agent"] = v
                k.equals("cookie", true) -> allowed["Cookie"] = v
                k.equals("authorization", true) -> allowed["Authorization"] = v
            }
        }
        if (!allowed.containsKey("User-Agent")) allowed["User-Agent"] = DEFAULT_UA
        if (url.contains("mycdn.me", true) || url.contains("ok.ru", true)) allowed["Referer"] = "https://ok.ru/"
        else if (url.contains("mail.ru", true)) allowed.remove("Referer")
        return allowed
    }

    fun seekTo(positionMs: Long) { player?.seekTo(positionMs) }
    fun pause() { player?.pause() }
    fun resume() { player?.play() }
    fun setVolume(vol: Float) { player?.volume = vol.coerceIn(0f, 1f) }
    fun toggleSubtitles() {
        val p = player ?: return
        val disabled = p.trackSelectionParameters.disabledTrackTypes.contains(androidx.media3.common.C.TRACK_TYPE_TEXT)
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, !disabled)
            .setPreferredTextLanguage(if (disabled) "es" else null).build()
    }
    val currentPositionMs: Long get() = player?.currentPosition?.coerceAtLeast(0L) ?: 0L
    val durationMs: Long get() = player?.duration?.coerceAtLeast(0L) ?: 0L
    val isPlaying: Boolean get() = player?.isPlaying ?: false

    fun stop() { player?.stop(); player?.clearMediaItems() }
    fun release() { stop(); player?.release(); player = null }
}
