package com.omnicast.receiver.directstream

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.omnicast.core.domain.models.MediaBranch
import com.omnicast.core.protocol.SignalingMessage

/**
 * Constructor de MediaItem y sanitizador de cabeceras para ExoPlayer en Android TV.
 * Desacopla la inferencia de contenedores, subtítulos, perfiles de transporte y cabeceras
 * seguras de ExoPlayerTvManager.
 */
@OptIn(UnstableApi::class)
object TvMediaItemBuilder {

    const val DEFAULT_UA = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    fun inferTargetMime(url: String, branch: MediaBranch, fallbackMime: String? = null): String {
        val uri = Uri.parse(url)
        val p = (uri.path ?: "").lowercase()
        val q = (uri.query ?: "").lowercase()
        val isOk = (uri.host ?: "").lowercase().let { it.contains("mycdn.me") || it.contains("okcdn.ru") }
        val isHls = p.contains(".m3u8") || p.contains("/hls") || (isOk && (p.contains("m3u8") || q.contains("format=hls"))) || (p.contains(".txt") && (p.contains("master") || p.contains("index"))) || q.contains("format=hls")
        val isDash = p.contains(".mpd") || (isOk && (q.contains("ct=6") || q.contains("format=dash"))) || q.contains("format=dash")

        val inferred = when {
            branch == MediaBranch.AUDIO_ONLY || p.endsWith(".mp3") || p.endsWith(".aac") -> MimeTypes.AUDIO_MPEG
            isHls -> MimeTypes.APPLICATION_M3U8
            branch == MediaBranch.DASH_MPD || isDash -> MimeTypes.APPLICATION_MPD
            p.endsWith(".webm") || q.contains("format=webm") -> MimeTypes.VIDEO_WEBM
            else -> MimeTypes.APPLICATION_MP4
        }
        // Si el stream es explícitamente un manifiesto HLS (.m3u8), no permitir que un fallback residual lo fuerce a MP4
        if (isHls && (p.contains(".m3u8") || q.contains(".m3u8"))) {
            return MimeTypes.APPLICATION_M3U8
        }
        return fallbackMime ?: inferred
    }

    fun buildMediaItem(
        url: String,
        title: String?,
        branch: MediaBranch = MediaBranch.VOD_HLS_MP4,
        subtitles: List<SignalingMessage.SubtitleTrack> = emptyList(),
        fallbackMime: String? = null
    ): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title ?: "Transmisión Web")
            .setDisplayTitle(title ?: "Transmisión Web")
            .build()

        val builder = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(metadata)

        if (subtitles.isNotEmpty()) {
            val subConfigs = subtitles.map { sub ->
                val mime = if (sub.url.contains(".srt", true)) MimeTypes.APPLICATION_SUBRIP else MimeTypes.TEXT_VTT
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                    .setMimeType(mime)
                    .setLanguage(sub.lang)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
            }
            builder.setSubtitleConfigurations(subConfigs)
        }

        if (branch == MediaBranch.LIVE_HLS) {
            builder.setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setMaxPlaybackSpeed(1.02f)
                    .setMinPlaybackSpeed(0.98f)
                    .build()
            )
        }

        val targetMime = inferTargetMime(url, branch, fallbackMime)
        builder.setMimeType(targetMime)
        return builder.build()
    }

    fun filterSafeHeaders(url: String, headers: Map<String, String>): Map<String, String> {
        val allowed = mutableMapOf<String, String>()
        headers.forEach { (k, v) ->
            if (v.isNotBlank()) {
                when {
                    k.equals("referer", true) -> allowed["Referer"] = v
                    k.equals("origin", true) -> allowed["Origin"] = v
                    k.equals("user-agent", true) -> allowed["User-Agent"] = v
                    k.equals("cookie", true) -> allowed["Cookie"] = v
                    k.equals("authorization", true) -> allowed["Authorization"] = v
                }
            }
        }
        val lower = url.lowercase()
        if ((lower.contains("mycdn.me") || lower.contains("okcdn.ru")) && !allowed.containsKey("Referer")) {
            allowed["Referer"] = "https://ok.ru/"
        }
        if (!allowed.containsKey("User-Agent")) allowed["User-Agent"] = DEFAULT_UA
        return allowed
    }
}
