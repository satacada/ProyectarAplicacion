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
    var tvScaleMode by remember { mutableStateOf(prefs.getString("saved_tv_scale_mode", "FIT_100") ?: "FIT_100") }
    var smoothBufferEnabled by remember { mutableStateOf(prefs.getBoolean("saved_smooth_buffer", true)) }
    var isStreaming by remember { mutableStateOf(ProjectionForegroundService.isProjecting) }
    var detectionResult by remember { mutableStateOf<StreamDetectionResult?>(null) }
    var showInAppBrowser by rememberSaveable { mutableStateOf(initialOpenBrowser) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showSideDrawer by remember { mutableStateOf(false) }
    var tooltipsEnabled by remember { mutableStateOf(prefs.getBoolean("saved_tooltips_enabled", false)) }
    val isCasting by com.omnicast.sender.stream.ActiveStreamHolder.isCasting.collectAsState()
    val isPlaying by com.omnicast.sender.stream.ActiveStreamHolder.isPlaying.collectAsState()
    val posMs by com.omnicast.sender.stream.ActiveStreamHolder.positionMs.collectAsState()
    val durMs by com.omnicast.sender.stream.ActiveStreamHolder.durationMs.collectAsState()
    val castTitle by com.omnicast.sender.stream.ActiveStreamHolder.currentMediaTitle.collectAsState()

    LaunchedEffect(initialOpenBrowser) { if (initialOpenBrowser) showInAppBrowser = true }
    LaunchedEffect(initialOpenGallery) {
        if (initialOpenGallery) context.startActivity(Intent(context, com.omnicast.sender.overlay.GalleryPickerActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    fun saveTarget(ip: String, portStr: String) {
        val port = portStr.toIntOrNull() ?: 8088
        prefs.edit().putString("saved_tv_ip", ip).putInt("saved_tv_port", port).apply()
    }

    fun clearTarget() {
        prefs.edit().remove("saved_tv_ip").remove("saved_tv_port").apply()
        manualIp = ""; selectedDevice = null
        devices.firstOrNull()?.let {
            selectedDevice = it; manualIp = it.ipAddress; manualPort = it.port.toString()
            saveTarget(it.ipAddress, manualPort); onConnectToTv(it.ipAddress, it.port)
        }
        Toast.makeText(context, "🧹 IP limpiada. Auto-detectando TV...", Toast.LENGTH_SHORT).show()
    }

    fun getSafeCaptureIntent(): Intent {
        val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return mpm.createScreenCaptureIntent()
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
            isStreaming = true
            onStartProjection(target, selectedProfile, tvScaleMode, result.data!!)
        } else {
            Toast.makeText(context, "Permiso de captura de pantalla denegado", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(autoPromptCapture) {
        if (autoPromptCapture) {
            try { projectionLauncher.launch(getSafeCaptureIntent()) } catch (e: Throwable) {
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
            prefs.edit().putString("saved_browser_last_url", it).apply()
            showInAppBrowser = true
        }
    ) {
        if (showInAppBrowser) {
            com.omnicast.sender.ui.browser.InAppBrowserScreen(
                onSendStreamToTv = { url, title, hdrs ->
                    onSendDirectStream(url, title, hdrs)
                    Toast.makeText(context, "Transmitiendo a TV Box...", Toast.LENGTH_SHORT).show()
                },
                onCloseBrowser = { showInAppBrowser = false },
                isTvConnected = isTvConnected
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
                        onOpenDrawer = { showSideDrawer = true }
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
                        isConnected = isTvConnected, tooltipsEnabled = tooltipsEnabled,
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
                        isStreaming = isStreaming,
                        selectedProfile = selectedProfile,
                        smoothBufferEnabled = smoothBufferEnabled,
                        tvScaleMode = tvScaleMode,
                        tooltipsEnabled = tooltipsEnabled,
                        onOpenBrowser = { showInAppBrowser = true },
                        onStartProjection = { projectionLauncher.launch(getSafeCaptureIntent()) },
                        onStopProjection = {
                            isStreaming = false
                            onStopProjection()
                        },
                        onOpenProjectionConfig = { showConfigDialog = true },
                        onOpenGallery = {
                            context.startActivity(
                                Intent(context, com.omnicast.sender.overlay.GalleryPickerActivity::class.java).apply {
                                    putExtra(com.omnicast.sender.overlay.GalleryPickerActivity.EXTRA_MIME_FILTER, "image/*")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        },
                        onPickExternalFile = {
                            context.startActivity(
                                Intent(context, com.omnicast.sender.overlay.GalleryPickerActivity::class.java).apply {
                                    putExtra(com.omnicast.sender.overlay.GalleryPickerActivity.EXTRA_MIME_FILTER, "video/*")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    if (showConfigDialog) {
        ProjectionConfigDialog(
            selectedProfile = selectedProfile,
            onSelectProfile = {
                selectedProfile = it
                prefs.edit().putString("saved_stream_profile", it.mode.name).apply()
            },
            smoothBufferEnabled = smoothBufferEnabled,
            onToggleSmoothBuffer = {
                smoothBufferEnabled = it
                prefs.edit().putBoolean("saved_smooth_buffer", it).apply()
            },
            tvScaleMode = tvScaleMode,
            onSelectTvScaleMode = {
                tvScaleMode = it
                prefs.edit().putString("saved_tv_scale_mode", it).apply()
                val preset = TvZoomManager.PRESETS.firstOrNull { p -> p.id == it || p.mode == it }
                if (isStreaming) {
                    ProjectionForegroundService.setTvScaleMode(it)
                } else if (preset != null) {
                    com.omnicast.sender.network.QuickSignalingHelper.sendQuickMessage(context, TvZoomManager.toSignalingMessage(preset))
                }
            },
            tooltipsEnabled = tooltipsEnabled,
            onDismiss = { showConfigDialog = false }
        )
    }

    detectionResult?.let { res ->
        StreamResultDialog(
            result = res,
            onSendDirectStream = { url -> onSendDirectStream(url, null, emptyMap()) },
            onStartScreenMirroring = { projectionLauncher.launch(getSafeCaptureIntent()) },
            onDismiss = { detectionResult = null }
        )
    }

    if (showHelpDialog) {
        SenderHelpDialog(onDismiss = { showHelpDialog = false })
    }
}
