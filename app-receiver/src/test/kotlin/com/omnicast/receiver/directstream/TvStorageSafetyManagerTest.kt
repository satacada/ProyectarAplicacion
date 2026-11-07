package com.omnicast.receiver.directstream

import org.junit.Assert.*
import org.junit.Test

class TvStorageSafetyManagerTest {

    @Test
    fun testInternalStorageReserveEnforcement() {
        val reserve = TvStorageSafetyManager.INTERNAL_SAFETY_RESERVE_BYTES
        assertEquals(2L * 1024L * 1024L * 1024L, reserve)

        // 3.0 GB libres -> 1.0 GB grabable útil
        val threeGb = 3L * 1024L * 1024L * 1024L
        val usableThreeGb = TvStorageSafetyManager.calculateUsableBytes(threeGb, isUsb = false)
        assertEquals(1L * 1024L * 1024L * 1024L, usableThreeGb)
        assertTrue(TvStorageSafetyManager.isStorageSafeForRecording(threeGb, isUsb = false))

        // Exactamente 2.0 GB libres -> 0 bytes útiles, NO seguro para grabar (blindaje anti-brick)
        val exactlyTwoGb = 2L * 1024L * 1024L * 1024L
        val usableTwoGb = TvStorageSafetyManager.calculateUsableBytes(exactlyTwoGb, isUsb = false)
        assertEquals(0L, usableTwoGb)
        assertFalse(TvStorageSafetyManager.isStorageSafeForRecording(exactlyTwoGb, isUsb = false))

        // 1.5 GB libres -> 0 bytes útiles, NO seguro
        val oneAndHalfGb = (1.5 * 1024.0 * 1024.0 * 1024.0).toLong()
        val usableBelow = TvStorageSafetyManager.calculateUsableBytes(oneAndHalfGb, isUsb = false)
        assertEquals(0L, usableBelow)
        assertFalse(TvStorageSafetyManager.isStorageSafeForRecording(oneAndHalfGb, isUsb = false))
    }

    @Test
    fun testExternalUsbReserveEnforcement() {
        val reserve = TvStorageSafetyManager.EXTERNAL_USB_SAFETY_RESERVE_BYTES
        assertEquals(500L * 1024L * 1024L, reserve)

        // 1.5 GB en USB -> 1.0 GB útil
        val oneAndHalfGb = (1.5 * 1024.0 * 1024.0 * 1024.0).toLong()
        val usable = TvStorageSafetyManager.calculateUsableBytes(oneAndHalfGb, isUsb = true)
        val expectedOneGb = oneAndHalfGb - reserve
        assertEquals(expectedOneGb, usable)
        assertTrue(TvStorageSafetyManager.isStorageSafeForRecording(oneAndHalfGb, isUsb = true))

        // 500 MB exactos en USB -> 0 útil, bloqueado
        val exactly500Mb = 500L * 1024L * 1024L
        val usable500 = TvStorageSafetyManager.calculateUsableBytes(exactly500Mb, isUsb = true)
        assertEquals(0L, usable500)
        assertFalse(TvStorageSafetyManager.isStorageSafeForRecording(exactly500Mb, isUsb = true))

        // 200 MB en USB -> 0 útil, bloqueado
        val lowUsb = 200L * 1024L * 1024L
        assertEquals(0L, TvStorageSafetyManager.calculateUsableBytes(lowUsb, isUsb = true))
        assertFalse(TvStorageSafetyManager.isStorageSafeForRecording(lowUsb, isUsb = true))
    }

