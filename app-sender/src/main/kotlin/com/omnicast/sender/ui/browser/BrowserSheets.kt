package com.omnicast.sender.ui.browser

import androidx.compose.runtime.Composable
import com.omnicast.sender.history.HistoryItem

@Composable
fun BrowserModalSheets(
    showOptionsSheet: Boolean,
    showHistorySheet: Boolean,
    pageTitle: String,
    qualityOptions: List<StreamQualityOption>,
    historyList: List<HistoryItem>,
    onTransmitOption: (StreamQualityOption) -> Unit,
    onTransmitHistory: (HistoryItem) -> Unit,
    onDismissOptions: () -> Unit,
    onClearOptions: () -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDismissHistory: () -> Unit,
    showTracksSheet: Boolean = false,
    tracksInfo: com.omnicast.core.protocol.SignalingMessage.TracksInfo? = null,
    onToggleSubtitles: () -> Unit = {},
    onSelectTextTrack: (Int) -> Unit = {},
    onSelectAudioTrack: (Int) -> Unit = {},
    onDismissTracks: () -> Unit = {}
) {
    if (showOptionsSheet && qualityOptions.isNotEmpty()) {
        WvcVideoListSheet(
            pageTitle = pageTitle, options = qualityOptions,
            onSelectAndTransmit = onTransmitOption,
            onDismiss = onDismissOptions,
            onClear = onClearOptions
        )
    }

    if (showHistorySheet) {
        PlayHistorySheet(
            historyList = historyList,
            onSelectAndTransmit = onTransmitHistory,
            onDelete = onDeleteHistory,
            onClearAll = onClearHistory,
            onDismiss = onDismissHistory
        )
    }

    if (showTracksSheet) {
        com.omnicast.sender.ui.components.TrackSelectorBottomSheet(
            tracksInfo = tracksInfo,
            onToggleSubtitles = onToggleSubtitles,
            onSelectTextTrack = onSelectTextTrack,
            onSelectAudioTrack = onSelectAudioTrack,
            onDismiss = onDismissTracks
        )
    }
}
