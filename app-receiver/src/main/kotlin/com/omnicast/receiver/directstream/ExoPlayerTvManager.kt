package com.omnicast.receiver.directstream

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

class ExoPlayerTvManager(private val context: Context) {

    companion object {
        private const val TAG = "ExoPlayerTvManager"
        private const val DEFAULT_UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    var player: ExoPlayer? = null; private set
    var trackManager: TvTrackManager? = null; private set
    var onTracksChanged: ((com.omnicast.core.protocol.SignalingMessage.TracksInfo) -> Unit)? = null
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
            val tm = TvTrackManager(context, trackSelector)
            trackManager = tm

            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(DEFAULT_UA).setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(20000).setReadTimeoutMs(20000)

            val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(httpFactory)

            player = ExoPlayer.Builder(context, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .build().apply {
                    playWhenReady = true
                    addListener(object : Player.Listener {
                        override fun onTracksChanged(tracks: Tracks) {
                            val info = tm.getTracksInfo(this@apply)
                            onTracksChanged?.invoke(info)
                        }
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
    fun playStream(url: String, headers: Map<String, String> = emptyMap(), title: String? = null, startPositionMs: Long = 0L) {
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
            playClean(url, title, startPositionMs)
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
                if (startPositionMs > 0L) seekTo(startPositionMs) else seekToDefaultPosition()
                prepare()
                play()
            }
            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "Reproduciendo con cabeceras (${safeHeaders.keys.joinToString()}): $url (pos=${startPositionMs}ms)")
        } catch (e: Throwable) {
            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "Error con cabeceras, fallback a limpio", e)
            playClean(url, title, startPositionMs)
        }
    }

    fun playClean(url: String, title: String? = null, startPositionMs: Long = 0L) {
        if (url.contains("stub.mp4", ignoreCase = true) || url.contains("dummy.mp4", ignoreCase = true)) return
        val mediaItem = buildMediaItem(url, title)
        player?.apply {
            stop()
            clearMediaItems()
            setMediaItem(mediaItem)
            if (startPositionMs > 0L) seekTo(startPositionMs) else seekToDefaultPosition()
            prepare()
            play()
        }
        com.omnicast.receiver.log.ReceiverLogger.log(TAG, "Reproduciendo flujo directo limpio: $url (pos=${startPositionMs}ms)")
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
        return allowed
    }

    var resizeMode: Int = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT; private set
    var onResizeModeChanged: ((Int) -> Unit)? = null
    var currentZoom: Float = 1.0f; private set
    var onZoomChanged: ((Float) -> Unit)? = null

    fun toggleResizeMode(): Int {
        resizeMode = if (resizeMode == androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        } else {
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
        onResizeModeChanged?.invoke(resizeMode)
        return resizeMode
    }

    fun setResizeMode(isZoom169: Boolean) {
        resizeMode = if (isZoom169) androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM else androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        onResizeModeChanged?.invoke(resizeMode)
    }

    fun setDisplayMode(mode: String, zoom: Float = 1.0f) {
        currentZoom = zoom
        val isZoom169 = mode == "FILL" || mode == "EXTEND_16_9"
        resizeMode = if (isZoom169) androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM else androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        onResizeModeChanged?.invoke(resizeMode)
        onZoomChanged?.invoke(zoom)
    }

    fun seekTo(pos: Long) { player?.seekTo(pos) }
    fun pause() { player?.pause() }
    fun resume() { player?.play() }
    fun setVolume(vol: Float) { player?.volume = vol.coerceIn(0f, 1f) }
    fun toggleSubtitles(enable: Boolean? = null): Boolean {
        val p = player ?: return false
        val st = trackManager?.toggleSubtitles(enable) ?: false
        trackManager?.getTracksInfo(p)?.let { onTracksChanged?.invoke(it) }
        return st
    }
    fun selectTextTrack(idx: Int) { player?.let { trackManager?.selectTextTrack(it, idx); trackManager?.getTracksInfo(it)?.let { info -> onTracksChanged?.invoke(info) } } }
    fun selectAudioTrack(idx: Int) { player?.let { trackManager?.selectAudioTrack(it, idx); trackManager?.getTracksInfo(it)?.let { info -> onTracksChanged?.invoke(info) } } }
    val currentPositionMs: Long get() = player?.currentPosition?.coerceAtLeast(0L) ?: 0L
    val durationMs: Long get() = player?.duration?.coerceAtLeast(0L) ?: 0L
    val isPlaying: Boolean get() = player?.isPlaying ?: false
    fun stop() { player?.stop(); player?.clearMediaItems() }
    fun release() { stop(); player?.release(); player = null }
}
