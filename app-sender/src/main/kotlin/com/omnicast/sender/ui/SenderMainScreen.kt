package com.omnicast.sender.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnicast.core.domain.models.DeviceTarget
import com.omnicast.core.domain.models.StreamDetectionResult
import com.omnicast.core.domain.models.StreamProfile
import com.omnicast.sender.discovery.NsdScannerManager
import com.omnicast.sender.service.ProjectionForegroundService
import com.omnicast.sender.service.TvZoomManager
import com.omnicast.sender.ui.components.*
import com.omnicast.sender.ui.theme.OmniThemeColors

@Composable
fun SenderMainScreen(
    nsdScanner: NsdScannerManager,
    autoPromptCapture: Boolean = false,
    isTvConnected: Boolean = false,
    initialOpenBrowser: Boolean = false,
    initialOpenGallery: Boolean = false,
    onConnectToTv: (String, Int) -> Unit = { _, _ -> },
    onAnalyzeStream: (String?) -> StreamDetectionResult,
    onSendDirectStream: (String, String?, Map<String, String>) -> Unit,
    onStartProjection: (DeviceTarget, StreamProfile, String, Intent) -> Unit,
    onStopProjection: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE) }
    val devices by nsdScanner.discoveredDevices.collectAsState()
    var selectedDevice by remember { mutableStateOf<DeviceTarget?>(null) }
    var manualIp by remember { mutableStateOf(prefs.getString("saved_tv_ip", "") ?: "") }
    var manualPort by remember { mutableStateOf(prefs.getInt("saved_tv_port", 8088).toString()) }
    val savedProfileName = prefs.getString("saved_stream_profile", StreamProfile.BALANCED_30FPS.mode.name)
    var selectedProfile by remember {
        mutableStateOf(StreamProfile.ALL_PROFILES.firstOrNull { it.mode.name == savedProfileName } ?: StreamProfile.BALANCED_30FPS)
    }
    var tvScaleMode by remember {
        val raw = prefs.getString("saved_tv_scale_mode", "FIT_100") ?: "FIT_100"
        mutableStateOf(if (raw.startsWith("ZOOM")) "FIT_100" else raw)
    }
    var smoothBufferEnabled by remember { mutableStateOf(prefs.getBoolean("saved_smooth_buffer", true)) }
    val isStreaming by com.omnicast.sender.service.CastSessionState.isProjectingFlow.collectAsState()
    var detectionResult by remember { mutableStateOf<StreamDetectionResult?>(null) }
    var showInAppBrowser by rememberSaveable { mutableStateOf(initialOpenBrowser) }
    var browserInitialUrl by remember { mutableStateOf<String?>(null) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showSideDrawer by remember { mutableStateOf(false) }
    var tooltipsEnabled by remember { mutableStateOf(prefs.getBoolean("saved_tooltips_enabled", false)) }
    val isCasting by com.omnicast.sender.stream.ActiveStreamHolder.isCasting.collectAsState()
    val isGalleryCasting by com.omnicast.sender.service.CastSessionState.isGalleryCastingFlow.collectAsState()
    val isPlaying by com.omnicast.sender.stream.ActiveStreamHolder.isPlaying.collectAsState()
    val posMs by com.omnicast.sender.stream.ActiveStreamHolder.positionMs.collectAsState()
    val durMs by com.omnicast.sender.stream.ActiveStreamHolder.durationMs.collectAsState()
    val castTitle by com.omnicast.sender.stream.ActiveStreamHolder.currentMediaTitle.collectAsState()
    val secondaryIps by com.omnicast.sender.discovery.MultiReceiverManager.secondaryIps.collectAsState()
    DisposableEffect(Unit) {
        SenderMainActivity.onRequestReturnToMain = { showInAppBrowser = false }
        SenderMainActivity.onRequestOpenProjectionConfig = { showInAppBrowser = false; showConfigDialog = true }
        if (SenderMainActivity.pendingOpenProjectionConfig.value) {
            SenderMainActivity.pendingOpenProjectionConfig.value = false
            showInAppBrowser = false; showConfigDialog = true
        }
        onDispose { SenderMainActivity.onRequestReturnToMain = null; SenderMainActivity.onRequestOpenProjectionConfig = null }
    }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    androidx.activity.compose.BackHandler(enabled = !showInAppBrowser) {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000L) (context as? Activity)?.finishAffinity()
        else { lastBackPressTime = now; Toast.makeText(context, "Presione nuevamente para salir", Toast.LENGTH_SHORT).show() }
    }

    LaunchedEffect(initialOpenBrowser) { if (initialOpenBrowser) showInAppBrowser = true }
    LaunchedEffect(initialOpenGallery) {
        if (initialOpenGallery) context.startActivity(Intent(context, com.omnicast.sender.overlay.GalleryPickerActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    fun saveTarget(ip: String, portStr: String) = prefs.edit().putString("saved_tv_ip", ip).putInt("saved_tv_port", portStr.toIntOrNull() ?: 8088).apply()

    fun clearTarget() {
        prefs.edit().remove("saved_tv_ip").remove("saved_tv_port").apply()
        manualIp = ""; selectedDevice = null
        devices.firstOrNull()?.let {
            selectedDevice = it; manualIp = it.ipAddress; manualPort = it.port.toString()
            saveTarget(it.ipAddress, manualPort); onConnectToTv(it.ipAddress, it.port)
        }
        Toast.makeText(context, "🧹 IP limpiada. Auto-detectando TV...", Toast.LENGTH_SHORT).show()
    }

    var pendingProjectionMode by remember { mutableStateOf("FULL_SCREEN") }
    fun getCaptureIntent(isFull: Boolean = true): Intent {
        val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val cfg = if (isFull) android.media.projection.MediaProjectionConfig.createConfigForDefaultDisplay() else android.media.projection.MediaProjectionConfig.createConfigForUserChoice()
            mpm.createScreenCaptureIntent(cfg)
        } else mpm.createScreenCaptureIntent()
    }

    LaunchedEffect(devices) {
        if (devices.isNotEmpty()) {
            val matching = devices.firstOrNull { it.ipAddress == manualIp }
            if (matching != null) {
                selectedDevice = matching
            } else if (!isTvConnected || manualIp.isBlank()) {
                val first = devices.first()
                selectedDevice = first; manualIp = first.ipAddress; manualPort = first.port.toString()
                saveTarget(first.ipAddress, manualPort); onConnectToTv(first.ipAddress, first.port)
            }
        }
    }

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            saveTarget(manualIp.trim(), manualPort)
            val target = selectedDevice ?: DeviceTarget("direct-tv", "OmniCast TV Box", manualIp.trim(), manualPort.toIntOrNull() ?: 8088)
            onStartProjection(target, selectedProfile, pendingProjectionMode, result.data!!)
        } else {
            Toast.makeText(context, "Permiso de captura de pantalla denegado", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(autoPromptCapture) {
        if (autoPromptCapture) {
            try { pendingProjectionMode = "FULL_SCREEN"; projectionLauncher.launch(getCaptureIntent(true)) } catch (e: Throwable) {
                android.util.Log.e("SenderMainScreen", "Error autoPromptCapture", e)
            }
        }
    }

    SideMediaDrawer(
        isOpen = showSideDrawer,
        onClose = { showSideDrawer = false },
        onPlayOnTv = { url, title, hdrs, _ ->
            onSendDirectStream(url, title, hdrs)
            Toast.makeText(context, "Transmitiendo a TV Box...", Toast.LENGTH_SHORT).show()
        },
        onOpenInBrowser = {
            browserInitialUrl = it
            showInAppBrowser = true
        }
    ) {
        if (showInAppBrowser) {
            com.omnicast.sender.ui.browser.InAppBrowserScreen(
                onSendStreamToTv = { url, title, hdrs ->
                    onSendDirectStream(url, title, hdrs)
                    Toast.makeText(context, "Transmitiendo a TV Box...", Toast.LENGTH_SHORT).show()
                },
                onCloseBrowser = { showInAppBrowser = false; browserInitialUrl = null },
                isTvConnected = isTvConnected,
                initialUrl = browserInitialUrl
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OmniThemeColors.Background)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Encabezado con OmniCast Pro y botón de ayuda
                item {
                    SenderHeaderBar(
                        tooltipsEnabled = tooltipsEnabled,
                        onToggleTooltips = {
                            tooltipsEnabled = it
                            prefs.edit().putBoolean("saved_tooltips_enabled", it).apply()
                        },
                        onOpenHelp = { showHelpDialog = true },
                        onOpenDrawer = { showSideDrawer = true },
                        onExitApp = { SenderMainActivity.exitCompletely(context) }
                    )
                }

                // 2. Activar agente que se superpone (Asistente Flotante)
                item {
                    FloatingAssistantCard(tooltipsEnabled = tooltipsEnabled)
                }

                // 3. IP del TV al que se conecta (TV Receptora)
                item {
                    DeviceSelectionCard(
                        discoveredDevices = devices, selectedDevice = selectedDevice, manualIp = manualIp, manualPort = manualPort,
                        isConnected = isTvConnected, tooltipsEnabled = tooltipsEnabled, secondaryIps = secondaryIps,
                        onToggleSecondaryIp = { com.omnicast.sender.discovery.MultiReceiverManager.toggleIp(context, it) },
                        onManualIpChange = { manualIp = it; saveTarget(it, manualPort) },
                        onManualPortChange = { manualPort = it; saveTarget(manualIp, it) },
                        onSelectDevice = {
                            selectedDevice = it; manualIp = it.ipAddress; manualPort = it.port.toString()
                            saveTarget(it.ipAddress, manualPort); onConnectToTv(it.ipAddress, it.port)
                        },
                        onClearIp = { clearTarget() },
                        onConnect = { saveTarget(manualIp.trim(), manualPort); onConnectToTv(manualIp.trim(), manualPort.toIntOrNull() ?: 8088) }
                    )
                }

                if (isCasting) {
                    item {
                        MoviePlaybackControlCard(
                            title = castTitle, isPlaying = isPlaying, positionMs = posMs, durationMs = durMs,
                            onPlayPause = {
                                val act = if (isPlaying) "PAUSE" else "PLAY"
                                com.omnicast.sender.stream.ActiveStreamHolder.onSendPlayControl?.invoke(act, posMs, 1f, null)
                                com.omnicast.sender.stream.ActiveStreamHolder.updatePlayback(!isPlaying, posMs, durMs)
                            },
                            onSeek = { targetMs ->
                                com.omnicast.sender.stream.ActiveStreamHolder.onSendPlayControl?.invoke("SEEK", targetMs, 1f, null)
                                com.omnicast.sender.stream.ActiveStreamHolder.updatePlayback(isPlaying, targetMs, durMs)
                            },
                            onStop = {
                                com.omnicast.sender.stream.ActiveStreamHolder.stopCasting()
                                com.omnicast.sender.service.CastMediaNotificationService.stop(context)
                            },
                            onOpenBrowser = { showInAppBrowser = true }
                        )
                    }
                }

                // 4. Las TRES opciones para poder elegirlas: Captura Web, Casteo-TV y Galería
                item {
                    MainActionCards(
                        isStreaming = isStreaming, isCasting = isCasting, isGalleryCasting = isGalleryCasting,
                        selectedProfile = selectedProfile, smoothBufferEnabled = smoothBufferEnabled, tvScaleMode = tvScaleMode, tooltipsEnabled = tooltipsEnabled,
                        onOpenBrowser = { showInAppBrowser = true },
                        onStartFullScreen = { pendingProjectionMode = "FULL_SCREEN"; projectionLauncher.launch(getCaptureIntent(true)) },
                        onStartAppProjection = {
                            pendingProjectionMode = "APP"
                            Toast.makeText(context, "🔒 Modo Por Aplicación: mantén seleccionada 'Una sola app'", Toast.LENGTH_LONG).show()
                            projectionLauncher.launch(getCaptureIntent(false))
                        },
                        onStopProjection = {
                            if (isStreaming) onStopProjection()
                            if (isCasting) { com.omnicast.sender.stream.ActiveStreamHolder.stopCasting(); com.omnicast.sender.service.CastMediaNotificationService.stop(context) }
                            if (isGalleryCasting) com.omnicast.sender.service.CastSessionState.isGalleryCasting = false
                            SenderMainActivity.sendPlayControlStatic(context, "STOP")
                        },
                        onOpenProjectionConfig = { showConfigDialog = true },
                        onOpenGallery = { context.startActivity(Intent(context, com.omnicast.sender.overlay.GalleryPickerActivity::class.java).apply { putExtra(com.omnicast.sender.overlay.GalleryPickerActivity.EXTRA_MIME_FILTER, "image/*") }) },
                        onPickExternalFile = { context.startActivity(Intent(context, com.omnicast.sender.overlay.GalleryPickerActivity::class.java).apply { putExtra(com.omnicast.sender.overlay.GalleryPickerActivity.EXTRA_MIME_FILTER, "video/*") }) }
                    )
                }
            }
        }
    }

    if (showConfigDialog) {
        ProjectionConfigDialog(
            selectedProfile = selectedProfile,
            onSelectProfile = { selectedProfile = it; prefs.edit().putString("saved_stream_profile", it.mode.name).apply() },
            smoothBufferEnabled = smoothBufferEnabled,
            onToggleSmoothBuffer = { smoothBufferEnabled = it; prefs.edit().putBoolean("saved_smooth_buffer", it).apply() },
            tvScaleMode = tvScaleMode,
            onSelectTvScaleMode = {
                tvScaleMode = it; prefs.edit().putString("saved_tv_scale_mode", it).apply()
                val preset = TvZoomManager.PRESETS.firstOrNull { p -> p.id == it || p.mode == it }
                if (isStreaming) ProjectionForegroundService.setTvScaleMode(it)
                else if (preset != null) com.omnicast.sender.network.QuickSignalingHelper.sendQuickMessage(context, TvZoomManager.toSignalingMessage(preset))
            },
            tooltipsEnabled = tooltipsEnabled, onDismiss = { showConfigDialog = false }
        )
    }

    detectionResult?.let { res ->
        StreamResultDialog(
            result = res, onSendDirectStream = { url -> onSendDirectStream(url, null, emptyMap()) },
            onStartScreenMirroring = { pendingProjectionMode = "FULL_SCREEN"; projectionLauncher.launch(getCaptureIntent(true)) },
            onDismiss = { detectionResult = null }
        )
    }

    if (showHelpDialog) SenderHelpDialog(onDismiss = { showHelpDialog = false })
}
