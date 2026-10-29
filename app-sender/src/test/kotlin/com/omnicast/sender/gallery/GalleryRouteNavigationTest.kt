package com.omnicast.sender.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GalleryRouteNavigationTest {

    @Before
    fun setUp() {
        GallerySessionHolder.clear()
    }

    @Test
    fun `secuencia arbitraria de carpetas 1-3-2-4-5 retrocede exactamente 5-4-2-3-1 sin saltar`() {
        GallerySessionHolder.ensureRouteInitialized()
        assertEquals(GallerySessionHolder.ROOT_NAME, GallerySessionHolder.currentRouteNode().name)

        // Navegar por la secuencia: Carpeta 1 -> Carpeta 3 -> Carpeta 2 -> Carpeta 4 -> Carpeta 5
        GallerySessionHolder.navigateToFolder("Carpeta 1", "Carpeta 1", currentScrollIndex = 2, expandAncestorsIfFromRoot = false)
        GallerySessionHolder.navigateToFolder("Carpeta 3", "Carpeta 3", currentScrollIndex = 5, expandAncestorsIfFromRoot = false)
        GallerySessionHolder.navigateToFolder("Carpeta 2", "Carpeta 2", currentScrollIndex = 8, expandAncestorsIfFromRoot = false)
        GallerySessionHolder.navigateToFolder("Carpeta 4", "Carpeta 4", currentScrollIndex = 11, expandAncestorsIfFromRoot = false)
        GallerySessionHolder.navigateToFolder("Carpeta 5", "Carpeta 5", currentScrollIndex = 14, expandAncestorsIfFromRoot = false)

        assertEquals("Carpeta 5", GallerySessionHolder.currentRouteNode().name)
        assertTrue(GallerySessionHolder.canGoBackFolder())

        // Retroceder paso a paso: de 5 debe ir a 4, luego 2, luego 3, luego 1, luego Raíz
        assertEquals("Carpeta 4", GallerySessionHolder.popPreviousFolder()?.name)
        assertEquals(14, GallerySessionHolder.currentRouteNode().firstVisibleIndex)

        assertEquals("Carpeta 2", GallerySessionHolder.popPreviousFolder()?.name)
        assertEquals(11, GallerySessionHolder.currentRouteNode().firstVisibleIndex)

        assertEquals("Carpeta 3", GallerySessionHolder.popPreviousFolder()?.name)
        assertEquals(8, GallerySessionHolder.currentRouteNode().firstVisibleIndex)

        assertEquals("Carpeta 1", GallerySessionHolder.popPreviousFolder()?.name)
        assertEquals(5, GallerySessionHolder.currentRouteNode().firstVisibleIndex)

        assertEquals(GallerySessionHolder.ROOT_NAME, GallerySessionHolder.popPreviousFolder()?.name)
        assertEquals(2, GallerySessionHolder.currentRouteNode().firstVisibleIndex)

        assertFalse(GallerySessionHolder.canGoBackFolder())
    }

    @Test
    fun `salto a carpeta anidada 1-3-2-4-5 reconstruye todos los ancestros sin saltar niveles`() {
        GallerySessionHolder.ensureRouteInitialized()
        GallerySessionHolder.navigateToFolder(
            targetPath = "Carpeta 1/Carpeta 3/Carpeta 2/Carpeta 4/Carpeta 5",
            targetName = "Carpeta 5",
            expandAncestorsIfFromRoot = true
        )

        assertEquals("Carpeta 5", GallerySessionHolder.currentRouteNode().name)
        assertEquals("Carpeta 1/Carpeta 3/Carpeta 2/Carpeta 4/Carpeta 5", GallerySessionHolder.currentRouteNode().path)

        assertEquals("Carpeta 4", GallerySessionHolder.popPreviousFolder()?.name)
        assertEquals("Carpeta 2", GallerySessionHolder.popPreviousFolder()?.name)
        assertEquals("Carpeta 3", GallerySessionHolder.popPreviousFolder()?.name)
        assertEquals("Carpeta 1", GallerySessionHolder.popPreviousFolder()?.name)
        assertEquals(GallerySessionHolder.ROOT_NAME, GallerySessionHolder.popPreviousFolder()?.name)
        assertFalse(GallerySessionHolder.canGoBackFolder())
    }

    @Test
    fun `extractNormalizedFolderPath extrae la jerarquia completa sin perder subcarpetas`() {
        val path = GalleryMediaHelper.extractNormalizedFolderPath(
            relativePath = "Pictures/Carpeta 1/Carpeta 3/Carpeta 2/Carpeta 4/Carpeta 5/",
            dataPath = null,
            bucketName = "Carpeta 5"
        )
        assertEquals("Pictures/Carpeta 1/Carpeta 3/Carpeta 2/Carpeta 4/Carpeta 5", path)
    }
}
