package com.omnicast.sender.ui.browser

import androidx.compose.runtime.Composable
import com.omnicast.sender.history.HistoryItem
import com.omnicast.sender.playlist.PlaylistManager

@Composable
fun BrowserModalSheets(
    showOptionsSheet: Boolean,
    showHistorySheet: Boolean,
    showSavedPlaylistsSheet: Boolean = false,
    videoToSave: HistoryItem? = null,
    playlistManager: PlaylistManager? = null,
    pageTitle: String,
    qualityOptions: List<StreamQualityOption>,
    historyList: List<HistoryItem>,
    onTransmitOption: (StreamQualityOption) -> Unit,
    onTransmitHistory: (HistoryItem) -> Unit,
    onDismissOptions: () -> Unit,
    onClearOptions: () -> Unit,
    onRescanOptions: (() -> Unit)? = null,
    onDeleteHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onToggleFavorite: (String) -> Unit = {},
    onDismissHistory: () -> Unit,
    onDismissPlaylists: () -> Unit = {},
    onDismissSaveDialog: () -> Unit = {},
    onSavedToPlaylist: (String) -> Unit = {},
    showWhitelistDialog: Boolean = false,
    currentUrl: String = "",
    onDismissWhitelist: () -> Unit = {},
    onWhitelistUpdated: () -> Unit = {}
) {
    if (showWhitelistDialog) {
        AdBlockWhitelistDialog(
            currentUrl = currentUrl,
            onDismiss = onDismissWhitelist,
            onWhitelistUpdated = onWhitelistUpdated
        )
    }

    if (showOptionsSheet) {
        WvcVideoListSheet(
            pageTitle = pageTitle,
            options = qualityOptions,
            onSelectAndTransmit = onTransmitOption,
            onDismiss = onDismissOptions,
            onClear = onClearOptions,
            onRescan = onRescanOptions
        )
    }

    if (showHistorySheet) {
        PlayHistorySheet(
            historyList = historyList,
            onSelectAndTransmit = onTransmitHistory,
            onToggleFavorite = onToggleFavorite,
            onDelete = onDeleteHistory,
            onClearAll = onClearHistory,
            onDismiss = onDismissHistory
        )
    }

    if (showSavedPlaylistsSheet && playlistManager != null) {
        SavedPlaylistsSheet(
            playlistManager = playlistManager,
            onSelectAndTransmit = onTransmitHistory,
            onDismiss = onDismissPlaylists
        )
    }

    if (videoToSave != null && playlistManager != null) {
        SaveToPlaylistSheet(
            videoItem = videoToSave,
            playlistManager = playlistManager,
            onDismiss = onDismissSaveDialog,
            onSaved = onSavedToPlaylist
        )
    }
}
