package com.omnicast.receiver.directstream

import android.webkit.CookieManager
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

/**
 * Adaptador de Red Hexagonal para Descarga y Grabación PVR en TV Box.
 * Asegura la sesión HTTP/TLS completa (Cookies de Chromium, User-Agent de navegador,
 * Referer, Origin, cabeceras anti-WAF y SSL permisivo) para evitar errores HTTP 403.
 */
object PvrNetworkHelper {

    private const val UA_DEFAULT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

    private val TRUST_ALL_SSL_FACTORY: SSLSocketFactory by lazy {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
        })
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        sslContext.socketFactory
    }

    fun openConnection(
        urlStr: String,
        headers: Map<String, String>,
        connectTimeoutMs: Int = 10000,
        readTimeoutMs: Int = 15000
    ): HttpURLConnection {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            if (this is HttpsURLConnection) {
                sslSocketFactory = TRUST_ALL_SSL_FACTORY
                hostnameVerifier = HostnameVerifier { _, _ -> true }
            }
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            instanceFollowRedirects = true

            // 1. User-Agent real de navegador
            val ua = headers["User-Agent"]?.takeIf { it.isNotBlank() } ?: UA_DEFAULT
            setRequestProperty("User-Agent", ua)

            // 2. Referer y Origin
            headers["Referer"]?.takeIf { it.isNotBlank() }?.let { ref ->
                setRequestProperty("Referer", ref)
                val origin = headers["Origin"]?.takeIf { it.isNotBlank() } ?: try {
                    val u = java.net.URI(ref)
                    if (!u.scheme.isNullOrBlank() && !u.host.isNullOrBlank()) "${u.scheme}://${u.host}" else null
                } catch (_: Throwable) { null }
                origin?.let { setRequestProperty("Origin", it) }
            }

            // 3. Cookies de Chromium + headers
            val cmCookie = runCatching { CookieManager.getInstance().getCookie(urlStr) }.getOrNull()
            val headerCookie = headers["Cookie"]
            val combinedCookie = listOfNotNull(headerCookie, cmCookie)
                .flatMap { it.split("; ") }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString("; ")
            if (combinedCookie.isNotBlank()) {
                setRequestProperty("Cookie", combinedCookie)
            }

            // 4. Cabeceras anti-WAF estándar
            setRequestProperty("Accept", "*/*")
            setRequestProperty("Sec-Fetch-Mode", headers["Sec-Fetch-Mode"] ?: "cors")
            setRequestProperty("Sec-Fetch-Dest", headers["Sec-Fetch-Dest"] ?: "empty")
            setRequestProperty("Sec-Fetch-Site", headers["Sec-Fetch-Site"] ?: "cross-site")
        }
        return conn
    }

    fun fetchText(
        urlStr: String,
        headers: Map<String, String>,
        connectTimeoutMs: Int = 8000,
        readTimeoutMs: Int = 8000
    ): String {
        val conn = openConnection(urlStr, headers, connectTimeoutMs, readTimeoutMs)
        val code = conn.responseCode
        if (code !in 200..299) {
            val errBody = runCatching { conn.errorStream?.bufferedReader()?.use { it.readText() } }.getOrNull()
            throw IOException("HTTP $code al consultar $urlStr ${errBody?.take(100) ?: ""}".trim())
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    }
}
