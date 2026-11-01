package com.omnicast.sender.ui.browser

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Pruebas unitarias de desempaquetado Dean Edwards (FASE 1).
 * Verifica la equivalencia byte a byte y la resiliencia del desempaquetador.
 */
class DeanEdwardsUnpackerTest {

    @Test
    fun testUnpackDeanEdwardsByteToByteEquivalence() {
        val packedFile = File("../scratch/packed.js").takeIf { it.exists() }
            ?: File("scratch/packed.js").takeIf { it.exists() }
            ?: File("../../scratch/packed.js")
        val expectedFile = File("../scratch/unpacked.js").takeIf { it.exists() }
            ?: File("scratch/unpacked.js").takeIf { it.exists() }
            ?: File("../../scratch/unpacked.js")

        assertTrue("scratch/packed.js debe existir para la prueba de equivalencia", packedFile.exists())
        assertTrue("scratch/unpacked.js debe existir para la prueba de equivalencia", expectedFile.exists())

        val packed = packedFile.readText()
        val expected = expectedFile.readText()

        val unpacked = MediaPlatformResolver.unpackDeanEdwards(packed)
        assertNotNull("El desempaquetado no debe ser nulo", unpacked)
        assertEquals("El resultado desempaquetado debe ser 100% idéntico byte a byte al esperado", expected, unpacked)
    }

    @Test
    fun testUnpackInvalidOrPlainTextReturnsNull() {
        assertNull(MediaPlatformResolver.unpackDeanEdwards(""))
        assertNull(MediaPlatformResolver.unpackDeanEdwards("var a = 123;"))
        assertNull(MediaPlatformResolver.unpackDeanEdwards("eval(function(){})"))
    }

    @Test
    fun testUnpackSpeedBenchmark() {
        val packedFile = File("../scratch/packed.js").takeIf { it.exists() }
            ?: File("scratch/packed.js").takeIf { it.exists() }
            ?: File("../../scratch/packed.js")
        if (!packedFile.exists()) return

        val packed = packedFile.readText()
        // Calentamiento
        repeat(5) { MediaPlatformResolver.unpackDeanEdwards(packed) }

        val start = System.nanoTime()
        val iterations = 50
        repeat(iterations) {
            MediaPlatformResolver.unpackDeanEdwards(packed)
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0 / iterations
        println("Tiempo promedio de desempaquetado: $elapsedMs ms")
    }
}
