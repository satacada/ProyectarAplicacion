package com.omnicast.sender.ui.browser

import androidx.compose.foundation.layout.Box
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
    detectedCount: Int,
    pageTitle: String,
    onTransmitDirect: () -> Unit,
    onOpenQualities: () -> Unit,
    onPlayControl: (String, Long, Float) -> Unit,
    onStopCasting: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (isCasting) {
            RemoteControlBar(
                title = castTitle.ifBlank { pageTitle },
                isPlaying = isPlaying, positionMs = posMs, durationMs = durMs, volume = 1f,
                onPlayPause = { onPlayControl(if (isPlaying) "PAUSE" else "PLAY", posMs, 1f) },
                onSeekTo = { onPlayControl("SEEK", it, 1f) },
                onSkip = { onPlayControl("SEEK", (posMs + it).coerceAtLeast(0L), 1f) },
                onVolumeChange = { onPlayControl("VOLUME", 0L, it) },
                onStop = onStopCasting,
                onSelectQuality = onOpenQualities
            )
        } else if (detectedCount > 0) {
            DetectedStreamBar(
                streamCount = detectedCount, title = pageTitle,
                onTransmitDirect = onTransmitDirect, onOpenQualities = onOpenQualities
            )
        }
    }
}
