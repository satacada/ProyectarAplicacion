package com.omnicast.sender.ui.browser

import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas de regresión para garantizar el soporte inamovible de YouTube y Dailymotion.
 * LEY INVIOLABLE: Ningún commit o refactorización puede romper la captura o la interfaz de estas plataformas.
 */
class MediaPlatformRegressionTest {

    @Test
    fun testYouTubeIdExtraction() {
        val standard = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val shorts = "https://www.youtube.com/shorts/dQw4w9WgXcQ"
        val share = "https://youtu.be/dQw4w9WgXcQ"
        val embed = "https://www.youtube.com/embed/dQw4w9WgXcQ"

        assertEquals("dQw4w9WgXcQ", BrowserUrlHelper.extractYouTubeId(standard))
        assertEquals("dQw4w9WgXcQ", BrowserUrlHelper.extractYouTubeId(shorts))
        assertEquals("dQw4w9WgXcQ", BrowserUrlHelper.extractYouTubeId(share))
        assertEquals("dQw4w9WgXcQ", BrowserUrlHelper.extractYouTubeId(embed))
    }

    @Test
    fun testYouTubeQualityOptionsGeneration() {
        val options = BrowserUrlHelper.createYouTubeQualityOptions("dQw4w9WgXcQ", "Never Gonna Give You Up")
        assertFalse(options.isEmpty())
        assertEquals(4, options.size)
        assertTrue(options.any { it.resolution == "1080p" && it.isMaster })
        assertTrue(options.all { it.streamUrl.contains("dQw4w9WgXcQ") })
    }

    @Test
    fun testYouTubeIsRealMedia() {
        val ytUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertTrue("YouTube debe ser considerado medio real para detección", BrowserUrlHelper.isRealMedia(ytUrl))
    }

    @Test
    fun testDailymotionIdExtraction() {
        val dmUrl = "https://www.dailymotion.com/video/x8abcdef"
        val dmShort = "https://dai.ly/x8abcdef"

        assertEquals("x8abcdef", BrowserUrlHelper.extractDailymotionId(dmUrl))
        assertEquals("x8abcdef", BrowserUrlHelper.extractDailymotionId(dmShort))
    }

    @Test
    fun testDailymotionAndYouTubeAssetsAreWhitelisted() {
        val defaultList = AdBlockWhitelistManager.DEFAULT_WHITELIST
        assertTrue("dmcdn.net debe estar en DEFAULT_WHITELIST para proteger el buscador de Dailymotion", defaultList.contains("dmcdn.net"))
        assertTrue("ytimg.com debe estar en DEFAULT_WHITELIST para miniaturas de YouTube", defaultList.contains("ytimg.com"))
        assertTrue("googlevideo.com debe estar en DEFAULT_WHITELIST", defaultList.contains("googlevideo.com"))
    }

    @Test
    fun testKnownMediaHostStrictMatching() {
        assertTrue(AdBlocker.isKnownMediaHost("ok.ru"))
        assertTrue(AdBlocker.isKnownMediaHost("sub.streamwish.to"))
        assertTrue(AdBlocker.isKnownMediaHost("fastream.to"))
    }

    @Test
    fun testVerificationAndCaptchaProtection() {
        assertTrue(AdBlocker.isVerificationOrCaptcha("https://challenges.cloudflare.com/turnstile/v0/api.js"))
        assertTrue(AdBlocker.isVerificationOrCaptcha("challenges.cloudflare.com"))
        assertFalse(AdBlocker.isVerificationOrCaptcha("https://pelisplus.lat/movie/123"))
    }

    @Test
    fun testSearchEngineDetectionAndWhitelist() {
        assertTrue(AdBlocker.isSearchEngine("yandex.com"))
        assertTrue(AdBlocker.isSearchEngine("yandex.ru"))
        assertTrue(AdBlocker.isSearchEngine("ya.ru"))
        assertTrue(AdBlocker.isSearchEngine("google.com"))
        assertTrue(AdBlocker.isSearchEngine("duckduckgo.com"))
        assertFalse(AdBlocker.isSearchEngine("pelisplus.lat"))

        val defaultList = AdBlockWhitelistManager.DEFAULT_WHITELIST
        assertTrue(defaultList.contains("yandex.com"))
        assertTrue(defaultList.contains("yandex.ru"))
        assertTrue(defaultList.contains("ya.ru"))
    }

    @Test
    fun testDropCdnHeadersUseCanonEmbedAndOrigin() {
        val dropUrl = "https://ds4.dropcdn.io/hls2/01/00038/argalp6ab1fu_n/master.m3u8?t=123&s=abc"
        val fastUrl = "https://s40.fastream.to/hls2/01/00010/abcdefgh_n/master.m3u8"
        val page = "https://pelisplus.hair/peliculas/8536-mad-max-2.html"

        val (_, dropHeaders) = MediaPlatformResolver.prepareStreamForPlayback(dropUrl, page, emptyMap())
        val (_, fastHeaders) = MediaPlatformResolver.prepareStreamForPlayback(fastUrl, page, emptyMap())

        assertEquals("https://dr0pstream.com/e/argalp6ab1fu", dropHeaders["Referer"])
        assertEquals("https://fastream.to/embed-abcdefgh.html", fastHeaders["Referer"])
        assertEquals("https://dr0pstream.com", dropHeaders["Origin"])
        assertEquals("https://fastream.to", fastHeaders["Origin"])
    }

