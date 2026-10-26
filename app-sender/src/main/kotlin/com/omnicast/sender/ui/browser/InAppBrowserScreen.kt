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
    val historyManager = remember { PlayHistoryManager(context) }
    val playlistManager = remember { PlaylistManager(context) }
    val prefs = remember { context.getSharedPreferences("omnicast_prefs", android.content.Context.MODE_PRIVATE) }
    var inputUrl by rememberSaveable { mutableStateOf("") }; var currentUrl by rememberSaveable { mutableStateOf("") }; var isHome by rememberSaveable { mutableStateOf(true) }
    var pageTitle by rememberSaveable { mutableStateOf("OmniCast Browser") }; var progress by remember { mutableIntStateOf(0) }; var isDesktopMode by remember { mutableStateOf(false) }; var isAdBlockEnabled by remember { mutableStateOf(true) }
    val detectedStreams = remember { mutableStateListOf<String>() }; val streamHeaders = remember { mutableStateMapOf<String, Map<String, String>>() }; val streamLabels = remember { mutableStateMapOf<String, String>() }; val streamDurations = remember { mutableStateMapOf<String, Float>() }
    var qualityOptions by remember { mutableStateOf<List<StreamQualityOption>>(emptyList()) }; var showOptionsSheet by remember { mutableStateOf(false) }; var showSideDrawer by remember { mutableStateOf(false) }; var showWhitelistDialog by remember { mutableStateOf(false) }
    var showBottomControls by rememberSaveable { mutableStateOf(ActiveStreamHolder.isCasting.value && ActiveStreamHolder.isPlaying.value) }; var videoToSave by remember { mutableStateOf<HistoryItem?>(null) }; var webViewInstance by remember { mutableStateOf<WebView?>(null) }; var popupWebView by remember { mutableStateOf<WebView?>(null) }
    var playlistsVersion by remember { mutableIntStateOf(0) }; val scope = rememberCoroutineScope(); var userDismissedSheetForUrl by rememberSaveable { mutableStateOf("") }; var autoOpenJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }; val testedSuccess = remember { mutableStateListOf<String>() }; val testedFailed = remember { mutableStateListOf<String>() }
    LaunchedEffect(Unit) { com.omnicast.sender.ui.browser.reputation.StreamReputationSyncManager.initAndSync(context) }
    val isCasting by ActiveStreamHolder.isCasting.collectAsState(); val isPlaying by ActiveStreamHolder.isPlaying.collectAsState(); val posMs by ActiveStreamHolder.positionMs.collectAsState(); val durMs by ActiveStreamHolder.durationMs.collectAsState(); val volume by ActiveStreamHolder.volume.collectAsState(); val castTitle by ActiveStreamHolder.currentMediaTitle.collectAsState()
    val pErr by ActiveStreamHolder.playbackError.collectAsState()
    LaunchedEffect(isCasting, isPlaying, posMs) { val c = ActiveStreamHolder.streamUrl; if (isCasting && isPlaying && posMs > 0L && !c.isNullOrBlank()) { if (!testedSuccess.contains(c)) testedSuccess.add(c); testedFailed.remove(c) } }
    LaunchedEffect(pErr) { val c = ActiveStreamHolder.streamUrl; if (!pErr.isNullOrBlank() && !c.isNullOrBlank()) { if (!testedFailed.contains(c)) testedFailed.add(c); testedSuccess.remove(c) } }
    fun isRealMedia(s: String) = BrowserUrlHelper.isRealMedia(s, streamDurations[s] ?: 0f)

    val isMovieSaved = remember(detectedStreams.size, playlistsVersion, castTitle, pageTitle) {
        val pri = ActiveStreamHolder.streamUrl?.takeIf { it.isNotBlank() } ?: detectedStreams.firstOrNull { isRealMedia(it) } ?: ""; val t = castTitle.takeIf { it.isNotBlank() } ?: pageTitle.takeIf { it.isNotBlank() && it != "Cargando..." } ?: ""; if (pri.isBlank()) false else playlistManager.isVideoSaved(pri, t)
    }

    fun deleteMovieFromSaved() {
        val primary = ActiveStreamHolder.streamUrl?.takeIf { it.isNotBlank() } ?: detectedStreams.firstOrNull() ?: ""
        if (primary.isNotBlank()) { historyManager.remove(primary); playlistManager.getPlaylists().forEach { pl -> playlistManager.removeItemFromPlaylist(pl.id, primary) }; playlistsVersion++; Toast.makeText(context, "🗑️ Video eliminado de guardados", Toast.LENGTH_SHORT).show() }
    }

    fun openOptionsSheet() {
        val currentStreams = detectedStreams.filter { isRealMedia(it) }
        val cleanTitle = pageTitle.replace(" - YouTube", "").replace(" | OK", "").trim().take(22)
        if (currentStreams.isEmpty()) {
            qualityOptions = emptyList(); showOptionsSheet = true
            webViewInstance?.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null)
            return
        }
        val isMovieSite = BrowserUrlHelper.isMovieOrVideoPage(currentUrl)
        qualityOptions = currentStreams.flatMap { u ->
            val ytId = BrowserUrlHelper.extractYouTubeId(u); val rep = com.omnicast.sender.ui.browser.reputation.StreamReputationDatabase.getScore(currentUrl, u)
            val isSucc = testedSuccess.contains(u); val isFail = testedFailed.contains(u)
            if (ytId != null) { if (isMovieSite) emptyList() else BrowserUrlHelper.createYouTubeQualityOptions(ytId, cleanTitle) }
            else {
                val lbl = streamLabels[u]; val isM = u.contains("master", true) || u.contains("playlist", true) || lbl?.contains("auto", true) == true
                val vName = lbl?.takeIf { it.isNotBlank() } ?: StreamQualityDetector.extractFileName(u, if (isM) "master" else "video")
                val fmt = when { isM -> "m3u8 (adaptive)"; u.contains(".mp4", true) || u.contains("mycdn.me", true) -> "mp4 (Directo)"; else -> "m3u8" }
                val res = lbl?.takeIf { it.contains("p", true) } ?: if (isM) "Auto" else "Original"
                listOf(StreamQualityOption("▶ [${cleanTitle.take(16)}] $vName", res, if (isM) "Adaptativa" else "Directo", u, isMaster = true, isMovie = true, isSuggested = isM, healedHeaders = streamHeaders[u] ?: emptyMap(), fileName = vName, formatDetail = fmt, reputationScore = rep, isTestedSuccess = isSucc, isTestedFailed = isFail))
            }
        }.sortedByDescending { StreamQualityDetector.calculateMovieProbability(it, currentUrl) }
        showOptionsSheet = true
        scope.launch {
            val deep = currentStreams.filter { BrowserUrlHelper.extractYouTubeId(it) == null }.flatMap { u ->
                val opts = StreamQualityDetector.detectQualities(u, streamHeaders[u] ?: emptyMap(), cleanTitle, currentUrl)
                opts.forEach { o -> if (o.healedHeaders.isNotEmpty()) streamHeaders[o.streamUrl] = o.healedHeaders }; opts
            }.map { o ->
                val rep = com.omnicast.sender.ui.browser.reputation.StreamReputationDatabase.getScore(currentUrl, o.streamUrl)
                o.copy(reputationScore = rep, isTestedSuccess = testedSuccess.contains(o.streamUrl), isTestedFailed = testedFailed.contains(o.streamUrl))
            }
            if (deep.isNotEmpty()) {
                val sorted = (deep + qualityOptions).filterNot { it.streamUrl.contains("iframes", true) }.distinctBy { it.streamUrl }.sortedByDescending { StreamQualityDetector.calculateMovieProbability(it, currentUrl) }
                qualityOptions = sorted.mapIndexed { idx, opt -> if (idx == 0 && StreamQualityDetector.calculateMovieProbability(opt, currentUrl) >= 50) opt.copy(isSuggested = true) else if (idx > 0) opt.copy(isSuggested = false) else opt }
            }
        }
    }

    fun registerStream(url: String, hdrs: Map<String, String>, isPrimary: Boolean = false, label: String? = null) {
        val cleanUrl = BrowserUrlHelper.sanitizeMediaUrl(url)
        if (cleanUrl.isBlank() || cleanUrl.contains("iframes-", true) || AdBlocker.isAd(cleanUrl) || AdBlocker.isVideoAd(cleanUrl) || !isRealMedia(cleanUrl) || (BrowserUrlHelper.isMovieOrVideoPage(currentUrl) && BrowserUrlHelper.extractYouTubeId(cleanUrl) != null)) return
        val existingIndex = detectedStreams.indexOfFirst { BrowserUrlHelper.isSameLogicalStream(it, cleanUrl) }
        val isVar = cleanUrl.contains("index-", true) || cleanUrl.contains("video.m3u8", true)
        val isM = isPrimary || cleanUrl.contains("master", true) || cleanUrl.contains("playlist", true) || isVar
        if (existingIndex >= 0) {
            val oldUrl = detectedStreams[existingIndex]
            if (isVar || !oldUrl.contains("index-", true)) {
                detectedStreams[existingIndex] = cleanUrl; streamHeaders.remove(oldUrl); val oldLbl = streamLabels.remove(oldUrl)
                if (hdrs.isNotEmpty()) streamHeaders[cleanUrl] = hdrs; (label?.takeIf { it.isNotBlank() } ?: oldLbl)?.let { streamLabels[cleanUrl] = it }
            }
            val active = detectedStreams[existingIndex]; if (isM) ActiveStreamHolder.update(active, pageTitle, streamHeaders[active] ?: hdrs); return
        }
        if (detectedStreams.size >= 12) return
        val nonM = detectedStreams.indexOfFirst { !it.contains("master", true) && !it.contains("playlist", true) && !it.contains("video.m3u8", true) }; if (isM) detectedStreams.add(0, cleanUrl) else if (nonM >= 0) detectedStreams.add(nonM, cleanUrl) else detectedStreams.add(cleanUrl)
        val isMov = BrowserUrlHelper.isMovieOrVideoPage(currentUrl); val varUrl = detectedStreams.firstOrNull { it.contains("index-", true) }
        val nonYt = detectedStreams.firstOrNull { BrowserUrlHelper.extractYouTubeId(it) == null && (it.contains("master", true) || it.contains("video.m3u8", true)) }
        val primaryUrl = varUrl ?: if (isMov && nonYt != null) nonYt else (detectedStreams.firstOrNull { it.contains("master", true) || it.contains("video.m3u8", true) } ?: cleanUrl)
        val cleanT = if (pageTitle.isNotBlank() && pageTitle != "Cargando..." && pageTitle != "Transmisión") pageTitle else "Video Web"
        ActiveStreamHolder.update(primaryUrl, cleanT, streamHeaders[primaryUrl] ?: hdrs)
        android.util.Log.i("StreamDetection", "🎬 Flujo registrado [isMaster=$isM]: $cleanUrl")
        if (!showOptionsSheet && !isCasting && userDismissedSheetForUrl != currentUrl) {
            autoOpenJob?.cancel(); autoOpenJob = scope.launch { kotlinx.coroutines.delay(450L); if (!showOptionsSheet && !isCasting && userDismissedSheetForUrl != currentUrl && detectedStreams.any { isRealMedia(it) }) openOptionsSheet() }
        }
    }

    fun checkPlatformUrls(url: String?, title: String? = null) {
        MediaPlatformResolver.checkPlatform(url = url, title = title ?: webViewInstance?.title, onYouTube = { ytUrl, cleanT, hdrs ->
            val curYt = detectedStreams.firstOrNull()?.let { BrowserUrlHelper.extractYouTubeId(it) }
            val newYt = BrowserUrlHelper.extractYouTubeId(ytUrl)
            if (curYt != null && curYt != newYt) { detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); qualityOptions = emptyList() }
            if (!cleanT.isNullOrBlank() && cleanT != "Transmisión") pageTitle = cleanT
            registerStream(ytUrl, hdrs, isPrimary = true, label = "YouTube")
        }, onDailymotion = { sUrl, dTitle, dHdrs ->
            val actual = dTitle ?: webViewInstance?.title?.replace(" - Dailymotion", "")?.takeIf { it.isNotBlank() && it != "Dailymotion" } ?: "Video Dailymotion"
            pageTitle = actual; registerStream(sUrl, dHdrs, isPrimary = true, label = actual)
        })
    }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    fun navigateTo(url: String) {
        focusManager.clearFocus()
        detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); streamDurations.clear(); qualityOptions = emptyList()
        userDismissedSheetForUrl = ""; ActiveStreamHolder.clearDetectedStream(); showBottomControls = false; pageTitle = "Cargando..."; val target = BrowserUrlHelper.normalizeUrl(url)
        inputUrl = target; currentUrl = target; isHome = false; prefs.edit().putString("saved_browser_last_url", target).apply()
        checkPlatformUrls(target); webViewInstance?.loadUrl(target)
    }
    LaunchedEffect(initialUrl) { if (!initialUrl.isNullOrBlank()) navigateTo(initialUrl) }

    fun transmitWithHistory(url: String, title: String, hdrs: Map<String, String>, savedPageUrl: String? = null) {
        if (url.isBlank()) return
        val cleanT = if (title.isNotBlank() && title != "Cargando..." && title != "Transmisión") title else pageTitle
        val refPage = savedPageUrl?.takeIf { it.isNotBlank() } ?: currentUrl
        pageTitle = cleanT; com.omnicast.sender.history.NavigationTrackManager.saveTrackLocally(context, refPage, cleanT)
        scope.launch(Dispatchers.IO) {
            val (finalUrl, finalHdrs) = MediaPlatformResolver.prepareStreamForPlayback(url, refPage, hdrs)
            val probe = StreamQualityDetector.probeAndHealStream(finalUrl, finalHdrs, refPage)
            if (!probe.isPlayable && (probe.httpCode == 404 || probe.httpCode == 410)) {
                withContext(Dispatchers.Main) {
                    testedFailed.add(url); testedFailed.add(finalUrl)
                    com.omnicast.sender.ui.browser.reputation.StreamReputationSyncManager.reportStreamResult(refPage, url, false)
                    detectedStreams.removeAll(listOf(url, finalUrl)); qualityOptions = qualityOptions.filterNot { it.streamUrl == url || it.streamUrl == finalUrl }
                    if (!savedPageUrl.isNullOrBlank() && savedPageUrl.startsWith("http")) { Toast.makeText(context, "🔄 Token vencido: abriendo página...", Toast.LENGTH_LONG).show(); navigateTo(savedPageUrl) } else Toast.makeText(context, "⚠️ Enlace inválido descartado", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            val baseHdrs = if (probe.isPlayable && probe.healedHeaders.isNotEmpty()) probe.healedHeaders else finalHdrs
            withContext(Dispatchers.Main) {
                streamHeaders[finalUrl] = baseHdrs; historyManager.addOrUpdate(finalUrl, cleanT, refPage, baseHdrs)
                onSendStreamToTv(finalUrl, cleanT, baseHdrs); showBottomControls = true
                scope.launch { kotlinx.coroutines.delay(12000L); if (ActiveStreamHolder.streamUrl == finalUrl && (!ActiveStreamHolder.isPlaying.value || ActiveStreamHolder.positionMs.value == 0L)) { if (!testedFailed.contains(finalUrl)) testedFailed.add(finalUrl); testedSuccess.remove(finalUrl) } }
            }
        }
    }

    fun handleBrowserBack() {
        if (!isHome) {
            if (webViewInstance?.canGoBack() == true) {
                if (detectedStreams.isNotEmpty() || BrowserUrlHelper.isMovieOrVideoPage(currentUrl)) com.omnicast.sender.history.NavigationTrackManager.saveTrackLocally(context, currentUrl, pageTitle)
                webViewInstance?.goBack()
            } else { isHome = true; inputUrl = ""; currentUrl = ""; detectedStreams.clear(); qualityOptions = emptyList() }
        } else onCloseBrowser()
    }

    BackHandler(enabled = true) {
        if (showSideDrawer) showSideDrawer = false else if (showWhitelistDialog) showWhitelistDialog = false else if (popupWebView != null) { popupWebView?.destroy(); popupWebView = null } else if (videoToSave != null) videoToSave = null else if (showOptionsSheet) { showOptionsSheet = false; userDismissedSheetForUrl = currentUrl } else handleBrowserBack()
    }

    DisposableEffect(webViewInstance) {
        com.omnicast.sender.overlay.FloatingIndicatorService.setBrowserActive(true)
        SenderMainActivity.onBrowserRescanRequested = { webViewInstance?.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null) }
        onDispose { SenderMainActivity.onBrowserRescanRequested = null; com.omnicast.sender.overlay.FloatingIndicatorService.setBrowserActive(false) }
    }

    SideMediaDrawer(
        isOpen = showSideDrawer, onClose = { showSideDrawer = false },
        onPlayOnTv = { url, title, hdrs, _ ->
            val sp = playlistManager.getPlaylists().flatMap { it.items }.firstOrNull { it.url == url }?.pageUrl ?: historyManager.getHistory().firstOrNull { it.url == url }?.pageUrl ?: hdrs["Referer"]
            transmitWithHistory(url, title, hdrs, sp)
        }, onOpenInBrowser = { navigateTo(it) }
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
            BrowserTopBar(
                inputUrl = inputUrl, onInputUrlChange = { inputUrl = it }, onNavigate = { navigateTo(it) }, onClose = onCloseBrowser, onBack = { handleBrowserBack() }, onForward = { if (webViewInstance?.canGoForward() == true) webViewInstance?.goForward() }, canGoForward = webViewInstance?.canGoForward() == true, onReload = { webViewInstance?.reload() },
                isDesktopMode = !isDesktopMode, onToggleDesktopMode = { isDesktopMode = !isDesktopMode; webViewInstance?.settings?.userAgentString = if (isDesktopMode) UA_DESKTOP else UA_MOBILE; webViewInstance?.settings?.useWideViewPort = isDesktopMode; webViewInstance?.settings?.loadWithOverviewMode = isDesktopMode; webViewInstance?.reload() }, isAdBlockEnabled = isAdBlockEnabled, onToggleAdBlock = { isAdBlockEnabled = !isAdBlockEnabled },
                onForceRescan = { webViewInstance?.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null) },
                onClearData = { webViewInstance?.clearCache(true); webViewInstance?.clearHistory(); detectedStreams.clear(); qualityOptions = emptyList(); isHome = true; inputUrl = ""; currentUrl = ""; ActiveStreamHolder.clear(); prefs.edit().remove("saved_browser_last_url").apply() },
                detectedStreamsCount = maxOf(detectedStreams.count { isRealMedia(it) }, qualityOptions.size), onToggleBottomControls = { showBottomControls = !showBottomControls }, onOpenDetectedVideos = { openOptionsSheet() }, isCasting = isCasting, isPlaying = isPlaying, showBottomControls = showBottomControls,
                onOpenHistory = { showSideDrawer = true }, onOpenPlaylists = { showSideDrawer = true }, onOpenDrawer = { showSideDrawer = true }, onOpenWhitelist = { showWhitelistDialog = true }, onExitApp = { SenderMainActivity.exitCompletely(context) }
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                BrowserWebView(
                    isHome = isHome, isAdBlockEnabled = isAdBlockEnabled, currentUrl = currentUrl,
                    hasStreams = detectedStreams.any { isRealMedia(it) },
                    onProgress = { progress = it },
                    onClearFocus = { focusManager.clearFocus() },
                    onPageStarted = { newUrl ->
                        if (currentUrl.isNotBlank() && currentUrl != newUrl && !newUrl.startsWith("about:")) {
                            detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); qualityOptions = emptyList(); ActiveStreamHolder.clearDetectedStream(); pageTitle = "Cargando..."; userDismissedSheetForUrl = ""
                        }
                        inputUrl = newUrl; currentUrl = newUrl; com.omnicast.sender.history.NavigationTrackManager.recordStep(newUrl, pageTitle)
                        if (!newUrl.startsWith("about:")) { prefs.edit().putString("saved_browser_last_url", newUrl).apply(); com.omnicast.sender.history.NavigationTrackManager.pushBrowserHistory(context, newUrl, pageTitle) }
                        checkPlatformUrls(newUrl)
                    },
                    onPageFinished = { t, u ->
                        if (!t.isNullOrBlank() && t != "Transmisión" && !t.contains("about:blank")) {
                            pageTitle = t
                            u?.let { com.omnicast.sender.history.NavigationTrackManager.updateTitleIfPresent(context, it, t) }
                        }
                        u?.let { checkPlatformUrls(it, pageTitle); com.omnicast.sender.history.NavigationTrackManager.recordStep(it, pageTitle) }
                    },
                    onHistoryUpdated = { u, t ->
                        if (currentUrl.isNotBlank() && currentUrl != u && !u.startsWith("about:") && !u.contains("favicon")) {
                            detectedStreams.clear(); streamHeaders.clear(); streamLabels.clear(); qualityOptions = emptyList(); ActiveStreamHolder.clearDetectedStream(); userDismissedSheetForUrl = ""
                        }
                        currentUrl = u; if (!t.isNullOrBlank() && t != "Transmisión" && !t.contains("about:blank")) pageTitle = t
                        com.omnicast.sender.history.NavigationTrackManager.recordStep(u, pageTitle)
                        if (!u.startsWith("about:")) { prefs.edit().putString("saved_browser_last_url", u).apply(); com.omnicast.sender.history.NavigationTrackManager.pushBrowserHistory(context, u, pageTitle) }
                        checkPlatformUrls(u, t)
                    },
                    onStreamIntercepted = { sUrl, hdrs, isM -> registerStream(sUrl, hdrs, isPrimary = isM) },
                    onSniffedVideo = { sniffed ->
                        if (AdBlocker.isAd(sniffed.src) || sniffed.duration in 0.1f..59.9f || (BrowserUrlHelper.isMovieOrVideoPage(currentUrl) && BrowserUrlHelper.extractYouTubeId(sniffed.src) != null)) return@BrowserWebView
                        val sourceFrameUrl = sniffed.frameUrl.takeIf { it.startsWith("http") } ?: currentUrl
                        if (sniffed.src.contains("fastream.to") && !sniffed.src.contains(".m3u8")) {
                            scope.launch(Dispatchers.IO) { MediaPlatformResolver.resolveFastreamSync(sniffed.src, sourceFrameUrl)?.let { res -> withContext(Dispatchers.Main) { registerStream(res.first, res.third, isPrimary = true, label = sniffed.title) } } }
                            return@BrowserWebView
                        }
                        val hdrs = BrowserUrlHelper.buildSafeHeaders(sniffed.src, sourceFrameUrl, UA_MOBILE); if (sniffed.duration > 0f) streamDurations[sniffed.src] = sniffed.duration
                        if (sniffed.title.isNotBlank() && sniffed.title != "Video Web" && sniffed.title != "Transmisión Web") pageTitle = sniffed.title; if (sniffed.subtitles.isNotEmpty()) ActiveStreamHolder.update(sniffed.src, pageTitle, hdrs, sniffed.subtitles)
                        registerStream(sniffed.src, hdrs, isPrimary = sniffed.isPlaying, label = sniffed.label ?: pageTitle)
                        if (sniffed.isPlaying) { userDismissedSheetForUrl = ""; Toast.makeText(context, "▶ Video activo: " + pageTitle.take(24), Toast.LENGTH_SHORT).show(); if (!showOptionsSheet) openOptionsSheet() }
                    },
                    onTitleReceived = { t -> if (t.isNotBlank() && t != "Transmisión" && !t.contains("about:blank")) pageTitle = t },
                    onOpenPopup = { pView -> popupWebView = pView }, onClosePopup = { popupWebView?.destroy(); popupWebView = null },
                    onWebViewCreated = { webViewInstance = it; if (!isHome && currentUrl.isNotBlank() && it.url == null) it.loadUrl(currentUrl) }
                )
                if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter), color = Color(0xFF38BDF8))
                if (isHome) BrowserHomeScreen(onSelectUrl = { navigateTo(it) })

                if (showBottomControls && isCasting && isPlaying) {
                    BrowserBottomBarContainer(
                        isCasting = isCasting, castTitle = castTitle, isPlaying = isPlaying, posMs = posMs, durMs = durMs, volume = volume,
                        onOpenQualities = { openOptionsSheet() },
                        onPlayControl = { act, pos, vol -> if (act == "VOLUME") ActiveStreamHolder.setVolume(vol); ActiveStreamHolder.onSendPlayControl?.invoke(act, pos, vol, null) },
                        onStopCasting = { ActiveStreamHolder.stopCasting(); com.omnicast.sender.service.CastMediaNotificationService.stop(context); Toast.makeText(context, "🛑 Transmisión detenida", Toast.LENGTH_SHORT).show() },
                        onSaveToPlaylist = {
                            val active = ActiveStreamHolder.streamUrl ?: ""; val primary = if (active.isNotBlank()) active else detectedStreams.firstOrNull { isRealMedia(it) } ?: ""
                            val target = if (primary.isNotBlank()) primary else if (currentUrl.startsWith("http")) currentUrl else ""
                            if (target.isNotBlank()) {
                                val t = if (castTitle.isNotBlank()) castTitle else if (pageTitle.isNotBlank() && pageTitle != "Cargando...") pageTitle else "Película / Video"
                                val origUrl = com.omnicast.sender.media.LocalStreamProxyServer.getOriginalUrl(target) ?: target
                                val isProx = ActiveStreamHolder.isProxied.value || target.contains(":8092"); videoToSave = HistoryItem(origUrl, t, currentUrl, streamHeaders[primary] ?: emptyMap(), requiresProxy = isProx)
                            }
                        },
                        onDeleteFromSaved = { deleteMovieFromSaved() }, isSaved = isMovieSaved,
                        onDismiss = { showBottomControls = false }, modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
                WebInputAssistantBar(modifier = Modifier.align(Alignment.BottomCenter).imePadding())
                popupWebView?.let { pView ->
                    BrowserPopupOverlay(popupView = pView, onDismiss = {
                        val curHost = try { java.net.URI(currentUrl).host?.lowercase() } catch (_: Throwable) { null }
                        val pHost = try { java.net.URI(pView.url).host?.lowercase() } catch (_: Throwable) { null }
                        if ((detectedStreams.isNotEmpty() || ActiveStreamHolder.isCasting.value) && pHost != null && curHost != null && pHost != curHost && AdBlocker.learnAndBlockAdHost(pHost, curHost)) Toast.makeText(context, "🚫 Publicidad emergente bloqueada: $pHost", Toast.LENGTH_SHORT).show()
                        popupWebView?.destroy(); popupWebView = null
                    })
                }
            }
        }
    }
    BrowserModalSheets(
        showOptionsSheet = showOptionsSheet, showHistorySheet = false, showSavedPlaylistsSheet = false, videoToSave = videoToSave,
        playlistManager = playlistManager, pageTitle = pageTitle, qualityOptions = qualityOptions, historyList = emptyList(),
        onTransmitOption = { opt -> showOptionsSheet = false; if (opt.subtitles.isNotEmpty()) ActiveStreamHolder.update(opt.streamUrl, pageTitle, (streamHeaders[opt.streamUrl] ?: emptyMap()) + opt.healedHeaders, opt.subtitles); val bT = webViewInstance?.title?.takeIf { it.isNotBlank() && !it.contains("about:blank") && it != "Transmisión" } ?: pageTitle; transmitWithHistory(opt.streamUrl, bT, (streamHeaders[opt.streamUrl] ?: emptyMap()) + opt.healedHeaders) },
        onTransmitHistory = { transmitWithHistory(it.url, it.title, it.headers, it.pageUrl) }, onDismissOptions = { showOptionsSheet = false; userDismissedSheetForUrl = currentUrl },
        onClearOptions = { detectedStreams.clear(); qualityOptions = emptyList(); showOptionsSheet = false },
        onRescanOptions = { webViewInstance?.evaluateJavascript(WvcSnifferScripts.buildAggressiveRescanScript(WvcDomSniffer.CONSOLE_PREFIX), null); Toast.makeText(context, "🔍 Buscando videos...", Toast.LENGTH_SHORT).show() },
        onDeleteHistory = { historyManager.remove(it) }, onClearHistory = { historyManager.clear() }, onToggleFavorite = { historyManager.toggleFavorite(it) },
        onDismissHistory = {}, onDismissPlaylists = {}, onDismissSaveDialog = { videoToSave = null }, onSavedToPlaylist = { playlistsVersion++; Toast.makeText(context, "💾 Guardado en $it", Toast.LENGTH_SHORT).show() }, showWhitelistDialog = showWhitelistDialog, currentUrl = currentUrl, onDismissWhitelist = { showWhitelistDialog = false }, onWhitelistUpdated = { webViewInstance?.reload() }
    )
}
