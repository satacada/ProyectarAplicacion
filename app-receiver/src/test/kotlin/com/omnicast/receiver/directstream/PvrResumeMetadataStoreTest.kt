package com.omnicast.receiver.directstream

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Pruebas unitarias para PvrResumeMetadataStore (HU-019 / Actividad 3.2).
 * Valida guardado, carga y eliminación de metadatos de reanudación VOD/PVR.
 */
class PvrResumeMetadataStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSaveAndLoadResumeInfo() {
        val root = tempFolder.newFolder("Pelis_descargas")
        val mediaFile = File(root, "movie_1080p.mp4").apply {
            writeText("dummy content for movie file")
        }

        val info = PvrResumeInfo(
            url = "https://example.com/vod/master.m3u8",
            filePath = mediaFile.absolutePath,
            lastSegmentIndex = 42,
            bytesDownloaded = 450_000_000L,
            totalBytes = 900_000_000L,
            timestamp = 1700000000000L
        )

        PvrResumeMetadataStore.saveProgress(info)

        val metaFile = PvrResumeMetadataStore.getMetadataFile(mediaFile)
        assertTrue("El archivo .meta.json debe existir", metaFile.exists())

        val loaded = PvrResumeMetadataStore.loadResumeInfo(mediaFile)
        assertNotNull("La información de reanudación no debe ser nula", loaded)
        assertEquals(info.url, loaded?.url)
        assertEquals(info.filePath, loaded?.filePath)
        assertEquals(42, loaded?.lastSegmentIndex)
        assertEquals(450_000_000L, loaded?.bytesDownloaded)
        assertEquals(900_000_000L, loaded?.totalBytes)
        assertEquals(1700000000000L, loaded?.timestamp)
    }

    @Test
    fun testDeleteMetadata() {
        val root = tempFolder.newFolder("PVR_test")
        val mediaFile = File(root, "test_live_1080p.ts").apply { writeText("data") }
        val info = PvrResumeInfo(
            url = "https://example.com/live.m3u8",
            filePath = mediaFile.absolutePath,
            lastSegmentIndex = 5,
            bytesDownloaded = 10_000_000L,
            totalBytes = 20_000_000L
        )

        PvrResumeMetadataStore.saveProgress(info)
        assertTrue(PvrResumeMetadataStore.getMetadataFile(mediaFile).exists())

        PvrResumeMetadataStore.deleteMetadata(mediaFile)
        assertFalse(PvrResumeMetadataStore.getMetadataFile(mediaFile).exists())
        assertNull(PvrResumeMetadataStore.loadResumeInfo(mediaFile))
    }

    @Test
    fun testLoadReturnsNullWhenFileMissing() {
        val nonExistent = File(tempFolder.root, "ghost_video.mp4")
        assertNull(PvrResumeMetadataStore.loadResumeInfo(nonExistent))
    }
}
