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

        assertEquals(5, permutations.size)
        assertEquals("ORIGINAL", permutations[0].first)
        assertEquals("https://player-iframe.example.net/embed/xyz", permutations[0].second["Referer"])
        assertEquals("https://player-iframe.example.net", permutations[0].second["Origin"])

        assertEquals("PAGE_URL", permutations[1].first)
        assertEquals("https://pelismart.example.com/pelicula-test", permutations[1].second["Referer"])
        assertEquals("https://pelismart.example.com", permutations[1].second["Origin"])

        assertEquals("SELF_CDN", permutations[2].first)
        assertEquals("https://cdn-node12.example.org/", permutations[2].second["Referer"])
        assertEquals("https://cdn-node12.example.org", permutations[2].second["Origin"])

        assertEquals("REFERER_ONLY", permutations[3].first)
        assertEquals("https://player-iframe.example.net/embed/xyz", permutations[3].second["Referer"])

        assertEquals("CLEAN", permutations[4].first)
        assertFalse(permutations[4].second.containsKey("Referer"))
        assertFalse(permutations[4].second.containsKey("Origin"))
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

    @Test
    fun `buildHeaderPermutations prioritizes EMBED_CLEAN and EMBED_HOST for DropCDN`() {
        val streamUrl = "https://ds4.dropcdn.io/hls2/01/00038/loaxg3kae97x_n/master.m3u8?t=xyz"
        val permutations = StreamManifestProbe.buildHeaderPermutations(streamUrl, emptyMap(), "https://pelisplus.hair/pelicula/test")
        val first = permutations[0]
        val second = permutations[1]
        assertEquals("EMBED_CLEAN", first.first)
        assertEquals("https://dr0pstream.com/e/loaxg3kae97x", first.second["Referer"])
        assertFalse(first.second.containsKey("Origin"))
        assertFalse(first.second.containsKey("Cookie"))

        assertEquals("EMBED_HOST", second.first)
        assertEquals("https://dr0pstream.com/e/loaxg3kae97x", second.second["Referer"])
        assertEquals("https://dr0pstream.com", second.second["Origin"])
    }
}
