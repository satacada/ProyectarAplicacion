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
    var currentHeaders: Map<String, String> = emptyMap(); private set
    private var isRetryingClean = false
    private var fallbackMime: String? = null
    private var activeMimeType: String? = null
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
                .setUserAgent(TvMediaItemBuilder.DEFAULT_UA).setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(20000).setReadTimeoutMs(20000)
            val dsFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(dsFactory)

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
                            val code = error.errorCode
                            val httpCode = (error.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)?.responseCode ?: -1
                            val errDetails = mapOf(
                                "stream_url" to (currentUrl ?: ""),
                                "target_player" to "EXOPLAYER",
                                "branch" to activeBranch.name,
                                "error_code" to code,
                                "error_name" to error.errorCodeName,
                                "http_code" to httpCode,
                                "headers_sent" to currentHeaders.keys.joinToString(",")
                            )
                            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "❌ Error en ExoPlayer ($activeBranch): ${error.errorCodeName} ($code): ${error.message}", error, errDetails)
                            if (TvBranchPlayerConfig.isLiveOrContinuous(activeBranch)) {
                                com.omnicast.receiver.log.ReceiverLogger.log(TAG, "🔄 Auto-reconexión silenciosa en vivo (live edge)")
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ player?.seekToDefaultPosition(); player?.prepare(); player?.play() }, 500)
                                return
                            }
                            // Auto-reparación proactiva: Si falla el parser de manifiesto (ej. era MP4 sin extensión o HLS en .txt)
                            if ((code == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED || code == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED) && fallbackMime == null && currentUrl != null) {
                                val alt = if (activeMimeType == androidx.media3.common.MimeTypes.APPLICATION_M3U8) androidx.media3.common.MimeTypes.APPLICATION_MP4 else androidx.media3.common.MimeTypes.APPLICATION_M3U8
                                fallbackMime = alt
                                com.omnicast.receiver.log.ReceiverLogger.log(TAG, "🔄 Auto-reparación al vuelo: Formato erróneo, conmutando a $alt")
                                playStream(currentUrl!!, currentHeaders, currentTitle, currentPositionMs, activeBranch)
                                return
                            }
                            val targetUrl = currentUrl
                            val triggerSelfHeal: () -> Unit = {
                                if (!isRetryingClean && targetUrl != null) {
                                    isRetryingClean = true
                                    ioScope.launch {
                                        val probe = com.omnicast.core.domain.usecases.StreamManifestProbe.probeAndHeal(targetUrl, currentHeaders, isHealingAttempt = true)
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
                                hasPlaybackStarted = true
                                recoveryManager.onPlaybackReady()
                                fallbackMime = null
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
    fun playStream(
        url: String,
        headers: Map<String, String> = emptyMap(),
        title: String? = null,
        startPositionMs: Long = 0L,
        branch: com.omnicast.core.domain.models.MediaBranch = com.omnicast.core.domain.models.MediaBranch.VOD_HLS_MP4,
        subtitles: List<com.omnicast.core.protocol.SignalingMessage.SubtitleTrack> = emptyList()
    ) {
        if (url.contains("stub.mp4", ignoreCase = true) || url.contains("dummy.mp4", ignoreCase = true)) {
            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "⚠️ Ignorando video dummy/placeholder: $url")
            return
        }
        if (currentUrl != url) fallbackMime = null
        initialize(branch)
        val effectiveUrl = com.omnicast.core.domain.usecases.HlsMasterResolver.deriveMasterPlaylistUrl(url) ?: url
        currentUrl = effectiveUrl
        currentTitle = title
        currentHeaders = headers
        isRetryingClean = false
        hasPlaybackStarted = false

        val safeHeaders = TvMediaItemBuilder.filterSafeHeaders(effectiveUrl, headers)
        activeMimeType = TvMediaItemBuilder.inferTargetMime(effectiveUrl, branch, fallbackMime)
        val mediaItem = TvMediaItemBuilder.buildMediaItem(effectiveUrl, title, branch, subtitles, fallbackMime)

        if (safeHeaders.isEmpty() || effectiveUrl.startsWith("/") || effectiveUrl.startsWith("file://")) {
            playClean(effectiveUrl, title, startPositionMs, branch, subtitles)
            return
        }

        try {
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(safeHeaders["User-Agent"] ?: TvMediaItemBuilder.DEFAULT_UA)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)
                .setDefaultRequestProperties(safeHeaders)

            val dsFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpFactory)
            val mediaSource = DefaultMediaSourceFactory(context).setDataSourceFactory(dsFactory).createMediaSource(mediaItem)

            player?.apply {
                stop()
                clearMediaItems()
                setMediaSource(mediaSource)
                if (startPositionMs > 0L) seekTo(startPositionMs) else seekToDefaultPosition()
                prepare()
                play()
            }
            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "Reproduciendo con cabeceras (${safeHeaders.keys.joinToString()}): $effectiveUrl (pos=${startPositionMs}ms)")
        } catch (e: Throwable) {
            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "Error con cabeceras, fallback a limpio", e)
            playClean(effectiveUrl, title, startPositionMs)
        }
    }

    fun playClean(
        url: String,
        title: String? = null,
        startPositionMs: Long = 0L,
        branch: com.omnicast.core.domain.models.MediaBranch = activeBranch,
        subtitles: List<com.omnicast.core.protocol.SignalingMessage.SubtitleTrack> = emptyList()
    ) {
        if (url.contains("stub.mp4", ignoreCase = true) || url.contains("dummy.mp4", ignoreCase = true)) return
        initialize(branch)
        val effectiveUrl = com.omnicast.core.domain.usecases.HlsMasterResolver.deriveMasterPlaylistUrl(url) ?: url
        runCatching {
            activeMimeType = TvMediaItemBuilder.inferTargetMime(effectiveUrl, branch, fallbackMime)
            val mediaItem = TvMediaItemBuilder.buildMediaItem(effectiveUrl, title, branch, subtitles, fallbackMime)
            val safe = TvMediaItemBuilder.filterSafeHeaders(effectiveUrl, emptyMap())
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(safe["User-Agent"] ?: TvMediaItemBuilder.DEFAULT_UA)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)
            if (safe.isNotEmpty()) httpFactory.setDefaultRequestProperties(safe)
            val dsFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpFactory)
            val mediaSource = DefaultMediaSourceFactory(context).setDataSourceFactory(dsFactory).createMediaSource(mediaItem)
            player?.apply {
                stop()
                clearMediaItems()
                setMediaSource(mediaSource)
                if (startPositionMs > 0L) seekTo(startPositionMs) else seekToDefaultPosition()
                prepare()
                play()
            }
            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "Reproduciendo flujo limpio ($branch): $url (pos=${startPositionMs}ms)")
        }.onFailure { err ->
            com.omnicast.receiver.log.ReceiverLogger.log(TAG, "Error crítico reproduciendo flujo limpio: ${err.message}", err)
        }
    }

    var resizeMode: Int = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT; private set
    var onResizeModeChanged: ((Int) -> Unit)? = null
    var currentZoom: Float = 1.0f; private set
    var currentPanX: Float = 0.0f; private set
    var currentPanY: Float = 0.0f; private set
    var onZoomChanged: ((Float) -> Unit)? = null
    var onTransformChanged: ((Float, Float, Float) -> Unit)? = null
    var isControlsShowing: Boolean = false
    var onWakeControls: (() -> Unit)? = null
    var hasPlaybackStarted: Boolean = false; private set

    fun toggleResizeMode(): Int { resizeMode = aspectRatioHelper.cycleNextMode().resizeMode; onResizeModeChanged?.invoke(resizeMode); return resizeMode }
    fun setResizeMode(isZoom169: Boolean) { resizeMode = if (isZoom169) androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM else androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT; onResizeModeChanged?.invoke(resizeMode) }
    fun setDisplayMode(mode: String, zoom: Float = 1.0f, panY: Float = 0f, panX: Float = 0f) {
        currentZoom = zoom
        val resetPan = mode == "FIT" || mode == "FILL" || mode == "EXTEND_16_9" || zoom <= 1.01f
        currentPanX = if (resetPan && panX == 0f) 0f else panX
        currentPanY = if (resetPan && panY == 0f) 0f else panY
        val isZoom169 = mode == "FILL" || mode == "EXTEND_16_9"
        resizeMode = if (isZoom169) androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM else androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        onResizeModeChanged?.invoke(resizeMode); onZoomChanged?.invoke(zoom); onTransformChanged?.invoke(currentZoom, currentPanX, currentPanY)
    }
    fun isNearEnd(): Boolean = player?.let { it.playbackState == androidx.media3.common.Player.STATE_ENDED || (it.duration > 0L && it.currentPosition >= it.duration - 1500L) } ?: false
    fun seekTo(pos: Long) { player?.seekTo(pos) }; fun pause() { player?.pause() }; fun resume() { val p = player ?: return; if (isNearEnd()) p.seekTo(0L); p.play() }
    fun setVolume(vol: Float) { player?.volume = vol.coerceIn(0f, 1f) }
    fun toggleSubtitles(enable: Boolean? = null): Boolean = player?.let { p -> val st = trackManager?.toggleSubtitles(enable) ?: false; trackManager?.getTracksInfo(p)?.let { onTracksChanged?.invoke(it) }; st } ?: false
    fun selectTextTrack(idx: Int) { player?.let { trackManager?.selectTextTrack(it, idx); trackManager?.getTracksInfo(it)?.let { info -> onTracksChanged?.invoke(info) } } }
    fun selectAudioTrack(idx: Int) { player?.let { trackManager?.selectAudioTrack(it, idx); trackManager?.getTracksInfo(it)?.let { info -> onTracksChanged?.invoke(info) } } }
    fun cycleAudioTrack(): String = player?.let { p -> val label = trackManager?.cycleAudioTrack(p) ?: "Original"; trackManager?.getTracksInfo(p)?.let { onTracksChanged?.invoke(it) }; label } ?: "Original"
    fun getCurrentAudioTrackLabel(): String = player?.let { trackManager?.getCurrentAudioTrackLabel(it) } ?: "Original"
    fun cycleVideoQuality(): String = player?.let { trackManager?.cycleVideoQuality(it) } ?: "Auto"
    fun getCurrentVideoQuality(): String = trackManager?.getCurrentVideoQuality() ?: "Auto"
    val areSubtitlesEnabled: Boolean get() = trackManager?.subtitlesEnabled ?: false
    val currentPositionMs: Long get() = player?.currentPosition?.coerceAtLeast(0L) ?: 0L
    val durationMs: Long get() = player?.duration?.coerceAtLeast(0L) ?: 0L
    val isPlaying: Boolean get() = player?.isPlaying ?: false
    fun stop(reason: String = "desconocido") { com.omnicast.receiver.log.ReceiverLogger.log(TAG, "⏹️ Deteniendo ExoPlayer (motivo: $reason)"); player?.stop(); player?.clearMediaItems() }
    fun release() { stop("release"); playerView = null; player?.release(); player = null; TvMediaCacheManager.release() }
}
