package com.omnicast.sender.ui.browser

import com.omnicast.sender.ui.browser.sentinel.WebPlayerDiagnosticSentinel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebPlayerDiagnosticsTest {

    @Test
    fun testHandleDiagnosticConsoleMessageCatchesErrors() {
        val page = "https://cineplus123.org/capitulo/1x3"
        // 1. Mensajes de error de etiqueta <video> en DOM
        val domErr = "OMNI_DOM_VIDEO_ERROR:{\"code\":4,\"message\":\"MEDIA_ERR_SRC_NOT_SUPPORTED\",\"src\":\"https://test.com/v.mp4\"}"
        assertTrue(WebPlayerDiagnosticSentinel.handleDiagnosticConsoleMessage(domErr, page))

        val domStalled = "OMNI_DOM_VIDEO_STALLED:{\"src\":\"https://test.com/v.mp4\"}"
        assertTrue(WebPlayerDiagnosticSentinel.handleDiagnosticConsoleMessage(domStalled, page))

        // 2. Logs de reproductores web conocidos
        assertTrue(WebPlayerDiagnosticSentinel.handleDiagnosticConsoleMessage("olplayer start loading", page))
        assertTrue(WebPlayerDiagnosticSentinel.handleDiagnosticConsoleMessage("Cant connect to server, try to disable VPN or AdBlock", page))
        assertTrue(WebPlayerDiagnosticSentinel.handleDiagnosticConsoleMessage("VAST adError", page))

        // 3. Mensajes comunes no relacionados deben retornar false
        assertFalse(WebPlayerDiagnosticSentinel.handleDiagnosticConsoleMessage("JQMIGRATE: Migrate is installed", page))
        assertFalse(WebPlayerDiagnosticSentinel.handleDiagnosticConsoleMessage("Google Tag Manager loaded", page))
    }
}
