package com.omnicast.sender.ui.browser

import com.omnicast.core.domain.models.StreamCandidate
import com.omnicast.sender.ui.browser.extractors.UniversalHlsExtractorAdapter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalHlsExtractorAdapterTest {

    private val adapter = UniversalHlsExtractorAdapter()

    @Test
    fun testRejectsWebAssetsEvenWithStreamInPath() {
        assertFalse(adapter.canHandle(StreamCandidate("https://example.com/stream/poster.webp")))
        assertFalse(adapter.canHandle(StreamCandidate("https://example.com/stream/thumbnail.jpg")))
        assertFalse(adapter.canHandle(StreamCandidate("https://example.com/stream/icon.png")))
        assertFalse(adapter.canHandle(StreamCandidate("https://example.com/stream/banner.gif")))
        assertFalse(adapter.canHandle(StreamCandidate("https://example.com/stream/script.js")))
        assertFalse(adapter.canHandle(StreamCandidate("https://example.com/stream/page.html")))
    }

    @Test
    fun testRejectsFakePrerolls() {
        assertFalse(adapter.canHandle(StreamCandidate("https://cdn.doodstream.com/overroll_ad.m3u8")))
        assertFalse(adapter.canHandle(StreamCandidate("https://gasparin.example.com/hls/stream/live.m3u8")))
        assertFalse(adapter.canHandle(StreamCandidate("https://cdn.example.com/public/video.mp4")))
    }

    @Test
    fun testAcceptsValidHlsStreams() {
        assertTrue(adapter.canHandle(StreamCandidate("https://cdn.example.com/live/master.m3u8")))
        assertTrue(adapter.canHandle(StreamCandidate("https://cdn.example.com/stream/chunklist.m3u8")))
        assertTrue(adapter.canHandle(StreamCandidate("https://cdn.pelisgo.com/hls/master.txt")))
    }

    @Test
    fun testAdBlockerDetectsFakePrerollVideos() {
        assertTrue(AdBlocker.isVideoAd("https://cdn.doodstream.com/overroll_video_123.mp4"))
        assertTrue(AdBlocker.isVideoAd("https://gasparin.cdn.com/ad.mp4"))
        assertTrue(AdBlocker.isVideoAd("https://cdn.example.com/public/video.mp4"))
    }
}
