package com.omnicast.core.domain.usecases

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HlsMasterResolverTest {

    @Test
    fun `isMasterContent identifies master playlists strictly by RFC 8216 tags`() {
        val validMasterWithStreams = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-STREAM-INF:BANDWIDTH=1280000,RESOLUTION=1280x720
            stream01.m3u8
        """.trimIndent()
        assertTrue(HlsMasterResolver.isMasterContent(validMasterWithStreams))

        val validMasterWithMedia = """
            #EXTM3U
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="English",URI="audio.m3u8"
        """.trimIndent()
        assertTrue(HlsMasterResolver.isMasterContent(validMasterWithMedia))

        val mediaPlaylistWithSegments = """
            #EXTM3U
            #EXT-X-TARGETDURATION:10
            #EXTINF:9.009,
            segment1.ts
        """.trimIndent()
        assertFalse(HlsMasterResolver.isMasterContent(mediaPlaylistWithSegments))

        val fakeMasterWithExtinf = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=1280000
            #EXTINF:9.009,
            chunk.ts
        """.trimIndent()
        assertFalse(HlsMasterResolver.isMasterContent(fakeMasterWithExtinf))
    }

    @Test
    fun `isAudioOrRawSegment detects audio and raw media segments`() {
        assertTrue(HlsMasterResolver.isAudioOrRawSegment("https://cdn.example.com/live/masteraudio1-A-20261030_02679.aac"))
        assertTrue(HlsMasterResolver.isAudioOrRawSegment("https://cdn.example.com/live/segment-001.ts"))
        assertTrue(HlsMasterResolver.isAudioOrRawSegment("https://cdn.example.com/live/video_1080p.m4s"))
        assertTrue(HlsMasterResolver.isAudioOrRawSegment("https://cdn.example.com/live/subtitles_es.vtt"))
        assertFalse(HlsMasterResolver.isAudioOrRawSegment("https://cdn.example.com/live/master.m3u8"))
        assertFalse(HlsMasterResolver.isAudioOrRawSegment("https://cdn.example.com/live/playlist.m3u8?token=xyz"))
    }

    @Test
    fun `isLikelySubVariantUrl catches audio sub-playlists and chunklists`() {
        assertTrue(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/hls/masteraudio1.m3u8"))
        assertTrue(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/hls/mastersubs.m3u8"))
        assertTrue(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/hls/audio/prog_index.m3u8"))
        assertTrue(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/hls/stream04/chunklist.m3u8"))
        assertFalse(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/hls/master.m3u8"))
        assertFalse(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/live/playlist.m3u8"))
    }

    @Test
    fun `isMasterPlaylistUrl accepts legitimate master filenames and rejects raw audio or chunks`() {
        assertTrue(HlsMasterResolver.isMasterPlaylistUrl("https://cdn.example.com/hls/master.m3u8"))
        assertTrue(HlsMasterResolver.isMasterPlaylistUrl("https://cdn.example.com/hls/playlist.m3u8?token=abc"))
        assertTrue(HlsMasterResolver.isMasterPlaylistUrl("https://fast-channels.sinclairstoryline.com/CHARGE/index.m3u8"))
        assertFalse(HlsMasterResolver.isMasterPlaylistUrl("https://linear901-oo-hls0-prd-gtm.delivery.skycdp.com/v1/master/stream04/streamPlaylist.m3u8"))
        assertFalse(HlsMasterResolver.isMasterPlaylistUrl("https://fast-channels.sinclairstoryline.com/CHARGE/index_2.m3u8"))
        assertFalse(HlsMasterResolver.isMasterPlaylistUrl("https://cdn.example.com/hls/masteraudio1-A-20261030_02679.aac"))
        assertFalse(HlsMasterResolver.isMasterPlaylistUrl("https://cdn.example.com/hls/masteraudio1.m3u8"))
    }

    @Test
    fun `deriveMasterPlaylistUrl ascends from subvariant to parent master`() {
        val skySubvariant = "https://linear901-oo-hls0-prd-gtm.delivery.skycdp.com/v1/master/3722/prod-gb-lin-skynews-hls-25-web/stream04/streamPlaylist.m3u8"
        val expectedMaster = "https://linear901-oo-hls0-prd-gtm.delivery.skycdp.com/v1/master/3722/prod-gb-lin-skynews-hls-25-web/master.m3u8"
        assertEquals(expectedMaster, HlsMasterResolver.deriveMasterPlaylistUrl(skySubvariant))

        val sinclairSubvariant = "https://fast-channels.sinclairstoryline.com/CHARGE/index_2.m3u8?token=123"
        val expectedSinclairMaster = "https://fast-channels.sinclairstoryline.com/CHARGE/index.m3u8?token=123"
        assertEquals(expectedSinclairMaster, HlsMasterResolver.deriveMasterPlaylistUrl(sinclairSubvariant))

        val dasErsteSubvariant = "https://derste247liveint.akamaized.net/hls/live/662735/daserste_int/master720p3200.m3u8"
        val expectedDasErsteMaster = "https://derste247liveint.akamaized.net/hls/live/662735/daserste_int/master.m3u8"
        assertEquals(expectedDasErsteMaster, HlsMasterResolver.deriveMasterPlaylistUrl(dasErsteSubvariant))

        assertNull(HlsMasterResolver.deriveMasterPlaylistUrl("https://example.com/hls/master.m3u8"))
        assertNull(HlsMasterResolver.deriveMasterPlaylistUrl("https://fast-channels.sinclairstoryline.com/CHARGE/index.m3u8"))
    }
}
