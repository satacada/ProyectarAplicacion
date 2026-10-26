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
    fun testSameSiteDetection() {
        assertTrue(AdBlocker.isSameSite("pelisplus.lat", "ww3.pelisplus.lat"))
        assertTrue(AdBlocker.isSameSite("www.ok.ru", "ok.ru"))
        assertTrue(AdBlocker.isSameSite("808ball.com", "www.808ball16.com"))
        assertTrue(AdBlocker.isSameSite("pelisplus.lat", "pelisplus2.lat"))
        assertFalse(AdBlocker.isSameSite("pelisplus.lat", "adnetwork.com"))
        assertFalse(AdBlocker.isSameSite("reypelis.net", "bet365.com"))
        assertTrue(AdBlocker.isMediaOrStreamingHost("www.808ball16.com"))
        assertTrue(AdBlocker.isMediaOrStreamingHost("thestreameast.to"))
        assertFalse(AdBlocker.isMediaOrStreamingHost("adnetwork.com"))
    }

    @Test
    fun testKnownMediaHostStrictMatching() {
        assertTrue(AdBlocker.isKnownMediaHost("ok.ru"))
        assertTrue(AdBlocker.isKnownMediaHost("sub.streamwish.to"))
        assertTrue(AdBlocker.isKnownMediaHost("fastream.to"))
        assertFalse(AdBlocker.isKnownMediaHost("fake-ads-cdn.com"))
        assertFalse(AdBlocker.isKnownMediaHost("adstreamer.xyz"))
        assertFalse(AdBlocker.isKnownMediaHost("player-bonus.net"))
    }

    @Test
    fun testVerificationAndCaptchaProtection() {
        assertTrue(AdBlocker.isVerificationOrCaptcha("https://challenges.cloudflare.com/turnstile/v0/api.js"))
        assertTrue(AdBlocker.isVerificationOrCaptcha("https://pelicinehd.com/cdn-cgi/challenge-platform/h/b/orchestrate/chl_page/v1"))
        assertTrue(AdBlocker.isVerificationOrCaptcha("https://pelicinehd.com/?__cf_chl_tk=12345"))
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
    fun testTrailerRelegationOnMoviePages() {
        val ytOpt = StreamQualityOption("Tráiler", "1080p", "Original", "https://www.youtube.com/watch?v=bwcADuJZDNA", isMaster = true)
        val movieOpt = StreamQualityOption("Película", "Auto", "Adaptativa", "https://ds4.dropcdn.io/hls2/01/00038/argalp6ab1fu_n/master.m3u8?t=123&s=abc", isMaster = true)
        val moviePage = "https://pelisplus.hair/peliculas/8536-mad-max-2-el-guerrero-de-la-carretera-ver-online-hd.html"

        val ytProb = StreamQualityDetector.calculateMovieProbability(ytOpt, moviePage)
        val movieProb = StreamQualityDetector.calculateMovieProbability(movieOpt, moviePage)

        assertEquals("El tráiler de YouTube en página de películas debe tener probabilidad baja (10)", 10, ytProb)
        assertTrue("La película real debe tener mayor probabilidad que el tráiler", movieProb > ytProb)
    }

    @Test
    fun testDropCdnHeadersUseDesktopUaAndOrigin() {
        val dropUrl = "https://ds4.dropcdn.io/hls2/01/00038/argalp6ab1fu_n/master.m3u8?t=123&s=abc"
        val fastUrl = "https://s40.fastream.to/hls2/01/00010/abcdefgh_n/master.m3u8"
        val page = "https://pelisplus.hair/peliculas/8536-mad-max-2.html"

        val (_, dropHeaders) = MediaPlatformResolver.prepareStreamForPlayback(dropUrl, page, emptyMap())
        val (_, fastHeaders) = MediaPlatformResolver.prepareStreamForPlayback(fastUrl, page, emptyMap())

        assertEquals(MediaPlatformResolver.UA_FASTREAM, dropHeaders["User-Agent"])
        assertEquals(MediaPlatformResolver.UA_FASTREAM, fastHeaders["User-Agent"])
        assertEquals("https://dr0pstream.com/e/argalp6ab1fu", dropHeaders["Referer"])
        assertEquals("https://fastream.to/embed-abcdefgh.html", fastHeaders["Referer"])
        assertEquals("https://dr0pstream.com", dropHeaders["Origin"])
        assertEquals("https://fastream.to", fastHeaders["Origin"])
    }
}
