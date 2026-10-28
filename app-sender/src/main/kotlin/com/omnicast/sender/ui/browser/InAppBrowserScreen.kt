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
    isTvConnected: Boolean = true,
    initialUrl: String? = null
) {
    val context = LocalContext.current
    val historyManager = remember { PlayHistoryManager(context) }; val playlistManager = remember { PlaylistManager(context) }
    val prefs = remember { context.getSharedPreferences("omnicast_prefs", android.content.Context.MODE_PRIVATE) }
    val savedLastUrl = remember { prefs.getString("saved_browser_last_url", "") ?: "" }
    val startUrl = if (!initialUrl.isNullOrBlank()) initialUrl else savedLastUrl; val initialIsHome = startUrl.isBlank() || startUrl == "about:blank"

    var inputUrl by rememberSaveable { mutableStateOf(if (initialIsHome) "" else startUrl) }; var currentUrl by rememberSaveable { mutableStateOf(if (initialIsHome) "" else startUrl) }
    var isHome by rememberSaveable { mutableStateOf(initialIsHome) }; var pageTitle by rememberSaveable { mutableStateOf("OmniCast Browser") }; var progress by remember { mutableIntStateOf(0) }; var isDesktopMode by remember { mutableStateOf(false) }; var isAdBlockEnabled by remember { mutableStateOf(true) }
    val detectedStreams = remember { mutableStateListOf<String>() }; val streamHeaders = remember { mutableStateMapOf<String, Map<String, String>>() }; val streamLabels = remember { mutableStateMapOf<String, String>() }; val streamDurations = remember { mutableStateMapOf<String, Float>() }
    var qualityOptions by remember { mutableStateOf<List<StreamQualityOption>>(emptyList()) }; var showOptionsSheet by remember { mutableStateOf(false) }; var showSideDrawer by remember { mutableStateOf(false) }; var showWhitelistDialog by remember { mutableStateOf(false) }; var showBottomControls by rememberSaveable { mutableStateOf(ActiveStreamHolder.isCasting.value && ActiveStreamHolder.isPlaying.value) }
    var videoToSave by remember { mutableStateOf<HistoryItem?>(null) }; var webViewInstance by remember { mutableStateOf<WebView?>(null) }; var popupWebView by remember { mutableStateOf<WebView?>(null) }
    var playlistsVersion by remember { mutableIntStateOf(0) }; val scope = rememberCoroutineScope(); var userDismissedSheetForUrl by rememberSaveable { mutableStateOf("") }; var autoOpenJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val isCasting by ActiveStreamHolder.isCasting.collectAsState(); val isPlaying by ActiveStreamHolder.isPlaying.collectAsState()
    val posMs by ActiveStreamHolder.positionMs.collectAsState(); val durMs by ActiveStreamHolder.durationMs.collectAsState(); val volume by ActiveStreamHolder.volume.collectAsState(); val castTitle by ActiveStreamHolder.currentMediaTitle.collectAsState()

    fun isRealMedia(s: String) = BrowserUrlHelper.isRealMedia(s, streamDurations[s] ?: 0f)

    val isMovieSaved = remember(detectedStreams.size, playlistsVersion, castTitle, pageTitle) {
        val active = ActiveStreamHolder.streamUrl ?: ""
        val primary = if (active.isNotBlank()) active else detectedStreams.firstOrNull { isRealMedia(it) } ?: ""
        val t = if (castTitle.isNotBlank()) castTitle else if (pageTitle.isNotBlank() && pageTitle != "Cargando...") pageTitle else ""
        if (primary.isBlank()) false else playlistManager.isVideoSaved(primary, t)
    }

    fun deleteMovieFromSaved() {
        val primary = ActiveStreamHolder.streamUrl?.takeIf { it.isNotBlank() } ?: detectedStreams.firstOrNull() ?: ""
        if (primary.isNotBlank()) { historyManager.remove(primary); playlistManager.getPlaylists().forEach { pl -> playlistManager.removeItemFromPlaylist(pl.id, primary) }; playlistsVersion++; Toast.makeText(context, "🗑️ Video eliminado de guardados", Toast.LENGTH_SHORT).show() }
    }

    fun toggleBookmark() {
        if (isMovieSaved) { deleteMovieFromSaved(); return }
        val active = ActiveStreamHolder.streamUrl ?: ""; val primary = if (active.isNotBlank()) active else detectedStreams.firstOrNull { isRealMedia(it) } ?: ""
        val target = if (primary.isNotBlank()) primary else if (currentUrl.startsWith("http")) currentUrl else ""
        if (target.isNotBlank()) {
            val t = if (castTitle.isNotBlank()) castTitle else if (pageTitle.isNotBlank() && pageTitle != "Cargando...") pageTitle else "Película / Video"
            videoToSave = HistoryItem(target, t, currentUrl, streamHeaders[primary] ?: emptyMap(), requiresProxy = ActiveStreamHolder.isProxied.value)
        } else Toast.makeText(context, "No hay video para guardar en esta página", Toast.LENGTH_SHORT).show()
    }

    fun openOptionsSheet() {
        val currentStreams = detectedStreams.filter { isRealMedia(it) }
        val cleanTitle = pageTitle.replace(" - YouTube", "").replace(" | OK", "").trim().take(22)
        if (currentStreams.isEmpty()) {
            qualityOptions = emptyList(); showOptionsSheet = true
            webViewInstance?.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null); return
        }
        qualityOptions = currentStreams.flatMap { u ->
            val ytId = BrowserUrlHelper.extractYouTubeId(u)
            if (ytId != null) BrowserUrlHelper.createYouTubeQualityOptions(ytId, cleanTitle)
            else {
                val vName = StreamQualityDetector.extractFileName(u, if (u.contains("master", true)) "master" else "video")
                val isM = u.contains("master", true) || u.contains("playlist", true)
                val fmt = when { isM -> "m3u8 (adaptive)"; u.contains(".mp4", true) -> "mp4 (Directo)"; u.contains(".mpd", true) -> "mpd (adaptive)"; else -> "m3u8" }
                val gKey = BrowserUrlHelper.extractMediaIdentityKey(u).ifBlank { u.substringBefore('?') }
                listOf(StreamQualityOption("▶ [${cleanTitle.take(16)}] $vName", "Adaptive", "Original", u, isMovie = true, isSuggested = isM, healedHeaders = streamHeaders[u] ?: emptyMap(), fileName = vName, formatDetail = fmt, groupKey = gKey))
            }
        }
        showOptionsSheet = true
        scope.launch {
            val deep = currentStreams.flatMap { u ->
                val ytId = BrowserUrlHelper.extractYouTubeId(u)
                if (ytId != null) BrowserUrlHelper.createYouTubeQualityOptions(ytId, cleanTitle)
                else StreamQualityDetector.detectQualities(u, streamHeaders[u] ?: emptyMap(), cleanTitle, currentUrl).map { o ->
                    val gKey = BrowserUrlHelper.extractMediaIdentityKey(u).ifBlank { o.groupKey.ifBlank { u.substringBefore('?') } }
                    o.copy(groupKey = gKey)
                }.onEach { o -> if (o.healedHeaders.isNotEmpty()) streamHeaders[o.streamUrl] = o.healedHeaders }
            }
            val finalOpts = if (deep.isNotEmpty()) deep.distinctBy { it.streamUrl } else qualityOptions
            val orderMap = currentStreams.withIndex().associate { it.value to it.index }
            qualityOptions = finalOpts.sortedWith(
                compareByDescending<StreamQualityOption> { it.isMaster || it.resolution.equals("Adaptive", true) }
                    .thenBy { opt -> orderMap[opt.groupKey] ?: orderMap[opt.streamUrl] ?: 999 }
            )
        }
    }

    fun resetStreamDetections() {
        detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); streamDurations.clear(); qualityOptions = emptyList()
        userDismissedSheetForUrl = ""; autoOpenJob?.cancel(); ActiveStreamHolder.clearDetectedStream()
    }

    fun registerStream(url: String, hdrs: Map<String, String>, isPrimary: Boolean = false, label: String? = null) {
        val curHost = try { android.net.Uri.parse(currentUrl).host } catch (_: Throwable) { null }
        if (AdBlocker.isSearchEngine(curHost)) return
        val cleanUrl = BrowserUrlHelper.sanitizeMediaUrl(url); if (cleanUrl.isBlank() || AdBlocker.isAd(cleanUrl) || AdBlocker.isVideoAd(cleanUrl) || !isRealMedia(cleanUrl)) return
        val derivedMaster = BrowserUrlHelper.deriveMasterPlaylistUrl(cleanUrl)
        val targetUrl = if (derivedMaster != null && !cleanUrl.contains("master", true)) derivedMaster else cleanUrl
        val isM = targetUrl.contains("master", true) || targetUrl.contains("playlist", true) || targetUrl.contains(".mpd", true)
        val key = BrowserUrlHelper.extractMediaIdentityKey(targetUrl)
        val existingIndex = detectedStreams.indexOfFirst { it == targetUrl || (key.isNotBlank() && BrowserUrlHelper.extractMediaIdentityKey(it) == key && (it.contains(".urlset") == targetUrl.contains(".urlset"))) }
        if (existingIndex >= 0) {
            val existingUrl = detectedStreams[existingIndex]
            val existingIsM = existingUrl.contains("master", true) || existingUrl.contains("playlist", true) || existingUrl.contains(".mpd", true)
            if (existingIsM && !isM) return
            detectedStreams.removeAt(existingIndex); streamHeaders.remove(existingUrl); streamLabels.remove(existingUrl)
        }
        val nonM = detectedStreams.indexOfFirst { !it.contains("master", true) && !it.contains("playlist", true) && !it.contains(".mpd", true) }
        if (isM) detectedStreams.add(0, targetUrl) else if (nonM >= 0) detectedStreams.add(nonM, targetUrl) else detectedStreams.add(targetUrl)
        if (hdrs.isNotEmpty()) streamHeaders[targetUrl] = hdrs; if (!label.isNullOrBlank()) streamLabels[targetUrl] = label
        val primaryUrl = detectedStreams.firstOrNull { it.contains("master", true) } ?: detectedStreams.firstOrNull { isRealMedia(it) } ?: targetUrl
        val cleanT = if (pageTitle.isNotBlank() && pageTitle != "Cargando..." && pageTitle != "Transmisión") pageTitle else "Video Web"
        ActiveStreamHolder.update(primaryUrl, cleanT, streamHeaders[primaryUrl] ?: hdrs)
        android.util.Log.i("StreamDetection", "🎬 Flujo registrado [isMaster=$isM, freshToken=true]: $targetUrl")
        if (!showOptionsSheet && userDismissedSheetForUrl != currentUrl) {
            autoOpenJob?.cancel()
            autoOpenJob = scope.launch {
                kotlinx.coroutines.delay(450)
                if (!showOptionsSheet && userDismissedSheetForUrl != currentUrl && detectedStreams.any { isRealMedia(it) }) openOptionsSheet()
            }
        }
    }

    fun checkPlatformUrls(url: String?, title: String? = null) {
        MediaPlatformResolver.checkPlatform(url = url, title = title ?: webViewInstance?.title, onYouTube = { ytUrl, yTitle, yHdrs ->
            val actual = yTitle ?: webViewInstance?.title?.replace(" - YouTube", "")?.takeIf { it.isNotBlank() && it != "YouTube" } ?: "Video YouTube"
            pageTitle = actual; registerStream(ytUrl, yHdrs, isPrimary = true, label = actual)
        }, onDailymotion = { sUrl, dTitle, dHdrs ->
            val actual = dTitle ?: webViewInstance?.title?.replace(" - Dailymotion", "")?.takeIf { it.isNotBlank() && it != "Dailymotion" } ?: "Video Dailymotion"
            pageTitle = actual; registerStream(sUrl, dHdrs, isPrimary = true, label = actual)
        })
    }

    fun navigateTo(url: String) {
        resetStreamDetections(); showBottomControls = false; pageTitle = "Cargando..."; val target = BrowserUrlHelper.normalizeUrl(url)
        inputUrl = target; currentUrl = target; isHome = false; prefs.edit().putString("saved_browser_last_url", target).apply()
        checkPlatformUrls(target); webViewInstance?.loadUrl(target)
    }

    fun transmitWithHistory(url: String, title: String, hdrs: Map<String, String>, savedPageUrl: String? = null) {
        if (url.isBlank()) return
        val cleanT = if (title.isNotBlank() && title != "Cargando..." && title != "Transmisión") title else pageTitle
        val refPage = savedPageUrl?.takeIf { it.isNotBlank() } ?: currentUrl
        pageTitle = cleanT; com.omnicast.sender.history.NavigationTrackManager.saveTrackLocally(context, refPage, cleanT)
        scope.launch(Dispatchers.IO) {
            val isForceProxy = hdrs["X-Force-Proxy"] == "true"
            val (finalUrl, finalHdrs) = MediaPlatformResolver.prepareStreamForPlayback(url, refPage, hdrs)
            val probe = StreamQualityDetector.probeAndHealStream(finalUrl, finalHdrs, refPage)
            if (!probe.isPlayable && (probe.httpCode == 404 || probe.httpCode == 410)) {
                withContext(Dispatchers.Main) {
                    detectedStreams.remove(url); detectedStreams.remove(finalUrl); qualityOptions = qualityOptions.filterNot { it.streamUrl == url || it.streamUrl == finalUrl }
                    if (!savedPageUrl.isNullOrBlank() && savedPageUrl.startsWith("http")) {
                        Toast.makeText(context, "🔄 Token vencido: abriendo página de la película para renovar...", Toast.LENGTH_LONG).show()
                        navigateTo(savedPageUrl)
                    } else Toast.makeText(context, "⚠️ Enlace inválido o expirado descartado de la lista", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            val baseHdrs = if (probe.isPlayable && probe.healedHeaders.isNotEmpty()) probe.healedHeaders else finalHdrs
            val healedHdrs = baseHdrs.toMutableMap().apply { if (isForceProxy) put("X-Force-Proxy", "true") }
            withContext(Dispatchers.Main) { streamHeaders[finalUrl] = healedHdrs; historyManager.addOrUpdate(finalUrl, cleanT, refPage, healedHdrs); onSendStreamToTv(finalUrl, cleanT, healedHdrs); showBottomControls = true }
        }
    }

    fun handleBrowserBack() {
        if (webViewInstance?.canGoBack() == true) {
            if (detectedStreams.isNotEmpty() || BrowserUrlHelper.isMovieOrVideoPage(currentUrl)) com.omnicast.sender.history.NavigationTrackManager.saveTrackLocally(context, currentUrl, pageTitle)
            webViewInstance?.goBack()
        } else if (com.omnicast.sender.history.NavigationTrackManager.canGoBackPersistent(context)) {
            val prev = com.omnicast.sender.history.NavigationTrackManager.popPreviousBrowserUrl(context)
            if (!prev.isNullOrBlank()) { inputUrl = prev; currentUrl = prev; isHome = false; webViewInstance?.loadUrl(prev) } else if (!isHome) isHome = true else onCloseBrowser()
        } else if (!isHome) isHome = true else onCloseBrowser()
    }

    BackHandler(enabled = true) {
        if (showSideDrawer) showSideDrawer = false else if (showWhitelistDialog) showWhitelistDialog = false else if (popupWebView != null) { popupWebView?.destroy(); popupWebView = null } else if (videoToSave != null) videoToSave = null else if (showOptionsSheet) { showOptionsSheet = false; userDismissedSheetForUrl = currentUrl } else handleBrowserBack()
    }

    DisposableEffect(webViewInstance) {
        com.omnicast.sender.overlay.FloatingIndicatorService.setBrowserActive(true); SenderMainActivity.onBrowserRescanRequested = { webViewInstance?.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null) }
        onDispose { SenderMainActivity.onBrowserRescanRequested = null; com.omnicast.sender.overlay.FloatingIndicatorService.setBrowserActive(false) }
    }

    SideMediaDrawer(
        isOpen = showSideDrawer, onClose = { showSideDrawer = false },
        onPlayOnTv = { url, title, hdrs, _ -> val savedPage = playlistManager.getPlaylists().flatMap { it.items }.firstOrNull { it.url == url }?.pageUrl ?: historyManager.getHistory().firstOrNull { it.url == url }?.pageUrl ?: hdrs["Referer"]; transmitWithHistory(url, title, hdrs, savedPage) },
        onOpenInBrowser = { navigateTo(it) }
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
            BrowserTopBar(
                inputUrl = inputUrl, onInputUrlChange = { inputUrl = it }, onNavigate = { navigateTo(it) }, onClose = onCloseBrowser,
                onHome = { isHome = true; inputUrl = "" }, onBack = { handleBrowserBack() },
                onForward = { if (webViewInstance?.canGoForward() == true) webViewInstance?.goForward() }, canGoForward = webViewInstance?.canGoForward() == true,
                onReload = { resetStreamDetections(); pageTitle = "Cargando..."; webViewInstance?.reload() }, isDesktopMode = isDesktopMode,
                onToggleDesktopMode = { isDesktopMode = !isDesktopMode; webViewInstance?.settings?.userAgentString = if (isDesktopMode) UA_DESKTOP else UA_MOBILE; webViewInstance?.reload() },
                isAdBlockEnabled = isAdBlockEnabled, onToggleAdBlock = { isAdBlockEnabled = !isAdBlockEnabled },
                onForceRescan = { webViewInstance?.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null) },
                onClearData = { webViewInstance?.clearCache(true); webViewInstance?.clearHistory(); detectedStreams.clear(); qualityOptions = emptyList(); isHome = true; inputUrl = ""; currentUrl = ""; ActiveStreamHolder.clear(); prefs.edit().remove("saved_browser_last_url").apply() },
                detectedStreamsCount = detectedStreams.count { isRealMedia(it) }, onToggleBottomControls = { showBottomControls = !showBottomControls },
                onOpenDetectedVideos = { openOptionsSheet() }, isCasting = isCasting, isPlaying = isPlaying, showBottomControls = showBottomControls,
                isBookmarked = isMovieSaved, onToggleBookmark = { toggleBookmark() }, onOpenHistory = { showSideDrawer = true }, onOpenPlaylists = { showSideDrawer = true },
                onOpenDrawer = { showSideDrawer = true }, onOpenWhitelist = { showWhitelistDialog = true }, onExitApp = { SenderMainActivity.exitCompletely(context) }
            )

            if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(3.dp), color = Color(0xFF38BDF8))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                BrowserWebView(
                    isHome = isHome, isAdBlockEnabled = isAdBlockEnabled, currentUrl = currentUrl,
                    hasStreams = detectedStreams.any { isRealMedia(it) }, onProgress = { progress = it },
                    onPageStarted = { newUrl ->
                        if (!newUrl.startsWith("about:") && currentUrl.isNotBlank() && currentUrl != newUrl) { resetStreamDetections(); pageTitle = "Cargando..." }
                        inputUrl = newUrl; currentUrl = newUrl; com.omnicast.sender.history.NavigationTrackManager.recordStep(newUrl, pageTitle)
                        if (!newUrl.startsWith("about:")) { prefs.edit().putString("saved_browser_last_url", newUrl).apply(); com.omnicast.sender.history.NavigationTrackManager.pushBrowserHistory(context, newUrl, pageTitle) }; checkPlatformUrls(newUrl)
                    },
                    onPageFinished = { t, u -> if (!t.isNullOrBlank() && t != "Transmisión" && !t.contains("about:blank")) pageTitle = t; u?.let { checkPlatformUrls(it, pageTitle); com.omnicast.sender.history.NavigationTrackManager.recordStep(it, pageTitle) } },
                    onHistoryUpdated = { u, t ->
                        if (currentUrl.isNotBlank() && currentUrl != u && !u.startsWith("about:") && !u.contains("favicon")) resetStreamDetections()
                        currentUrl = u; if (!t.isNullOrBlank() && t != "Transmisión" && !t.contains("about:blank")) pageTitle = t
                        com.omnicast.sender.history.NavigationTrackManager.recordStep(u, pageTitle); if (!u.startsWith("about:")) { prefs.edit().putString("saved_browser_last_url", u).apply(); com.omnicast.sender.history.NavigationTrackManager.pushBrowserHistory(context, u, pageTitle) }; checkPlatformUrls(u, t)
                    },
                    onStreamIntercepted = { sUrl, hdrs, isM -> registerStream(sUrl, hdrs, isPrimary = isM) },
                    onSniffedVideo = { sniffed ->
                        if (AdBlocker.isAd(sniffed.src) || sniffed.duration in 0.1f..59.9f || (BrowserUrlHelper.extractYouTubeId(sniffed.src) != null && !currentUrl.contains("youtube.com") && !currentUrl.contains("youtu.be"))) return@BrowserWebView
                        val sourceFrameUrl = sniffed.frameUrl.takeIf { it.startsWith("http") } ?: currentUrl
                        if (sniffed.src.contains("fastream.to") && !sniffed.src.contains(".m3u8")) {
                            scope.launch(Dispatchers.IO) {
                                MediaPlatformResolver.resolveFastreamSync(sniffed.src, sourceFrameUrl)?.let { res -> withContext(Dispatchers.Main) { registerStream(res.first, res.third, isPrimary = true, label = sniffed.title) } }
                            }
                            return@BrowserWebView
                        }
                        val hdrs = BrowserUrlHelper.buildSafeHeaders(sniffed.src, sourceFrameUrl, UA_MOBILE); if (sniffed.duration > 0f) streamDurations[sniffed.src] = sniffed.duration
                        if (sniffed.title.isNotBlank() && sniffed.title != "Video Web" && sniffed.title != "Transmisión Web") pageTitle = sniffed.title
                        registerStream(sniffed.src, hdrs, isPrimary = sniffed.isPlaying, label = sniffed.label ?: pageTitle)
                        if (sniffed.isPlaying) Toast.makeText(context, "▶ Video activo: " + pageTitle.take(24), Toast.LENGTH_SHORT).show()
                    },
                    onTitleReceived = { t -> if (t.isNotBlank() && t != "Transmisión" && !t.contains("about:blank")) pageTitle = t },
                    onOpenPopup = { pView -> popupWebView = pView }, onClosePopup = { popupWebView?.destroy(); popupWebView = null },
                    onWebViewCreated = { webViewInstance = it; if (!isHome && currentUrl.isNotBlank() && it.url == null) it.loadUrl(currentUrl) }
                )

                if (isHome) BrowserHomeScreen(onSelectUrl = { navigateTo(it) })

                if (showBottomControls && isCasting && isPlaying) {
                    BrowserBottomBarContainer(
                        isCasting = isCasting, castTitle = castTitle, isPlaying = isPlaying, posMs = posMs, durMs = durMs, volume = volume,
                        onOpenQualities = { openOptionsSheet() },
                        onPlayControl = { act, pos, vol -> if (act == "VOLUME") ActiveStreamHolder.setVolume(vol); ActiveStreamHolder.onSendPlayControl?.invoke(act, pos, vol, null) },
                        onStopCasting = { ActiveStreamHolder.stopCasting(); com.omnicast.sender.service.CastMediaNotificationService.stop(context); Toast.makeText(context, "🛑 Transmisión detenida", Toast.LENGTH_SHORT).show() },
                        onSaveToPlaylist = { toggleBookmark() },
                        onDeleteFromSaved = { deleteMovieFromSaved() }, isSaved = isMovieSaved,
                        onDismiss = { showBottomControls = false }, modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
                popupWebView?.let { pView ->
                    BrowserPopupOverlay(popupView = pView, onDismiss = {
                        val isMovie = detectedStreams.isNotEmpty() || ActiveStreamHolder.isCasting.value
                        val curHost = try { java.net.URI(currentUrl).host?.lowercase() } catch (_: Throwable) { null }
                        val pHost = try { java.net.URI(pView.url).host?.lowercase() } catch (_: Throwable) { null }
                        if (isMovie && pHost != null && curHost != null && pHost != curHost && AdBlocker.learnAndBlockAdHost(pHost, curHost)) Toast.makeText(context, "🚫 Publicidad emergente bloqueada: $pHost", Toast.LENGTH_SHORT).show()
                        popupWebView?.destroy(); popupWebView = null
                    })
                }
            }
        }
    }
    BrowserModalSheets(
        showOptionsSheet = showOptionsSheet, showHistorySheet = false, showSavedPlaylistsSheet = false, videoToSave = videoToSave,
        playlistManager = playlistManager, pageTitle = pageTitle, qualityOptions = qualityOptions, historyList = emptyList(),
        onTransmitOption = { opt -> showOptionsSheet = false; val bT = webViewInstance?.title?.takeIf { it.isNotBlank() && !it.contains("about:blank") && it != "Transmisión" } ?: pageTitle; transmitWithHistory(opt.streamUrl, bT, (streamHeaders[opt.streamUrl] ?: emptyMap()) + opt.healedHeaders) },
        onTransmitHistory = { transmitWithHistory(it.url, it.title, it.headers, it.pageUrl) }, onDismissOptions = { showOptionsSheet = false; userDismissedSheetForUrl = currentUrl },
        onClearOptions = { resetStreamDetections(); showOptionsSheet = false },
        onRescanOptions = { webViewInstance?.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null); Toast.makeText(context, "🔍 Buscando videos...", Toast.LENGTH_SHORT).show() },
        onDeleteHistory = { historyManager.remove(it) }, onClearHistory = { historyManager.clear() }, onToggleFavorite = { historyManager.toggleFavorite(it) },
        onDismissHistory = {}, onDismissPlaylists = {}, onDismissSaveDialog = { videoToSave = null }, onSavedToPlaylist = { playlistsVersion++; Toast.makeText(context, "💾 Guardado en $it", Toast.LENGTH_SHORT).show() },
        showWhitelistDialog = showWhitelistDialog, currentUrl = currentUrl, onDismissWhitelist = { showWhitelistDialog = false }, onWhitelistUpdated = { webViewInstance?.reload() }
    )
}