    @Test
    fun testFastreamExtractorStrictness() {
        val adapter = com.omnicast.sender.ui.browser.extractors.FastreamExtractorAdapter()
        val rumCandidate = com.omnicast.core.domain.models.StreamCandidate("https://dr0pstream.com/cdn-cgi/rum?")
        val embedCandidate = com.omnicast.core.domain.models.StreamCandidate("https://dr0pstream.com/e/loaxgshq39l6")
        val realHlsCandidate = com.omnicast.core.domain.models.StreamCandidate("https://dr0pstream.com/hls2/01/00038/loaxgshq39l6_n/master.m3u8?t=123")

        assertFalse("Debe rechazar telemetría Cloudflare RUM", adapter.canHandle(rumCandidate))
        assertFalse("Debe rechazar página web /e/", adapter.canHandle(embedCandidate))
        assertTrue("Debe aceptar el flujo HLS real", adapter.canHandle(realHlsCandidate))

        assertFalse("isRealMedia debe rechazar telemetría", BrowserUrlHelper.isRealMedia("https://dr0pstream.com/cdn-cgi/rum?"))
        assertFalse("isRealMedia debe rechazar página /e/", BrowserUrlHelper.isRealMedia("https://dr0pstream.com/e/loaxgshq39l6"))
        assertTrue("isRealMedia debe aceptar master.m3u8", BrowserUrlHelper.isRealMedia("https://dr0pstream.com/hls2/01/00038/loaxgshq39l6_n/master.m3u8?t=123"))
    }

    @Test
    fun testOkRuExtractorStrictness() {
        val adapter = com.omnicast.sender.ui.browser.extractors.OkRuSiteExtractorAdapter()
        val previewCandidate = com.omnicast.core.domain.models.StreamCandidate("https://mycdn.me/videopreview?id=123")
        val byteRangeCandidate = com.omnicast.core.domain.models.StreamCandidate("https://mycdn.me/video.mp4?bytes=0-1000")
        val trackerCandidate = com.omnicast.core.domain.models.StreamCandidate("https://top-fwz1.mail.ru/tracker?_=0.919;u=https://ok.ru/video/5771009198676")
        val showcaseCandidate = com.omnicast.core.domain.models.StreamCandidate("https://ok.ru/dk?st.cmd=anonymVideo&st.m=SHOWCASE")
        val realVideoCandidate = com.omnicast.core.domain.models.StreamCandidate("https://vd45.mycdn.me/?sig=abc&expires=999&type=4")

        assertFalse("Debe rechazar videopreview", adapter.canHandle(previewCandidate))
        assertFalse("Debe rechazar byte range preview", adapter.canHandle(byteRangeCandidate))
        assertFalse("Debe rechazar tracker de Mail.ru", adapter.canHandle(trackerCandidate))
        assertFalse("Debe rechazar showcase de OK.ru", adapter.canHandle(showcaseCandidate))
        assertTrue("Debe aceptar video real firmado de CDN", adapter.canHandle(realVideoCandidate))
    }

    @Test
    fun testUniversalHlsExtractorStrictness() {
        val adapter = com.omnicast.sender.ui.browser.extractors.UniversalHlsExtractorAdapter()
        val embedCandidate = com.omnicast.core.domain.models.StreamCandidate("https://morencius.com/embed/0qres6vduysj")
        val rumCandidate = com.omnicast.core.domain.models.StreamCandidate("https://morencius.com/cdn-cgi/rum?")
        val realHlsCandidate = com.omnicast.core.domain.models.StreamCandidate("https://morencius.com/stream/0qres6vduysj/master.m3u8")

        assertFalse("Debe rechazar iframe embed de Morencius", adapter.canHandle(embedCandidate))
        assertFalse("Debe rechazar telemetría RUM de Morencius", adapter.canHandle(rumCandidate))
        assertTrue("Debe aceptar master.m3u8 de Morencius", adapter.canHandle(realHlsCandidate))
    }

    @Test
    fun testTelemetryFilterAdapterCentralGatekeeper() {
        val filter = com.omnicast.sender.ui.browser.extractors.TelemetryFilterAdapter()

        assertTrue("Debe descartar /cdn-cgi/", filter.isTelemetryOrGarbage("https://site.com/cdn-cgi/rum?"))
        assertTrue("Debe descartar /embed/ sin extensión", filter.isTelemetryOrGarbage("https://morencius.com/embed/0qres6vduysj"))
        assertTrue("Debe descartar tracker", filter.isTelemetryOrGarbage("https://top-fwz1.mail.ru/tracker?_=123"))
        assertTrue("Debe descartar anonymVideo", filter.isTelemetryOrGarbage("https://ok.ru/dk?st.cmd=anonymVideo&st.m=SHOWCASE"))
        assertFalse("No debe descartar master.m3u8", filter.isTelemetryOrGarbage("https://morencius.com/stream/master.m3u8"))
        assertFalse("No debe descartar CDN firmada", filter.isTelemetryOrGarbage("https://vd45.mycdn.me/?sig=abc&type=4"))
    }
}
