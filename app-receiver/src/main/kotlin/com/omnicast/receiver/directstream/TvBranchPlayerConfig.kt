package com.omnicast.receiver.directstream

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.omnicast.core.domain.models.MediaBranch

/**
 * Configurador polimórfico de reproducción en 6 ramas según ADR-006 y HU-009.
 * Ajusta buffers, decodificadores de hardware y políticas de reconexión.
 */
@OptIn(UnstableApi::class)
object TvBranchPlayerConfig {

    fun createLoadControl(branch: MediaBranch): LoadControl {
        return when (branch) {
            MediaBranch.LIVE_HLS -> DefaultLoadControl.Builder()
                .setBufferDurationsMs(2_000, 4_000, 1_000, 2_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(0, false)
                .build()

            MediaBranch.VOD_HLS_MP4 -> DefaultLoadControl.Builder()
                .setBufferDurationsMs(30_000, 60_000, 2_500, 5_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(20_000, true)
                .build()

            MediaBranch.DASH_MPD -> DefaultLoadControl.Builder()
                .setBufferDurationsMs(25_000, 50_000, 2_000, 4_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(15_000, true)
                .build()

            MediaBranch.AUDIO_ONLY -> DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 30_000, 1_500, 3_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(10_000, true)
                .build()

            MediaBranch.IPTV_STREAM -> DefaultLoadControl.Builder()
                .setBufferDurationsMs(8_000, 20_000, 1_500, 3_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(5_000, false)
                .build()

            MediaBranch.LOCAL_MEDIA -> DefaultLoadControl.Builder()
                .setBufferDurationsMs(5_000, 15_000, 1_000, 2_000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(5_000, true)
                .build()

            MediaBranch.DISCARD -> DefaultLoadControl.Builder().build()
        }
    }

    fun applyHardwareCodecSettings(trackSelector: DefaultTrackSelector?, branch: MediaBranch) {
        val ts = trackSelector ?: return
        val isAudioOnly = branch == MediaBranch.AUDIO_ONLY
        val newParams = ts.buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, isAudioOnly)
            .build()
        ts.parameters = newParams
    }

    fun isLiveOrContinuous(branch: MediaBranch): Boolean {
        return branch == MediaBranch.LIVE_HLS || branch == MediaBranch.IPTV_STREAM
    }
}
