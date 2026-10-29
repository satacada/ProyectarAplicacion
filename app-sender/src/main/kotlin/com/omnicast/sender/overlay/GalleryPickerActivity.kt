package com.omnicast.sender.overlay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.omnicast.core.protocol.SignalingMessage
import com.omnicast.sender.gallery.GalleryMediaHelper
import com.omnicast.sender.gallery.GalleryPhotoItem
import com.omnicast.sender.gallery.GallerySessionHolder
import com.omnicast.sender.media.LocalImageServer
import com.omnicast.sender.network.QuickSignalingHelper
import com.omnicast.sender.ui.SenderMainActivity
import kotlinx.coroutines.launch

class GalleryPickerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MIME_FILTER = "EXTRA_MIME_FILTER"
    }

    private val allItemsState = mutableStateListOf<GalleryPhotoItem>()
    private val isLoadingState = mutableStateOf(false)
    private val hasPermissionState = mutableStateOf(false)
    private val mimeFilterState = mutableStateOf("*/*")

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val granted = GalleryMediaHelper.hasGalleryPermission(this, mimeFilterState.value) ||
            GalleryMediaHelper.hasGalleryPermission(this, "*/*")
        hasPermissionState.value = granted
        if (granted) refreshMediaItems()
    }

    private val viewerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == PhotoCastViewerActivity.RESULT_EXIT_TO_MAIN) exitGalleryCompletely()
    }

    private val videoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == PhotoCastViewerActivity.RESULT_EXIT_TO_MAIN) exitGalleryCompletely()
    }

    private val systemMultiPicker = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) handleExternalUrisSelected(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mimeFilterState.value = intent.getStringExtra(EXTRA_MIME_FILTER) ?: "*/*"

        if (savedInstanceState == null) {
            GallerySessionHolder.clearRoute()
            GallerySessionHolder.ensureRouteInitialized()
        } else {
            GallerySessionHolder.ensureRouteInitialized()
        }

        if (GallerySessionHolder.cachedAllItems.isNotEmpty()) {
            allItemsState.clear()
            allItemsState.addAll(GallerySessionHolder.cachedAllItems)
        }

        val hasAnyPerm = GalleryMediaHelper.hasGalleryPermission(this, "*/*")
        val hasAllPerm = GalleryMediaHelper.hasAllMediaPermissions(this)
        hasPermissionState.value = hasAnyPerm
        if (hasAnyPerm) refreshMediaItems()
        if (!hasAllPerm) permissionLauncher.launch(GalleryMediaHelper.getRequiredPermissions())

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleStepBack() }
        })

        setContent {
            GalleryExplorerScreen(
                allItems = allItemsState,
                isLoading = isLoadingState.value,
                hasPermission = hasPermissionState.value,
                mimeFilter = mimeFilterState.value,
                onFilterChange = { newFilter ->
                    mimeFilterState.value = newFilter
                    if (!GalleryMediaHelper.hasGalleryPermission(this, newFilter)) {
                        permissionLauncher.launch(GalleryMediaHelper.getRequiredPermissions())
                    } else if (allItemsState.none { it.isVideo } && newFilter != "image/*") {
                        refreshMediaItems()
                    }
                },
                onRequestPermission = { permissionLauncher.launch(GalleryMediaHelper.getRequiredPermissions()) },
                onOpenSystemPicker = { launchSystemExplorer() },
                onSelectPhoto = { folderPhotos, index, folderName -> openPhotoInContext(folderPhotos, index, folderName) },
                onSelectVideo = { folderVideos, index, folderName -> openVideoInContext(folderVideos, index, folderName) },
                onStepBack = { handleStepBack() },
                onExitToMain = { exitGalleryCompletely() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_MIME_FILTER)?.let { mimeFilterState.value = it }
    }

    private fun refreshMediaItems() {
        lifecycleScope.launch {
            isLoadingState.value = true
            val loaded = GalleryMediaHelper.loadAllMedia(this@GalleryPickerActivity, "*/*")
            allItemsState.clear()
            allItemsState.addAll(loaded)
            isLoadingState.value = false
        }
    }

    private fun handleStepBack() {
        if (GallerySessionHolder.canGoBackFolder()) GallerySessionHolder.popPreviousFolder() else exitGalleryCompletely()
    }

    private fun exitGalleryCompletely() {
        QuickSignalingHelper.sendQuickMessage(this, SignalingMessage.PlayControl("STOP"))
        GallerySessionHolder.clearRoute()
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun openPhotoInContext(folderPhotos: List<GalleryPhotoItem>, startIndex: Int, folderName: String) {
        if (folderPhotos.isEmpty()) return
        val safeIdx = startIndex.coerceIn(0, folderPhotos.lastIndex)
        val targetItem = folderPhotos[safeIdx]
        GallerySessionHolder.setPhotos(folderPhotos, safeIdx, folderName, explicitContext = true)
        val viewerIntent = Intent(this, PhotoCastViewerActivity::class.java).apply {
            data = targetItem.uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra("EXTRA_START_INDEX", safeIdx)
            putExtra("IMAGE_URI", targetItem.uri.toString())
        }
        viewerLauncher.launch(viewerIntent)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun openVideoInContext(folderVideos: List<GalleryPhotoItem>, startIndex: Int, folderName: String) {
        if (folderVideos.isEmpty()) return
        val safeIdx = startIndex.coerceIn(0, folderVideos.lastIndex)
        val targetItem = folderVideos[safeIdx]
        GallerySessionHolder.setVideos(folderVideos, safeIdx, folderName)
        val mediaUrl = LocalImageServer.serveMedia(targetItem.uri, targetItem.mimeType, this)
        SenderMainActivity.sendDirectStreamStatic(this, mediaUrl, targetItem.name)
        Toast.makeText(this, "🎬 Proyectando video ($folderName)", Toast.LENGTH_SHORT).show()
        val videoIntent = Intent(this, VideoCastPlayerActivity::class.java).apply {
            putExtra(VideoCastPlayerActivity.EXTRA_VIDEO_URI, targetItem.uri.toString())
            putExtra(VideoCastPlayerActivity.EXTRA_VIDEO_TITLE, targetItem.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        videoLauncher.launch(videoIntent)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun launchSystemExplorer() {
        val filter = mimeFilterState.value
        Toast.makeText(this, "💡 En Personas/Colecciones puedes tocar 1 o seleccionar las fotos del álbum", Toast.LENGTH_SHORT).show()
        try {
            systemMultiPicker.launch(filter)
        } catch (_: Throwable) {
            try { systemMultiPicker.launch("*/*") } catch (_: Throwable) {
                Toast.makeText(this, "No se encontró explorador de archivos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resolveMime(uri: Uri): String {
        val type = contentResolver.getType(uri)
        if (!type.isNullOrBlank() && type != "application/octet-stream") return type
        val path = uri.toString().lowercase()
        return when {
            path.endsWith(".mp4") || path.contains(".mp4?") -> "video/mp4"
            path.endsWith(".mkv") || path.contains(".mkv?") -> "video/x-matroska"
            path.endsWith(".mov") || path.contains(".mov?") -> "video/quicktime"
            path.endsWith(".webm") || path.contains(".webm?") -> "video/webm"
            path.endsWith(".avi") || path.contains(".avi?") -> "video/x-msvideo"
            path.endsWith(".png") || path.contains(".png?") -> "image/png"
            path.endsWith(".webp") || path.contains(".webp?") -> "image/webp"
            path.endsWith(".gif") || path.contains(".gif?") -> "image/gif"
            else -> "image/jpeg"
        }
    }

    private fun handleExternalUrisSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        uris.forEach { u -> try { contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Throwable) {} }
        val firstUri = uris.first()
        val firstMime = resolveMime(firstUri)
        val isVideo = firstMime.startsWith("video/")

        if (uris.size > 1 || GalleryMediaHelper.isVirtualCollectionUri(firstUri)) {
            val collectionName = if (uris.size > 1) "Colección (${uris.size})" else "Colección Seleccionada"
            val customList = uris.mapIndexed { idx, u ->
                val m = resolveMime(u)
                val v = m.startsWith("video/")
                GalleryPhotoItem((idx + 1).toLong(), u, if (v) "Video ${idx + 1}" else "Foto ${idx + 1}", collectionName, System.currentTimeMillis() - idx, v, m, "__collection__")
            }
            GallerySessionHolder.navigateToFolder("__collection_${System.currentTimeMillis()}", collectionName, customItems = customList)
            if (isVideo) openVideoInContext(customList.filter { it.isVideo }.ifEmpty { customList }, 0, collectionName)
            else openPhotoInContext(customList.filter { !it.isVideo }.ifEmpty { customList }, 0, collectionName)
            return
        }

        lifecycleScope.launch {
            val (folderPath, contextItems) = GalleryMediaHelper.loadFolderContextForUri(this@GalleryPickerActivity, firstUri, isVideo)
            val folderName = folderPath.substringAfterLast('/').ifBlank { "Selección" }
            if (folderPath.isNotBlank()) GallerySessionHolder.navigateToFolder(folderPath, folderName)
            val targetId = firstUri.lastPathSegment?.substringAfterLast(':') ?: ""
            val matchIdx = contextItems.indexOfFirst { it.uri == firstUri || (targetId.isNotBlank() && it.id.toString() == targetId) }
            val finalItems = if (matchIdx >= 0) contextItems else listOf(
                GalleryPhotoItem(0L, firstUri, if (isVideo) "Video" else "Foto", folderName, System.currentTimeMillis(), isVideo, firstMime, folderPath)
            ) + contextItems
            val finalIdx = if (matchIdx >= 0) matchIdx else 0
            if (isVideo) openVideoInContext(finalItems, finalIdx, folderName) else openPhotoInContext(finalItems, finalIdx, folderName)
        }
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && !com.omnicast.sender.service.CastSessionState.isGalleryCasting && !com.omnicast.sender.stream.ActiveStreamHolder.isCasting.value) {
            QuickSignalingHelper.sendQuickMessage(this, SignalingMessage.PlayControl("STOP"))
        }
    }
}
