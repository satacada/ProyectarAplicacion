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

        val fakeMasterWithEndList = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=1280000
            #EXT-X-ENDLIST
        """.trimIndent()
        assertFalse(HlsMasterResolver.isMasterContent(fakeMasterWithEndList))

        val fakeMasterWithMediaSeq = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=1280000
            #EXT-X-MEDIA-SEQUENCE:100
        """.trimIndent()
        assertFalse(HlsMasterResolver.isMasterContent(fakeMasterWithMediaSeq))
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
    fun `isLikelySubVariantUrl detects decoupled audio and subtitle tracks patterns`() {
        assertTrue(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/hls/tracks-a1.m3u8"))
        assertTrue(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/live/audio/stream.m3u8"))
        assertTrue(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/live/stream_audio_en.m3u8"))
        assertTrue(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/live/audio_spa.m3u8"))
        assertTrue(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/live/subs/es.m3u8"))
        assertTrue(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/live/_spa/stream.m3u8"))
        assertTrue(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/live/masteraudio.m3u8"))
        assertTrue(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/live/mastersubs.m3u8"))
        assertTrue(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/live/subtitles_spa.m3u8"))
        assertTrue(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/live/audio_rendition.m3u8"))
        assertFalse(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/live/master.m3u8"))
        assertFalse(HlsMasterResolver.isLikelySubVariantUrl("https://cdn.example.com/live/index.m3u8"))
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

    @Test
    fun `isMasterContent handles CRLF line endings and empty or blank manifests`() {
        val crlfMaster = "#EXTM3U\r\n#EXT-X-VERSION:3\r\n#EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360\r\nstream.m3u8\r\n"
        assertTrue(HlsMasterResolver.isMasterContent(crlfMaster))

        val crlfMedia = "#EXTM3U\r\n#EXT-X-TARGETDURATION:6\r\n#EXTINF:6.0,\r\nchunk01.ts\r\n#EXT-X-ENDLIST\r\n"
        assertFalse(HlsMasterResolver.isMasterContent(crlfMedia))

        assertFalse(HlsMasterResolver.isMasterContent(""))
        assertFalse(HlsMasterResolver.isMasterContent("   \n\r\n   \t  "))
    }

    @Test
    fun `isMasterContent strictly handles multiple EXT-X-MEDIA tags without false negatives or collisions`() {
        val masterWithMultipleMedia = """
            #EXTM3U
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="English",DEFAULT=YES,AUTOSELECT=YES,URI="audio/en.m3u8"
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="Spanish",DEFAULT=NO,AUTOSELECT=YES,URI="audio/es.m3u8"
            #EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="subs",NAME="English",DEFAULT=YES,URI="subs/en.m3u8"
            #EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="subs",NAME="Spanish",DEFAULT=NO,URI="subs/es.m3u8"
            #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720,AUDIO="audio",SUBTITLES="subs"
            720p.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080,AUDIO="audio",SUBTITLES="subs"
            1080p.m3u8
        """.trimIndent()
        assertTrue(HlsMasterResolver.isMasterContent(masterWithMultipleMedia))

        val audioSubvariantWithDisqualifier = """
            #EXTM3U
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="English",URI="audio.m3u8"
            #EXT-X-TARGETDURATION:10
            #EXTINF:10.0,
            audio_segment1.m4a
        """.trimIndent()
        assertFalse(HlsMasterResolver.isMasterContent(audioSubvariantWithDisqualifier))
    }

    @Test
    fun `deriveMasterPlaylistUrl handles relative URLs and encoded query parameters`() {
        val encodedUrl = "https://cdn.example.com/hls/stream03/chunklist.m3u8?token=abc%2Bxyz%3D%3D&expires=1700000000&sig=foo%26bar"
        val expectedMaster = "https://cdn.example.com/hls/master.m3u8?token=abc%2Bxyz%3D%3D&expires=1700000000&sig=foo%26bar"
        assertEquals(expectedMaster, HlsMasterResolver.deriveMasterPlaylistUrl(encodedUrl))

        val relIndexed = "channels/live_1080.m3u8?auth=123"
        assertEquals("channels/live.m3u8?auth=123", HlsMasterResolver.deriveMasterPlaylistUrl(relIndexed))

        val relFolder = "live/stream02/chunklist.m3u8"
        assertEquals("live/master.m3u8", HlsMasterResolver.deriveMasterPlaylistUrl(relFolder))

        assertNull(HlsMasterResolver.deriveMasterPlaylistUrl(""))
        assertNull(HlsMasterResolver.deriveMasterPlaylistUrl("   "))
    }

    @Test
    fun `isMasterPlaylistUrl and isLikelySubVariantUrl support encoded query strings and relative paths`() {
        assertTrue(HlsMasterResolver.isMasterPlaylistUrl("hls/master.m3u8?auth=token%3D123%26sig%3Dabc"))
        assertTrue(HlsMasterResolver.isMasterPlaylistUrl("index.m3u8"))
        assertFalse(HlsMasterResolver.isMasterPlaylistUrl("stream01/chunklist.m3u8"))

        assertTrue(HlsMasterResolver.isLikelySubVariantUrl("/audio/stream.m3u8?token=abc%20123"))
        assertTrue(HlsMasterResolver.isLikelySubVariantUrl("tracks-a1.m3u8"))
        assertFalse(HlsMasterResolver.isLikelySubVariantUrl("master.m3u8?token=abc%20123"))
    }
}

