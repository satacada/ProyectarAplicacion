package com.omnicast.core.domain.usecases

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamCandidateValidatorTest {

    @Test
    fun testRejectsWebDocumentsAndAssets() {
        assertTrue(StreamCandidateValidator.isWebDocumentOrPlayerRoute("https://example.com/movie.html"))
        assertTrue(StreamCandidateValidator.isWebDocumentOrPlayerRoute("https://example.com/page.htm?param=1"))
        assertTrue(StreamCandidateValidator.isWebDocumentOrPlayerRoute("https://example.com/app.js"))
        assertTrue(StreamCandidateValidator.isWebDocumentOrPlayerRoute("https://example.com/app.js?v=1.2"))
        assertTrue(StreamCandidateValidator.isWebDocumentOrPlayerRoute("https://cdn.jsdelivr.net/npm/hls.js@1.6.16"))
        assertTrue(StreamCandidateValidator.isWebDocumentOrPlayerRoute("https://unpkg.com/video.js@8.0.0/dist/video.min.js"))
        assertTrue(StreamCandidateValidator.isWebDocumentOrPlayerRoute("https://example.com/style.css"))
        assertTrue(StreamCandidateValidator.isWebDocumentOrPlayerRoute("https://example.com/data.json"))
        assertTrue(StreamCandidateValidator.isWebDocumentOrPlayerRoute("https://example.com/image.png"))
    }

    @Test
    fun testRejectsWebPlayerAndIframeRoutes() {
        assertTrue(StreamCandidateValidator.isWebDocumentOrPlayerRoute("https://playmogo.com/dood?op=watch&hash=123"))
        assertTrue(StreamCandidateValidator.isWebDocumentOrPlayerRoute("https://playmogo.com/dood?op=download&hash=123"))
        assertTrue(StreamCandidateValidator.isWebDocumentOrPlayerRoute("https://doopstream.com/e/abc123xyz"))
        assertTrue(StreamCandidateValidator.isWebDocumentOrPlayerRoute("https://dr0pstream.com/embed-abc123456.html"))
        assertTrue(StreamCandidateValidator.isWebDocumentOrPlayerRoute("https://dr0pstream.com/cdn-cgi/rum?test"))
        assertTrue(StreamCandidateValidator.isWebDocumentOrPlayerRoute("https://playmogo.com/pass_md5/token-hash"))
        assertTrue(StreamCandidateValidator.isWebDocumentOrPlayerRoute("https://ok.ru/video/videostat?id=123"))
    }

    @Test
    fun testAcceptsBinaryMediaSignatures() {
        assertTrue(StreamCandidateValidator.isBinaryMediaStream("https://cdn.example.com/video.mp4"))
        assertTrue(StreamCandidateValidator.isBinaryMediaStream("https://cdn.example.com/video.mp4?token=abc&exp=123"))
        assertTrue(StreamCandidateValidator.isBinaryMediaStream("https://cdn.example.com/master.m3u8"))
        assertTrue(StreamCandidateValidator.isBinaryMediaStream("https://cdn.example.com/hls/index.m3u8?t=999"))
        assertTrue(StreamCandidateValidator.isBinaryMediaStream("https://dr0pstream.com/hls2/01/00038/master.m3u8?t=123"))
        assertTrue(StreamCandidateValidator.isBinaryMediaStream("https://m12.doodcdn.co/pass/xyz123/video.mp4?token=abc"))
        assertTrue(StreamCandidateValidator.isBinaryMediaStream("https://playmogo.com/video~123abc456?token=secret"))
        assertTrue(StreamCandidateValidator.isBinaryMediaStream("https://uha211.cloudatacdn.com/path/xmobbs6ryg~1mD7KEshPZ?token=s3qrrcubquq0jgu10n1g7glx&expiry=1793794255217"))
        assertTrue(StreamCandidateValidator.isBinaryMediaStream("https://cdn.example.com/manifest.mpd"))
        assertTrue(StreamCandidateValidator.isBinaryMediaStream("https://cdn.example.com/stream/chunklist.m3u8"))
    }

    @Test
    fun testIsValidCdnStreamContract() {
        val goodStream = "https://playmogo.com/video~123abc456?token=secret"
        val iframeUrl = "https://playmogo.com/dood?op=watch&hash=123"
        val wrongDomain = "https://unrelated.com/video.mp4"

        assertTrue(StreamCandidateValidator.isValidCdnStream(goodStream, domainMatches = true))
        assertFalse("Debe rechazar si domainMatches es false", StreamCandidateValidator.isValidCdnStream(goodStream, domainMatches = false))
        assertFalse("Debe rechazar si es ruta de reproductor/iframe", StreamCandidateValidator.isValidCdnStream(iframeUrl, domainMatches = true))
        assertFalse("Debe rechazar dominio no coincidente", StreamCandidateValidator.isValidCdnStream(wrongDomain, domainMatches = false))
    }
}