    @Test
    fun testBitrateEstimationMinutes() {
        // 262 MB útiles para 1080p (26.2 MB/min) -> 10 minutos
        val bytesFor10Min1080p = (262.0 * 1024.0 * 1024.0).toLong()
        val min1080p = TvStorageSafetyManager.calculateMaxSafeMinutes(bytesFor10Min1080p, VideoBitratePreset.FULL_HD_1080P)
        assertEquals(10L, min1080p)

        // 150 MB útiles para 720p (15.0 MB/min) -> 10 minutos
        val bytesFor10Min720p = (150.0 * 1024.0 * 1024.0).toLong()
        val min720p = TvStorageSafetyManager.calculateMaxSafeMinutes(bytesFor10Min720p, VideoBitratePreset.HD_720P)
        assertEquals(10L, min720p)

        // 75 MB útiles para 480p (7.5 MB/min) -> 10 minutos
        val bytesFor10Min480p = (75.0 * 1024.0 * 1024.0).toLong()
        val min480p = TvStorageSafetyManager.calculateMaxSafeMinutes(bytesFor10Min480p, VideoBitratePreset.SD_480P)
        assertEquals(10L, min480p)

        // 0 bytes útiles -> 0 minutos
        assertEquals(0L, TvStorageSafetyManager.calculateMaxSafeMinutes(0L, VideoBitratePreset.FULL_HD_1080P))
    }

    @Test
    fun testFormatBytes() {
        assertEquals("0 MB", TvStorageSafetyManager.formatBytes(0L))
        assertEquals("500 MB", TvStorageSafetyManager.formatBytes(500L * 1024L * 1024L))
        assertEquals("2.00 GB", TvStorageSafetyManager.formatBytes(2L * 1024L * 1024L * 1024L))
    }

    @Test
    fun testTimerAutoCappingLogic() {
        // Pedir 90 minutos con espacio seguro para 50 minutos -> tope a 50 min con advertencia
        val (capped1, warn1) = TvRecordTimerManager.calculateAutoCappedMinutes(90, 50L)
        assertEquals(50, capped1)
        assertNotNull(warn1)
        assertTrue(warn1!!.contains("50 minutos"))

        // Pedir 30 minutos con espacio seguro para 50 minutos -> 30 min sin recorte
        val (capped2, warn2) = TvRecordTimerManager.calculateAutoCappedMinutes(30, 50L)
        assertEquals(30, capped2)
        assertNull(warn2)

        // Modo continuo (0 min) -> 0 min sin recorte
        val (capped3, warn3) = TvRecordTimerManager.calculateAutoCappedMinutes(0, 50L)
        assertEquals(0, capped3)
        assertNull(warn3)
    }

    @Test
    fun testTvPvrRecordingFileDataClass() {
        val file = TvPvrRecordingFile(
            name = "partido_final.ts",
            path = "/storage/emulated/0/Download/PVR_Grabaciones/partido_final.ts",
            sizeBytes = 1024L * 1024L * 500L,
            formattedSize = "500 MB",
            lastModified = 1700000000000L,
            isUsb = false
        )
        assertEquals("partido_final.ts", file.name)
        assertEquals("/storage/emulated/0/Download/PVR_Grabaciones/partido_final.ts", file.path)
        assertEquals(500L * 1024L * 1024L, file.sizeBytes)
        assertEquals("500 MB", file.formattedSize)
        assertEquals(1700000000000L, file.lastModified)
        assertFalse(file.isUsb)
    }

