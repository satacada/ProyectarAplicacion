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
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class ExoPlayerTvManager(private val context: Context) {

    companion object {
        private const val TAG = "ExoPlayerTvManager"
        private const val DEFAULT_UA = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    var player: ExoPlayer? = null; private set
    var playerView: androidx.media3.ui.PlayerView? = null
    var trackManager: TvTrackManager? = null; private set
    var onTracksChanged: ((com.omnicast.core.protocol.SignalingMessage.TracksInfo) -> Unit)? = null
    var onPlaybackFailed: ((url: String?, title: String?) -> Unit)? = null
    val recoveryManager = PlaybackRecoveryManager()
    val aspectRatioHelper = TvAspectRatioHelper()
    var activeBranch: com.omnicast.core.domain.models.MediaBranch = com.omnicast.core.domain.models.MediaBranch.VOD_HLS_MP4; private set
    private var currentUrl: String? = null
    private var currentTitle: String? = null
    private var currentHeaders: Map<String, String> = emptyMap()
    private var isRetryingClean = false
    private val ioScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    fun initialize(branch: com.omnicast.core.domain.models.MediaBranch = com.omnicast.core.domain.models.MediaBranch.VOD_HLS_MP4) {
        if (player != null && activeBranch != branch) {
            player?.release(); player = null
        }
        activeBranch = branch
        if (player == null) {
            val loadControl = TvBranchPlayerConfig.createLoadControl(branch)
            val renderersFactory = DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                .setEnableDecoderFallback(true)

            val trackSelector = DefaultTrackSelector(context).apply {
                parameters = buildUponParameters()
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
                    .setForceHighestSupportedBitrate(true)
                    .build()
            }
            TvBranchPlayerConfig.applyHardwareCodecSettings(trackSelector, branch)
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
                            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "❌ Error en ExoPlayer ($activeBranch): ${error.errorCodeName} (${error.errorCode}): ${error.message}", error)
                            if (TvBranchPlayerConfig.isLiveOrContinuous(activeBranch)) {
                                com.omnicast.receiver.log.ReceiverLogger.log(TAG, "🔄 Auto-reconexión silenciosa en vivo (live edge)")
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ player?.prepare(); player?.play() }, 800)
                                return
                            }
                            val targetUrl = currentUrl
                            val triggerSelfHeal: () -> Unit = {
                                if (!isRetryingClean && targetUrl != null) {
                                    isRetryingClean = true
                                    ioScope.launch {
                                        val probe = com.omnicast.core.domain.usecases.StreamManifestProbe.probeAndHeal(targetUrl, currentHeaders)
                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                            if (probe.isPlayable) {
                                                com.omnicast.receiver.log.ReceiverLogger.log(TAG, "✅ HeaderSelfHealer recuperó el flujo (live=${probe.isLiveStream})")
                                                val healedBranch = if (probe.isLiveStream) com.omnicast.core.domain.models.MediaBranch.LIVE_HLS else activeBranch
                                                playStream(probe.resolvedUrl, probe.healedHeaders, currentTitle, if (probe.isLiveStream) 0L else currentPositionMs, healedBranch)
                                                isRetryingClean = true
                                            } else {
                                                onPlaybackFailed?.invoke(targetUrl, currentTitle)
                                            }
                                        }
                                    }
                                } else if (isRetryingClean) {
                                    onPlaybackFailed?.invoke(targetUrl, currentTitle)
                                }
                            }
                            val handled = recoveryManager.handlePlaybackError(error, player) { triggerSelfHeal() }
                            if (!handled) triggerSelfHeal()
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_READY) {
                                recoveryManager.onPlaybackReady()
                            }
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
    fun playStream(url: String, headers: Map<String, String> = emptyMap(), title: String? = null, startPositionMs: Long = 0L, branch: com.omnicast.core.domain.models.MediaBranch = com.omnicast.core.domain.models.MediaBranch.VOD_HLS_MP4) {
        if (url.contains("stub.mp4", ignoreCase = true) || url.contains("dummy.mp4", ignoreCase = true)) {
            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "⚠️ Ignorando video dummy/placeholder: $url")
            return
        }
        initialize(branch)
        val cleanUrl = com.omnicast.core.domain.usecases.StreamManifestProbe.sanitizeMediaUrl(url)
        currentUrl = cleanUrl
        currentTitle = title
        currentHeaders = headers
        isRetryingClean = false

        val safeHeaders = filterSafeHeaders(cleanUrl, headers)
        val mediaItem = buildMediaItem(cleanUrl, title, branch)

        if (safeHeaders.isEmpty()) {
            playClean(cleanUrl, title, startPositionMs, branch)
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

    fun playClean(url: String, title: String? = null, startPositionMs: Long = 0L, branch: com.omnicast.core.domain.models.MediaBranch = activeBranch) {
        if (url.contains("stub.mp4", ignoreCase = true) || url.contains("dummy.mp4", ignoreCase = true)) return
        initialize(branch)
        val cleanUrl = com.omnicast.core.domain.usecases.StreamManifestProbe.sanitizeMediaUrl(url)
        runCatching {
            val mediaItem = buildMediaItem(cleanUrl, title, branch)
            val safe = filterSafeHeaders(cleanUrl, emptyMap())
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(safe["User-Agent"] ?: DEFAULT_UA).setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(15000).setReadTimeoutMs(15000)
            if (safe.isNotEmpty()) httpFactory.setDefaultRequestProperties(safe)
            val mediaSource = DefaultMediaSourceFactory(context).setDataSourceFactory(httpFactory).createMediaSource(mediaItem)
            player?.apply {
                stop(); clearMediaItems(); setMediaSource(mediaSource)
                if (startPositionMs > 0L) seekTo(startPositionMs) else seekToDefaultPosition()
                prepare(); play()
            }
            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "Reproduciendo flujo limpio ($branch): $url (pos=${startPositionMs}ms)")
        }.onFailure { err ->
            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "Error crítico reproduciendo flujo limpio: ${err.message}", err)
        }
    }

    private fun buildMediaItem(url: String, title: String?, branch: com.omnicast.core.domain.models.MediaBranch = activeBranch): MediaItem {
        val metadata = MediaMetadata.Builder().setTitle(title ?: "Transmisión Web").setDisplayTitle(title ?: "Transmisión Web").build()
        val builder = MediaItem.Builder().setUri(url).setMediaMetadata(metadata)
        if (branch == com.omnicast.core.domain.models.MediaBranch.LIVE_HLS) {
            builder.setLiveConfiguration(MediaItem.LiveConfiguration.Builder().setMaxPlaybackSpeed(1.02f).setMinPlaybackSpeed(0.98f).build())
        }
        val lower = url.lowercase()
        val isOk = lower.contains("mycdn.me") || lower.contains("okcdn.ru")
        val isOkHls = isOk && (lower.contains("/hls") || lower.contains("m3u8") || lower.contains("format=hls"))
        val isTxtHls = (lower.contains(".txt") || lower.contains(".txt?")) && (lower.contains("/hls") || lower.contains("master") || lower.contains("index"))
        when {
            branch == com.omnicast.core.domain.models.MediaBranch.AUDIO_ONLY || lower.contains(".mp3") || lower.contains(".aac") ->
                builder.setMimeType(androidx.media3.common.MimeTypes.AUDIO_MPEG)
            branch == com.omnicast.core.domain.models.MediaBranch.DASH_MPD || lower.contains(".mpd") || lower.contains("dash") ->
                builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
            lower.contains(".m3u8") || lower.contains("m3u8") || lower.contains("/hls") || isOkHls || isTxtHls ->
                builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
            lower.contains(".webm") -> builder.setMimeType(androidx.media3.common.MimeTypes.VIDEO_WEBM)
            lower.contains(".mp4") || lower.contains("/mp4") || (isOk && !isOkHls) -> builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MP4)
        }
        return builder.build()
    }

    private fun filterSafeHeaders(@Suppress("UNUSED_PARAMETER") url: String, headers: Map<String, String>): Map<String, String> {
        val allowed = mutableMapOf<String, String>()
        headers.forEach { (k, v) ->
            if (v.isNotBlank()) {
                when {
                    k.equals("referer", true) -> allowed["Referer"] = v
                    k.equals("origin", true) -> allowed["Origin"] = v
                    k.equals("user-agent", true) -> allowed["User-Agent"] = v
                    k.equals("cookie", true) -> allowed["Cookie"] = v
                    k.equals("authorization", true) -> allowed["Authorization"] = v
                }
            }
        }
        if (!allowed.containsKey("User-Agent")) allowed["User-Agent"] = DEFAULT_UA
        val ref = allowed["Referer"]
        if (!ref.isNullOrBlank() && !allowed.containsKey("Origin")) {
            runCatching {
                val u = java.net.URI(ref)
                if (!u.scheme.isNullOrBlank() && !u.host.isNullOrBlank()) {
                    allowed["Origin"] = "${u.scheme}://${u.host}"
                }
            }
        }
        return allowed
    }

    var resizeMode: Int = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT; private set
    var onResizeModeChanged: ((Int) -> Unit)? = null
    var currentZoom: Float = 1.0f; private set
    var onZoomChanged: ((Float) -> Unit)? = null

    fun toggleResizeMode(): Int {
        resizeMode = aspectRatioHelper.cycleNextMode().resizeMode
        onResizeModeChanged?.invoke(resizeMode); return resizeMode
    }
    fun setResizeMode(isZoom169: Boolean) {
        resizeMode = if (isZoom169) androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM else androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        onResizeModeChanged?.invoke(resizeMode)
    }
    fun setDisplayMode(mode: String, zoom: Float = 1.0f) {
        currentZoom = zoom; val isZoom169 = mode == "FILL" || mode == "EXTEND_16_9"
        resizeMode = if (isZoom169) androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM else androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        onResizeModeChanged?.invoke(resizeMode); onZoomChanged?.invoke(zoom)
    }

    fun isNearEnd(): Boolean {
        val p = player ?: return false
        return p.playbackState == androidx.media3.common.Player.STATE_ENDED || (p.duration > 0L && p.currentPosition >= p.duration - 1500L)
    }

    fun seekTo(pos: Long) { player?.seekTo(pos) }
    fun pause() { player?.pause() }
    fun resume() {
        val p = player ?: return
        if (isNearEnd()) p.seekTo(0L)
        p.play()
    }
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
    fun stop(reason: String = "desconocido") {
        com.omnicast.receiver.log.ReceiverLogger.log(TAG, "⏹️ Deteniendo ExoPlayer (motivo: $reason)")
        player?.stop()
        player?.clearMediaItems()
    }
    fun release() { stop("release"); playerView = null; player?.release(); player = null; TvMediaCacheManager.release() }
}
