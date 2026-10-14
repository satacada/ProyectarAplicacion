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

        fun sendDirectStreamStatic(context: Context, url: String, title: String? = null, headers: Map<String, String> = emptyMap()) {
            val savedPos = com.omnicast.sender.stream.StreamResumeManager.getPosition(context, url)
            val msg = SignalingMessage.DirectStreamHandoff(mediaUrl = url, title = title, headers = headers, startPositionMs = savedPos)
            instance?.sendDirectStreamToTv(url, title, headers) ?: QuickSignalingHelper.sendQuickMessage(context, msg)
        }

        fun sendPlayControlStatic(context: Context, action: String, positionMs: Long = 0L, volume: Float = 1.0f) {
            val msg = SignalingMessage.PlayControl(action, positionMs, volume)
            instance?.sendPlayControl(action, positionMs, volume) ?: QuickSignalingHelper.sendQuickMessage(context, msg)
        }

        fun sendDisconnectStatic(context: Context) {
            SignalingClientHolder.get().sendMessage(SignalingMessage.Disconnect())
        }

        var onBrowserRescanRequested: (() -> Unit)? = null

        fun requestBrowserRescan(context: Context) {
            val inst = instance
            if (inst != null && onBrowserRescanRequested != null) {
                inst.runOnUiThread { onBrowserRescanRequested?.invoke() }
            }
        }

        fun exitCompletely(context: Context) {
            try {
                com.omnicast.sender.overlay.FloatingIndicatorService.stop(context)
                com.omnicast.sender.service.CastMediaNotificationService.stop(context)
                com.omnicast.sender.service.ProjectionForegroundService.stopService(context)
                com.omnicast.sender.stream.ActiveStreamHolder.clear(); com.omnicast.sender.stream.ActiveStreamHolder.stopCasting()
                sendDisconnectStatic(context)
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager)?.cancelAll()
                instance?.let { act -> act.runOnUiThread { act.finishAffinity() } }
            } catch (e: Throwable) { Log.e("Sender", "exitCompletely", e) }
        }
    }

    private lateinit var nsdScanner: NsdScannerManager
    private lateinit var clipboardExtractor: ClipboardStreamExtractor
    private val signalingClient: SignalingClient get() = SignalingClientHolder.get()
    private val autoPromptCapture = androidx.compose.runtime.mutableStateOf(false)
    private val isTvConnected = androidx.compose.runtime.mutableStateOf(false)
    private val openBrowserDirectly = androidx.compose.runtime.mutableStateOf(false)
    private val openGalleryDirectly = androidx.compose.runtime.mutableStateOf(false)
    private lateinit var historyManager: PlayHistoryManager
    private lateinit var playlistManager: com.omnicast.sender.playlist.PlaylistManager

    private val audioPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(this, "Audio interno desactivado. Transmitirá solo video.", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this; historyManager = PlayHistoryManager(this)
        com.omnicast.sender.log.SenderLogger.init(this)
        playlistManager = com.omnicast.sender.playlist.PlaylistManager(this).apply {
            onPlaylistsChanged = { jsonStr ->
                if (signalingClient.isConnected) signalingClient.sendMessage(SignalingMessage.SyncPlaylists(jsonStr))
            }
        }
        try { nsdScanner = NsdScannerManager(this).apply { startDiscovery() } } catch (e: Throwable) { Log.e("Sender", "NSD", e) }
        clipboardExtractor = ClipboardStreamExtractor(this)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            try { audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) } catch (e: Throwable) { Log.e("Sender", "Perm", e) }
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
                if (isTvConnected.value) return@collect
                list.firstOrNull()?.let { dev ->
                    val curIp = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE).getString("saved_tv_ip", "") ?: ""
                    Log.i("TvConnection", "🔍 TV detectada por NSD: ${dev.name} (${dev.ipAddress}:${dev.port})")
                    if (!isTvConnected.value && (curIp.isBlank() || curIp == dev.ipAddress)) {
                        connectToTv(dev.ipAddress, dev.port)
                    }
                }
            }
        }

        com.omnicast.sender.media.LocalImageServer.start(this)
        val prefs = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
        prefs.getString("saved_tv_ip", null)?.takeIf { it.isNotBlank() }?.let { connectToTv(it, prefs.getInt("saved_tv_port", 8088)) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            FloatingIndicatorService.start(this)
        }
        handleIncomingSharedIntent(intent)
    }

    private var inactivityJob: kotlinx.coroutines.Job? = null
    private var isAppResumed = false

    override fun onResume() {
        super.onResume()
        isAppResumed = true
        resetInactivityTimer()
        if (signalingClient?.isConnected != true) {
            val prefs = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
            prefs.getString("saved_tv_ip", null)?.takeIf { it.isNotBlank() }?.let { connectToTv(it, prefs.getInt("saved_tv_port", 8088)) }
        }
    }

    override fun onPause() {
        super.onPause()
        isAppResumed = false
        checkInactivityAutoExit()
    }

    private fun resetInactivityTimer() {
        inactivityJob?.cancel(); inactivityJob = null
    }

    private fun checkInactivityAutoExit() {
        if (isTvConnected.value || ProjectionForegroundService.isProjecting || ActiveStreamHolder.isCasting.value) {
            resetInactivityTimer(); return
        }
        if (inactivityJob?.isActive == true) return
        inactivityJob = lifecycleScope.launch {
            val waitSec = if (!isAppResumed) 60L else 180L
            kotlinx.coroutines.delay(waitSec * 1000L)
            if (!isTvConnected.value && !ProjectionForegroundService.isProjecting && !ActiveStreamHolder.isCasting.value) {
                exitCompletely(this@SenderMainActivity)
            }
        }
    }

    private fun checkAutoMirroring(intent: Intent?) {
        if (intent?.getBooleanExtra("EXTRA_START_MIRRORING_AUTO", false) == true) {
            intent.removeExtra("EXTRA_START_MIRRORING_AUTO"); autoPromptCapture.value = true
        }
        if (intent?.getBooleanExtra("EXTRA_OPEN_BROWSER", false) == true) { intent.removeExtra("EXTRA_OPEN_BROWSER"); openBrowserDirectly.value = true }
        if (intent?.getBooleanExtra("EXTRA_OPEN_GALLERY", false) == true) { intent.removeExtra("EXTRA_OPEN_GALLERY"); openGalleryDirectly.value = true }
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
                checkInactivityAutoExit()
            })
        } catch (e: Throwable) { isTvConnected.value = false; checkInactivityAutoExit() }
    }

    private fun listenToPlaybackStatus() {
        lifecycleScope.launch {
            signalingClient?.messages?.collect { msg ->
                if (msg is SignalingMessage.PlaybackStatus) {
                    val mUrl = msg.mediaUrl
                    if ((msg.state == "PLAYING" || msg.state == "PAUSED") && !mUrl.isNullOrBlank()) {
                        if (!ActiveStreamHolder.isCasting.value || ActiveStreamHolder.streamUrl != mUrl) {
                            val streamTitle = msg.title?.ifBlank { "Película en TV" } ?: "Película en TV"
                            ActiveStreamHolder.startCasting(mUrl, streamTitle, emptyMap())
                            ActiveStreamHolder.onSendPlayControl = { act, pos, vol, par -> sendPlayControl(act, pos, vol, par) }
                            com.omnicast.sender.service.CastMediaNotificationService.start(this@SenderMainActivity)
                        }
                    }
                    ActiveStreamHolder.updatePlayback(msg.state == "PLAYING", msg.positionMs, msg.durationMs)
                    if (msg.positionMs > 5000L) {
                        com.omnicast.sender.stream.StreamResumeManager.savePosition(this@SenderMainActivity, ActiveStreamHolder.streamUrl, msg.positionMs)
                        ActiveStreamHolder.streamUrl?.let { sUrl ->
                            com.omnicast.sender.history.PlayHistoryManager(this@SenderMainActivity).updateProgress(sUrl, msg.positionMs, msg.durationMs)
                        }
                    }
                } else if (msg is SignalingMessage.TracksInfo) {
                    ActiveStreamHolder.updateTracksInfo(msg)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkAutoMirroring(intent); handleIncomingSharedIntent(intent)
    }

    private fun handleIncomingSharedIntent(intent: Intent) {
        SharedIntentHandler.handle(this, intent, clipboardExtractor, { u, t -> sendDirectStreamToTv(u, t) }, { u, t -> sendImageToTv(u, t) }, { moveTaskToBack(true) })
    }

    fun sendDirectStreamToTv(url: String, title: String? = null, headers: Map<String, String> = emptyMap(), resumePositionMs: Long? = null) {
        val safeTitle = title?.ifBlank { "Transmisión Web" } ?: "Transmisión Web"
        val isFirstPlay = !historyManager.getHistory().any { it.url == url || it.url.substringBefore('?') == url.substringBefore('?') }
        val savedPos = if (isFirstPlay) 0L else (resumePositionMs ?: com.omnicast.sender.stream.StreamResumeManager.getPosition(this, url))
        historyManager.addOrUpdate(url, safeTitle, headers = headers)
        ActiveStreamHolder.startCasting(url, safeTitle, headers)
        com.omnicast.sender.service.CastMediaNotificationService.start(this)
        ActiveStreamHolder.onSendPlayControl = { act, pos, vol, par -> sendPlayControl(act, pos, vol, par) }
        sendSignalingMessage(SignalingMessage.DirectStreamHandoff(mediaUrl = url, title = safeTitle, headers = headers, startPositionMs = savedPos))
        if (savedPos > 0L) {
            val formatted = com.omnicast.sender.stream.StreamResumeManager.formatTime(savedPos)
            Toast.makeText(this, "▶ Reanudando en $formatted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Enviando video directo a la TV Box...", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendPlayControl(action: String, positionMs: Long = 0L, volume: Float = 1.0f, param: String? = null) {
        sendSignalingMessage(SignalingMessage.PlayControl(action, positionMs, volume, param))
    }

    fun sendImageToTv(url: String, title: String? = null) {
        sendSignalingMessage(SignalingMessage.ShowImage(imageUrl = url, title = title))
        Toast.makeText(this, "🖼️ Foto proyectada en la TV Box", Toast.LENGTH_SHORT).show()
    }

    private fun sendSignalingMessage(msg: SignalingMessage) {
        val client = signalingClient
        if (client.isConnected) client.sendMessage(msg) else {
            val ip = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE).getString("saved_tv_ip", "") ?: ""
            if (ip.isNotBlank()) client.connect(ip, 8088, { client.sendMessage(msg) }, {})
        }
    }

    private fun startScreenStreaming(target: DeviceTarget, profile: StreamProfile, tvScaleMode: String, permissionData: Intent) {
        autoPromptCapture.value = false
        com.omnicast.sender.service.CastMediaNotificationService.stop(this)
        val prefs = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
        val smoothBuffer = prefs.getBoolean("saved_smooth_buffer", true)
        ProjectionForegroundService.startProjection(this, target.ipAddress, target.port, profile, permissionData, tvScaleMode, smoothBuffer)
        Toast.makeText(this, "Proyectando pantalla en ${target.name}", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch { kotlinx.coroutines.delay(700); if (!isFinishing && !isDestroyed) moveTaskToBack(true) }
    }

    private fun stopScreenStreaming() {
        ProjectionForegroundService.stopService(this); sendDisconnectStatic(this); Toast.makeText(this, "Proyección finalizada", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy(); instance = null; com.omnicast.sender.media.LocalImageServer.stop()
        try { nsdScanner.stopDiscovery() } catch (e: Throwable) { Log.e("Sender", "NSD stop", e) }
    }
}
