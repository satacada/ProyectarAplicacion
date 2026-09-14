package com.omnicast.core.domain.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias para el modelo DeviceTarget y perfiles de transmisión.
 */
class DeviceTargetTest {

    @Test
    fun testDeviceTargetCreation() {
        val target = DeviceTarget(
            id = "test-tv",
            name = "Android TV Sala",
            ipAddress = "192.168.0.4",
            port = 8088,
            deviceType = DeviceType.ANDROID_TV
        )

        assertEquals("test-tv", target.id)
        assertEquals("192.168.0.4", target.ipAddress)
        assertEquals(8088, target.port)
        assertTrue(target.isAvailable)
    }

    @Test
    fun testStreamProfiles() {
        val gamer = StreamProfile.GAMER_720P
        assertEquals(1280, gamer.width)
        assertEquals(720, gamer.height)
        assertEquals(60, gamer.frameRate)

        val balanced = StreamProfile.BALANCED_1080P
        assertEquals(1920, balanced.width)
        assertEquals(1080, balanced.height)
        assertEquals(60, balanced.frameRate)
    }
}
