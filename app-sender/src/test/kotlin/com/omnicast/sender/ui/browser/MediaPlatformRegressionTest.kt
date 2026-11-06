package com.omnicast.sender.ui.browser

import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas de regresión para garantizar el soporte inamovible de YouTube, Dailymotion y adaptadores modulares.
 * LEY INVIOLABLE: Ningún commit o refactorización puede romper la captura o la interfaz de estas plataformas.
 */
class MediaPlatformRegressionTest {

    @Test
    fun testYouTubeIdExtraction() {
        assertEquals("dQw4w9WgXcQ", BrowserUrlHelper.extractYouTubeId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", BrowserUrlHelper.extractYouTubeId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", BrowserUrlHelper.extractYouTubeId("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", BrowserUrlHelper.extractYouTubeId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
    }

    @Test
    fun testYouTubeChannelLiveExtraction() {
        val embedLive = "https://www.youtube.com/embed/live_stream?channel=UCvJJ_dzjViJCoLf5uKUTwoA&autoplay=1"
        val channelUrl = "https://www.youtube.com/channel/UCvJJ_dzjViJCoLf5uKUTwoA"
        assertEquals("live_stream?channel=UCvJJ_dzjViJCoLf5uKUTwoA", BrowserUrlHelper.extractYouTubeId(embedLive))
        assertEquals("live_stream?channel=UCvJJ_dzjViJCoLf5uKUTwoA", BrowserUrlHelper.extractYouTubeId(channelUrl))

        val options = BrowserUrlHelper.createYouTubeQualityOptions("live_stream?channel=UCvJJ_dzjViJCoLf5uKUTwoA", "CNBC En Vivo")
        assertEquals(1, options.size)
        assertTrue(options[0].isLive)
        assertEquals("LIVE", options[0].resolution)
        assertEquals("live_stream?channel=UCvJJ_dzjViJCoLf5uKUTwoA", options[0].healedHeaders["X-YouTube-Id"])
    }

    @Test
    fun testStreamIntelligenceEnginePreservesCustomHeadersAndStripsHopByHop() {
        val raw = mapOf(
            "Connection" to "keep-alive", "Keep-Alive" to "timeout=5",
            "X-Target-Player" to "CHROMIUM_ANTI_CORS", "X-Force-Proxy" to "true",
            "Sec-Fetch-Dest" to "video", "Referer" to "https://cxtv.com.br/cnbc"
        )
        val resolved = StreamIntelligenceEngine.resolveDynamicHeaders("https://cdn.example.com/master.m3u8", "https://cxtv.com.br", raw)
        assertFalse(resolved.containsKey("Connection"))
        assertFalse(resolved.containsKey("Keep-Alive"))
        assertEquals("CHROMIUM_ANTI_CORS", resolved["X-Target-Player"])
        assertEquals("true", resolved["X-Force-Proxy"])
        assertEquals("video", resolved["Sec-Fetch-Dest"])
        assertEquals("https://cxtv.com.br/cnbc", resolved["Referer"])
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
        assertEquals("x8abcdef", BrowserUrlHelper.extractDailymotionId("https://www.dailymotion.com/video/x8abcdef"))
        assertEquals("x8abcdef", BrowserUrlHelper.extractDailymotionId("https://dai.ly/x8abcdef"))
    }

    @Test
    fun testDailymotionAndYouTubeAssetsAreWhitelisted() {
        val defaultList = AdBlockWhitelistManager.DEFAULT_WHITELIST
        assertTrue(defaultList.contains("dmcdn.net"))
        assertTrue(defaultList.contains("ytimg.com"))
        assertTrue(defaultList.contains("googlevideo.com"))
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
        listOf("yandex.com", "yandex.ru", "ya.ru", "google.com", "duckduckgo.com").forEach {
            assertTrue(AdBlocker.isSearchEngine(it))
        }
        assertFalse(AdBlocker.isSearchEngine("pelisplus.lat"))
        val defaultList = AdBlockWhitelistManager.DEFAULT_WHITELIST
        listOf("yandex.com", "yandex.ru", "ya.ru").forEach { assertTrue(defaultList.contains(it)) }
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
        assertFalse("isRealMedia debe rechazar librería hls.js", BrowserUrlHelper.isRealMedia("https://cdn.jsdelivr.net/npm/hls.js@1.6.16"))
        assertTrue("isRealMedia debe aceptar master.m3u8", BrowserUrlHelper.isRealMedia("https://dr0pstream.com/hls2/01/00038/loaxgshq39l6_n/master.m3u8?t=123"))
    }

    @Test
    fun testOkRuExtractorStrictness() {
        val adapter = com.omnicast.sender.ui.browser.extractors.OkRuSiteExtractorAdapter()
        assertFalse(adapter.canHandle(com.omnicast.core.domain.models.StreamCandidate("https://mycdn.me/videopreview?id=123")))
        assertFalse(adapter.canHandle(com.omnicast.core.domain.models.StreamCandidate("https://mycdn.me/video.mp4?bytes=0-1000")))
        assertFalse(adapter.canHandle(com.omnicast.core.domain.models.StreamCandidate("https://top-fwz1.mail.ru/tracker?_=0.919")))
        assertFalse(adapter.canHandle(com.omnicast.core.domain.models.StreamCandidate("https://ok.ru/dk?st.cmd=anonymVideo&st.m=SHOWCASE")))
        assertTrue(adapter.canHandle(com.omnicast.core.domain.models.StreamCandidate("https://vd45.mycdn.me/?sig=abc&expires=999&type=4")))
    }

    @Test
    fun testUniversalHlsExtractorStrictness() {
        val adapter = com.omnicast.sender.ui.browser.extractors.UniversalHlsExtractorAdapter()
        assertFalse(adapter.canHandle(com.omnicast.core.domain.models.StreamCandidate("https://morencius.com/embed/0qres6vduysj")))
        assertFalse(adapter.canHandle(com.omnicast.core.domain.models.StreamCandidate("https://morencius.com/cdn-cgi/rum?")))
        assertTrue(adapter.canHandle(com.omnicast.core.domain.models.StreamCandidate("https://morencius.com/stream/0qres6vduysj/master.m3u8")))
    }

    @Test
    fun testTelemetryFilterAdapterCentralGatekeeper() {
        val filter = com.omnicast.sender.ui.browser.extractors.TelemetryFilterAdapter()
        assertTrue(filter.isTelemetryOrGarbage("https://site.com/cdn-cgi/rum?"))
        assertTrue(filter.isTelemetryOrGarbage("https://morencius.com/embed/0qres6vduysj"))
        assertTrue(filter.isTelemetryOrGarbage("https://top-fwz1.mail.ru/tracker?_=123"))
        assertTrue(filter.isTelemetryOrGarbage("https://ok.ru/dk?st.cmd=anonymVideo&st.m=SHOWCASE"))
        assertFalse(filter.isTelemetryOrGarbage("https://morencius.com/stream/master.m3u8"))
        assertFalse(filter.isTelemetryOrGarbage("https://vd45.mycdn.me/?sig=abc&type=4"))
    }

    @Test
    fun testOkRuMultiQualitySynthesis() = kotlinx.coroutines.runBlocking {
        val okUrl = "https://vd736.okcdn.ru/?expires=1793204612101&type=4&sig=22L2HBBdWIk"
        val options = StreamQualityDetector.detectQualities(okUrl, mapOf("Referer" to "https://ok.ru/"), "Caligula")
        assertEquals(5, options.size)
        assertTrue(options.any { it.resolution == "Adaptive" && it.isMaster })
        assertTrue(options.any { it.resolution == "1080p" && it.streamUrl.contains("type=5") })
        assertTrue(options.any { it.resolution == "720p" && it.streamUrl.contains("type=4") })
        assertTrue(options.any { it.resolution == "480p" && it.streamUrl.contains("type=3") })
        assertTrue(options.any { it.resolution == "360p" && it.streamUrl.contains("type=2") })
        assertTrue(options.all { it.groupKey == okUrl })
        assertTrue(options.all { it.healedHeaders["Referer"] == "https://ok.ru/" })
    }

    @Test
    fun testInIframeBannerAdBlocking() {
        assertTrue(AdBlocker.isVideoAd("https://domain.com/adsterra/banner.js"))
        assertTrue(AdBlocker.isVideoAd("https://domain.com/propeller/pop.js"))
        assertTrue(AdBlocker.isVideoAd("https://tnmr.org/assets/player-banner.png"))
        assertTrue(AdBlocker.isVideoAd("https://site.com/banner-ads/300x250.jpg"))
    }

    @Test
    fun testDoodstreamExtractorAdapter() {
        val adapter = com.omnicast.sender.ui.browser.extractors.DoodstreamExtractorAdapter()
        val cdnCandidate = com.omnicast.core.domain.models.StreamCandidate(
            url = "https://m12.doodcdn.co/pass/xyz123/video.mp4?token=abc-123&expiry=1800000000",
            pageUrl = "https://doopstream.com/e/abc123xyz", pageTitle = "Spider-Man"
        )
        assertTrue(adapter.canHandle(cdnCandidate))
        val extracted = adapter.extract(cdnCandidate)
        assertEquals("MP4", extracted.format)
        assertEquals("Spider-Man", extracted.title)
        assertEquals("https://doopstream.com/", extracted.headers["Referer"])
        assertEquals("https://doopstream.com", extracted.headers["Origin"])

        assertFalse(adapter.canHandle(com.omnicast.core.domain.models.StreamCandidate("https://doopstream.com/e/abc123xyz", "https://pelisplus.hair/pelicula")))
        assertFalse(adapter.canHandle(com.omnicast.core.domain.models.StreamCandidate("https://playmogo.com/dood?op=watch&hash=285", "https://cinecalidad.ro/")))
        assertFalse(adapter.canHandle(com.omnicast.core.domain.models.StreamCandidate("https://playmogo.com/pass_md5/285-token", "https://doopstream.com/e/abc")))

        val playmogoCandidate = com.omnicast.core.domain.models.StreamCandidate(
            url = "https://playmogo.com/video~123abc456?token=secret123&expiry=1800000000",
            pageUrl = "https://playmogo.com/e/6jsygz89bckajy0ylm2e9837", pageTitle = "Película Playmogo"
        )
        assertTrue(adapter.canHandle(playmogoCandidate))
        assertEquals("https://playmogo.com/", adapter.extract(playmogoCandidate).headers["Referer"])
    }

    @Test
    fun testLuluStreamExtractorAdapter() {
        val adapter = com.omnicast.sender.ui.browser.extractors.LuluStreamExtractorAdapter()
        val hlsCandidate = com.omnicast.core.domain.models.StreamCandidate(
            url = "https://s4.luluvdo.com/hls/01/00123/master.m3u8?token=xyz",
            pageUrl = "https://luluvdo.com/e/abc123xyz",
            pageTitle = "Película Lulu"
        )
        assertTrue("Debe manejar stream HLS de LuluStream", adapter.canHandle(hlsCandidate))
        val extracted = adapter.extract(hlsCandidate)
        assertEquals("HLS", extracted.format)
        assertEquals("LuluStream HD", extracted.qualityLabel)
        assertEquals("https://luluvdo.com/", extracted.headers["Referer"])
        assertEquals("https://luluvdo.com", extracted.headers["Origin"])
        assertEquals(UA_MOBILE, extracted.headers["User-Agent"])
        assertEquals("cors", extracted.headers["Sec-Fetch-Mode"])

        val iframeCandidate = com.omnicast.core.domain.models.StreamCandidate(
            url = "https://luluvdo.com/e/abc123xyz",
            pageUrl = "https://pelisplus.lat/movie"
        )
        assertFalse("Debe descartar iframe HTML de Lulu", adapter.canHandle(iframeCandidate))
    }

    @Test
    fun testStreamwishExtractorAdapter() {
        val adapter = com.omnicast.sender.ui.browser.extractors.StreamwishExtractorAdapter()
        val hlsCandidate = com.omnicast.core.domain.models.StreamCandidate(
            url = "https://cdn.streamwish.to/hls/02/00456/index.m3u8",
            pageUrl = "https://streamwish.to/e/def456",
            pageTitle = "Video Streamwish"
        )
        assertTrue("Debe manejar stream HLS de Streamwish", adapter.canHandle(hlsCandidate))
        val extracted = adapter.extract(hlsCandidate)
        assertEquals("HLS", extracted.format)
        assertEquals("Streamwish HD", extracted.qualityLabel)
        assertEquals("https://streamwish.to/", extracted.headers["Referer"])
        assertEquals("https://streamwish.to", extracted.headers["Origin"])
        assertEquals(UA_MOBILE, extracted.headers["User-Agent"])
        assertEquals("cors", extracted.headers["Sec-Fetch-Mode"])

        val iframeCandidate = com.omnicast.core.domain.models.StreamCandidate(
            url = "https://streamwish.to/e/def456",
            pageUrl = "https://pelisplus.lat/movie"
        )
        assertFalse("Debe descartar iframe HTML de Streamwish", adapter.canHandle(iframeCandidate))
    }

    @Test
    fun testBuildSafeHeadersAdoptsDynamicHeaders() {
        val streamUrl = "https://t9lr3d.cfglobalcdn.com/silverlight/secip/123/hls-vod/flv/videos/test.mp4.m3u8"
        val pageUrl = "https://cineplus123.org/capitulo/1x1"
        val dynamicHeaders = mapOf("Referer" to "https://netu.ac/", "Origin" to "https://netu.ac")
        val headers = BrowserUrlHelper.buildSafeHeaders(streamUrl, pageUrl, "MobileUA", dynamicHeaders)
        assertEquals("https://netu.ac/", headers["Referer"])
        assertEquals("https://netu.ac", headers["Origin"])
    }

    @Test
    fun testAdGatewayIsolationAndValidPlayerPreservation() {
        val curPage = "https://cineplus123.org/capitulo/1x3"
        assertFalse(WvcShieldSecurityEngine.shouldBlockClickHijack(hasGesture = true, "https://netu.ac/e/KCF3y774chHk", curPage))
        assertFalse(WvcShieldSecurityEngine.shouldBlockClickHijack(hasGesture = true, "https://streamwish.to/e/abc123xyz", curPage))
        assertFalse(WvcShieldSecurityEngine.shouldBlockClickHijack(hasGesture = true, "https://cineplus123.org/capitulo/1x4", curPage))

        assertTrue(WvcShieldSecurityEngine.shouldBlockClickHijack(hasGesture = true, "https://netu.ac/out.php?noads=0", curPage))
        assertTrue(WvcShieldSecurityEngine.shouldBlockClickHijack(hasGesture = true, "https://ams.re3r.com/impcs/ad123", curPage))
        assertTrue(WvcShieldSecurityEngine.shouldBlockDelayedRedirect(hasGesture = false, "https://netu.ac/out.php", curPage))
    }
}
