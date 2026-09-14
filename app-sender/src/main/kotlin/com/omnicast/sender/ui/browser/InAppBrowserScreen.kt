package com.omnicast.sender.ui.browser

import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnicast.sender.history.PlayHistoryManager
import com.omnicast.sender.stream.ActiveStreamHolder
import kotlinx.coroutines.launch

@Composable
fun InAppBrowserScreen(
    onSendStreamToTv: (url: String, title: String, headers: Map<String, String>) -> Unit,
    onCloseBrowser: () -> Unit,
    isTvConnected: Boolean = true
) {
    val context = LocalContext.current
    val historyManager = remember { PlayHistoryManager(context) }
    var inputUrl by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf("") }
    var isHome by remember { mutableStateOf(true) }
    var pageTitle by remember { mutableStateOf("OmniCast Browser") }
    var progress by remember { mutableStateOf(0) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var isAdBlockEnabled by remember { mutableStateOf(true) }
    val detectedStreams = remember { mutableStateListOf<String>() }
    val streamHeaders = remember { mutableStateMapOf<String, Map<String, String>>() }
    val streamLabels = remember { mutableStateMapOf<String, String>() }
    var qualityOptions by remember { mutableStateOf<List<StreamQualityOption>>(emptyList()) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var historyList by remember { mutableStateOf(historyManager.getHistory()) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var popupWebView by remember { mutableStateOf<WebView?>(null) }
    val scope = rememberCoroutineScope()

    val isCasting by ActiveStreamHolder.isCasting.collectAsState()
    val isPlaying by ActiveStreamHolder.isPlaying.collectAsState()
    val posMs by ActiveStreamHolder.positionMs.collectAsState()
    val durMs by ActiveStreamHolder.durationMs.collectAsState()
    val castTitle by ActiveStreamHolder.currentMediaTitle.collectAsState()

    androidx.activity.compose.BackHandler(enabled = true) {
        if (popupWebView != null) { popupWebView?.destroy(); popupWebView = null }
        else if (showHistorySheet) showHistorySheet = false
        else if (showOptionsSheet) showOptionsSheet = false
        else if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack()
        else if (!isHome) isHome = true else onCloseBrowser()
    }

    fun registerStream(url: String, hdrs: Map<String, String>, isPrimary: Boolean = false, label: String? = null) {
        if (!detectedStreams.contains(url)) { if (isPrimary) detectedStreams.add(0, url) else detectedStreams.add(url) }
        else if (isPrimary && detectedStreams.firstOrNull() != url) { detectedStreams.remove(url); detectedStreams.add(0, url) }
        if (hdrs.isNotEmpty()) streamHeaders[url] = hdrs; if (!label.isNullOrBlank()) streamLabels[url] = label
        ActiveStreamHolder.update(url, pageTitle, hdrs)
    }

    fun checkAndRegisterYouTube(url: String?, title: String? = null) {
        val ytId = BrowserUrlHelper.extractYouTubeId(url ?: return) ?: return
        registerStream("https://www.youtube.com/watch?v=$ytId", mapOf("X-YouTube-Id" to ytId, "User-Agent" to UA_MOBILE), isPrimary = true, label = "YouTube")
        if (!title.isNullOrBlank()) pageTitle = title.replace(" - YouTube", "")
    }

    fun navigateTo(url: String) {
        detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); qualityOptions = emptyList()
        val target = BrowserUrlHelper.normalizeUrl(url)
        inputUrl = target; currentUrl = target; isHome = false; checkAndRegisterYouTube(target); webViewInstance?.loadUrl(target)
    }

    fun openOptionsSheet() {
        if (detectedStreams.isEmpty()) {
            Toast.makeText(context, "Reproduce el video en la página para capturar el flujo", Toast.LENGTH_SHORT).show()
            webViewInstance?.evaluateJavascript(WvcDomSniffer.getInjectionScript(context), null)
            return
        }
        val cleanTitle = pageTitle.replace(" - YouTube", "").replace(" | OK", "").trim().take(22)
        val prefix = if (cleanTitle.isNotBlank() && cleanTitle != "Transmisión" && cleanTitle != "OmniCast Browser") "[$cleanTitle] " else ""
        val allOpts = mutableListOf<StreamQualityOption>()
        detectedStreams.forEach { url ->
            val ytId = BrowserUrlHelper.extractYouTubeId(url)
            if (ytId != null) {
                allOpts.addAll(BrowserUrlHelper.createYouTubeQualityOptions(ytId, cleanTitle))
            } else {
                val qLabel = streamLabels[url] ?: if (url.contains(".m3u8", true)) "HLS Adaptativo" else "Video Directo"
                allOpts.add(StreamQualityOption("🎬 $prefix$qLabel", qLabel, "Máxima Calidad", url, isMaster = qLabel.contains("1080") || qLabel.contains("HLS")))
            }
        }
        qualityOptions = allOpts.distinctBy { it.streamUrl }
        showOptionsSheet = true
        scope.launch {
            val deep = mutableListOf<StreamQualityOption>()
            detectedStreams.forEach { u ->
                val hdrs = streamHeaders[u] ?: emptyMap()
                deep.addAll(StreamQualityDetector.detectQualities(u, hdrs, cleanTitle))
            }
            if (deep.isNotEmpty()) qualityOptions = (qualityOptions + deep).distinctBy { it.label }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        BrowserTopBar(
            inputUrl = inputUrl, onInputUrlChange = { inputUrl = it }, onNavigate = { navigateTo(it) }, onClose = onCloseBrowser,
            onBack = { if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack() else isHome = true },
            onReload = { webViewInstance?.reload() }, isDesktopMode = isDesktopMode,
            onToggleDesktopMode = { isDesktopMode = !isDesktopMode; webViewInstance?.settings?.userAgentString = if (isDesktopMode) UA_DESKTOP else UA_MOBILE; webViewInstance?.reload() },
            isAdBlockEnabled = isAdBlockEnabled, onToggleAdBlock = { isAdBlockEnabled = !isAdBlockEnabled },
            onForceRescan = { webViewInstance?.evaluateJavascript(WvcDomSniffer.getInjectionScript(context), null) },
            onClearData = { webViewInstance?.clearCache(true); webViewInstance?.clearHistory(); detectedStreams.clear(); qualityOptions = emptyList(); isHome = true; inputUrl = "" },
            detectedStreamsCount = detectedStreams.size, onOpenVideoList = { openOptionsSheet() },
            onOpenHistory = { historyList = historyManager.getHistory(); showHistorySheet = true }, isTvConnected = isTvConnected
        )

        if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(3.dp), color = Color(0xFF38BDF8))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isHome) BrowserHomeScreen(onSelectUrl = { navigateTo(it) })

            BrowserWebView(
                isHome = isHome, isAdBlockEnabled = isAdBlockEnabled, currentUrl = currentUrl,
                onProgress = { progress = it },
                onPageStarted = {
                    val prevHost = try { java.net.URI(currentUrl).host } catch (_: Throwable) { "" }
                    val newHost = try { java.net.URI(it).host } catch (_: Throwable) { "" }
                    if (!prevHost.isNullOrEmpty() && !newHost.isNullOrEmpty() && prevHost != newHost) {
                        detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); qualityOptions = emptyList()
                    }
                    inputUrl = it; currentUrl = it; checkAndRegisterYouTube(it)
                },
                onPageFinished = { t, u -> pageTitle = t; checkAndRegisterYouTube(u, pageTitle) },
                onHistoryUpdated = { u, t -> currentUrl = u; if (!t.isNullOrBlank()) pageTitle = t; checkAndRegisterYouTube(u, t) },
                onStreamIntercepted = { sUrl, hdrs, isM -> registerStream(sUrl, hdrs, isPrimary = isM) },
                onSniffedVideo = { sniffed ->
                    val hdrs = BrowserUrlHelper.buildSafeHeaders(sniffed.src, currentUrl, UA_MOBILE)
                    if (sniffed.isPlaying) {
                        detectedStreams.clear()
                        registerStream(sniffed.src, hdrs, isPrimary = true, label = sniffed.label)
                        if (sniffed.title.isNotBlank()) pageTitle = sniffed.title
                        Toast.makeText(context, "▶ Video: ${pageTitle.take(24)}", Toast.LENGTH_SHORT).show()
                    } else {
                        registerStream(sniffed.src, hdrs, isPrimary = false, label = sniffed.label)
                        if (sniffed.title.isNotBlank()) pageTitle = sniffed.title
                    }
                },
                onOpenPopup = { popupWebView = it; Toast.makeText(context, "⚠️ Pestaña emergente aislada. Pulsa 'Descartar' para volver.", Toast.LENGTH_SHORT).show() },
                onClosePopup = { popupWebView?.destroy(); popupWebView = null },
                onWebViewCreated = { webViewInstance = it }
            )

            if (isCasting || (!isHome && detectedStreams.isNotEmpty())) {
                BrowserBottomBarContainer(
                    isCasting = isCasting, castTitle = castTitle, isPlaying = isPlaying,
                    posMs = posMs, durMs = durMs, detectedCount = detectedStreams.size, pageTitle = pageTitle,
                    onTransmitDirect = {
                        val first = detectedStreams.firstOrNull() ?: return@BrowserBottomBarContainer
                        onSendStreamToTv(first, pageTitle, streamHeaders[first] ?: emptyMap())
                    },
                    onOpenQualities = { openOptionsSheet() },
                    onPlayControl = { act, pos, vol -> ActiveStreamHolder.onSendPlayControl?.invoke(act, pos, vol, null) },
                    onStopCasting = { ActiveStreamHolder.stopCasting() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            popupWebView?.let { pView ->
                BrowserPopupOverlay(popupView = pView, onDismiss = { popupWebView?.destroy(); popupWebView = null })
            }
        }
    }

    BrowserModalSheets(
        showOptionsSheet = showOptionsSheet, showHistorySheet = showHistorySheet,
        pageTitle = pageTitle, qualityOptions = qualityOptions, historyList = historyList,
        onTransmitOption = { opt -> showOptionsSheet = false; onSendStreamToTv(opt.streamUrl, pageTitle, streamHeaders[opt.streamUrl] ?: streamHeaders.values.firstOrNull { it.isNotEmpty() } ?: emptyMap()) },
        onTransmitHistory = { item -> showHistorySheet = false; onSendStreamToTv(item.url, item.title, item.headers) },
        onDismissOptions = { showOptionsSheet = false },
        onClearOptions = { detectedStreams.clear(); qualityOptions = emptyList(); showOptionsSheet = false },
        onDeleteHistory = { historyManager.remove(it); historyList = historyManager.getHistory() },
        onClearHistory = { historyManager.clear(); historyList = emptyList() }, onDismissHistory = { showHistorySheet = false }
    )
}
