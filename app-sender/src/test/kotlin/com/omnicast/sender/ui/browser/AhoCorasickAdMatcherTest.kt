package com.omnicast.sender.ui.browser

import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias para la FASE 2:
 * 1. Motor de filtrado de anuncios y 0 falsos positivos en dominios oficiales (Leyes 4 y 6).
 * 2. Blindaje anti-secuestro de clic sobre reproductores de video (Mixdrop / StreamWish).
 */
class AhoCorasickAdMatcherTest {

    @Test
    fun testZeroFalsePositivesOnOfficialPlatforms() {
        val officialUrls = listOf(
            "https://rr1---sn-4g5ednks.googlevideo.com/videoplayback?expire=1730000000&itag=22",
            "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://www.dailymotion.com/cdn/manifest/video/x8abcdef.m3u8",
            "https://static1.dmcdn.net/neon/player/release/core.js",
            "https://ds4.dropcdn.io/hls2/01/00038/argalp6ab1fu_n/master.m3u8?t=123",
            "https://dr0pstream.com/e/argalp6ab1fu",
            "https://s40.fastream.to/hls2/01/00010/abcdefgh_n/master.m3u8?t=xyz",
            "https://vd45.mycdn.me/?sig=abc&expires=999&type=4",
            "https://vd736.okcdn.ru/?expires=1793204612101&type=4",
            "https://challenges.cloudflare.com/turnstile/v0/api.js"
        )

        for (url in officialUrls) {
            assertFalse("URL oficial jamás debe ser bloqueada por el motor de anuncios: $url", AdBlocker.isAd(url))
        }
    }

    @Test
    fun testAdNetworksAndTrackersAreBlocked() {
        val adUrls = listOf(
            "https://innodesdevering.com/ad/pop.js?id=123",
            "https://luugy.com/redirect?target=abc",
            "https://adsterra.com/tag.js",
            "https://propellerads.com/zone?id=456",
            "https://clickadu.com/banner.png",
            "https://googleads.g.doubleclick.net/pagead/ads?client=ca-pub-123",
            "https://cat-bolt.com/delivery/vast.xml",
            "https://imasdk.googleapis.com/js/sdkloader/ima3.js"
        )

        for (url in adUrls) {
            assertTrue("Red de anuncios conocida debe ser bloqueada: $url", AdBlocker.isAd(url))
        }
    }

    @Test
    fun testClickHijackProtectionOnMixdropAndVideoPlayers() {
        val mixdropPage = "https://mixdrop.co/e/abcdefgh"
        val googleRedirect = "https://www.google.com/?gws_rd=ssl"
        val adRedirect = "https://innodesdevering.com/click"
        val legitimateMixdropInternal = "https://mixdrop.co/e/abcdefgh?stream=1"
        val legitimateCdnMedia = "https://s40.fastream.to/embed-123.html"

        // 1. Clic sobre Play que intenta desviar a Google -> DEBE SER BLOQUEADO
        val blockedGoogle = WvcShieldSecurityEngine.shouldBlockClickHijack(
            hasGesture = true,
            targetUrl = googleRedirect,
            currentUrl = mixdropPage
        )
        assertTrue("El secuestro de clic hacia Google en Mixdrop debe ser bloqueado", blockedGoogle)

        // 2. Clic sobre Play que intenta desviar a red de anuncios -> DEBE SER BLOQUEADO
        val blockedAd = WvcShieldSecurityEngine.shouldBlockClickHijack(
            hasGesture = true,
            targetUrl = adRedirect,
            currentUrl = mixdropPage
        )
        assertTrue("El secuestro de clic hacia ad-network en Mixdrop debe ser bloqueado", blockedAd)

        // 3. Navegación legítima interna dentro del mismo dominio -> NO DEBE BLOQUEARSE
        val allowedInternal = WvcShieldSecurityEngine.shouldBlockClickHijack(
            hasGesture = true,
            targetUrl = legitimateMixdropInternal,
            currentUrl = mixdropPage
        )
        assertFalse("Navegación interna dentro de Mixdrop debe permitirse", allowedInternal)

        // 4. Navegación hacia otro host multimedia conocido -> NO DEBE BLOQUEARSE
        val allowedMedia = WvcShieldSecurityEngine.shouldBlockClickHijack(
            hasGesture = true,
            targetUrl = legitimateCdnMedia,
            currentUrl = mixdropPage
        )
        assertFalse("Navegación hacia host multimedia conocido debe permitirse", allowedMedia)
    }

    @Test
    fun testKnownMediaHostsIncludesMixdropVariants() {
        assertTrue(AdBlocker.isKnownMediaHost("mixdrop.co"))
        assertTrue(AdBlocker.isKnownMediaHost("mixdrop.to"))
        assertTrue(AdBlocker.isKnownMediaHost("mixdrop.ag"))
        assertTrue(AdBlocker.isKnownMediaHost("mixdrop.club"))
    }
}
