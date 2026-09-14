package com.omnicast.receiver.directstream

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.omnicast.core.protocol.SignalingMessage

/**
 * Gestor modular de pistas de audio y subtítulos para ExoPlayer en TV Box.
 * Soporta activación/desactivación de subtítulos y selección de idioma dinámico.
 */
@OptIn(UnstableApi::class)
class TvTrackManager(
    private val context: Context,
    val trackSelector: DefaultTrackSelector
) {
    var subtitlesEnabled: Boolean = false
        private set

    fun getTracksInfo(player: ExoPlayer): SignalingMessage.TracksInfo {
        val currentTracks = player.currentTracks
        val audioList = mutableListOf<String>()
        val textList = mutableListOf<String>()
        var selAudio = -1
        var selText = -1

        var audioIdx = 0
        var textIdx = 0

        for (group in currentTracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val lang = format.language?.uppercase() ?: "Pista ${audioIdx + 1}"
                    val label = format.label?.let { " - $it" } ?: ""
                    audioList.add("$lang$label")
                    if (group.isTrackSelected(i)) selAudio = audioIdx
                    audioIdx++
                }
            } else if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val lang = format.language?.uppercase() ?: "Sub ${textIdx + 1}"
                    val label = format.label?.let { " - $it" } ?: ""
                    textList.add("$lang$label")
                    if (group.isTrackSelected(i)) selText = textIdx
                    textIdx++
                }
            }
        }

        return SignalingMessage.TracksInfo(
            audioTracks = audioList,
            textTracks = textList,
            selectedAudioIndex = selAudio,
            selectedTextIndex = if (subtitlesEnabled) selText else -1,
            subtitlesEnabled = subtitlesEnabled
        )
    }

    fun toggleSubtitles(enable: Boolean? = null): Boolean {
        subtitlesEnabled = enable ?: !subtitlesEnabled
        val params = trackSelector.buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesEnabled)
        if (subtitlesEnabled) {
            params.setPreferredTextLanguage("es")
        }
        trackSelector.parameters = params.build()
        return subtitlesEnabled
    }

    fun selectTextTrack(player: ExoPlayer, index: Int) {
        if (index < 0) {
            toggleSubtitles(false)
            return
        }
        subtitlesEnabled = true
        var currentIdx = 0
        for (group in player.currentTracks.groups) {
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    if (currentIdx == index) {
                        val override = TrackSelectionOverride(group.mediaTrackGroup, i)
                        trackSelector.parameters = trackSelector.buildUponParameters()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .setOverrideForType(override)
                            .build()
                        return
                    }
                    currentIdx++
                }
            }
        }
    }

    fun selectAudioTrack(player: ExoPlayer, index: Int) {
        if (index < 0) return
        var currentIdx = 0
        for (group in player.currentTracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    if (currentIdx == index) {
                        val override = TrackSelectionOverride(group.mediaTrackGroup, i)
                        trackSelector.parameters = trackSelector.buildUponParameters()
                            .setOverrideForType(override)
                            .build()
                        return
                    }
                    currentIdx++
                }
            }
        }
    }
}
