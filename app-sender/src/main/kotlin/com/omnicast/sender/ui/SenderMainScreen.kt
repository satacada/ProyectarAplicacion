package com.omnicast.sender.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.core.domain.models.DeviceTarget
import com.omnicast.core.domain.models.StreamDetectionResult
import com.omnicast.core.domain.models.StreamProfile
import com.omnicast.sender.discovery.NsdScannerManager
import com.omnicast.sender.ui.components.*

@Composable
fun SenderMainScreen(
    nsdScanner: NsdScannerManager,
    autoPromptCapture: Boolean = false,
    isTvConnected: Boolean = false,
    initialOpenBrowser: Boolean = false,
    onConnectToTv: (String, Int) -> Unit = { _, _ -> },
    onAnalyzeStream: (String?) -> StreamDetectionResult,
    onSendDirectStream: (String, String?, Map<String, String>) -> Unit,
    onStartProjection: (DeviceTarget, StreamProfile, String, Intent) -> Unit,
    onStopProjection: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE) }
    val devices by nsdScanner.discoveredDevices.collectAsState()
    var selectedDevice by remember { mutableStateOf<DeviceTarget?>(null) }
    var manualIp by remember { mutableStateOf(prefs.getString("saved_tv_ip", "192.168.0.136") ?: "192.168.0.136") }
    var manualPort by remember { mutableStateOf(prefs.getInt("saved_tv_port", 8088).toString()) }
    val savedProfileName = prefs.getString("saved_stream_profile", StreamProfile.BALANCED_30FPS.mode.name)
    var selectedProfile by remember {
        mutableStateOf(
            StreamProfile.ALL_PROFILES.firstOrNull { it.mode.name == savedProfileName } ?: StreamProfile.BALANCED_30FPS
        )
    }
    var tvScaleMode by remember { mutableStateOf(prefs.getString("saved_tv_scale_mode", "EXTEND_16_9") ?: "EXTEND_16_9") }
    var smoothBufferEnabled by remember { mutableStateOf(prefs.getBoolean("saved_smooth_buffer", true)) }
    var isStreaming by remember { mutableStateOf(com.omnicast.sender.service.ProjectionForegroundService.isProjecting) }
    var detectionResult by remember { mutableStateOf<StreamDetectionResult?>(null) }
    var showInAppBrowser by remember { mutableStateOf(initialOpenBrowser) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(initialOpenBrowser) { if (initialOpenBrowser) showInAppBrowser = true }

    fun saveTarget(ip: String, portStr: String) {
        val port = portStr.toIntOrNull() ?: 8088
        prefs.edit().putString("saved_tv_ip", ip).putInt("saved_tv_port", port).apply()
    }

    fun getSafeCaptureIntent(): Intent {
        val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return mpm.createScreenCaptureIntent()
    }

    LaunchedEffect(devices) {
        if (selectedDevice == null && devices.isNotEmpty()) {
            val first = devices.first()
            selectedDevice = first
            manualIp = first.ipAddress
            manualPort = first.port.toString()
            saveTarget(first.ipAddress, manualPort)
            onConnectToTv(first.ipAddress, first.port)
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
            try {
                projectionLauncher.launch(getSafeCaptureIntent())
            } catch (e: Throwable) {
                android.util.Log.e("SenderMainScreen", "Error autoPromptCapture", e)
            }
        }
    }

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
            modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SenderHeaderBar(onOpenHelp = { showHelpDialog = true }) }
            item { OverlayPermissionBanner() }
            item {
                DeviceSelectionCard(
                    discoveredDevices = devices,
                    selectedDevice = selectedDevice,
                    manualIp = manualIp,
                    manualPort = manualPort,
                    isConnected = isTvConnected,
                    onManualIpChange = {
                        manualIp = it
                        saveTarget(it, manualPort)
                    },
                    onManualPortChange = {
                        manualPort = it
                        saveTarget(manualIp, it)
                    },
                    onSelectDevice = {
                        selectedDevice = it
                        manualIp = it.ipAddress
                        manualPort = it.port.toString()
                        saveTarget(it.ipAddress, manualPort)
                        onConnectToTv(it.ipAddress, it.port)
                    },
                    onConnect = {
                        saveTarget(manualIp.trim(), manualPort)
                        onConnectToTv(manualIp.trim(), manualPort.toIntOrNull() ?: 8088)
                    }
                )
            }
            item {
                val tabTitles = listOf("🌐 Web / Video", "📱 Proyección TV", "🖼️ Galería")
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xFF38BDF8),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF38BDF8)
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                )
                            }
                        )
                    }
                }
            }
            when (selectedTabIndex) {
                0 -> {
                    item { InAppBrowserCard(onOpenBrowser = { showInAppBrowser = true }) }
                    item {
                        DirectStreamSection(onAnalyzeStream = { url -> detectionResult = onAnalyzeStream(url) })
                    }
                }
                1 -> {
                    item {
                        ScreenMirrorSection(
                            isStreaming = isStreaming,
                            selectedProfile = selectedProfile,
                            onSelectProfile = {
                                selectedProfile = it
                                prefs.edit().putString("saved_stream_profile", it.mode.name).apply()
                            },
                            tvScaleMode = tvScaleMode,
                            onSelectTvScaleMode = {
                                tvScaleMode = it
                                prefs.edit().putString("saved_tv_scale_mode", it).apply()
                                if (isStreaming) {
                                    com.omnicast.sender.service.ProjectionForegroundService.setTvScaleMode(it)
                                }
                            },
                            smoothBufferEnabled = smoothBufferEnabled,
                            onToggleSmoothBuffer = {
                                smoothBufferEnabled = it
                                prefs.edit().putBoolean("saved_smooth_buffer", it).apply()
                            },
                            onStartProjection = { projectionLauncher.launch(getSafeCaptureIntent()) },
                            onStopProjection = {
                                isStreaming = false
                                onStopProjection()
                            }
                        )
                    }
                }
                2 -> {
                    item {
                        GallerySection(onPickImage = {
                            context.startActivity(
                                Intent(context, com.omnicast.sender.overlay.GalleryPickerActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        })
                    }
                }
            }
        }

        detectionResult?.let { res ->
            StreamResultDialog(
                result = res,
                onSendDirectStream = { url -> onSendDirectStream(url, null, emptyMap()) },
                onStartScreenMirroring = { projectionLauncher.launch(getSafeCaptureIntent()) },
                onDismiss = { detectionResult = null }
            )
        }
    }

    if (showHelpDialog) {
        SenderHelpDialog(onDismiss = { showHelpDialog = false })
    }
}
