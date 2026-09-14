package com.omnicast.core.domain

import com.omnicast.core.domain.models.StreamDetectionResult
import com.omnicast.core.domain.usecases.UrlStreamExtractor
import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias para el extractor de flujos de video y análisis de enlaces.
 */
class StreamExtractorTest {

    private val extractor = UrlStreamExtractor()

    @Test
    fun testHlsStreamDetection() {
        val input = "Mira el partido aquí: https://stream.sports.com/live/match_event.m3u8?token=abc123"
        val result = extractor.extractStream(input)

        assertTrue(result.isSuccess)
        assertEquals(StreamDetectionResult.Status.SUCCESS, result.status)
        assertEquals("HLS", result.format)
        assertEquals("https://stream.sports.com/live/match_event.m3u8?token=abc123", result.mediaUrl)
    }

    @Test
    fun testMp4StreamDetection() {
        val input = "Descarga o mira: https://media.example.com/video/sports_recap.mp4"
        val result = extractor.extractStream(input)

        assertTrue(result.isSuccess)
        assertEquals("MP4", result.format)
    }

    @Test
    fun testEmptyOrInvalidInputReturnsNoStream() {
        val resultNull = extractor.extractStream(null)
        assertFalse(resultNull.isSuccess)
        assertEquals(StreamDetectionResult.Status.NO_STREAM_DETECTED, resultNull.status)

        val resultText = extractor.extractStream("Hola amigos, viendo el partido de hoy en la app")
        assertFalse(resultText.isSuccess)
        assertEquals(StreamDetectionResult.Status.NO_STREAM_DETECTED, resultText.status)
    }

    @Test
    fun testWebStreamDetection() {
        val input = "https://www.youtube.com/watch?v=live_event_123"
        val result = extractor.extractStream(input)

        assertTrue(result.isSuccess)
        assertEquals("WEB_STREAM", result.format)
    }
}
