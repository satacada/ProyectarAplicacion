package com.omnicast.core.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias para la serialización y deserialización del protocolo de señalización.
 * Garantiza cero caídas en la comunicación P2P entre celular y TV Box.
 */
class SignalingProtocolTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testOfferSerializationAndDeserialization() {
        val offer = SignalingMessage.Offer(
            sdp = "v=0\r\no=alice 2890844526 2890844526 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n",
            width = 1920,
            height = 1080,
            fps = 60,
            bitrateKbps = 8000
        )

        val encoded = json.encodeToString<SignalingMessage>(offer)
        assertTrue(encoded.contains("\"type\":\"OFFER\""))
        assertTrue(encoded.contains("\"fps\":60"))

        val decoded = json.decodeFromString<SignalingMessage>(encoded)
        assertTrue(decoded is SignalingMessage.Offer)
        val decodedOffer = decoded as SignalingMessage.Offer
        assertEquals(1920, decodedOffer.width)
        assertEquals(1080, decodedOffer.height)
        assertEquals(60, decodedOffer.fps)
        assertTrue(decodedOffer.smoothBuffer)
    }

    @Test
    fun testAnswerSerializationAndDeserialization() {
        val answer = SignalingMessage.Answer(
            sdp = "v=0\r\no=bob 2890844526 2890844526 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n"
        )

        val encoded = json.encodeToString<SignalingMessage>(answer)
        assertTrue(encoded.contains("\"type\":\"ANSWER\""))

        val decoded = json.decodeFromString<SignalingMessage>(encoded)
        assertTrue(decoded is SignalingMessage.Answer)
        val decodedAnswer = decoded as SignalingMessage.Answer
        assertEquals(answer.sdp, decodedAnswer.sdp)
    }

    @Test
    fun testIceCandidateSerialization() {
        val ice = SignalingMessage.IceCandidate(
            sdpMid = "0",
            sdpMLineIndex = 0,
            candidate = "candidate:1 1 UDP 2130706431 192.168.0.4 8088 typ host"
        )

        val encoded = json.encodeToString<SignalingMessage>(ice)
        assertTrue(encoded.contains("\"type\":\"ICE_CANDIDATE\""))

        val decoded = json.decodeFromString<SignalingMessage>(encoded)
        assertTrue(decoded is SignalingMessage.IceCandidate)
        val decodedIce = decoded as SignalingMessage.IceCandidate
        assertEquals("0", decodedIce.sdpMid)
        assertEquals(0, decodedIce.sdpMLineIndex)
    }

    @Test
    fun testDirectStreamHandoffSerialization() {
        val handoff = SignalingMessage.DirectStreamHandoff(
            mediaUrl = "https://example.com/live/stream.m3u8",
            title = "Evento Deportivo en Vivo"
        )

        val encoded = json.encodeToString<SignalingMessage>(handoff)
        assertTrue(encoded.contains("\"type\":\"DIRECT_STREAM_HANDOFF\""))

        val decoded = json.decodeFromString<SignalingMessage>(encoded)
        assertTrue(decoded is SignalingMessage.DirectStreamHandoff)
        val decodedHandoff = decoded as SignalingMessage.DirectStreamHandoff
        assertEquals("https://example.com/live/stream.m3u8", decodedHandoff.mediaUrl)
        assertEquals("Evento Deportivo en Vivo", decodedHandoff.title)
        assertTrue(decodedHandoff.subtitles.isEmpty())
    }

    @Test
    fun testDirectStreamHandoffWithSubtitlesSerialization() {
        val subs = listOf(
            SignalingMessage.SubtitleTrack(url = "https://example.com/sub_es.vtt", lang = "es"),
            SignalingMessage.SubtitleTrack(url = "https://example.com/sub_en.vtt", lang = "en")
        )
        val handoff = SignalingMessage.DirectStreamHandoff(
            mediaUrl = "https://example.com/movie/master.m3u8",
            title = "Película con Subtítulos",
            subtitles = subs
        )

        val encoded = json.encodeToString<SignalingMessage>(handoff)
        assertTrue(encoded.contains("sub_es.vtt"))

        val decoded = json.decodeFromString<SignalingMessage>(encoded) as SignalingMessage.DirectStreamHandoff
        assertEquals(2, decoded.subtitles.size)
        assertEquals("es", decoded.subtitles[0].lang)
    }

    @Test
    fun testDisconnectMessage() {
        val disconnect = SignalingMessage.Disconnect("Usuario detuvo la proyección")
        val encoded = json.encodeToString<SignalingMessage>(disconnect)
        val decoded = json.decodeFromString<SignalingMessage>(encoded)
        assertTrue(decoded is SignalingMessage.Disconnect)
    }

    @Test
    fun testImageTransformSerialization() {
        val transform = SignalingMessage.ImageTransform(
            scale = 1.5f,
            panX = 20.0f,
            panY = -10.0f,
            rotation = 90.0f
        )
        val encoded = json.encodeToString<SignalingMessage>(transform)
        assertTrue(encoded.contains("\"type\":\"IMAGE_TRANSFORM\""))
        assertTrue(encoded.contains("\"rotation\":90.0"))

        val decoded = json.decodeFromString<SignalingMessage>(encoded)
        assertTrue(decoded is SignalingMessage.ImageTransform)
        val decodedTransform = decoded as SignalingMessage.ImageTransform
        assertEquals(1.5f, decodedTransform.scale, 0.001f)
        assertEquals(20.0f, decodedTransform.panX, 0.001f)
        assertEquals(-10.0f, decodedTransform.panY, 0.001f)
        assertEquals(90.0f, decodedTransform.rotation, 0.001f)
    }

    @Test
    fun testRecordTelemetrySerializationAndDefaults() {
        val telemetry = SignalingMessage.RecordTelemetry(
            isRecording = true,
            isMovieDownload = false,
            canRecord = true
        )
        val encoded = json.encodeToString<SignalingMessage>(telemetry)
        assertTrue(encoded.contains("\"type\":\"RECORD_TELEMETRY\""))
        assertTrue(encoded.contains("\"isRecording\":true"))

        val decoded = json.decodeFromString<SignalingMessage>(encoded)
        assertTrue(decoded is SignalingMessage.RecordTelemetry)
        val decodedTelemetry = decoded as SignalingMessage.RecordTelemetry
        assertTrue(decodedTelemetry.isRecording)
        assertEquals(0L, decodedTelemetry.bytesDownloaded)
        assertEquals(0L, decodedTelemetry.totalSizeBytes)
        assertFalse(decodedTelemetry.isResumable)
        assertNull(decodedTelemetry.errorNotice)
    }

    @Test
    fun testRecordTelemetryEnrichedSerialization() {
        val telemetry = SignalingMessage.RecordTelemetry(
            isRecording = true,
            isMovieDownload = true,
            canRecord = true,
            bytesDownloaded = 524288000L,
            totalSizeBytes = 1048576000L,
            isResumable = true,
            errorNotice = "Reanudando tras corte de red"
        )
        val encoded = json.encodeToString<SignalingMessage>(telemetry)
        val decoded = json.decodeFromString<SignalingMessage>(encoded) as SignalingMessage.RecordTelemetry
        assertEquals(524288000L, decoded.bytesDownloaded)
        assertEquals(1048576000L, decoded.totalSizeBytes)
        assertTrue(decoded.isResumable)
        assertEquals("Reanudando tras corte de red", decoded.errorNotice)
    }

    @Test
    fun testResilienceAgainstUnknownAndExtraJsonKeys() {
        val offerWithExtras = """
            {
                "type": "OFFER",
                "sdp": "v=0\r\no=alice 123 456\r\n",
                "width": 1920,
                "height": 1080,
                "fps": 60,
                "bitrateKbps": 8000,
                "smoothBuffer": true,
                "unknown_experimental_feature": true,
                "future_nested_data": {
                    "codec_profile": "high",
                    "latency_tier": 1
                },
                "arbitrary_tags": ["tv", "hdr", 42]
            }
        """.trimIndent()

        val decodedOffer = json.decodeFromString<SignalingMessage>(offerWithExtras)
        assertTrue(decodedOffer is SignalingMessage.Offer)
        val offer = decodedOffer as SignalingMessage.Offer
        assertEquals(1920, offer.width)
        assertEquals(1080, offer.height)

        val handoffWithExtras = """
            {
                "type": "DIRECT_STREAM_HANDOFF",
                "mediaUrl": "https://example.com/stream.m3u8",
                "format": "HLS",
                "title": "Partido en Vivo",
                "extraTokenField": "token_12345",
                "analyticsPayload": {"bitrate": 5000}
            }
        """.trimIndent()
        val decodedHandoff = json.decodeFromString<SignalingMessage>(handoffWithExtras)
        assertTrue(decodedHandoff is SignalingMessage.DirectStreamHandoff)
        assertEquals("https://example.com/stream.m3u8", (decodedHandoff as SignalingMessage.DirectStreamHandoff).mediaUrl)
    }

    @Test
    fun testSerializationWithEmptyStrings() {
        val emptyOffer = SignalingMessage.Offer(sdp = "", width = 0, height = 0, fps = 0, bitrateKbps = 0)
        val encodedOffer = json.encodeToString<SignalingMessage>(emptyOffer)
        val decodedOffer = json.decodeFromString<SignalingMessage>(encodedOffer) as SignalingMessage.Offer
        assertEquals("", decodedOffer.sdp)

        val emptyHandoff = SignalingMessage.DirectStreamHandoff(mediaUrl = "", title = "")
        val encodedHandoff = json.encodeToString<SignalingMessage>(emptyHandoff)
        val decodedHandoff = json.decodeFromString<SignalingMessage>(encodedHandoff) as SignalingMessage.DirectStreamHandoff
        assertEquals("", decodedHandoff.mediaUrl)
        assertEquals("", decodedHandoff.title)

        val emptyDisconnect = SignalingMessage.Disconnect(reason = "")
        val decodedDisconnect = json.decodeFromString<SignalingMessage>(json.encodeToString<SignalingMessage>(emptyDisconnect))
        assertEquals("", (decodedDisconnect as SignalingMessage.Disconnect).reason)
    }

    @Test
    fun testSerializationWithExtremeNumericValues() {
        val extremeOffer = SignalingMessage.Offer(
            sdp = "test",
            width = Int.MAX_VALUE,
            height = Int.MIN_VALUE,
            fps = Int.MAX_VALUE,
            bitrateKbps = Int.MAX_VALUE
        )
        val decodedOffer = json.decodeFromString<SignalingMessage>(json.encodeToString<SignalingMessage>(extremeOffer)) as SignalingMessage.Offer
        assertEquals(Int.MAX_VALUE, decodedOffer.width)
        assertEquals(Int.MIN_VALUE, decodedOffer.height)
        assertEquals(Int.MAX_VALUE, decodedOffer.fps)

        val extremeTelemetry = SignalingMessage.RecordTelemetry(
            freeBytes = Long.MAX_VALUE,
            usableBytes = Long.MIN_VALUE,
            progressPct = Int.MAX_VALUE
        )
        val decodedTelemetry = json.decodeFromString<SignalingMessage>(json.encodeToString<SignalingMessage>(extremeTelemetry)) as SignalingMessage.RecordTelemetry
        assertEquals(Long.MAX_VALUE, decodedTelemetry.freeBytes)
        assertEquals(Long.MIN_VALUE, decodedTelemetry.usableBytes)
        assertEquals(Int.MAX_VALUE, decodedTelemetry.progressPct)

        val ping = SignalingMessage.Ping(clientTimestamp = Long.MAX_VALUE)
        val pong = SignalingMessage.Pong(serverTimestamp = Long.MIN_VALUE)
        assertEquals(Long.MAX_VALUE, (json.decodeFromString<SignalingMessage>(json.encodeToString<SignalingMessage>(ping)) as SignalingMessage.Ping).clientTimestamp)
        assertEquals(Long.MIN_VALUE, (json.decodeFromString<SignalingMessage>(json.encodeToString<SignalingMessage>(pong)) as SignalingMessage.Pong).serverTimestamp)
    }
}

