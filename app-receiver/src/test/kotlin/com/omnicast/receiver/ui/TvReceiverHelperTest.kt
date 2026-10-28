package com.omnicast.receiver.ui

import org.junit.Assert.*
import org.junit.Test

class TvReceiverHelperTest {

    @Test
    fun testShouldUseWebPlayerTargetHeaderOverride() {
        val webHeaders = mapOf("X-Target-Player" to "WEBVIEW")
        val exoHeaders = mapOf("X-Target-Player" to "EXOPLAYER")
        val neutralUrl = "https://example.com/video.mp4"

        assertTrue(TvReceiverHelper.shouldUseWebPlayer(neutralUrl, webHeaders))
        assertFalse(TvReceiverHelper.shouldUseWebPlayer("https://ds4.dropcdn.io/hls/master.m3u8", exoHeaders))
    }

    @Test
    fun testShouldUseWebPlayerProtectedEmbeds() {
        val protectedReferers = listOf(
            "https://luluvdo.com/e/abc",
            "https://lulustream.com/e/abc",
            "https://pelisgo.online/ver/abc"
        )
        val dummyUrl = "https://example.com/stream.m3u8"
        for (ref in protectedReferers) {
            val hdrs = mapOf("Referer" to ref)
            assertTrue("Referer $ref debe activar WebPlayer", TvReceiverHelper.shouldUseWebPlayer(dummyUrl, hdrs))
        }
    }

    @Test
    fun testShouldUseWebPlayerForSignedTokensDropCdn() {
        val dropUrls = listOf(
            "https://ds4.dropcdn.io/hls2/01/00038/master.m3u8?t=oV8cxTiNEmX1J6c4kfT1aAmW6u0Xt0p-5zKXBPvMoWc&s=1730000000",
            "https://fastream.to/hls2/01/00038/master.m3u8?t=oV8cxTiNEmX1J6c4kfT1aAmW6u0Xt0p-5zKXBPvMoWc&s=1730000000"
        )
        for (url in dropUrls) {
            val hdrs = mapOf("Referer" to "https://dr0pstream.com/e/123")
            assertTrue("DropCDN con token firmado debe usar WebPlayer (Navegador): $url", TvReceiverHelper.shouldUseWebPlayer(url, hdrs))
        }
    }

    @Test
    fun testShouldUseWebPlayerSessionCookies() {
        val hdrs = mapOf("Cookie" to "session=abc123xyz")
        val dummyUrl = "https://example.com/stream.m3u8"
        assertTrue(TvReceiverHelper.shouldUseWebPlayer(dummyUrl, hdrs))
    }

    @Test
    fun testShouldUseWebPlayerSignedSecurityTokens() {
        val signedUrl = "https://s1.tnmr.org/hls/master.m3u8?t=abcdef1234567890abcdef&s=1730000000"
        assertTrue(TvReceiverHelper.shouldUseWebPlayer(signedUrl, emptyMap()))
    }

    @Test
    fun testShouldNotUseWebPlayerForDirectStreams() {
        val normalMp4 = "https://storage.googleapis.com/test/sample.mp4"
        val directM3u8 = "https://808ball.com/live/match1.m3u8"
        assertFalse(TvReceiverHelper.shouldUseWebPlayer(normalMp4, emptyMap()))
        assertFalse(TvReceiverHelper.shouldUseWebPlayer(directM3u8, emptyMap()))
    }

    @Test
    fun testShouldUseAntiCorsInterceptor() {
        assertTrue(TvReceiverHelper.shouldUseAntiCorsInterceptor("https://ds4.dropcdn.io/hls/master.m3u8", emptyMap()))
        assertTrue(TvReceiverHelper.shouldUseAntiCorsInterceptor("https://fastream.to/hls/master.m3u8", emptyMap()))
        assertFalse(TvReceiverHelper.shouldUseAntiCorsInterceptor("https://example.com/stream.m3u8", mapOf("X-Target-Player" to "CHROMIUM_PURE")))
        assertTrue(TvReceiverHelper.shouldUseAntiCorsInterceptor("https://pelisgo.online/stream/master.txt", emptyMap()))
    }
}
