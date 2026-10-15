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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    var inputUrl by rememberSaveable { mutableStateOf(if (initialIsHome) "" else savedLastUrl) }; var currentUrl by rememberSaveable { mutableStateOf(if (initialIsHome) "" else savedLastUrl) }
    var isHome by rememberSaveable { mutableStateOf(initialIsHome) }; var pageTitle by rememberSaveable { mutableStateOf("OmniCast Browser") }; var progress by remember { mutableIntStateOf(0) }
    var isDesktopMode by remember { mutableStateOf(false) }; var isAdBlockEnabled by remember { mutableStateOf(true) }
    val detectedStreams = remember { mutableStateListOf<String>() }; val streamHeaders = remember { mutableStateMapOf<String, Map<String, String>>() }; val streamLabels = remember { mutableStateMapOf<String, String>() }; val streamDurations = remember { mutableStateMapOf<String, Float>() }
    var qualityOptions by remember { mutableStateOf<List<StreamQualityOption>>(emptyList()) }; var showOptionsSheet by remember { mutableStateOf(false) }; var showSideDrawer by remember { mutableStateOf(false) }; var showWhitelistDialog by remember { mutableStateOf(false) }; var showBottomControls by rememberSaveable { mutableStateOf(false) }
    var videoToSave by remember { mutableStateOf<HistoryItem?>(null) }; var webViewInstance by remember { mutableStateOf<WebView?>(null) }; var popupWebView by remember { mutableStateOf<WebView?>(null) }
    var playlistsVersion by remember { mutableIntStateOf(0) }; val scope = rememberCoroutineScope()
    val isCasting by ActiveStreamHolder.isCasting.collectAsState(); val isPlaying by ActiveStreamHolder.isPlaying.collectAsState()
    val posMs by ActiveStreamHolder.positionMs.collectAsState(); val durMs by ActiveStreamHolder.durationMs.collectAsState()
    val volume by ActiveStreamHolder.volume.collectAsState(); val castTitle by ActiveStreamHolder.currentMediaTitle.collectAsState()

    fun isRealMedia(s: String) = BrowserUrlHelper.isRealMedia(s, streamDurations[s] ?: 0f)

    val isMovieSaved = remember(detectedStreams.size, playlistsVersion, castTitle, pageTitle) {
        val active = ActiveStreamHolder.streamUrl ?: ""
        val primary = if (active.isNotBlank()) active else detectedStreams.firstOrNull { isRealMedia(it) } ?: ""
        val t = if (castTitle.isNotBlank()) castTitle else if (pageTitle.isNotBlank() && pageTitle != "Cargando...") pageTitle else ""
        if (primary.isBlank()) false else playlistManager.isVideoSaved(primary, t)
    }

    fun deleteMovieFromSaved() {
        val primary = ActiveStreamHolder.streamUrl?.takeIf { it.isNotBlank() } ?: detectedStreams.firstOrNull() ?: ""
        if (primary.isNotBlank()) {
            historyManager.remove(primary)
            playlistManager.getPlaylists().forEach { pl -> playlistManager.removeItemFromPlaylist(pl.id, primary) }
            playlistsVersion++; Toast.makeText(context, "🗑️ Video eliminado de guardados", Toast.LENGTH_SHORT).show()
        }
    }

    fun transmitWithHistory(url: String, title: String, hdrs: Map<String, String>) {
        if (url.isBlank()) return
        val cleanT = if (title.isNotBlank() && title != "Cargando..." && title != "Transmisión") title else pageTitle
        pageTitle = cleanT; com.omnicast.sender.history.NavigationTrackManager.saveTrackLocally(context, currentUrl, cleanT)
        scope.launch(Dispatchers.IO) {
            val (finalUrl, finalHdrs) = MediaPlatformResolver.prepareStreamForPlayback(url, currentUrl, hdrs)
            withContext(Dispatchers.Main) { historyManager.addOrUpdate(finalUrl, cleanT, currentUrl, finalHdrs); onSendStreamToTv(finalUrl, cleanT, finalHdrs) }
        }
    }

    fun handleBrowserBack() {
        if (webViewInstance?.canGoBack() == true) {
            if (detectedStreams.isNotEmpty() || BrowserUrlHelper.isMovieOrVideoPage(currentUrl)) com.omnicast.sender.history.NavigationTrackManager.saveTrackLocally(context, currentUrl, pageTitle)
            webViewInstance?.goBack()
        } else if (com.omnicast.sender.history.NavigationTrackManager.canGoBackPersistent(context)) {
            val prevUrl = com.omnicast.sender.history.NavigationTrackManager.popPreviousBrowserUrl(context)
            if (!prevUrl.isNullOrBlank()) {
                inputUrl = prevUrl; currentUrl = prevUrl; isHome = false; webViewInstance?.loadUrl(prevUrl)
            } else if (!isHome) isHome = true else onCloseBrowser()
        } else if (!isHome) isHome = true else onCloseBrowser()
    }

    BackHandler(enabled = true) {
        if (showSideDrawer) showSideDrawer = false else if (showWhitelistDialog) showWhitelistDialog = false
        else if (popupWebView != null) { popupWebView?.destroy(); popupWebView = null }
        else if (videoToSave != null) videoToSave = null else if (showOptionsSheet) showOptionsSheet = false
        else handleBrowserBack()
    }

    DisposableEffect(webViewInstance) {
        SenderMainActivity.onBrowserRescanRequested = { webViewInstance?.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null) }
        onDispose { SenderMainActivity.onBrowserRescanRequested = null }
    }

    fun registerStream(url: String, hdrs: Map<String, String>, isPrimary: Boolean = false, label: String? = null) {
        if (url.isBlank() || AdBlocker.isAd(url) || !isRealMedia(url)) return
        if (detectedStreams.contains(url)) detectedStreams.remove(url)
        if (isPrimary) detectedStreams.add(0, url) else detectedStreams.add(url)
        if (hdrs.isNotEmpty()) streamHeaders[url] = hdrs; if (!label.isNullOrBlank()) streamLabels[url] = label
        val primaryUrl = detectedStreams.firstOrNull { isRealMedia(it) } ?: url
        val cleanT = if (pageTitle.isNotBlank() && pageTitle != "Cargando..." && pageTitle != "Transmisión") pageTitle else "Video Web"
        ActiveStreamHolder.update(primaryUrl, cleanT, streamHeaders[primaryUrl] ?: hdrs)
        android.util.Log.i("StreamDetection", "🎬 Flujo registrado [${if (isPrimary) "PRIMARY" else "ADDED"}]: $url (Total activos: ${detectedStreams.count { isRealMedia(it) }})")
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
                val actual = dTitle ?: webViewInstance?.title?.replace(" - Dailymotion", "")?.takeIf { it.isNotBlank() && it != "Dailymotion" } ?: "Video Dailymotion"
                pageTitle = actual; registerStream(sUrl, dHdrs, isPrimary = true, label = actual)
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
        val currentStreams = detectedStreams.filter { isRealMedia(it) }
        if (currentStreams.isEmpty()) {
            Toast.makeText(context, "Reproduce el video en la página para capturar el flujo", Toast.LENGTH_SHORT).show()
            webViewInstance?.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null)
            return
        }
        val activeStream = ActiveStreamHolder.streamUrl
        val cleanTitle = pageTitle.replace(" - YouTube", "").replace(" | OK", "").trim().take(22)
        val prefix = if (cleanTitle.isNotBlank() && cleanTitle != "Transmisión" && cleanTitle != "OmniCast Browser") "[$cleanTitle] " else ""
        qualityOptions = currentStreams.mapIndexed { index, url ->
            val ytId = BrowserUrlHelper.extractYouTubeId(url); val num = currentStreams.size - index
            val dur = streamDurations[url] ?: (durMs / 1000f).coerceAtLeast(0f)
            val isMovie = dur >= 2400f || (dur == 0f && url.contains(".m3u8", true)) || cleanTitle.contains("pelicula", true)
            val isSugg = (url == activeStream) || (index == 0 && activeStream.isNullOrBlank())
            if (ytId != null) StreamQualityOption("🎬 $prefix YouTube #$num", "YouTube", "1080p / 720p", url, durationSeconds = dur, isMovie = isMovie, isSuggested = isSugg)
            else {
                val qLabel = streamLabels[url] ?: if (url.contains(".m3u8", true)) "HLS Adaptativo" else "Video Directo"
                StreamQualityOption("🎬 $prefix$qLabel #$num", if (url.contains(".m3u8", true)) "Auto" else "Original", "Máxima Calidad", url, isMaster = qLabel.contains("HLS"), durationSeconds = dur, isMovie = isMovie, isSuggested = isSugg)
            }
        }.sortedByDescending { it.isSuggested }.distinctBy { it.streamUrl }
        showOptionsSheet = true
        scope.launch {
            runCatching {
                val deep = currentStreams.flatMap { u -> StreamQualityDetector.detectQualities(u, streamHeaders[u] ?: emptyMap(), cleanTitle) }
                if (deep.isNotEmpty()) qualityOptions = (deep + qualityOptions).distinctBy { it.streamUrl }.sortedWith(
                    compareByDescending<StreamQualityOption> { it.isSuggested }.thenByDescending { it.resolution.removeSuffix("p").toIntOrNull() ?: 0 }
                )
            }
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
                onBack = { handleBrowserBack() },
                onForward = { if (webViewInstance?.canGoForward() == true) webViewInstance?.goForward() },
                canGoForward = webViewInstance?.canGoForward() == true, onReload = { webViewInstance?.reload() },
                isDesktopMode = isDesktopMode,
                onToggleDesktopMode = { isDesktopMode = !isDesktopMode; webViewInstance?.settings?.userAgentString = if (isDesktopMode) UA_DESKTOP else UA_MOBILE; webViewInstance?.reload() },
                isAdBlockEnabled = isAdBlockEnabled, onToggleAdBlock = { isAdBlockEnabled = !isAdBlockEnabled },
                onForceRescan = { webViewInstance?.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null) },
                onClearData = {
                    webViewInstance?.clearCache(true); webViewInstance?.clearHistory(); detectedStreams.clear(); qualityOptions = emptyList()
                    isHome = true; inputUrl = ""; currentUrl = ""; ActiveStreamHolder.clear(); prefs.edit().remove("saved_browser_last_url").apply()
                },
                detectedStreamsCount = detectedStreams.count { isRealMedia(it) }, onToggleBottomControls = { showBottomControls = !showBottomControls },
                onOpenHistory = { showSideDrawer = true }, onOpenPlaylists = { showSideDrawer = true },
                onOpenDrawer = { showSideDrawer = true }, onOpenWhitelist = { showWhitelistDialog = true }
            )

            if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(3.dp), color = Color(0xFF38BDF8))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isHome) BrowserHomeScreen(onSelectUrl = { navigateTo(it) })

                BrowserWebView(
                    isHome = isHome, isAdBlockEnabled = isAdBlockEnabled, currentUrl = currentUrl,
                    hasStreams = detectedStreams.any { isRealMedia(it) },
                    onProgress = { progress = it },
                    onPageStarted = { newUrl ->
                        if (currentUrl.isNotBlank() && currentUrl != newUrl && !newUrl.startsWith("about:")) {
                            detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); qualityOptions = emptyList(); ActiveStreamHolder.clearDetectedStream(); pageTitle = "Cargando..."
                        }
                        inputUrl = newUrl; currentUrl = newUrl; com.omnicast.sender.history.NavigationTrackManager.recordStep(newUrl, pageTitle)
                        if (!newUrl.startsWith("about:")) { prefs.edit().putString("saved_browser_last_url", newUrl).apply(); com.omnicast.sender.history.NavigationTrackManager.pushBrowserHistory(context, newUrl, pageTitle) }
                        checkPlatformUrls(newUrl)
                    },
                    onPageFinished = { t, u ->
                        if (!t.isNullOrBlank() && t != "Transmisión" && !t.contains("about:blank")) pageTitle = t
                        u?.let { checkPlatformUrls(it, pageTitle); com.omnicast.sender.history.NavigationTrackManager.recordStep(it, pageTitle) }
                    },
                    onHistoryUpdated = { u, t ->
                        if (currentUrl.isNotBlank() && currentUrl != u && !u.startsWith("about:") && !u.contains("favicon")) {
                            detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); qualityOptions = emptyList(); ActiveStreamHolder.clearDetectedStream()
                        }
                        currentUrl = u; if (!t.isNullOrBlank() && t != "Transmisión" && !t.contains("about:blank")) pageTitle = t
                        com.omnicast.sender.history.NavigationTrackManager.recordStep(u, pageTitle)
                        if (!u.startsWith("about:")) { prefs.edit().putString("saved_browser_last_url", u).apply(); com.omnicast.sender.history.NavigationTrackManager.pushBrowserHistory(context, u, pageTitle) }
                        checkPlatformUrls(u, t)
                    },
                    onStreamIntercepted = { sUrl, hdrs, isM -> registerStream(sUrl, hdrs, isPrimary = isM) },
                    onSniffedVideo = { sniffed ->
                        if (AdBlocker.isAd(sniffed.src) || sniffed.duration in 0.1f..59.9f) return@BrowserWebView
                        if (sniffed.src.contains("fastream.to") && !sniffed.src.contains(".m3u8")) {
                            scope.launch(Dispatchers.IO) {
                                val res = MediaPlatformResolver.resolveFastreamSync(sniffed.src, currentUrl)
                                if (res != null) withContext(Dispatchers.Main) { registerStream(res.first, res.third, isPrimary = true, label = sniffed.title) }
                            }
                            return@BrowserWebView
                        }
                        val hdrs = BrowserUrlHelper.buildSafeHeaders(sniffed.src, currentUrl, UA_MOBILE); if (sniffed.duration > 0f) streamDurations[sniffed.src] = sniffed.duration
                        if (sniffed.title.isNotBlank() && sniffed.title != "Video Web" && sniffed.title != "Transmisión Web") pageTitle = sniffed.title
                        registerStream(sniffed.src, hdrs, isPrimary = sniffed.isPlaying, label = sniffed.label ?: pageTitle)
                        if (sniffed.isPlaying) Toast.makeText(context, "▶ Video activo: " + pageTitle.take(24), Toast.LENGTH_SHORT).show()
                    },
                    onTitleReceived = { t -> if (t.isNotBlank() && t != "Transmisión" && !t.contains("about:blank")) pageTitle = t },
                    onOpenPopup = { pView -> popupWebView = pView }, onClosePopup = { popupWebView?.destroy(); popupWebView = null },
                    onWebViewCreated = { webViewInstance = it; if (!isHome && currentUrl.isNotBlank() && it.url == null) it.loadUrl(currentUrl) }
                )

                if (showBottomControls && (!isHome || isCasting)) {
                    BrowserBottomBarContainer(
                        isCasting = isCasting, castTitle = castTitle, isPlaying = isPlaying, posMs = posMs, durMs = durMs, volume = volume,
                        detectedCount = detectedStreams.count { isRealMedia(it) }, streamSubtitle = streamLabels[detectedStreams.firstOrNull() ?: ""] ?: "HLS • m3u8 stream directo",
                        pageTitle = pageTitle,
                        onTransmitLatest = {
                            val first = detectedStreams.firstOrNull { isRealMedia(it) } ?: return@BrowserBottomBarContainer
                            if (!isTvConnected) Toast.makeText(context, "⚠️ Conecta la TV Box antes de transmitir", Toast.LENGTH_SHORT).show()
                            val bestT = webViewInstance?.title?.takeIf { it.isNotBlank() && !it.contains("about:blank") && it != "Transmisión" } ?: pageTitle
                            transmitWithHistory(first, bestT, streamHeaders[first] ?: emptyMap())
                        },
                        onShowDetectedStreams = { openOptionsSheet() }, onOpenQualities = { openOptionsSheet() },
                        onPlayControl = { act, pos, vol -> if (act == "VOLUME") ActiveStreamHolder.setVolume(vol); ActiveStreamHolder.onSendPlayControl?.invoke(act, pos, vol, null) },
                        onStopCasting = { ActiveStreamHolder.stopCasting(); com.omnicast.sender.service.CastMediaNotificationService.stop(context); Toast.makeText(context, "🛑 Transmisión detenida", Toast.LENGTH_SHORT).show() },
                        onClearStreams = {
                            val cur = currentUrl; detectedStreams.retainAll { isRealMedia(it) && !it.contains("cdndirector") }; qualityOptions = emptyList()
                            webViewInstance?.let { checkPlatformUrls(cur, pageTitle); it.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null) }
                            Toast.makeText(context, "⚡ Recapturando flujos...", Toast.LENGTH_SHORT).show()
                        },
                        onRescanStreams = {},
                        onSaveToPlaylist = {
                            val active = ActiveStreamHolder.streamUrl ?: ""; val primary = if (active.isNotBlank()) active else detectedStreams.firstOrNull { isRealMedia(it) } ?: ""
                            val target = if (primary.isNotBlank()) primary else if (currentUrl.startsWith("http")) currentUrl else ""
                            if (target.isNotBlank()) { val t = if (castTitle.isNotBlank()) castTitle else if (pageTitle.isNotBlank() && pageTitle != "Cargando...") pageTitle else "Película / Video"; videoToSave = HistoryItem(url = target, title = t, pageUrl = currentUrl, headers = streamHeaders[primary] ?: emptyMap()) }
                        },
                        onDeleteFromSaved = { deleteMovieFromSaved() }, isSaved = isMovieSaved, modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
                popupWebView?.let { pView ->
                    BrowserPopupOverlay(popupView = pView, onDismiss = {
                        val isMovie = detectedStreams.isNotEmpty() || ActiveStreamHolder.isCasting.value
                        val curHost = try { java.net.URI(currentUrl).host?.lowercase() } catch (_: Throwable) { null }
                        val pHost = try { java.net.URI(pView.url).host?.lowercase() } catch (_: Throwable) { null }
                        if (isMovie && pHost != null && curHost != null && pHost != curHost && AdBlocker.learnAndBlockAdHost(pHost, curHost)) {
                            Toast.makeText(context, "🚫 Publicidad emergente bloqueada: $pHost", Toast.LENGTH_SHORT).show()
                        }
                        popupWebView?.destroy(); popupWebView = null
                    })
                }
            }
        }
    }
    BrowserModalSheets(
        showOptionsSheet = showOptionsSheet, showHistorySheet = false, showSavedPlaylistsSheet = false, videoToSave = videoToSave,
        playlistManager = playlistManager, pageTitle = pageTitle, qualityOptions = qualityOptions, historyList = emptyList(),
        onTransmitOption = { opt -> showOptionsSheet = false; val bT = webViewInstance?.title?.takeIf { it.isNotBlank() && !it.contains("about:blank") && it != "Transmisión" } ?: pageTitle; transmitWithHistory(opt.streamUrl, bT, streamHeaders[opt.streamUrl] ?: emptyMap()) },
        onTransmitHistory = { transmitWithHistory(it.url, it.title, it.headers) }, onDismissOptions = { showOptionsSheet = false },
        onClearOptions = { detectedStreams.clear(); qualityOptions = emptyList(); showOptionsSheet = false },
        onDeleteHistory = { historyManager.remove(it) }, onClearHistory = { historyManager.clear() },
        onToggleFavorite = { historyManager.toggleFavorite(it) }, onDismissHistory = {}, onDismissPlaylists = {},
        onDismissSaveDialog = { videoToSave = null }, onSavedToPlaylist = { playlistsVersion++; Toast.makeText(context, "💾 Guardado en $it", Toast.LENGTH_SHORT).show() },
        showWhitelistDialog = showWhitelistDialog, currentUrl = currentUrl, onDismissWhitelist = { showWhitelistDialog = false }, onWhitelistUpdated = { webViewInstance?.reload() }
    )
}
