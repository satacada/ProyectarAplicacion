package com.omnicast.receiver.ui.components.sentinel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class CdnHoneypotSentinelTest {

    private class FakeHttpUrlConnection(
        private val cType: String,
        private val content: String
    ) : HttpURLConnection(URL("https://example.com")) {
        override fun connect() {}
        override fun disconnect() {}
        override fun usingProxy(): Boolean = false
        override fun getContentType(): String = cType
        override fun getInputStream(): InputStream = ByteArrayInputStream(content.toByteArray())
    }

    @Test
    fun testIsHoneypotHtmlDetectsHtmlOnMediaUrls() {
        val conn = FakeHttpUrlConnection("text/html; charset=UTF-8", "<!doctype html><title>Coming Soon</title>")
        val mediaUrl = "https://f9rw3r.cfglobalcdn.com/test.mp4.m3u8"
        assertTrue(CdnHoneypotSentinel.isHoneypotHtml(conn, mediaUrl))

        val nonMediaUrl = "https://example.com/index.html"
        assertFalse(CdnHoneypotSentinel.isHoneypotHtml(conn, nonMediaUrl))
    }

    @Test
    fun testIsHoneypotHtmlAllowsLegitimateMediaContentType() {
        val conn = FakeHttpUrlConnection("application/vnd.apple.mpegurl", "#EXTM3U\n#EXTINF:10,\nseg1.ts")
        val mediaUrl = "https://f9rw3r.cfglobalcdn.com/playlist.m3u8"
        assertFalse(CdnHoneypotSentinel.isHoneypotHtml(conn, mediaUrl))
    }

    @Test
    fun testIsHoneypotHtmlDetectsDashMpdHoneypot() {
        val htmlConn = FakeHttpUrlConnection("text/html; charset=UTF-8", "<html><body>Error 404</body></html>")
        val dashUrl = "https://cdn.example.com/dash/manifest.mpd"
        assertTrue(CdnHoneypotSentinel.isHoneypotHtml(htmlConn, dashUrl))

        val legitConn = FakeHttpUrlConnection("application/dash+xml", "<MPD xmlns=\"urn:mpeg:dash:schema:mpd:2011\"><Period></Period></MPD>")
        assertFalse(CdnHoneypotSentinel.isHoneypotHtml(legitConn, dashUrl))
    }

    @Test
    fun testIsHoneypotHtmlDetectsDeceptiveHtmlSignatures() {
        val signatures = listOf(
            "<!doctype html><html><body>Parking</body></html>",
            "<html><head><title>Bot Check</title></head></html>",
            "Welcome to domain parking service",
            "<script src=\"/cdn-cgi/challenge-platform/h/b/orchestrate/chl_page/v1\"></script>"
        )

        val mediaUrls = listOf(
            "https://cdn.example.com/video.m3u8",
            "https://cdn.example.com/manifest.mpd",
            "https://cdn.example.com/segment.ts",
            "https://cdn.example.com/clip.mp4"
        )

        for ((index, sig) in signatures.withIndex()) {
            val conn = FakeHttpUrlConnection("application/octet-stream", sig)
            val url = mediaUrls[index]
            assertTrue("Expected honeypot detected for signature: $sig on $url", CdnHoneypotSentinel.isHoneypotHtml(conn, url))
        }
    }
}
