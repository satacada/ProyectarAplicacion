package com.omnicast.core.domain

import com.omnicast.core.domain.models.MediaStream
import com.omnicast.core.domain.models.StreamCandidate
import com.omnicast.core.domain.ports.IMediaStreamExtractorPort
import com.omnicast.core.domain.ports.IStreamTelemetryFilterPort
import com.omnicast.core.domain.usecases.DetectMediaStreamUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DetectMediaStreamUseCaseTest {

    private val fakeTelemetryFilter = object : IStreamTelemetryFilterPort {
        override fun isTelemetryOrGarbage(url: String): Boolean {
            return url.contains("videoStat") || url.contains("beacon") || url.contains("analytics")
        }
    }

    private val fakeHlsExtractor = object : IMediaStreamExtractorPort {
        override val priority: Int = 10
        override fun canHandle(candidate: StreamCandidate): Boolean = candidate.url.contains(".m3u8")
        override fun extract(candidate: StreamCandidate): MediaStream {
            return MediaStream(url = candidate.url, format = "HLS", isMaster = candidate.url.contains("master"))
        }
    }

    private val fakeSiteExtractor = object : IMediaStreamExtractorPort {
        override val priority: Int = 20
        override fun canHandle(candidate: StreamCandidate): Boolean = candidate.url.contains("specialsite.com/video")
        override fun extract(candidate: StreamCandidate): MediaStream {
            return MediaStream(url = candidate.url, format = "SPECIAL_SITE", title = "Video Especial")
        }
    }

    @Test
    fun shouldDiscardTelemetryCallsEarly() {
        val useCase = DetectMediaStreamUseCase(fakeTelemetryFilter, listOf(fakeHlsExtractor, fakeSiteExtractor))
        val candidate = StreamCandidate(url = "https://specialsite.com/videoStatNew?id=123")
        val result = useCase.execute(candidate)
        assertNull(result)
    }

    @Test
    fun shouldSelectHigherPriorityExtractor() {
        val useCase = DetectMediaStreamUseCase(fakeTelemetryFilter, listOf(fakeHlsExtractor, fakeSiteExtractor))
        val candidate = StreamCandidate(url = "https://specialsite.com/video/stream.m3u8")
        val result = useCase.execute(candidate)
        assertNotNull(result)
        assertEquals("SPECIAL_SITE", result?.format)
    }

    @Test
    fun shouldFallbackToUniversalExtractorWhenSiteExtractorDoesNotMatch() {
        val useCase = DetectMediaStreamUseCase(fakeTelemetryFilter, listOf(fakeHlsExtractor, fakeSiteExtractor))
        val candidate = StreamCandidate(url = "https://cdn.example.com/live/master.m3u8")
        val result = useCase.execute(candidate)
        assertNotNull(result)
        assertEquals("HLS", result?.format)
        assertEquals(true, result?.isMaster)
    }
}
