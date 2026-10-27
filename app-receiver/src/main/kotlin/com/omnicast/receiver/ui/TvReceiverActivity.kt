package com.omnicast.receiver.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.omnicast.core.protocol.SignalingMessage
import com.omnicast.receiver.audio.DigitalAudioReceiver
import com.omnicast.receiver.directstream.ExoPlayerTvManager
import com.omnicast.receiver.server.ClientStatus
import com.omnicast.receiver.server.LocalSignalingServer
import com.omnicast.receiver.discovery.NsdAdvertiserManager
import com.omnicast.receiver.ui.components.*
import com.omnicast.receiver.webrtc.WebRtcReceiverManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class TvReceiverActivity : ComponentActivity() {

    private val receiverState = mutableStateOf<ReceiverUiState>(ReceiverUiState.Standby); private val localIpAddress = mutableStateOf("127.0.0.1")
    private val currentDisplayMode = mutableStateOf<SignalingMessage.SetDisplayMode?>(null); private val expiredItemForRenew = mutableStateOf<com.omnicast.receiver.directstream.TvPlaylistItem?>(null)
    private val youTubeControlFlow = MutableSharedFlow<SignalingMessage.PlayControl>(extraBufferCapacity = 16)
    private lateinit var webRtcReceiver: WebRtcReceiverManager; private lateinit var exoPlayerManager: ExoPlayerTvManager; private lateinit var signalingServer: LocalSignalingServer
    private lateinit var nsdAdvertiser: NsdAdvertiserManager; private lateinit var historyManager: com.omnicast.receiver.directstream.TvPlaybackHistoryManager
    private lateinit var playlistManager: com.omnicast.receiver.directstream.TvPlaylistManager; private lateinit var updateManager: com.omnicast.receiver.update.TvUpdateManager
    private var digitalAudioReceiver: DigitalAudioReceiver? = null; private var statusJob: Job? = null
    private var lastDirectHeaders: Map<String, String> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); localIpAddress.value = TvReceiverHelper.getLocalIpAddress()
        com.omnicast.receiver.log.ReceiverLogger.init(this)
        val ver = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (_: Throwable) { "?" }
        com.omnicast.receiver.log.ReceiverLogger.log("TV_RECEIVER", "🚀 OmniCast Receiver v$ver en ${localIpAddress.value}:8088")
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) { com.omnicast.receiver.directstream.TvMediaCacheManager.clearDiskCache(this@TvReceiverActivity) }
        webRtcReceiver = WebRtcReceiverManager(this).apply {
            initialize()
            onLocalIceCandidate = { c -> lifecycleScope.launch { signalingServer.sendMessage(SignalingMessage.IceCandidate(c.sdpMid, c.sdpMLineIndex, c.sdp)) } }
            onConnectionClosed = { runOnUiThread { if (receiverState.value is ReceiverUiState.ScreenMirroring) receiverState.value = ReceiverUiState.Standby } }
        }
        exoPlayerManager = ExoPlayerTvManager(this).apply {
            onTracksChanged = { info -> lifecycleScope.launch { signalingServer.sendMessage(info) } }
            onPlaybackFailed = { failedUrl, failedTitle -> runOnUiThread {
                statusJob?.cancel(); exoPlayerManager.stop("Fallback 403")
                val streamToReport = failedUrl ?: (receiverState.value as? ReceiverUiState.DirectStreamPlaying)?.streamUrl
                val titleToReport = failedTitle ?: (receiverState.value as? ReceiverUiState.DirectStreamPlaying)?.title
                if (!streamToReport.isNullOrBlank() && (streamToReport.startsWith("http://") || streamToReport.startsWith("https://")) && !streamToReport.contains(":8089")) {
                    com.omnicast.receiver.log.ReceiverLogger.log("TV_RECEIVER", "ExoPlayer 403 detectado, fallback a WebView HTML5: $streamToReport")
                    receiverState.value = ReceiverUiState.WebStreamPlaying(streamToReport, titleToReport ?: "Transmisión Web", headers = lastDirectHeaders, isFallbackFromExoPlayer = true)
                } else {
                    (receiverState.value as? ReceiverUiState.DirectStreamPlaying)?.let { cur ->
                        val found = playlistManager.findItemByUrlOrTitle(cur.streamUrl, cur.title)
                        if (found != null) expiredItemForRenew.value = found else Toast.makeText(this@TvReceiverActivity, "⚠️ Servidor no disponible (HTTP 403)", Toast.LENGTH_SHORT).show()
                    }
                    receiverState.value = ReceiverUiState.Standby
                    lifecycleScope.launch { signalingServer.sendMessage(SignalingMessage.PlaybackStatus("ERROR_FORBIDDEN_403", 0L, 0L, mediaUrl = streamToReport, title = titleToReport)) }
                }
            } }
        }
        digitalAudioReceiver = DigitalAudioReceiver()
        historyManager = com.omnicast.receiver.directstream.TvPlaybackHistoryManager(this)
        playlistManager = com.omnicast.receiver.directstream.TvPlaylistManager(this).apply { onPlaylistsChanged = { jsonStr -> lifecycleScope.launch { signalingServer.sendMessage(SignalingMessage.SyncPlaylists(jsonStr)) } } }
        updateManager = com.omnicast.receiver.update.TvUpdateManager(this)
        signalingServer = LocalSignalingServer(port = 8088).apply { start() }
        nsdAdvertiser = NsdAdvertiserManager(this).apply { registerService(port = 8088) }
        lifecycleScope.launch { delay(2500); updateManager.checkForUpdates() }

        lifecycleScope.launch {
            signalingServer.clientStatus.collect { st ->
                when (st) {
                    is ClientStatus.Connected -> {
                        if (receiverState.value is ReceiverUiState.Standby) receiverState.value = ReceiverUiState.ClientConnectedStandby(st.clientAddress)
                        (receiverState.value as? ReceiverUiState.DirectStreamPlaying)?.let { cur ->
                            val pState = if (exoPlayerManager.isPlaying) "PLAYING" else "PAUSED"
                            signalingServer.sendMessage(SignalingMessage.PlaybackStatus(pState, exoPlayerManager.currentPositionMs, exoPlayerManager.durationMs, cur.streamUrl, cur.title))
                        }
                    }
                    is ClientStatus.Disconnected -> {
                        digitalAudioReceiver?.stop(); webRtcReceiver.resetConnection()
                        if (receiverState.value !is ReceiverUiState.Standby && receiverState.value !is ReceiverUiState.DirectStreamPlaying && receiverState.value !is ReceiverUiState.WebStreamPlaying) {
                            receiverState.value = ReceiverUiState.Standby
                        }
                    }
                }
            }
        }
        lifecycleScope.launch { signalingServer.incomingMessages.collect { handleSignalingMessage(it) } }

        setContent {
            val historyList by historyManager.history.collectAsState()
            val playlistsList by playlistManager.playlists.collectAsState()
            val otaState by updateManager.updateState.collectAsState()
            MaterialTheme(colorScheme = darkColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF38BDF8), background = androidx.compose.ui.graphics.Color(0xFF0F172A))) {
                when (val state = receiverState.value) {
                    is ReceiverUiState.Standby -> TvStandbyCleanScreen(
                        ipAddress = localIpAddress.value, history = historyList, playlists = playlistsList,
                        onPlayHistory = { playHistoryItem(it) }, onPlayPlaylist = { playPlaylistItem(it) }, onDeletePlaylist = { playlistManager.removeItem(it.url) }, onDeleteMultiple = { playlistManager.removeItems(it) },
                        updateState = otaState, onCheckUpdate = { lifecycleScope.launch { updateManager.checkForUpdates() } },
                        onDownloadAndInstall = { url ->
                            lifecycleScope.launch {
                                if (otaState is com.omnicast.receiver.update.TvUpdateState.ReadyToInstall) {
                                    (otaState as com.omnicast.receiver.update.TvUpdateState.ReadyToInstall).apkFile.let { updateManager.triggerApkInstall(it) }
                                } else updateManager.downloadAndInstall(url)
                            }
                        },
                        expiredItemForRenew = expiredItemForRenew.value, onClearExpiredRenew = { expiredItemForRenew.value = null }
                    )
                    is ReceiverUiState.ClientConnectedStandby -> TvConnectedStandbyScreen(state.clientAddress) { receiverState.value = ReceiverUiState.Standby }
                    is ReceiverUiState.ScreenMirroring -> TvMirroringActiveScreen(
                        webRtcReceiver, state.resolution, state.fps,
                        onCloseClick = { digitalAudioReceiver?.stop(); webRtcReceiver.resetConnection(); receiverState.value = ReceiverUiState.Standby }
                    )
                    is ReceiverUiState.DirectStreamPlaying -> TvDirectStreamScreen(
                        exoPlayerManager, state.title, state.streamUrl,
                        isSaved = playlistManager.isMovieSaved(state.streamUrl, state.title),
                        onSaveToWatchLater = {
                            playlistManager.saveMovie(state.title, state.streamUrl, exoPlayerManager.currentPositionMs)
                            Toast.makeText(this@TvReceiverActivity, "✅ Guardada en Ver más tarde", Toast.LENGTH_SHORT).show()
                        },
                        onCloseClick = { exoPlayerManager.stop("Botón Cerrar TV"); receiverState.value = ReceiverUiState.Standby }
                    )
                    is ReceiverUiState.YouTubePlaying -> TvYouTubeScreen(
                        videoId = state.videoId, title = state.title, timestamp = state.timestamp, commandFlow = youTubeControlFlow,
                        onPlaybackStatus = { pos, dur, playing ->
                            lifecycleScope.launch { signalingServer.sendMessage(SignalingMessage.PlaybackStatus(if (playing) "PLAYING" else "PAUSED", pos, dur)) }
                        },
                        onCloseClick = { receiverState.value = ReceiverUiState.Standby }
                    )
                    is ReceiverUiState.WebStreamPlaying -> TvWebStreamScreen(
                        streamUrl = state.streamUrl, title = state.title, headers = state.headers, startPositionMs = state.startPositionMs,
                        subtitles = state.subtitles, timestamp = state.timestamp, commandFlow = youTubeControlFlow, displayMode = currentDisplayMode.value,
                        onPlaybackStatus = { pos, dur, playing ->
                            lifecycleScope.launch {
                                signalingServer.sendMessage(SignalingMessage.PlaybackStatus(if (playing) "PLAYING" else "PAUSED", pos, dur, state.streamUrl, state.title))
                                historyManager.updateProgress(state.streamUrl, pos, dur); playlistManager.updateProgress(state.streamUrl, pos)
                            }
                        },
                        onPlaybackFailed = { reason -> runOnUiThread {
                            val isSigned = com.omnicast.core.domain.usecases.StreamManifestProbe.hasSignedSecurityToken(state.streamUrl)
                            if (state.isFallbackFromExoPlayer || isSigned) {
                                com.omnicast.receiver.log.ReceiverLogger.log("TvReceiverActivity", "⚠️ WebStream falló ($reason). Finalizando y retornando a Standby.")
                                val found = playlistManager.findItemByUrlOrTitle(state.streamUrl, state.title)
                                if (found != null) expiredItemForRenew.value = found else Toast.makeText(this@TvReceiverActivity, if (reason == "WATCHDOG_TIMEOUT") "⚠️ Tiempo de espera agotado (buffer lento)" else "⚠️ Flujo no disponible ($reason)", Toast.LENGTH_SHORT).show()
                                receiverState.value = ReceiverUiState.Standby
                                val errCode = if (reason == "WATCHDOG_TIMEOUT") "ERROR_TIMEOUT" else "ERROR_FORBIDDEN_403"
                                lifecycleScope.launch { signalingServer.sendMessage(SignalingMessage.PlaybackStatus(errCode, 0L, 0L, mediaUrl = state.streamUrl, title = state.title)) }
                            } else {
                                com.omnicast.receiver.log.ReceiverLogger.log("TvReceiverActivity", "⚠️ WebStream falló ($reason). Fallback reactivo a ExoPlayer...")
                                receiverState.value = ReceiverUiState.DirectStreamPlaying(state.streamUrl, state.title)
                                exoPlayerManager.playStream(state.streamUrl, state.headers, state.title, state.startPositionMs, subtitles = state.subtitles)
                                startPlaybackStatusReporting()
                            }
                        } },
                        onCloseClick = { receiverState.value = ReceiverUiState.Standby }
                    )
                    is ReceiverUiState.ImageDisplaying -> TvImageScreen(
                        imageUrl = state.imageUrl, title = state.title, scale = state.scale, panX = state.panX, panY = state.panY, rotation = state.rotation,
                        onPrevClick = { lifecycleScope.launch { signalingServer.sendMessage(SignalingMessage.PlayControl("PREV")) }; Toast.makeText(this@TvReceiverActivity, "◀ Foto Anterior", Toast.LENGTH_SHORT).show() },
                        onNextClick = { lifecycleScope.launch { signalingServer.sendMessage(SignalingMessage.PlayControl("NEXT")) }; Toast.makeText(this@TvReceiverActivity, "Foto Siguiente ▶", Toast.LENGTH_SHORT).show() },
                        onCloseClick = { receiverState.value = ReceiverUiState.Standby },
                        onRotateClick = { (receiverState.value as? ReceiverUiState.ImageDisplaying)?.let { receiverState.value = it.copy(rotation = (it.rotation + 90f) % 360f) } }
                    )
                }
            }
        }
    }

    private fun handleSignalingMessage(message: SignalingMessage) {
        when (message) {
            is SignalingMessage.Offer -> {
                exoPlayerManager.stop(); digitalAudioReceiver?.start(); webRtcReceiver.resetConnection()
                receiverState.value = ReceiverUiState.ScreenMirroring("${message.width}x${message.height}", message.fps)
                webRtcReceiver.setSmoothBuffer(message.smoothBuffer)
                webRtcReceiver.handleOffer(message.sdp) { sdp -> lifecycleScope.launch { signalingServer.sendMessage(SignalingMessage.Answer(sdp)) } }
            }
            is SignalingMessage.IceCandidate -> webRtcReceiver.addRemoteIceCandidate(message.sdpMid, message.sdpMLineIndex, message.candidate)
            is SignalingMessage.DirectStreamHandoff -> {
                runCatching {
                    TvRemoteKeyHandler.notifyPlaybackStarted()
                    lastDirectHeaders = message.headers; digitalAudioReceiver?.stop(); webRtcReceiver.resetConnection()
                    val title = message.title?.ifBlank { "Transmisión Web" } ?: "Transmisión Web"
                    val exp = expiredItemForRenew.value
                    if (exp != null) { playlistManager.replaceExpiredUrl(exp.url, message.mediaUrl, message.headers); expiredItemForRenew.value = null }
                    else playlistManager.replaceExpiredUrl(message.mediaUrl, message.mediaUrl, message.headers)
                    val ytId = message.headers["X-YouTube-Id"] ?: TvReceiverHelper.extractYouTubeId(message.mediaUrl)
                    if (!ytId.isNullOrBlank()) {
                        exoPlayerManager.stop(); receiverState.value = ReceiverUiState.YouTubePlaying(ytId, title, System.currentTimeMillis())
                    } else if (TvReceiverHelper.shouldUseWebPlayer(message.mediaUrl, message.headers)) {
                        exoPlayerManager.stop("WebStream"); webRtcReceiver.resetConnection()
                        receiverState.value = ReceiverUiState.WebStreamPlaying(message.mediaUrl, title, message.headers, message.startPositionMs, subtitles = message.subtitles)
                    } else {
                        val defBr = if (message.mediaUrl.contains(".mpd", true) || message.mediaUrl.contains("ct=6")) com.omnicast.core.domain.models.MediaBranch.DASH_MPD else com.omnicast.core.domain.models.MediaBranch.VOD_HLS_MP4
                        val br = com.omnicast.core.domain.models.MediaBranch.entries.firstOrNull { it.name.equals(message.branch, true) } ?: defBr
                        receiverState.value = ReceiverUiState.DirectStreamPlaying(message.mediaUrl, title)
                        exoPlayerManager.playStream(message.mediaUrl, message.headers, title, message.startPositionMs, br, message.subtitles)
                        lifecycleScope.launch { historyManager.addOrUpdate(message.mediaUrl, title, message.headers, message.startPositionMs) }
                        startPlaybackStatusReporting()
                    }
                }.onFailure { err ->
                    com.omnicast.receiver.log.ReceiverLogger.log("TvReceiverActivity", "Error al procesar DirectStreamHandoff: ${err.message}", err)
                    receiverState.value = ReceiverUiState.Standby
                }
            }
            is SignalingMessage.PlayControl -> {
                if (receiverState.value is ReceiverUiState.YouTubePlaying || receiverState.value is ReceiverUiState.WebStreamPlaying) {
                    youTubeControlFlow.tryEmit(message)
                }
                when (message.action) {
                    "PLAY" -> { if (message.positionMs > 0L && !exoPlayerManager.isNearEnd()) exoPlayerManager.seekTo(message.positionMs); exoPlayerManager.resume() }
                    "PAUSE" -> exoPlayerManager.pause()
                    "STOP" -> { digitalAudioReceiver?.stop(); exoPlayerManager.stop("Comando STOP celular"); webRtcReceiver.resetConnection(); receiverState.value = ReceiverUiState.Standby }
                    "SEEK" -> exoPlayerManager.seekTo(message.positionMs)
                    "VOLUME" -> TvReceiverHelper.applySystemAndPlayerVolume(this, exoPlayerManager, message.volume)
                    "SUBTITLE_TOGGLE", "TOGGLE_SUBTITLES" -> exoPlayerManager.toggleSubtitles()
                    "SET_SUBTITLE_TRACK" -> message.param?.toIntOrNull()?.let { exoPlayerManager.selectTextTrack(it) }
                    "SET_AUDIO_TRACK" -> message.param?.toIntOrNull()?.let { exoPlayerManager.selectAudioTrack(it) }
                    "CYCLE_QUALITY" -> { val q = exoPlayerManager.cycleVideoQuality(); Toast.makeText(this@TvReceiverActivity, "📺 Calidad: $q", Toast.LENGTH_SHORT).show() }
                    "ROTATE" -> (receiverState.value as? ReceiverUiState.ImageDisplaying)?.let { receiverState.value = it.copy(rotation = (it.rotation + 90f) % 360f) }
                }
            }
            is SignalingMessage.ImageTransform -> (receiverState.value as? ReceiverUiState.ImageDisplaying)?.let { receiverState.value = it.copy(scale = message.scale, panX = message.panX, panY = message.panY, rotation = message.rotation) }
            is SignalingMessage.SetDisplayMode -> {
                currentDisplayMode.value = message
                webRtcReceiver.setDisplayMode(message.mode, message.zoom, message.panY, message.panX, message.isLandscape)
                exoPlayerManager.setDisplayMode(message.mode, message.zoom)
            }
            is SignalingMessage.ShowImage -> { digitalAudioReceiver?.stop(); exoPlayerManager.stop("Casteo Foto"); webRtcReceiver.resetConnection(); receiverState.value = ReceiverUiState.ImageDisplaying(message.imageUrl, message.title ?: "Foto de Galería") }
            is SignalingMessage.SyncPlaylists -> lifecycleScope.launch { playlistManager.syncPlaylistsJson(message.playlistsJson) }
            is SignalingMessage.Disconnect -> { digitalAudioReceiver?.stop(); exoPlayerManager.stop("Desconexión"); webRtcReceiver.resetConnection(); receiverState.value = ReceiverUiState.Standby }
            else -> Unit
        }
    }

    private fun startPlaybackStatusReporting() {
        statusJob?.cancel()
        statusJob = lifecycleScope.launch {
            while (true) {
                delay(1000)
                val cur = receiverState.value
                if (cur is ReceiverUiState.DirectStreamPlaying) {
                    val isPlaying = exoPlayerManager.isPlaying
                    val pos = exoPlayerManager.currentPositionMs
                    val dur = exoPlayerManager.durationMs
                    val st = if (isPlaying) "PLAYING" else "PAUSED"
                    signalingServer.sendMessage(SignalingMessage.PlaybackStatus(st, pos, dur, cur.streamUrl, cur.title))
                    historyManager.updateProgress(cur.streamUrl, pos, dur); playlistManager.updateProgress(cur.streamUrl, pos)
                } else {
                    signalingServer.sendMessage(SignalingMessage.PlaybackStatus("STOPPED", 0L, 0L))
                    break
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && TvRemoteKeyHandler.handleKeyDown(
                this, event.keyCode, event, receiverState.value, exoPlayerManager,
                webRtcReceiver, digitalAudioReceiver, youTubeControlFlow, lifecycleScope,
                { lifecycleScope.launch { signalingServer.sendMessage(it) } }, { receiverState.value = ReceiverUiState.Standby }
            )) return true
        return super.dispatchKeyEvent(event)
    }

    private fun playHistoryItem(item: com.omnicast.receiver.directstream.TvHistoryItem) {
        digitalAudioReceiver?.stop(); webRtcReceiver.resetConnection(); TvRemoteKeyHandler.notifyPlaybackStarted()
        if (TvReceiverHelper.shouldUseWebPlayer(item.mediaUrl, item.headers)) {
            exoPlayerManager.stop(); receiverState.value = ReceiverUiState.WebStreamPlaying(item.mediaUrl, item.title, item.headers, item.lastPositionMs)
        } else {
            receiverState.value = ReceiverUiState.DirectStreamPlaying(item.mediaUrl, item.title)
            exoPlayerManager.playStream(item.mediaUrl, item.headers, item.title, item.lastPositionMs); startPlaybackStatusReporting()
        }
    }

    private fun playPlaylistItem(item: com.omnicast.receiver.directstream.TvPlaylistItem) {
        digitalAudioReceiver?.stop(); webRtcReceiver.resetConnection(); TvRemoteKeyHandler.notifyPlaybackStarted()
        val pos = playlistManager.getResumePosition(item)
        if (TvReceiverHelper.shouldUseWebPlayer(item.url, item.headers)) {
            exoPlayerManager.stop(); receiverState.value = ReceiverUiState.WebStreamPlaying(item.url, item.title, item.headers, pos)
        } else {
            receiverState.value = ReceiverUiState.DirectStreamPlaying(item.url, item.title)
            exoPlayerManager.playStream(item.url, item.headers, item.title, pos); startPlaybackStatusReporting()
        }
    }

    override fun onDestroy() {
        super.onDestroy(); com.omnicast.receiver.log.ReceiverLogger.stop(); digitalAudioReceiver?.stop()
        nsdAdvertiser.unregisterService(); signalingServer.stop(); webRtcReceiver.release(); exoPlayerManager.release()
    }
}
