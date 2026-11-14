package com.omnicast.receiver.directstream

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolutor DNS de alta resiliencia con prioridad DNS-over-HTTPS (DoH).
 * 1. Prioridad DoH: Consulta Cloudflare (1.1.1.1) y Google (dns.google) sobre HTTPS (puerto 443).
 *    Inmune a censura, bloqueos de ISP y envenenamiento de DNS (DNS poisoning/NXDOMAIN).
 * 2. Caché RAM O(1): Almacena la IP resuelta para latencia cero (< 0.001 ms) en miles de fragmentos.
 * 3. Fallback Seguro: En casos extremos (sin salida a internet exterior), recurre al DNS del dispositivo.
 */
object TvSecureDnsResolver {

    private val ipCache = ConcurrentHashMap<String, String>()

    private val DOH_ENDPOINTS = listOf(
        "https://1.1.1.1/dns-query?type=A&name=",
        "https://dns.google/resolve?type=A&name="
    )

    fun resolveHost(host: String): String? {
        if (host.isBlank()) return null

        // 1. IPs directas y red local: Retorno inmediato sin tocar DNS
        if (isIpOrLocal(host)) return host

        // 2. Caché en RAM: Latencia cero (0 ms) para fragmentos subsiguientes
        ipCache[host]?.let { return it }

        // 3. Estrategia DoH Primero (Inmune a bloqueo de ISP)
        for (baseEndpoint in DOH_ENDPOINTS) {
            val ip = queryDoh(baseEndpoint + host)
            if (!ip.isNullOrBlank()) {
                com.omnicast.receiver.log.ReceiverLogger.log("TvSecureDnsResolver", "🛡️ Resuelto vía DoH: $host -> $ip")
                ipCache[host] = ip
                return ip
            }
        }

        // 4. Fallback al DNS del dispositivo como último recurso
        return try {
            val addr = InetAddress.getByName(host)
            val sysIp = addr.hostAddress
            if (!sysIp.isNullOrBlank()) {
                ipCache[host] = sysIp
                sysIp
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun queryDoh(endpointUrl: String): String? = try {
        val conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 3000
            readTimeout = 3000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/dns-json")
            setRequestProperty("User-Agent", "OmniCast-DnsResolver/1.0")
        }
        if (conn.responseCode in 200..299) {
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            extractIpFromJson(body)
        } else {
            conn.disconnect()
            null
        }
    } catch (_: Throwable) {
        null
    }

    private fun extractIpFromJson(json: String): String? {
        // Extrae IPs válidas del campo "data" evitando loopbacks (127.0.0.1) o nulos (0.0.0.0)
        val regex = Regex(""""data"\s*:\s*"([0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3})"""")
        return regex.findAll(json).map { it.groupValues[1] }
            .firstOrNull { it != "127.0.0.1" && it != "0.0.0.0" && !it.startsWith("255.") }
    }

    private fun isIpOrLocal(host: String): Boolean {
        if (host.equals("localhost", ignoreCase = true) || host.endsWith(".local", ignoreCase = true)) return true
        val parts = host.split('.')
        if (parts.size == 4 && parts.all { it.isNotEmpty() && it.all { c -> c.isDigit() } }) return true
        return host.contains(':') // IPv6 literal
    }

    fun clearCache() {
        ipCache.clear()
    }
}
