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
import androidx.compose.runtime.saveable.rememberSaveable
import com.omnicast.sender.history.PlayHistoryManager
import com.omnicast.sender.stream.ActiveStreamHolder
import com.omnicast.sender.ui.SenderMainActivity
import kotlinx.coroutines.launch

@Composable
fun InAppBrowserScreen(
    onSendStreamToTv: (url: String, title: String, headers: Map<String, String>) -> Unit,
    onCloseBrowser: () -> Unit,
    isTvConnected: Boolean = true
) {
    val context = LocalContext.current
    val historyManager = remember { PlayHistoryManager(context) }
    val prefs = remember { context.getSharedPreferences("omnicast_prefs", android.content.Context.MODE_PRIVATE) }
    val savedLastUrl = remember { prefs.getString("saved_browser_last_url", "") ?: "" }
    val initialIsHome = savedLastUrl.isBlank() || savedLastUrl == "about:blank"
    var inputUrl by rememberSaveable { mutableStateOf(if (initialIsHome) "" else savedLastUrl) }
    var currentUrl by rememberSaveable { mutableStateOf(if (initialIsHome) "" else savedLastUrl) }
    var isHome by rememberSaveable { mutableStateOf(initialIsHome) }
    var pageTitle by rememberSaveable { mutableStateOf("OmniCast Browser") }
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
    val volume by ActiveStreamHolder.volume.collectAsState()
    val castTitle by ActiveStreamHolder.currentMediaTitle.collectAsState()

    androidx.activity.compose.BackHandler(enabled = true) {
        if (popupWebView != null) { popupWebView?.destroy(); popupWebView = null }
        else if (showHistorySheet) showHistorySheet = false
        else if (showOptionsSheet) showOptionsSheet = false
        else if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack()
        else if (!isHome) isHome = true else onCloseBrowser()
    }

    androidx.compose.runtime.DisposableEffect(webViewInstance) {
        SenderMainActivity.onBrowserRescanRequested = {
            webViewInstance?.evaluateJavascript(WvcDomSniffer.getInjectionScript(context), null)
            webViewInstance?.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null)
        }
        onDispose {
            SenderMainActivity.onBrowserRescanRequested = null
        }
    }

    fun registerStream(url: String, hdrs: Map<String, String>, isPrimary: Boolean = false, label: String? = null) {
        if (url.isBlank() || AdBlocker.isAd(url)) return
        val isM3u8 = url.contains(".m3u8", ignoreCase = true)
        val shouldBePrimary = isPrimary || isM3u8
        if (isPrimary) {
            if (!detectedStreams.contains(url)) detectedStreams.add(0, url)
            else if (detectedStreams.firstOrNull() != url) { detectedStreams.remove(url); detectedStreams.add(0, url) }
        } else {
            if (detectedStreams.size >= 10) return
            if (!detectedStreams.contains(url)) {
                if (shouldBePrimary) detectedStreams.add(0, url) else detectedStreams.add(url)
            }
        }
        if (hdrs.isNotEmpty()) streamHeaders[url] = hdrs; if (!label.isNullOrBlank()) streamLabels[url] = label
        val primaryUrl = detectedStreams.firstOrNull() ?: url
        val effectiveTitle = if (pageTitle.isNotBlank() && pageTitle != "Cargando..." && pageTitle != "Transmisión") pageTitle else "Video Web"
        ActiveStreamHolder.update(primaryUrl, effectiveTitle, streamHeaders[primaryUrl] ?: hdrs)
    }

    fun checkAndRegisterYouTube(url: String?, title: String? = null) {
        val ytId = BrowserUrlHelper.extractYouTubeId(url ?: return) ?: return
        val currentPrimary = detectedStreams.firstOrNull()
        val currentYtId = currentPrimary?.let { BrowserUrlHelper.extractYouTubeId(it) }
        if (currentYtId != null && currentYtId != ytId) {
            detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); qualityOptions = emptyList()
            ActiveStreamHolder.clear()
        }
        registerStream("https://www.youtube.com/watch?v=$ytId", mapOf("X-YouTube-Id" to ytId, "User-Agent" to UA_MOBILE), isPrimary = true, label = "YouTube")
        val cleanT = if (!title.isNullOrBlank() && title != "Transmisión") title.replace(" - YouTube", "") else webViewInstance?.title?.replace(" - YouTube", "")
        if (!cleanT.isNullOrBlank() && cleanT != "Transmisión") pageTitle = cleanT
    }

    fun navigateTo(url: String) {
        detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); qualityOptions = emptyList()
        ActiveStreamHolder.clear()
        val target = BrowserUrlHelper.normalizeUrl(url)
        inputUrl = target; currentUrl = target; isHome = false
        prefs.edit().putString("saved_browser_last_url", target).apply()
        if (target.contains("youtube.com") || target.contains("youtu.be")) checkAndRegisterYouTube(target)
        webViewInstance?.loadUrl(target)
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
            onForceRescan = {
                webViewInstance?.evaluateJavascript(WvcDomSniffer.getInjectionScript(context), null)
                webViewInstance?.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null)
            },
            onClearData = {
                webViewInstance?.clearCache(true); webViewInstance?.clearHistory()
                detectedStreams.clear(); qualityOptions = emptyList(); isHome = true; inputUrl = ""; currentUrl = ""
                ActiveStreamHolder.clear()
                prefs.edit().remove("saved_browser_last_url").apply()
            },
            detectedStreamsCount = detectedStreams.size, onOpenVideoList = { openOptionsSheet() },
            onOpenHistory = { historyList = historyManager.getHistory(); showHistorySheet = true }, isTvConnected = isTvConnected
        )

        if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(3.dp), color = Color(0xFF38BDF8))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isHome) BrowserHomeScreen(onSelectUrl = { navigateTo(it) })

            BrowserWebView(
                isHome = isHome, isAdBlockEnabled = isAdBlockEnabled, currentUrl = currentUrl,
                onProgress = { progress = it },
                onPageStarted = { newUrl ->
                    val isRealChange = currentUrl.isNotBlank() && currentUrl != newUrl && !newUrl.startsWith("about:")
                    if (isRealChange) {
                        detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); qualityOptions = emptyList()
                        ActiveStreamHolder.clear()
                        pageTitle = "Cargando..."
                    }
                    inputUrl = newUrl; currentUrl = newUrl
                    if (!newUrl.startsWith("about:")) prefs.edit().putString("saved_browser_last_url", newUrl).apply()
                    if (newUrl.contains("youtube.com") || newUrl.contains("youtu.be")) checkAndRegisterYouTube(newUrl)
                },
                onPageFinished = { t, u ->
                    if (!t.isNullOrBlank() && t != "Transmisión" && !t.contains("about:blank")) pageTitle = t
                    if (u?.contains("youtube.com") == true || u?.contains("youtu.be") == true) checkAndRegisterYouTube(u, pageTitle)
                },
                onHistoryUpdated = { u, t ->
                    val isRealChange = currentUrl.isNotBlank() && currentUrl != u && !u.startsWith("about:") && !u.contains("favicon")
                    if (isRealChange) {
                        detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); qualityOptions = emptyList()
                        ActiveStreamHolder.clear()
                    }
                    currentUrl = u; if (!t.isNullOrBlank() && t != "Transmisión" && !t.contains("about:blank")) pageTitle = t
                    if (!u.startsWith("about:")) prefs.edit().putString("saved_browser_last_url", u).apply()
                    if (u.contains("youtube.com") || u.contains("youtu.be")) checkAndRegisterYouTube(u, t)
                },
                onStreamIntercepted = { sUrl, hdrs, isM -> registerStream(sUrl, hdrs, isPrimary = isM) },
                onSniffedVideo = { sniffed ->
                    if (AdBlocker.isAd(sniffed.src)) return@BrowserWebView
                    if (sniffed.duration in 0.1f..59.9f) {
                        android.util.Log.d("InAppBrowser", "🛑 Ignorando video menor de 1 minuto (${sniffed.duration}s)")
                        return@BrowserWebView
                    }
                    val hdrs = BrowserUrlHelper.buildSafeHeaders(sniffed.src, currentUrl, UA_MOBILE)
                    if (sniffed.isPlaying) {
                        detectedStreams.clear()
                        registerStream(sniffed.src, hdrs, isPrimary = true, label = sniffed.label ?: "▶ En Reproducción")
                        if (sniffed.title.isNotBlank() && sniffed.title != "Video Web" && sniffed.title != "Transmisión Web") {
                            pageTitle = sniffed.title
                        }
                        Toast.makeText(context, "▶ Video detectado: ${pageTitle.take(24)}", Toast.LENGTH_SHORT).show()
                    } else if (detectedStreams.size < 10) {
                        registerStream(sniffed.src, hdrs, isPrimary = false, label = sniffed.label)
                        if (sniffed.title.isNotBlank() && (pageTitle == "Cargando..." || pageTitle == "Transmisión")) pageTitle = sniffed.title
                    }
                },
                onOpenPopup = { popupWebView = it; Toast.makeText(context, "⚠️ Pestaña emergente aislada. Pulsa 'Descartar' para volver.", Toast.LENGTH_SHORT).show() },
                onClosePopup = { popupWebView?.destroy(); popupWebView = null },
                onWebViewCreated = {
                    webViewInstance = it
                    if (!isHome && currentUrl.isNotBlank() && it.url == null) it.loadUrl(currentUrl)
                }
            )

            if (isCasting || (!isHome && detectedStreams.isNotEmpty())) {
                BrowserBottomBarContainer(
                    isCasting = isCasting, castTitle = castTitle, isPlaying = isPlaying,
                    posMs = posMs, durMs = durMs, volume = volume, detectedCount = detectedStreams.size, pageTitle = pageTitle,
                    onTransmitDirect = {
                        val first = detectedStreams.firstOrNull() ?: return@BrowserBottomBarContainer
                        onSendStreamToTv(first, pageTitle, streamHeaders[first] ?: emptyMap())
                    },
                    onOpenQualities = { openOptionsSheet() },
                    onPlayControl = { act, pos, vol ->
                        if (act == "VOLUME") ActiveStreamHolder.setVolume(vol)
                        ActiveStreamHolder.onSendPlayControl?.invoke(act, pos, vol, null)
                    },
                    onStopCasting = {
                        ActiveStreamHolder.stopCastingAndClear()
                        detectedStreams.clear()
                        streamHeaders.clear()
                        streamLabels.clear()
                        qualityOptions = emptyList()
                        Toast.makeText(context, "🛑 Transmisión detenida y capturas limpiadas", Toast.LENGTH_SHORT).show()
                    },
                    onClearStreams = {
                        detectedStreams.clear()
                        streamHeaders.clear()
                        streamLabels.clear()
                        qualityOptions = emptyList()
                        ActiveStreamHolder.clear()
                        webViewInstance?.let { wv ->
                            val s = WvcDomSniffer.getInjectionScript(context)
                            wv.evaluateJavascript(s, null)
                            val u = wv.url
                            if (u?.contains("youtube.com") == true || u?.contains("youtu.be") == true) {
                                checkAndRegisterYouTube(u, wv.title)
                            }
                        }
                        Toast.makeText(context, "🗑️ Capturas limpiadas. Re-escaneando...", Toast.LENGTH_SHORT).show()
                    },
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
        onClearHistory = { historyManager.clear(keepFavorites = true); historyList = historyManager.getHistory() },
        onToggleFavorite = { historyManager.toggleFavorite(it); historyList = historyManager.getHistory() },
        onDismissHistory = { showHistorySheet = false }
    )
}
