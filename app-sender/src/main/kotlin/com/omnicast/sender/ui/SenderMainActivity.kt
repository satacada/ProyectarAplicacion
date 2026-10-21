package com.omnicast.sender.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.omnicast.core.domain.models.DeviceTarget
import com.omnicast.core.domain.models.StreamProfile
import com.omnicast.core.protocol.SignalingMessage
import com.omnicast.sender.discovery.NsdScannerManager
import com.omnicast.sender.history.PlayHistoryManager
import com.omnicast.sender.network.QuickSignalingHelper
import com.omnicast.sender.network.SignalingClient
import com.omnicast.sender.network.SignalingClientHolder
import com.omnicast.sender.overlay.FloatingIndicatorService
import com.omnicast.sender.service.ProjectionForegroundService
import com.omnicast.sender.stream.ActiveStreamHolder
import com.omnicast.sender.stream.ClipboardStreamExtractor
import kotlinx.coroutines.launch

class SenderMainActivity : ComponentActivity() {

    companion object {
        private var instance: SenderMainActivity? = null
        private var lastHandoffTime = 0L
        private var lastHandoffUrl: String? = null
        private var lastProxyTime = 0L
        var onRequestReturnToMain: (() -> Unit)? = null

        fun sendDirectStreamStatic(context: Context, url: String, title: String? = null, headers: Map<String, String> = emptyMap()) {
            val pos = com.omnicast.sender.stream.StreamResumeManager.getPosition(context, url); val br = com.omnicast.sender.nativeclassifier.NativeStreamClassifier.classify(url).name
            instance?.sendDirectStreamToTv(url, title, headers) ?: QuickSignalingHelper.sendQuickMessage(context, SignalingMessage.DirectStreamHandoff(mediaUrl = url, title = title, headers = headers, startPositionMs = pos, branch = br))
        }

        fun sendPlayControlStatic(context: Context, action: String, positionMs: Long = 0L, volume: Float = 1.0f) {
            instance?.sendPlayControl(action, positionMs, volume) ?: QuickSignalingHelper.sendQuickMessage(context, SignalingMessage.PlayControl(action, positionMs, volume))
        }

        fun sendDisconnectStatic(context: Context) { SignalingClientHolder.get().sendMessage(SignalingMessage.Disconnect()) }
        var onBrowserRescanRequested: (() -> Unit)? = null
        fun requestBrowserRescan(context: Context) { instance?.let { inst -> onBrowserRescanRequested?.let { req -> inst.runOnUiThread { req.invoke() } } } }

        fun exitCompletely(context: Context) {
            try {
                com.omnicast.sender.overlay.FloatingIndicatorService.stop(context); com.omnicast.sender.service.CastMediaNotificationService.stop(context)
                com.omnicast.sender.service.ProjectionForegroundService.stopService(context); com.omnicast.sender.media.LocalStreamProxyServer.stop()
                com.omnicast.sender.stream.ActiveStreamHolder.clear(); com.omnicast.sender.stream.ActiveStreamHolder.stopCasting(); sendDisconnectStatic(context)
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager)?.cancelAll()
                instance?.let { act -> act.runOnUiThread { act.finishAffinity(); android.os.Process.killProcess(android.os.Process.myPid()); kotlin.system.exitProcess(0) } }
            } catch (e: Throwable) { Log.e("Sender", "exitCompletely", e) }
        }
    }

    private lateinit var nsdScanner: NsdScannerManager; private lateinit var clipboardExtractor: ClipboardStreamExtractor
    private val signalingClient: SignalingClient get() = SignalingClientHolder.get()
    private val autoPromptCapture = androidx.compose.runtime.mutableStateOf(false); private val isTvConnected = androidx.compose.runtime.mutableStateOf(false); private val openBrowserDirectly = androidx.compose.runtime.mutableStateOf(false); private val openGalleryDirectly = androidx.compose.runtime.mutableStateOf(false)
    private lateinit var historyManager: PlayHistoryManager; private lateinit var playlistManager: com.omnicast.sender.playlist.PlaylistManager
    private val audioPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(this, "Audio interno desactivado. Transmitirá solo video.", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this; historyManager = PlayHistoryManager(this)
        com.omnicast.sender.log.SenderLogger.init(this)
        com.omnicast.sender.ui.browser.AdBlocker.init(this)
        com.omnicast.sender.stream.CloudStreamRuleSyncManager.initAndSync(this)
        com.omnicast.sender.discovery.MultiReceiverManager.init(this)
        playlistManager = com.omnicast.sender.playlist.PlaylistManager(this).apply {
            onPlaylistsChanged = { jsonStr -> if (signalingClient.isConnected) signalingClient.sendMessage(SignalingMessage.SyncPlaylists(jsonStr)) }
        }
        try { nsdScanner = NsdScannerManager(this).apply { startDiscovery() } } catch (e: Throwable) { Log.e("Sender", "NSD", e) }
        clipboardExtractor = ClipboardStreamExtractor(this)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            runCatching { audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) }
        }
        checkAutoMirroring(intent)

        setContent {
            OmniCastSenderTheme {
                SenderMainScreen(
                    nsdScanner = nsdScanner,
                    autoPromptCapture = autoPromptCapture.value,
                    isTvConnected = isTvConnected.value,
                    onConnectToTv = { ip, port -> connectToTv(ip, port) },
                    onAnalyzeStream = { url -> if (url != null) clipboardExtractor.extractStream(url) else clipboardExtractor.extractFromClipboard() },
                    onSendDirectStream = { url, title, hdrs -> sendDirectStreamToTv(url, title, hdrs) },
                    onStartProjection = { target, profile, scaleMode, permIntent -> startScreenStreaming(target, profile, scaleMode, permIntent) },
                    onStopProjection = { stopScreenStreaming() },
                    initialOpenBrowser = openBrowserDirectly.value,
                    initialOpenGallery = openGalleryDirectly.value
                )
            }
        }

        lifecycleScope.launch {
            nsdScanner.discoveredDevices.collect { list ->
                if (isTvConnected.value || list.isEmpty()) return@collect
                val curIp = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE).getString("saved_tv_ip", "") ?: ""
                val target = list.firstOrNull { it.ipAddress == curIp } ?: list.first()
                Log.i("TvConnection", "🔍 Auto-enlazando TV detectada: ${target.name} (${target.ipAddress}:${target.port})")
                connectToTv(target.ipAddress, target.port)
            }
        }

        com.omnicast.sender.media.LocalImageServer.start(this)
        val prefs = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
        prefs.getString("saved_tv_ip", null)?.takeIf { it.isNotBlank() }?.let { connectToTv(it, prefs.getInt("saved_tv_port", 8088)) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) FloatingIndicatorService.start(this); handleIncomingSharedIntent(intent)
    }

    private var inactivityJob: kotlinx.coroutines.Job? = null
    private var isAppResumed = false

    override fun onResume() {
        super.onResume(); isAppResumed = true; resetInactivityTimer()
        if (signalingClient?.isConnected != true) {
            val prefs = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
            prefs.getString("saved_tv_ip", null)?.takeIf { it.isNotBlank() }?.let { connectToTv(it, prefs.getInt("saved_tv_port", 8088)) }
        }
    }

    override fun onPause() { super.onPause(); isAppResumed = false; checkInactivityAutoExit() }
    private fun resetInactivityTimer() { inactivityJob?.cancel(); inactivityJob = null }

    private fun checkInactivityAutoExit() {
        if (isTvConnected.value || ProjectionForegroundService.isProjecting || ActiveStreamHolder.isCasting.value || com.omnicast.sender.service.CastSessionState.isGalleryCasting) {
            resetInactivityTimer(); return
        }
        if (inactivityJob?.isActive == true) return
        inactivityJob = lifecycleScope.launch {
            val waitSec = if (!isAppResumed) 60L else 180L
            kotlinx.coroutines.delay(waitSec * 1000L)
            if (!isTvConnected.value && !ProjectionForegroundService.isProjecting && !ActiveStreamHolder.isCasting.value && !com.omnicast.sender.service.CastSessionState.isGalleryCasting) {
                exitCompletely(this@SenderMainActivity)
            }
        }
    }

    private fun checkAutoMirroring(intent: Intent?) {
        if (intent?.getBooleanExtra("EXTRA_START_MIRRORING_AUTO", false) == true) { intent.removeExtra("EXTRA_START_MIRRORING_AUTO"); autoPromptCapture.value = true }
        if (intent?.getBooleanExtra("EXTRA_OPEN_BROWSER", false) == true) { intent.removeExtra("EXTRA_OPEN_BROWSER"); openBrowserDirectly.value = true }
        if (intent?.getBooleanExtra("EXTRA_OPEN_GALLERY", false) == true) { intent.removeExtra("EXTRA_OPEN_GALLERY"); openGalleryDirectly.value = true }
        if (intent?.getBooleanExtra("EXTRA_GO_TO_MAIN", false) == true) { intent.removeExtra("EXTRA_GO_TO_MAIN"); onRequestReturnToMain?.invoke() }
    }

    fun connectToTv(ipAddress: String, port: Int) {
        if (ipAddress.isBlank()) return
        try {
            signalingClient.connect(ipAddress, port, {
                isTvConnected.value = true; resetInactivityTimer()
                getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE).edit().putString("saved_tv_ip", ipAddress).putInt("saved_tv_port", port).apply()
                Log.i("TvConnection", "🟢 Conectado con TV: $ipAddress:$port")
                signalingClient.sendMessage(SignalingMessage.SyncPlaylists(playlistManager.getRawJson()))
                listenToPlaybackStatus()
                if (!isFinishing && !isDestroyed) Toast.makeText(this@SenderMainActivity, "🟢 Conectado con TV ($ipAddress)", Toast.LENGTH_SHORT).show()
            }, {
                isTvConnected.value = false; Log.w("TvConnection", "🔴 Desconectado de TV: $ipAddress:$port")
                ActiveStreamHolder.stopCasting(); com.omnicast.sender.service.CastMediaNotificationService.stop(this@SenderMainActivity); checkInactivityAutoExit()
            })
        } catch (e: Throwable) {
            isTvConnected.value = false; ActiveStreamHolder.stopCasting(); com.omnicast.sender.service.CastMediaNotificationService.stop(this@SenderMainActivity); checkInactivityAutoExit()
        }
    }

    private fun listenToPlaybackStatus() {
        lifecycleScope.launch {
            signalingClient?.messages?.collect { msg ->
                if (msg is SignalingMessage.PlaybackStatus) {
                    val mUrl = msg.mediaUrl
                    if (msg.state == "ERROR_FORBIDDEN_403") {
                        val origUrl = ActiveStreamHolder.streamUrl
                        val now = System.currentTimeMillis()
                        if (now - lastProxyTime < 5000L) return@collect
                        if (!mUrl.isNullOrBlank() && origUrl != null && !origUrl.contains(mUrl) && !mUrl.contains(origUrl)) return@collect
                        if (!ActiveStreamHolder.isProxied.value && !origUrl.isNullOrBlank()) {
                            lastProxyTime = now
                            com.omnicast.sender.log.SenderLogger.i("SenderMain", "🛡️ HTTP 403 en TV: Recuperación automática vía Proxy Jetty...")
                            runOnUiThread { Toast.makeText(this@SenderMainActivity, "🛡️ Flujo protegido: Enrutando vía teléfono (Jetty)...", Toast.LENGTH_SHORT).show() }
                            val pUrl = com.omnicast.sender.media.LocalStreamProxyServer.startAndGetProxyUrl(this@SenderMainActivity, origUrl, ActiveStreamHolder.headers)
                            val sTitle = ActiveStreamHolder.title ?: "Transmisión Web"
                            val br = com.omnicast.sender.nativeclassifier.NativeStreamClassifier.classify(origUrl).name
                            val pMsg = SignalingMessage.DirectStreamHandoff(mediaUrl = pUrl, title = sTitle, headers = ActiveStreamHolder.headers, startPositionMs = 0L, branch = br)
                            sendSignalingMessage(pMsg); ActiveStreamHolder.startCasting(pUrl, sTitle, ActiveStreamHolder.headers)
                            return@collect
                        }
                        com.omnicast.sender.log.SenderLogger.w("SenderMain", "⚠️ HTTP 403 en TV: Flujo protegido o expirado en origen")
                        runOnUiThread { Toast.makeText(this@SenderMainActivity, "⚠️ Enlace no disponible o protegido en el servidor origen", Toast.LENGTH_SHORT).show() }
                        ActiveStreamHolder.stopCasting(notifyTv = false); com.omnicast.sender.service.CastMediaNotificationService.stop(this@SenderMainActivity)
                    } else if ((msg.state == "PLAYING" || msg.state == "PAUSED") && !mUrl.isNullOrBlank()) {
                        if (!ActiveStreamHolder.isCasting.value || ActiveStreamHolder.streamUrl != mUrl) {
                            val streamTitle = msg.title?.ifBlank { "Película en TV" } ?: "Película en TV"
                            ActiveStreamHolder.startCasting(mUrl, streamTitle, emptyMap()); ActiveStreamHolder.onSendPlayControl = { act, pos, vol, par -> sendPlayControl(act, pos, vol, par) }
                            com.omnicast.sender.service.CastMediaNotificationService.start(this@SenderMainActivity)
                        }
                    } else if (msg.state in listOf("STOPPED", "ENDED", "IDLE")) {
                        if ((System.currentTimeMillis() - lastHandoffTime) >= 3500L) {
                            ActiveStreamHolder.stopCasting(notifyTv = false); com.omnicast.sender.service.CastMediaNotificationService.stop(this@SenderMainActivity); checkInactivityAutoExit()
                        }
                    }
                    ActiveStreamHolder.updatePlayback(msg.state == "PLAYING", msg.positionMs, msg.durationMs)
                    if (msg.positionMs > 5000L) {
                        com.omnicast.sender.stream.StreamResumeManager.savePosition(this@SenderMainActivity, ActiveStreamHolder.streamUrl, msg.positionMs)
                        ActiveStreamHolder.streamUrl?.let { sUrl ->
                            com.omnicast.sender.history.PlayHistoryManager(this@SenderMainActivity).updateProgress(sUrl, msg.positionMs, msg.durationMs)
                            playlistManager.updateProgress(sUrl, msg.positionMs, msg.durationMs)
                            if (msg.positionMs > 60000L && (msg.positionMs % 30000L < 2000L)) {
                                val host = com.omnicast.sender.stream.StreamProfileDatabase.extractHost(sUrl)
                                val br = com.omnicast.sender.nativeclassifier.NativeStreamClassifier.classify(sUrl)
                                com.omnicast.sender.stream.StreamProfileDatabase.get().recordSuccess(host, ActiveStreamHolder.headers, !ActiveStreamHolder.isProxied.value, br)
                                com.omnicast.sender.stream.CloudStreamRuleSyncManager.reportVerifiedSuccess(this@SenderMainActivity, host, br, !ActiveStreamHolder.isProxied.value)
                            }
                        }
                    }
                } else if (msg is SignalingMessage.TracksInfo) {
                    ActiveStreamHolder.updateTracksInfo(msg)
                } else if (msg is SignalingMessage.SyncPlaylists) {
                    val synced = playlistManager.syncFromTv(msg.playlistsJson)
                    if (synced) runOnUiThread { Toast.makeText(this@SenderMainActivity, "🎬 Lista sincronizada desde la TV", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent); checkAutoMirroring(intent); handleIncomingSharedIntent(intent)
    }

    private fun handleIncomingSharedIntent(intent: Intent) {
        SharedIntentHandler.handle(this, intent, clipboardExtractor, { u, t -> sendDirectStreamToTv(u, t) }, { u, t -> sendImageToTv(u, t) }, { moveTaskToBack(true) })
    }

    fun sendDirectStreamToTv(url: String, title: String? = null, headers: Map<String, String> = emptyMap(), resumePositionMs: Long? = null) {
        val now = System.currentTimeMillis()
        if (now - lastHandoffTime < 1000L && url == lastHandoffUrl) return
        lastHandoffTime = now; lastHandoffUrl = url
        val safeTitle = title?.ifBlank { "Transmisión Web" } ?: "Transmisión Web"
        val (cleanUrl, preparedHeaders) = com.omnicast.sender.ui.browser.MediaPlatformResolver.prepareStreamForPlayback(url, headers["Referer"] ?: "", headers)
        val isForceProxy = headers["X-Force-Proxy"] == "true" || preparedHeaders["X-Force-Proxy"] == "true"
        val finalMediaUrl = if (isForceProxy) com.omnicast.sender.media.LocalStreamProxyServer.startAndGetProxyUrl(this, cleanUrl, preparedHeaders) else cleanUrl
        val savedPos = resumePositionMs ?: com.omnicast.sender.stream.StreamResumeManager.getPosition(this, cleanUrl).takeIf { it > 0 } ?: playlistManager.getLastPosition(cleanUrl)
        historyManager.addOrUpdate(cleanUrl, safeTitle, headers = preparedHeaders, requiresProxy = isForceProxy)
        ActiveStreamHolder.startCasting(finalMediaUrl, safeTitle, preparedHeaders)
        com.omnicast.sender.service.CastMediaNotificationService.start(this)
        ActiveStreamHolder.onSendPlayControl = { act, pos, vol, par -> sendPlayControl(act, pos, vol, par) }
        val br = com.omnicast.sender.nativeclassifier.NativeStreamClassifier.classify(cleanUrl).name
        val msg = SignalingMessage.DirectStreamHandoff(mediaUrl = finalMediaUrl, title = safeTitle, headers = preparedHeaders, startPositionMs = savedPos, branch = br)
        sendSignalingMessage(msg); com.omnicast.sender.discovery.MultiReceiverManager.broadcastToSecondary(msg, nsdScanner.discoveredDevices.value)
        val toastMsg = if (savedPos > 0L) "Reanudando en ${com.omnicast.sender.stream.StreamResumeManager.formatTime(savedPos)}" else "Enviando video a receptores..."
        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
    }

    fun sendPlayControl(action: String, positionMs: Long = 0L, volume: Float = 1.0f, param: String? = null) {
        val msg = SignalingMessage.PlayControl(action, positionMs, volume, param); sendSignalingMessage(msg)
        com.omnicast.sender.discovery.MultiReceiverManager.broadcastToSecondary(msg, nsdScanner.discoveredDevices.value)
    }

    fun sendImageToTv(url: String, title: String? = null) {
        val msg = SignalingMessage.ShowImage(imageUrl = url, title = title); sendSignalingMessage(msg)
        com.omnicast.sender.discovery.MultiReceiverManager.broadcastToSecondary(msg, nsdScanner.discoveredDevices.value)
        Toast.makeText(this, "Foto proyectada en la TV Box", Toast.LENGTH_SHORT).show()
    }

    private fun sendSignalingMessage(msg: SignalingMessage) {
        if (signalingClient.isConnected) signalingClient.sendMessage(msg)
        else getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE).getString("saved_tv_ip", "")?.takeIf { it.isNotBlank() }?.let { ip -> signalingClient.connect(ip, 8088, { signalingClient.sendMessage(msg) }, {}) }
    }

    private fun startScreenStreaming(target: DeviceTarget, profile: StreamProfile, tvScaleMode: String, permissionData: Intent) {
        autoPromptCapture.value = false; com.omnicast.sender.service.CastMediaNotificationService.stop(this)
        val smoothBuffer = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE).getBoolean("saved_smooth_buffer", true)
        ProjectionForegroundService.startProjection(this, target.ipAddress, target.port, profile, permissionData, tvScaleMode, smoothBuffer)
        Toast.makeText(this, "Proyectando pantalla en ${target.name}", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch { kotlinx.coroutines.delay(700); if (!isFinishing && !isDestroyed) moveTaskToBack(true) }
    }

    private fun stopScreenStreaming() {
        ProjectionForegroundService.stopService(this); sendDisconnectStatic(this); Toast.makeText(this, "Proyección finalizada", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy(); instance = null; com.omnicast.sender.media.LocalImageServer.stop(); com.omnicast.sender.media.LocalStreamProxyServer.stop()
        try { nsdScanner.stopDiscovery() } catch (e: Throwable) { Log.e("Sender", "NSD stop", e) }
    }
}
