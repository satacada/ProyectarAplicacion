package com.omnicast.sender.ui.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.omnicast.sender.ui.components.RemoteControlBar

@Composable
fun BrowserBottomBarContainer(
    isCasting: Boolean,
    castTitle: String,
    isPlaying: Boolean,
    posMs: Long,
    durMs: Long,
    volume: Float = 1.0f,
    detectedCount: Int,
    streamSubtitle: String = "HLS • m3u8 stream directo",
    pageTitle: String,
    onTransmitLatest: () -> Unit,
    onShowDetectedStreams: () -> Unit,
    onOpenQualities: () -> Unit,
    onPlayControl: (String, Long, Float) -> Unit,
    onStopCasting: () -> Unit,
    onClearStreams: () -> Unit = {},
    onRescanStreams: () -> Unit = {},
    onSaveToPlaylist: (() -> Unit)? = null,
    onDeleteFromSaved: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        RemoteControlBar(
            title = if (isCasting) castTitle.ifBlank { pageTitle } else pageTitle,
            streamCount = detectedCount,
            streamSubtitle = streamSubtitle,
            isPlaying = isPlaying,
            positionMs = posMs,
            durationMs = durMs,
            volume = volume,
            onShowDetectedStreams = onShowDetectedStreams,
            onRescanStreams = onRescanStreams,
            onClearStreams = onClearStreams,
            onTransmitLatest = onTransmitLatest,
            onPlayPause = {
                val isNearEnd = durMs > 2000L && posMs >= (durMs - 1500L)
                if (isPlaying) {
                    onPlayControl("PAUSE", posMs, 1f)
                } else {
                    if (isNearEnd) onPlayControl("SEEK", 0L, 1f)
                    onPlayControl("PLAY", if (isNearEnd) 0L else posMs, 1f)
                }
            },
            onSeekTo = { onPlayControl("SEEK", it, 1f) },
            onSkip = { onPlayControl("SEEK", (posMs + it).coerceAtLeast(0L), 1f) },
            onVolumeChange = { onPlayControl("VOLUME", 0L, it) },
            onStop = onStopCasting,
            onSelectQuality = onOpenQualities,
            onSaveToPlaylist = onSaveToPlaylist,
            onDeleteFromSaved = onDeleteFromSaved
        )
    }
}
