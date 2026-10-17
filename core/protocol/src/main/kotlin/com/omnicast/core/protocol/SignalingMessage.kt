package com.omnicast.core.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mensajes del protocolo de señalización local WebSocket entre Emisor y Receptor.
 */
@Serializable
sealed class SignalingMessage {

    @Serializable
    @SerialName("OFFER")
    data class Offer(
        val sdp: String,
        val width: Int,
        val height: Int,
        val fps: Int,
        val bitrateKbps: Int,
        val smoothBuffer: Boolean = true
    ) : SignalingMessage()

    @Serializable
    @SerialName("ANSWER")
    data class Answer(
        val sdp: String
    ) : SignalingMessage()

    @Serializable
    @SerialName("ICE_CANDIDATE")
    data class IceCandidate(
        val sdpMid: String,
        val sdpMLineIndex: Int,
        val candidate: String
    ) : SignalingMessage()

    @Serializable
    @SerialName("DIRECT_STREAM_HANDOFF")
    data class DirectStreamHandoff(
        val mediaUrl: String,
        val format: String = "HLS",
        val headers: Map<String, String> = emptyMap(),
        val title: String? = null,
        val startPositionMs: Long = 0L,
        val branch: String = "VOD_HLS_MP4"
    ) : SignalingMessage()

    @Serializable
    @SerialName("PING")
    data class Ping(
        val clientTimestamp: Long
    ) : SignalingMessage()

    @Serializable
    @SerialName("PONG")
    data class Pong(
        val serverTimestamp: Long
    ) : SignalingMessage()

    @Serializable
    @SerialName("SHOW_IMAGE")
    data class ShowImage(
        val imageUrl: String,
        val title: String? = null
    ) : SignalingMessage()

    @Serializable
    @SerialName("IMAGE_TRANSFORM")
    data class ImageTransform(
        val scale: Float = 1.0f,
        val panX: Float = 0.0f,
        val panY: Float = 0.0f,
        val rotation: Float = 0.0f
    ) : SignalingMessage()

    @Serializable
    @SerialName("SET_DISPLAY_MODE")
    data class SetDisplayMode(
        val mode: String,
        val zoom: Float = 1.0f,
        val panY: Float = 0.0f,
        val panX: Float = 0.0f
    ) : SignalingMessage()

    @Serializable
    @SerialName("PLAY_CONTROL")
    data class PlayControl(
        val action: String,
        val positionMs: Long = 0L,
        val volume: Float = 1.0f,
        val param: String? = null
    ) : SignalingMessage()

    @Serializable
    @SerialName("PLAYBACK_STATUS")
    data class PlaybackStatus(
        val state: String,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L,
        val mediaUrl: String? = null,
        val title: String? = null
    ) : SignalingMessage()

    @Serializable
    @SerialName("TRACKS_INFO")
    data class TracksInfo(
        val audioTracks: List<String> = emptyList(),
        val textTracks: List<String> = emptyList(),
        val selectedAudioIndex: Int = -1,
        val selectedTextIndex: Int = -1,
        val subtitlesEnabled: Boolean = false
    ) : SignalingMessage()

    @Serializable
    @SerialName("DISCONNECT")
    data class Disconnect(
        val reason: String = "User requested disconnect"
    ) : SignalingMessage()

    @Serializable
    @SerialName("SYNC_PLAYLISTS")
    data class SyncPlaylists(
        val playlistsJson: String
    ) : SignalingMessage()
}
