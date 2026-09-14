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
    }

    @Test
    fun testDisconnectMessage() {
        val disconnect = SignalingMessage.Disconnect("Usuario detuvo la proyección")
        val encoded = json.encodeToString<SignalingMessage>(disconnect)
        val decoded = json.decodeFromString<SignalingMessage>(encoded)
        assertTrue(decoded is SignalingMessage.Disconnect)
    }
}
