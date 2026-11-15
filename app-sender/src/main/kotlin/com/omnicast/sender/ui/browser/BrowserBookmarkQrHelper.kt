package com.omnicast.sender.ui.browser

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.omnicast.sender.history.HistoryItem
import com.omnicast.sender.history.PlayHistoryManager
import com.omnicast.sender.playlist.PlaylistManager

/**
 * Helper modular para marcadores duales (videos vs páginas web sin video)
 * y escaneo QR offline para abrir enlaces del TV Receiver en el navegador.
 */
object BrowserBookmarkQrHelper {

    fun isCurrentSaved(
        playlistManager: PlaylistManager,
        primaryStreamUrl: String,
        title: String,
        currentUrl: String
    ): Boolean {
        if (primaryStreamUrl.isNotBlank() && playlistManager.isVideoSaved(primaryStreamUrl, title)) return true
        if (currentUrl.startsWith("http", ignoreCase = true)) {
            val pagesPl = playlistManager.getPlaylists().firstOrNull { it.id == PlaylistManager.DEFAULT_PAGES_ID }
            if (pagesPl?.items?.any { it.url == currentUrl || it.pageUrl == currentUrl } == true) return true
        }
        return false
    }

    fun deleteCurrentSaved(
        context: Context,
        playlistManager: PlaylistManager,
        historyManager: PlayHistoryManager,
        primaryStreamUrl: String,
        currentUrl: String
    ) {
        var deletedVideo = false
        if (primaryStreamUrl.isNotBlank()) {
            historyManager.remove(primaryStreamUrl)
            playlistManager.getPlaylists().forEach { pl ->
                if (pl.id != PlaylistManager.DEFAULT_PAGES_ID) {
                    playlistManager.removeItemFromPlaylist(pl.id, primaryStreamUrl)
                    deletedVideo = true
                }
            }
        }
        if (currentUrl.startsWith("http", ignoreCase = true)) {
            playlistManager.removeItemFromPlaylist(PlaylistManager.DEFAULT_PAGES_ID, currentUrl)
        }
        val msg = if (deletedVideo) "🗑️ Video eliminado de guardados" else "🗑️ Página eliminada de guardados"
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun toggleBookmark(
        context: Context,
        playlistManager: PlaylistManager,
        historyManager: PlayHistoryManager,
        primaryStreamUrl: String,
        currentUrl: String,
        pageTitle: String,
        castTitle: String,
        headers: Map<String, String>,
        requiresProxy: Boolean
    ) {
        val t = when {
            castTitle.isNotBlank() -> castTitle
            pageTitle.isNotBlank() && pageTitle != "Cargando..." -> pageTitle
            primaryStreamUrl.isNotBlank() -> "Película / Video"
            else -> currentUrl.removePrefix("https://").removePrefix("http://").take(40).ifBlank { "Página Web" }
        }
        if (isCurrentSaved(playlistManager, primaryStreamUrl, t, currentUrl)) {
            deleteCurrentSaved(context, playlistManager, historyManager, primaryStreamUrl, currentUrl)
            return
        }
        if (primaryStreamUrl.isNotBlank()) {
            val item = HistoryItem(primaryStreamUrl, t, currentUrl, headers, requiresProxy = requiresProxy)
            playlistManager.addItemToPlaylist(PlaylistManager.DEFAULT_WATCH_LATER_ID, item)
            historyManager.addOrUpdate(item.url, item.title, pageUrl = item.pageUrl, headers = item.headers, requiresProxy = item.requiresProxy)
            Toast.makeText(context, "⭐ Video guardado en Ver más tarde", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentUrl.startsWith("http", ignoreCase = true)) {
            val pageItem = HistoryItem(url = currentUrl, title = t, pageUrl = currentUrl, headers = emptyMap(), requiresProxy = false)
            playlistManager.addItemToPlaylist(PlaylistManager.DEFAULT_PAGES_ID, pageItem)
            Toast.makeText(context, "🌐 Página guardada en Páginas Web", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(context, "Abre una página web primero para guardarla", Toast.LENGTH_SHORT).show()
    }

    @Composable
    fun rememberQrScannerLauncher(
        context: Context,
        onUrlScanned: (String) -> Unit
    ): () -> Unit {
        val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
            val raw = result.contents?.trim() ?: return@rememberLauncherForActivityResult
            if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
                Toast.makeText(context, "🔗 Abriendo enlace del TV...", Toast.LENGTH_SHORT).show()
                onUrlScanned(raw)
            } else {
                Toast.makeText(context, "⚠️ El QR no contiene una URL válida", Toast.LENGTH_SHORT).show()
            }
        }

        fun launchScan() {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Apunta al código QR del TV Receiver")
                setBeepEnabled(false)
                setOrientationLocked(false)
            }
            qrLauncher.launch(options)
        }

        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchScan()
            else Toast.makeText(context, "Se requiere permiso de cámara para escanear el QR", Toast.LENGTH_SHORT).show()
        }

        return {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchScan()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}
