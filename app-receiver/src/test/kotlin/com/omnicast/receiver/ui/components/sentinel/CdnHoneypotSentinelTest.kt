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
}
