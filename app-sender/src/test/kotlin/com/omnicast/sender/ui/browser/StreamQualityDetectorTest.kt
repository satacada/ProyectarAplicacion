package com.omnicast.sender.ui.browser

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import kotlin.concurrent.thread

class StreamQualityDetectorTest {

    private var serverSocket: ServerSocket? = null
    private var serverPort: Int = 0
    @Volatile private var isRunning = true

    @Before
    fun setUp() {
        serverSocket = ServerSocket(0)
        serverPort = serverSocket!!.localPort
        isRunning = true
    }

    @After
    fun tearDown() {
        isRunning = false
        runCatching { serverSocket?.close() }
    }

    @Test
    fun testProbeAndHealStreamPreservesCustomXHeadersOnHttp403Fallback() {
        thread {
            while (isRunning) {
                val client = try { serverSocket?.accept() } catch (_: Throwable) { null } ?: break
                thread {
                    try {
                        val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                        while (true) {
                            val line = reader.readLine()
                            if (line.isNullOrEmpty()) break
                        }
                        val out = client.getOutputStream()
                        out.write("HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                        out.flush()
                        client.close()
                    } catch (_: Throwable) {}
                }
            }
        }

        val url = "http://127.0.0.1:$serverPort/video.m3u8"
        val safeHeaders = mapOf(
            "Referer" to "https://test-host.example/stream",
            "X-Target-Player" to "CHROMIUM_ANTI_CORS",
            "X-Single-Use-Token" to "token-12345",
            "X-With-Credentials" to "true",
            "X-Base-Url-Policy" to "STRICT",
            "X-Force-Proxy" to "true"
        )

        val probe = StreamQualityDetector.probeAndHealStream(url, safeHeaders)
        val headers = probe.healedHeaders

        assertEquals("CHROMIUM_ANTI_CORS", headers["X-Target-Player"])
        assertEquals("token-12345", headers["X-Single-Use-Token"])
        assertEquals("true", headers["X-With-Credentials"])
        assertEquals("STRICT", headers["X-Base-Url-Policy"])
        assertEquals("true", headers["X-Force-Proxy"])
    }

    @Test
    fun testProbeAndHealStreamPreservesCustomXHeadersOnPlayableStream() {
        val m3u8 = "#EXTM3U\n#EXT-X-VERSION:3\n#EXTINF:10.0,\nseg1.ts\n#EXT-X-ENDLIST\n"
        thread {
            while (isRunning) {
                val client = try { serverSocket?.accept() } catch (_: Throwable) { null } ?: break
                thread {
                    try {
                        val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                        while (true) {
                            val line = reader.readLine()
                            if (line.isNullOrEmpty()) break
                        }
                        val bytes = m3u8.toByteArray()
                        val out = client.getOutputStream()
                        out.write("HTTP/1.1 200 OK\r\nContent-Type: application/vnd.apple.mpegurl\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        out.write(bytes)
                        out.flush()
                        client.close()
                    } catch (_: Throwable) {}
                }
            }
        }

        val url = "http://127.0.0.1:$serverPort/stream.m3u8"
        val safeHeaders = mapOf(
            "Referer" to "https://test-host.example/embed",
            "X-Target-Player" to "CHROMIUM_ANTI_CORS",
            "X-Single-Use-Token" to "secure-token",
            "X-Custom-Feature" to "enabled"
        )

        val probe = StreamQualityDetector.probeAndHealStream(url, safeHeaders)
        assertTrue(probe.isPlayable)
        val headers = probe.healedHeaders

        assertEquals("CHROMIUM_ANTI_CORS", headers["X-Target-Player"])
        assertEquals("secure-token", headers["X-Single-Use-Token"])
        assertEquals("enabled", headers["X-Custom-Feature"])
    }
}
