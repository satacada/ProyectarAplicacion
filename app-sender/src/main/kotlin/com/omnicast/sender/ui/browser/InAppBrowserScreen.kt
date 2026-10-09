package com.omnicast.sender.ui.browser

import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnicast.sender.history.HistoryItem
import com.omnicast.sender.history.PlayHistoryManager
import com.omnicast.sender.playlist.PlaylistManager
import com.omnicast.sender.stream.ActiveStreamHolder
import com.omnicast.sender.ui.SenderMainActivity
import com.omnicast.sender.ui.components.SideMediaDrawer
import kotlinx.coroutines.launch

@Composable
fun InAppBrowserScreen(
    onSendStreamToTv: (url: String, title: String, headers: Map<String, String>) -> Unit,
    onCloseBrowser: () -> Unit,
    isTvConnected: Boolean = true
) {
    val context = LocalContext.current
    val historyManager = remember { PlayHistoryManager(context) }
    val playlistManager = remember { PlaylistManager(context) }
    val prefs = remember { context.getSharedPreferences("omnicast_prefs", android.content.Context.MODE_PRIVATE) }
    val savedLastUrl = remember { prefs.getString("saved_browser_last_url", "") ?: "" }
    val initialIsHome = savedLastUrl.isBlank() || savedLastUrl == "about:blank"

    var inputUrl by rememberSaveable { mutableStateOf(if (initialIsHome) "" else savedLastUrl) }
    var currentUrl by rememberSaveable { mutableStateOf(if (initialIsHome) "" else savedLastUrl) }
    var isHome by rememberSaveable { mutableStateOf(initialIsHome) }
    var pageTitle by rememberSaveable { mutableStateOf("OmniCast Browser") }
    var progress by remember { mutableIntStateOf(0) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var isAdBlockEnabled by remember { mutableStateOf(true) }
    val detectedStreams = remember { mutableStateListOf<String>() }
    val streamHeaders = remember { mutableStateMapOf<String, Map<String, String>>() }
    val streamLabels = remember { mutableStateMapOf<String, String>() }; val streamDurations = remember { mutableStateMapOf<String, Float>() }
    var qualityOptions by remember { mutableStateOf<List<StreamQualityOption>>(emptyList()) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showSideDrawer by remember { mutableStateOf(false) }
    var showWhitelistDialog by remember { mutableStateOf(false) }
    var showBottomControls by rememberSaveable { mutableStateOf(false) }
    var videoToSave by remember { mutableStateOf<HistoryItem?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null); }; var popupWebView by remember { mutableStateOf<WebView?>(null) }
    val scope = rememberCoroutineScope()
    val isCasting by ActiveStreamHolder.isCasting.collectAsState(); val isPlaying by ActiveStreamHolder.isPlaying.collectAsState()
    val posMs by ActiveStreamHolder.positionMs.collectAsState(); val durMs by ActiveStreamHolder.durationMs.collectAsState()
    val volume by ActiveStreamHolder.volume.collectAsState(); val castTitle by ActiveStreamHolder.currentMediaTitle.collectAsState()

    fun deleteMovieFromSaved() {
        val active = ActiveStreamHolder.streamUrl ?: ""
        val primary = if (active.isNotBlank()) active else detectedStreams.firstOrNull() ?: ""
        if (primary.isNotBlank()) {
            historyManager.remove(primary)
            playlistManager.getPlaylists().forEach { pl -> playlistManager.removeItemFromPlaylist(pl.id, primary) }
            Toast.makeText(context, "🗑️ Video eliminado de guardados", Toast.LENGTH_SHORT).show()
        }
    }

    fun transmitWithHistory(url: String, title: String, hdrs: Map<String, String>) {
        val cleanT = if (title.isNotBlank() && title != "Cargando..." && title != "Transmisión") title else pageTitle
        pageTitle = cleanT; historyManager.addOrUpdate(url, cleanT, currentUrl, hdrs)
        onSendStreamToTv(url, cleanT, hdrs)
    }

    BackHandler(enabled = true) {
        if (showSideDrawer) showSideDrawer = false else if (showWhitelistDialog) showWhitelistDialog = false
        else if (popupWebView != null) { popupWebView?.destroy(); popupWebView = null }
        else if (videoToSave != null) videoToSave = null else if (showOptionsSheet) showOptionsSheet = false
        else if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack() else if (!isHome) isHome = true else onCloseBrowser()
    }

    DisposableEffect(webViewInstance) {
        SenderMainActivity.onBrowserRescanRequested = {
            webViewInstance?.evaluateJavascript(WvcDomSniffer.getInjectionScript(context), null)
            webViewInstance?.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null)
        }
        onDispose { SenderMainActivity.onBrowserRescanRequested = null }
    }

    fun registerStream(url: String, hdrs: Map<String, String>, isPrimary: Boolean = false, label: String? = null) {
        if (url.isBlank() || AdBlocker.isAd(url)) return
        if (detectedStreams.contains(url)) detectedStreams.remove(url)
        detectedStreams.add(0, url)
        if (hdrs.isNotEmpty()) streamHeaders[url] = hdrs; if (!label.isNullOrBlank()) streamLabels[url] = label
        val primaryUrl = detectedStreams.firstOrNull() ?: url
        val cleanT = if (pageTitle.isNotBlank() && pageTitle != "Cargando..." && pageTitle != "Transmisión") pageTitle else "Video Web"
        ActiveStreamHolder.update(primaryUrl, cleanT, streamHeaders[primaryUrl] ?: hdrs)
        showBottomControls = true
    }

    fun checkPlatformUrls(url: String?, title: String? = null) {
        MediaPlatformResolver.checkPlatform(
            url = url, title = title ?: webViewInstance?.title,
            onYouTube = { ytUrl, cleanT, hdrs ->
                val curYt = detectedStreams.firstOrNull()?.let { BrowserUrlHelper.extractYouTubeId(it) }
                if (curYt != null && curYt != BrowserUrlHelper.extractYouTubeId(ytUrl)) {
                    detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); qualityOptions = emptyList(); ActiveStreamHolder.clearDetectedStream()
                }
                if (!cleanT.isNullOrBlank() && cleanT != "Transmisión") pageTitle = cleanT
                registerStream(ytUrl, hdrs, isPrimary = true, label = "YouTube")
            },
            onDailymotion = { sUrl, dTitle, dHdrs ->
                val actualTitle = dTitle ?: webViewInstance?.title?.replace(" - Dailymotion", "")?.takeIf { it.isNotBlank() && it != "Dailymotion" } ?: "Video Dailymotion"
                pageTitle = actualTitle; registerStream(sUrl, dHdrs, isPrimary = true, label = actualTitle)
            }
        )
    }

    fun navigateTo(url: String) {
        detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); streamDurations.clear(); qualityOptions = emptyList()
        ActiveStreamHolder.clearDetectedStream(); pageTitle = "Cargando..."
        val target = BrowserUrlHelper.normalizeUrl(url)
        inputUrl = target; currentUrl = target; isHome = false
        prefs.edit().putString("saved_browser_last_url", target).apply()
        checkPlatformUrls(target); webViewInstance?.loadUrl(target)
    }

    fun openOptionsSheet() {
        if (detectedStreams.isEmpty()) {
            Toast.makeText(context, "Reproduce el video en la página para capturar el flujo", Toast.LENGTH_SHORT).show()
            webViewInstance?.evaluateJavascript(WvcDomSniffer.getInjectionScript(context), null)
            return
        }
        val cleanTitle = pageTitle.replace(" - YouTube", "").replace(" | OK", "").trim().take(22)
        val prefix = if (cleanTitle.isNotBlank() && cleanTitle != "Transmisión" && cleanTitle != "OmniCast Browser") "[$cleanTitle] " else ""
        qualityOptions = detectedStreams.mapIndexed { index, url ->
            val ytId = BrowserUrlHelper.extractYouTubeId(url)
            val num = detectedStreams.size - index
            val dur = streamDurations[url] ?: (durMs / 1000f).coerceAtLeast(0f)
            val isMovie = dur >= 2400f || (dur == 0f && url.contains(".m3u8", true)) || cleanTitle.contains("pelicula", true)
            if (ytId != null) StreamQualityOption("🎬 $prefix YouTube #$num", "YouTube", "1080p / 720p", url, durationSeconds = dur, isMovie = isMovie)
            else {
                val qLabel = streamLabels[url] ?: if (url.contains(".m3u8", true)) "HLS Adaptativo" else "Video Directo"
                StreamQualityOption("🎬 $prefix$qLabel #$num", qLabel, "Máxima Calidad", url, isMaster = qLabel.contains("HLS"), durationSeconds = dur, isMovie = isMovie)
            }
        }.distinctBy { it.streamUrl }
        showOptionsSheet = true
        scope.launch {
            val deep = detectedStreams.flatMap { u -> StreamQualityDetector.detectQualities(u, streamHeaders[u] ?: emptyMap(), cleanTitle) }
            if (deep.isNotEmpty()) qualityOptions = (qualityOptions + deep).distinctBy { it.label }
        }
    }

    SideMediaDrawer(
        isOpen = showSideDrawer, onClose = { showSideDrawer = false },
        onPlayOnTv = { url, title, hdrs, _ -> transmitWithHistory(url, title, hdrs) },
        onOpenInBrowser = { navigateTo(it) }
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
            BrowserTopBar(
                inputUrl = inputUrl, onInputUrlChange = { inputUrl = it }, onNavigate = { navigateTo(it) }, onClose = onCloseBrowser,
                onBack = { if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack() else isHome = true },
                onForward = { if (webViewInstance?.canGoForward() == true) webViewInstance?.goForward() },
                canGoForward = webViewInstance?.canGoForward() == true, onReload = { webViewInstance?.reload() },
                isDesktopMode = isDesktopMode,
                onToggleDesktopMode = { isDesktopMode = !isDesktopMode; webViewInstance?.settings?.userAgentString = if (isDesktopMode) UA_DESKTOP else UA_MOBILE; webViewInstance?.reload() },
                isAdBlockEnabled = isAdBlockEnabled, onToggleAdBlock = { isAdBlockEnabled = !isAdBlockEnabled },
                onForceRescan = {
                    webViewInstance?.evaluateJavascript(WvcDomSniffer.getInjectionScript(context), null)
                    webViewInstance?.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null)
                },
                onClearData = {
                    webViewInstance?.clearCache(true); webViewInstance?.clearHistory()
                    detectedStreams.clear(); qualityOptions = emptyList(); isHome = true; inputUrl = ""; currentUrl = ""
                    ActiveStreamHolder.clear(); prefs.edit().remove("saved_browser_last_url").apply()
                },
                detectedStreamsCount = detectedStreams.size, onToggleBottomControls = { showBottomControls = !showBottomControls },
                onOpenHistory = { showSideDrawer = true }, onOpenPlaylists = { showSideDrawer = true },
                onOpenDrawer = { showSideDrawer = true }, onOpenWhitelist = { showWhitelistDialog = true }
            )

            if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(3.dp), color = Color(0xFF38BDF8))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isHome) BrowserHomeScreen(onSelectUrl = { navigateTo(it) })

                BrowserWebView(
                    isHome = isHome, isAdBlockEnabled = isAdBlockEnabled, currentUrl = currentUrl,
                    onProgress = { progress = it },
                    onPageStarted = { newUrl ->
                        val hOld = runCatching { java.net.URI(currentUrl).host }.getOrNull(); val hNew = runCatching { java.net.URI(newUrl).host }.getOrNull()
                        val pOld = runCatching { java.net.URI(currentUrl).path }.getOrNull(); val pNew = runCatching { java.net.URI(newUrl).path }.getOrNull()
                        if (currentUrl.isNotBlank() && (hOld != hNew || pOld != pNew) && !newUrl.startsWith("about:")) {
                            detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); qualityOptions = emptyList()
                            ActiveStreamHolder.clearDetectedStream(); pageTitle = "Cargando..."
                        }
                        inputUrl = newUrl; currentUrl = newUrl
                        if (!newUrl.startsWith("about:")) prefs.edit().putString("saved_browser_last_url", newUrl).apply()
                        checkPlatformUrls(newUrl)
                    },
                    onPageFinished = { t, u ->
                        if (!t.isNullOrBlank() && t != "Transmisión" && !t.contains("about:blank")) pageTitle = t
                        u?.let { checkPlatformUrls(it, pageTitle) }
                    },
                    onHistoryUpdated = { u, t ->
                        val hOld = runCatching { java.net.URI(currentUrl).host }.getOrNull(); val hNew = runCatching { java.net.URI(u).host }.getOrNull()
                        val pOld = runCatching { java.net.URI(currentUrl).path }.getOrNull(); val pNew = runCatching { java.net.URI(u).path }.getOrNull()
                        if (currentUrl.isNotBlank() && (hOld != hNew || pOld != pNew) && !u.startsWith("about:") && !u.contains("favicon")) {
                            detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); qualityOptions = emptyList()
                            ActiveStreamHolder.clearDetectedStream()
                        }
                        currentUrl = u; if (!t.isNullOrBlank() && t != "Transmisión" && !t.contains("about:blank")) pageTitle = t
                        if (!u.startsWith("about:")) prefs.edit().putString("saved_browser_last_url", u).apply()
                        checkPlatformUrls(u, t)
                    },
                    onStreamIntercepted = { sUrl, hdrs, isM -> registerStream(sUrl, hdrs, isPrimary = isM) },
                    onSniffedVideo = { sniffed ->
                        if (AdBlocker.isAd(sniffed.src) || sniffed.duration in 0.1f..59.9f) return@BrowserWebView
                        val hdrs = BrowserUrlHelper.buildSafeHeaders(sniffed.src, currentUrl, UA_MOBILE); if (sniffed.duration > 0f) streamDurations[sniffed.src] = sniffed.duration
                        if (sniffed.title.isNotBlank() && sniffed.title != "Video Web" && sniffed.title != "Transmisión Web") {
                            pageTitle = sniffed.title
                        }
                        if (sniffed.isPlaying) {
                            detectedStreams.clear()
                            registerStream(sniffed.src, hdrs, isPrimary = true, label = sniffed.label ?: pageTitle)
                            Toast.makeText(context, "▶ Video detectado: " + pageTitle.take(24), Toast.LENGTH_SHORT).show()
                        } else if (detectedStreams.size < 10) {
                            registerStream(sniffed.src, hdrs, isPrimary = false, label = sniffed.label ?: pageTitle)
                        }
                    },
                    onTitleReceived = { t -> if (t.isNotBlank() && t != "Transmisión" && !t.contains("about:blank")) pageTitle = t },
                    onOpenPopup = { pView -> popupWebView = pView },
                    onClosePopup = { popupWebView?.destroy(); popupWebView = null },
                    onWebViewCreated = { webViewInstance = it; if (!isHome && currentUrl.isNotBlank() && it.url == null) it.loadUrl(currentUrl) }
                )

                if ((showBottomControls || isCasting) && (!isHome && (detectedStreams.isNotEmpty() || isCasting))) {
                    BrowserBottomBarContainer(
                        isCasting = isCasting, castTitle = castTitle, isPlaying = isPlaying,
                        posMs = posMs, durMs = durMs, volume = volume, detectedCount = detectedStreams.size,
                        streamSubtitle = streamLabels[detectedStreams.firstOrNull() ?: ""] ?: "HLS • m3u8 stream directo",
                        pageTitle = pageTitle,
                        onTransmitLatest = {
                            val first = detectedStreams.firstOrNull() ?: return@BrowserBottomBarContainer
                            val bestT = webViewInstance?.title?.takeIf { it.isNotBlank() && !it.contains("about:blank") && it != "Transmisión" } ?: pageTitle
                            transmitWithHistory(first, bestT, streamHeaders[first] ?: emptyMap())
                        },
                        onShowDetectedStreams = { openOptionsSheet() },
                        onOpenQualities = { openOptionsSheet() },
                        onPlayControl = { act, pos, vol ->
                            if (act == "VOLUME") ActiveStreamHolder.setVolume(vol)
                            ActiveStreamHolder.onSendPlayControl?.invoke(act, pos, vol, null)
                        },
                        onStopCasting = { ActiveStreamHolder.stopCasting(); Toast.makeText(context, "🛑 Transmisión detenida", Toast.LENGTH_SHORT).show() },
                        onClearStreams = {
                            detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); streamDurations.clear(); qualityOptions = emptyList()
                            ActiveStreamHolder.clearDetectedStream()
                            webViewInstance?.let { wv ->
                                wv.evaluateJavascript(WvcDomSniffer.getInjectionScript(context), null)
                                wv.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null)
                                checkPlatformUrls(wv.url, wv.title)
                            }
                            Toast.makeText(context, "🗑️ Reiniciando y recapturando video...", Toast.LENGTH_SHORT).show()
                        },
                        onRescanStreams = {},
                        onSaveToPlaylist = {
                            val active = ActiveStreamHolder.streamUrl ?: ""
                            val primary = if (active.isNotBlank()) active else detectedStreams.firstOrNull() ?: ""
                            if (primary.isNotBlank()) {
                                val t = if (castTitle.isNotBlank()) castTitle else if (pageTitle.isNotBlank() && pageTitle != "Cargando...") pageTitle else "Película / Video"
                                videoToSave = HistoryItem(url = primary, title = t, pageUrl = currentUrl, headers = streamHeaders[primary] ?: emptyMap())
                            }
                        },
                        onDeleteFromSaved = { deleteMovieFromSaved() },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                popupWebView?.let { pView ->
                    BrowserPopupOverlay(popupView = pView, onDismiss = { popupWebView?.destroy(); popupWebView = null })
                }
            }
        }
    }

    BrowserModalSheets(
        showOptionsSheet = showOptionsSheet, showHistorySheet = false, showSavedPlaylistsSheet = false,
        videoToSave = videoToSave, playlistManager = playlistManager, pageTitle = pageTitle,
        qualityOptions = qualityOptions, historyList = emptyList(),
        onTransmitOption = { opt -> showOptionsSheet = false; val bestT = webViewInstance?.title?.takeIf { it.isNotBlank() && !it.contains("about:blank") && it != "Transmisión" } ?: pageTitle; transmitWithHistory(opt.streamUrl, bestT, streamHeaders[opt.streamUrl] ?: emptyMap()) },
        onTransmitHistory = { transmitWithHistory(it.url, it.title, it.headers) }, onDismissOptions = { showOptionsSheet = false },
        onClearOptions = { detectedStreams.clear(); qualityOptions = emptyList(); showOptionsSheet = false },
        onDeleteHistory = { historyManager.remove(it) }, onClearHistory = { historyManager.clear() },
        onToggleFavorite = { historyManager.toggleFavorite(it) }, onDismissHistory = {}, onDismissPlaylists = {},
        onDismissSaveDialog = { videoToSave = null }, onSavedToPlaylist = { Toast.makeText(context, "💾 Guardado en $it", Toast.LENGTH_SHORT).show() },
        showWhitelistDialog = showWhitelistDialog, currentUrl = currentUrl,
        onDismissWhitelist = { showWhitelistDialog = false }, onWhitelistUpdated = { webViewInstance?.reload() }
    )
}