    @Test
    fun testScanDirectoryForRecordingsFiltersZeroBytesAndNonMedia() {
        val tempDir = java.nio.file.Files.createTempDirectory("pvr_test").toFile()
        try {
            // 1. Archivos válidos > 0 bytes (.ts y .mp4)
            java.io.File(tempDir, "rec_20261107_120000.ts").apply {
                writeBytes(ByteArray(1024) { 0x47 })
                setLastModified(1000L)
            }
            java.io.File(tempDir, "pelicula_cine.mp4").apply {
                writeBytes(ByteArray(2048) { 1 })
                setLastModified(2000L)
            }

            // 2. Archivos vacíos (0 bytes) que deben ser descartados
            java.io.File(tempDir, "grabacion_vacia.ts").createNewFile()
            java.io.File(tempDir, "corrupto.mp4").createNewFile()

            // 3. Extensiones no válidas que deben ignorarse
            java.io.File(tempDir, "notes.txt").writeText("informacion")
            java.io.File(tempDir, "video.mkv").writeBytes(ByteArray(512))

            val scanned = TvStorageSafetyManager.scanDirectoryForRecordings(tempDir, isUsb = true)

            // Deben detectarse únicamente los 2 archivos válidos
            assertEquals(2, scanned.size)

            // Orden descendente por lastModified: validMp4 (2000L) primero, luego validTs (1000L)
            assertEquals("pelicula_cine.mp4", scanned[0].name)
            assertEquals(2048L, scanned[0].sizeBytes)
            assertTrue(scanned[0].isUsb)
            assertEquals(2000L, scanned[0].lastModified)

            assertEquals("rec_20261107_120000.ts", scanned[1].name)
            assertEquals(1024L, scanned[1].sizeBytes)
            assertTrue(scanned[1].isUsb)
            assertEquals(1000L, scanned[1].lastModified)

            // Verificar que no contenga archivos de 0 bytes ni extensiones descartadas
            assertFalse(scanned.any { it.sizeBytes == 0L })
            assertFalse(scanned.any { it.name.endsWith(".txt") || it.name.endsWith(".mkv") })
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testScanDirectoryNonExistentReturnsEmpty() {
        val nonExistent = java.io.File("/tmp/non_existent_pvr_dir_${System.currentTimeMillis()}")
        val results = TvStorageSafetyManager.scanDirectoryForRecordings(nonExistent, isUsb = false)
        assertTrue(results.isEmpty())
    }

    @Test
    fun testStorageBoundaryLimitsAndEdgeCases() {
        val tvReserve = TvStorageSafetyManager.INTERNAL_SAFETY_RESERVE_BYTES
        val usbReserve = TvStorageSafetyManager.EXTERNAL_USB_SAFETY_RESERVE_BYTES

        // Exactamente en el umbral (2.0 GB TV / 500 MB USB)
        assertEquals(0L, TvStorageSafetyManager.calculateUsableBytes(tvReserve, isUsb = false))
        assertFalse(TvStorageSafetyManager.isStorageSafeForRecording(tvReserve, isUsb = false))

        assertEquals(0L, TvStorageSafetyManager.calculateUsableBytes(usbReserve, isUsb = true))
        assertFalse(TvStorageSafetyManager.isStorageSafeForRecording(usbReserve, isUsb = true))

        // 1 byte por debajo de la reserva (no seguro)
        assertEquals(0L, TvStorageSafetyManager.calculateUsableBytes(tvReserve - 1L, isUsb = false))
        assertFalse(TvStorageSafetyManager.isStorageSafeForRecording(tvReserve - 1L, isUsb = false))

        assertEquals(0L, TvStorageSafetyManager.calculateUsableBytes(usbReserve - 1L, isUsb = true))
        assertFalse(TvStorageSafetyManager.isStorageSafeForRecording(usbReserve - 1L, isUsb = true))

        // 1 byte por encima de la reserva (seguro)
        assertEquals(1L, TvStorageSafetyManager.calculateUsableBytes(tvReserve + 1L, isUsb = false))
        assertTrue(TvStorageSafetyManager.isStorageSafeForRecording(tvReserve + 1L, isUsb = false))

        assertEquals(1L, TvStorageSafetyManager.calculateUsableBytes(usbReserve + 1L, isUsb = true))
        assertTrue(TvStorageSafetyManager.isStorageSafeForRecording(usbReserve + 1L, isUsb = true))

        // 0 bytes libres
        assertEquals(0L, TvStorageSafetyManager.calculateUsableBytes(0L, isUsb = false))
        assertFalse(TvStorageSafetyManager.isStorageSafeForRecording(0L, isUsb = false))
        assertEquals(0L, TvStorageSafetyManager.calculateUsableBytes(0L, isUsb = true))
        assertFalse(TvStorageSafetyManager.isStorageSafeForRecording(0L, isUsb = true))

        // Cálculo de minutos máximos con 0 bytes usables (todos los presets)
        VideoBitratePreset.values().forEach { preset ->
            assertEquals(0L, TvStorageSafetyManager.calculateMaxSafeMinutes(0L, preset))
            assertEquals(0L, TvStorageSafetyManager.calculateMaxSafeMinutes(-1024L, preset))
        }
    }
}

