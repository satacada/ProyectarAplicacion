package com.omnicast.core.domain.usecases

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamManifestProbeTest {

    @Test
    fun `buildHeaderPermutations generates 4 ordered permutations without hardcoded domains`() {
        val streamUrl = "https://cdn-node12.example.org/hls2/01/master.m3u8"
        val initialHeaders = mapOf(
            "Referer" to "https://player-iframe.example.net/embed/xyz",
            "User-Agent" to "CustomUA/1.0"
        )
        val pageUrl = "https://pelismart.example.com/pelicula-test"

        val permutations = StreamManifestProbe.buildHeaderPermutations(streamUrl, initialHeaders, pageUrl)

        assertEquals(4, permutations.size)
        assertEquals("ORIGINAL", permutations[0].first)
        assertEquals("https://player-iframe.example.net/embed/xyz", permutations[0].second["Referer"])
        assertEquals("https://player-iframe.example.net", permutations[0].second["Origin"])

        assertEquals("PAGE_URL", permutations[1].first)
        assertEquals("https://pelismart.example.com/pelicula-test", permutations[1].second["Referer"])
        assertEquals("https://pelismart.example.com", permutations[1].second["Origin"])

        assertEquals("SELF_CDN", permutations[2].first)
        assertEquals("https://cdn-node12.example.org/", permutations[2].second["Referer"])
        assertEquals("https://cdn-node12.example.org", permutations[2].second["Origin"])

        assertEquals("CLEAN", permutations[3].first)
        assertFalse(permutations[3].second.containsKey("Referer"))
        assertFalse(permutations[3].second.containsKey("Origin"))
    }

    @Test
    fun `probeAndHeal rejects raw YouTube watch webpage URLs`() {
        val result = StreamManifestProbe.probeAndHeal(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            emptyMap(),
            "https://pelismart.com/test"
        )
        assertFalse(result.isPlayable)
        assertEquals(415, result.httpCode)
    }

    @Test
    fun `probeAndHeal rejects blank URLs immediately`() {
        val result = StreamManifestProbe.probeAndHeal("", emptyMap())
        assertFalse(result.isPlayable)
        assertTrue(result.resolvedUrl.isEmpty())
    }
}
