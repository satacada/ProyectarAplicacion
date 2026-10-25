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
}
