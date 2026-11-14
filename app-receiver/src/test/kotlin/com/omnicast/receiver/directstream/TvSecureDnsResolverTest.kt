package com.omnicast.receiver.directstream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TvSecureDnsResolverTest {

    @Before
    fun setUp() {
        TvSecureDnsResolver.clearCache()
    }

    @Test
    fun `resolveHost returns literal IPv4 addresses immediately without querying DoH`() {
        val ip = TvSecureDnsResolver.resolveHost("192.168.0.136")
        assertEquals("192.168.0.136", ip)

        val loopback = TvSecureDnsResolver.resolveHost("127.0.0.1")
        assertEquals("127.0.0.1", loopback)
    }

    @Test
    fun `resolveHost returns localhost immediately without querying DoH`() {
        val local = TvSecureDnsResolver.resolveHost("localhost")
        assertEquals("localhost", local)
    }

    @Test
    fun `resolveHost resolves real sports streaming host via DoH and caches in RAM`() {
        val host = "2.ftlly.com"
        val ip1 = TvSecureDnsResolver.resolveHost(host)
        assertNotNull("La IP no debe ser nula al resolverse vía DoH", ip1)
        assertTrue("La IP resuelta debe tener formato IPv4 válido", ip1?.matches(Regex("""[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}""")) == true)

        // Segunda llamada debe salir de la caché en RAM
        val ip2 = TvSecureDnsResolver.resolveHost(host)
        assertEquals(ip1, ip2)
    }
}
