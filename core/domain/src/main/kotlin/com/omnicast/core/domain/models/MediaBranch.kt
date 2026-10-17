package com.omnicast.core.domain.models

/**
 * 6 Ramas polimórficas de despacho de medios según ADR-006 y HU-009.
 */
enum class MediaBranch(val code: Int) {
    DISCARD(0),
    VOD_HLS_MP4(1),
    LIVE_HLS(2),
    DASH_MPD(3),
    AUDIO_ONLY(4),
    IPTV_STREAM(5),
    LOCAL_MEDIA(6);

    companion object {
        fun fromCode(code: Int): MediaBranch = entries.firstOrNull { it.code == code } ?: DISCARD
    }
}
