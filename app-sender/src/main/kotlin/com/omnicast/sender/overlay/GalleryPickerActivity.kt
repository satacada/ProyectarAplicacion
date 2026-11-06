package com.omnicast.sender.overlay

import android.app.Activity
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
    private val mimeFilterState = mutableStateOf("image/*")
    private var openedFromSystemCollections = false
    private var hasAutoOpenedSettings = false

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val granted = GalleryPermissionHelper.checkGalleryPermission(this, mimeFilterState.value)
        hasPermissionState.value = granted
        if (granted) refreshMediaItems() else GalleryPermissionHelper.openAppPermissionSettings(this)
    }

    private fun handleViewerResult(resultCode: Int) {
        if (resultCode == PhotoCastViewerActivity.RESULT_EXIT_TO_MAIN) {
            exitGalleryCompletely()
        } else if (openedFromSystemCollections) {
            GalleryPermissionHelper.launchSystemExplorer(this, mimeFilterState.value, systemPickerLauncher, showToast = false)
        }
    }

    private val viewerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { handleViewerResult(it.resultCode) }
    private val videoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { handleViewerResult(it.resultCode) }

    private val systemPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uris = mutableListOf<Uri>()
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data!!
            val clip = data.clipData
            if (clip != null) {
                for (i in 0 until clip.itemCount) clip.getItemAt(i)?.uri?.let { uris.add(it) }
            }
            if (uris.isEmpty()) data.data?.let { uris.add(it) }
        }
        if (uris.isNotEmpty()) handleExternalUrisSelected(uris) else openedFromSystemCollections = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FloatingIndicatorService.setGalleryActive(true)
        mimeFilterState.value = intent.getStringExtra(EXTRA_MIME_FILTER) ?: "image/*"

        if (savedInstanceState == null) GallerySessionHolder.clearRoute()
        GallerySessionHolder.ensureRouteInitialized()

        if (GallerySessionHolder.cachedAllItems.isNotEmpty()) {
            allItemsState.clear(); allItemsState.addAll(GallerySessionHolder.cachedAllItems)
        }

        val hasPerm = GalleryPermissionHelper.checkGalleryPermission(this, mimeFilterState.value)
        hasPermissionState.value = hasPerm
        if (hasPerm) refreshMediaItems() else permissionLauncher.launch(GalleryPermissionHelper.getRequiredPermissions(mimeFilterState.value))

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { handleStepBack() }
        })

        setContent {
            GalleryExplorerScreen(
                allItems = allItemsState, isLoading = isLoadingState.value, hasPermission = hasPermissionState.value, mimeFilter = mimeFilterState.value,
                onRequestPermission = {
                    GalleryPermissionHelper.requestOrOpenPermissions(
                        activity = this, mimeFilter = mimeFilterState.value, allItems = allItemsState,
                        permissionLauncher = permissionLauncher, onRefreshMedia = { refreshMediaItems() }
                    )
                },
                onOpenSystemPicker = {
                    openedFromSystemCollections = true
                    GalleryPermissionHelper.launchSystemExplorer(this, mimeFilterState.value, systemPickerLauncher, showToast = true)
                },
                onSelectPhoto = { folderPhotos, index, folderName -> openedFromSystemCollections = false; openPhotoInContext(folderPhotos, index, folderName) },
                onSelectVideo = { folderVideos, index, folderName -> openedFromSystemCollections = false; openVideoInContext(folderVideos, index, folderName) },
                onStepBack = { handleStepBack() }, onExitToMain = { exitGalleryCompletely() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        FloatingIndicatorService.setGalleryActive(true)
        val granted = GalleryPermissionHelper.checkGalleryPermission(this, mimeFilterState.value)
        if (granted && (!hasPermissionState.value || allItemsState.isEmpty())) { hasPermissionState.value = true; refreshMediaItems() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent)
        intent.getStringExtra(EXTRA_MIME_FILTER)?.let {
            mimeFilterState.value = it
            val granted = GalleryPermissionHelper.checkGalleryPermission(this, it)
            hasPermissionState.value = granted
            if (granted) refreshMediaItems() else permissionLauncher.launch(GalleryPermissionHelper.getRequiredPermissions(it))
        }
    }

    private fun refreshMediaItems() {
        lifecycleScope.launch {
            isLoadingState.value = true
            val loaded = GalleryMediaHelper.loadAllMedia(this@GalleryPickerActivity, "*/*")
            allItemsState.clear(); allItemsState.addAll(loaded)
            isLoadingState.value = false
            val isVid = mimeFilterState.value.startsWith("video/")
            val relevantCount = if (isVid) loaded.count { it.isVideo } else loaded.count { !it.isVideo }
            if (relevantCount == 0 && !hasAutoOpenedSettings) {
                hasAutoOpenedSettings = true
                GalleryPermissionHelper.openAppPermissionSettings(this@GalleryPickerActivity)
            }
        }
    }

    private fun handleStepBack() {
        if (GallerySessionHolder.canGoBackFolder()) GallerySessionHolder.popPreviousFolder() else exitGalleryCompletely()
    }

    private fun exitGalleryCompletely() {
        FloatingIndicatorService.setGalleryActive(false)
        QuickSignalingHelper.sendQuickMessage(this, SignalingMessage.PlayControl("STOP"))
        GallerySessionHolder.clearRoute(); finish(); @Suppress("DEPRECATION") overridePendingTransition(0, 0)
    }

    private fun openPhotoInContext(
        folderPhotos: List<GalleryPhotoItem>, startIndex: Int, folderName: String, fromCollectionsSingle: Boolean = false
    ) {
        if (folderPhotos.isEmpty()) return
        val safeIdx = startIndex.coerceIn(0, folderPhotos.lastIndex)
        val targetItem = folderPhotos[safeIdx]
        GallerySessionHolder.setPhotos(folderPhotos, safeIdx, folderName, explicitContext = true)
        val viewerIntent = Intent(this, PhotoCastViewerActivity::class.java).apply {
            data = targetItem.uri; addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra("EXTRA_START_INDEX", safeIdx); putExtra("IMAGE_URI", targetItem.uri.toString())
            putExtra("EXTRA_FROM_COLLECTIONS_SINGLE", fromCollectionsSingle)
        }
        viewerLauncher.launch(viewerIntent); @Suppress("DEPRECATION") overridePendingTransition(0, 0)
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
        videoLauncher.launch(videoIntent); @Suppress("DEPRECATION") overridePendingTransition(0, 0)
    }

    private fun handleExternalUrisSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        uris.forEach { u -> try { contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Throwable) {} }
        val firstUri = uris.first()
        val firstMime = GalleryPermissionHelper.resolveMime(contentResolver, firstUri, mimeFilterState.value)
        val isVideo = firstMime.startsWith("video/") || mimeFilterState.value.startsWith("video/")

        if (uris.size > 1) {
            val collectionName = "Colección (${uris.size})"
            val customList = uris.mapIndexed { idx, u ->
                val m = GalleryPermissionHelper.resolveMime(contentResolver, u, mimeFilterState.value)
                val v = m.startsWith("video/")
                GalleryPhotoItem((idx + 1).toLong(), u, if (v) "Video ${idx + 1}" else "Foto ${idx + 1}", collectionName, System.currentTimeMillis() - idx, v, m, "__collection__")
            }
            if (!openedFromSystemCollections) {
                GallerySessionHolder.navigateToFolder("__collection_${System.currentTimeMillis()}", collectionName, customItems = customList)
            }
            if (isVideo) openVideoInContext(customList.filter { it.isVideo }.ifEmpty { customList }, 0, collectionName)
            else openPhotoInContext(customList.filter { !it.isVideo }.ifEmpty { customList }, 0, collectionName)
            return
        }

        if (openedFromSystemCollections && !isVideo) {
            val initialItem = GalleryPhotoItem(0L, firstUri, "Foto", "Personas y mascotas", System.currentTimeMillis(), false, firstMime, "__collections_person__")
            openPhotoInContext(listOf(initialItem), 0, "Personas y mascotas", fromCollectionsSingle = true)
            return
        }

        lifecycleScope.launch {
            val (folderPath, contextItems) = GalleryMediaHelper.loadFolderContextForUri(this@GalleryPickerActivity, firstUri, isVideo)
            val folderName = folderPath.substringAfterLast('/').ifBlank { "Colección" }
            val fallbackId = firstUri.lastPathSegment?.substringAfterLast(':')?.filter { it.isDigit() } ?: ""
            val matchIdx = contextItems.indexOfFirst { it.uri == firstUri || (fallbackId.isNotBlank() && it.id.toString() == fallbackId) }
            val finalItems = if (matchIdx >= 0) contextItems else listOf(
                GalleryPhotoItem(0L, firstUri, if (isVideo) "Video" else "Foto", folderName, System.currentTimeMillis(), isVideo, firstMime, folderPath)
            ) + contextItems
            val finalIdx = if (matchIdx >= 0) matchIdx else 0
            if (!openedFromSystemCollections) {
                GallerySessionHolder.navigateToFolder(folderPath.ifBlank { "__collection__" }, folderName, customItems = finalItems)
            }
            if (isVideo) openVideoInContext(finalItems, finalIdx, folderName) else openPhotoInContext(finalItems, finalIdx, folderName)
        }
    }

    override fun finish() { super.finish(); @Suppress("DEPRECATION") overridePendingTransition(0, 0) }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && !com.omnicast.sender.service.CastSessionState.isGalleryCasting) {
            FloatingIndicatorService.setGalleryActive(false)
            if (!com.omnicast.sender.stream.ActiveStreamHolder.isCasting.value) {
                QuickSignalingHelper.sendQuickMessage(this, SignalingMessage.PlayControl("STOP"))
            }
        }
    }
}
