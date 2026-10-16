package com.omnicast.sender.media

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * Proxy HTTP local ultraligero y aislado para transmisiones protegidas (Cloudflare Turnstile, DropCDN, Fastream).
 * Enruta la reproducción a través del teléfono preservando cookies y huella del navegador.
 * Cero dependencias invasivas, estricta adhesión a la Arquitectura Hexagonal.
 */
object LocalStreamProxyServer {
    private const val TAG = "StreamProxy"
    const val PORT = 8092
    private const val DEFAULT_UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val sessions = ConcurrentHashMap<String, ProxySession>()

    data class ProxySession(
        val originalUrl: String,
        val headers: Map<String, String>,
        val createdAt: Long = System.currentTimeMillis()
    )

    fun start(context: Context? = null) {
        if (isRunning) return
        isRunning = true
        thread(isDaemon = true, name = "StreamProxyThread") {
            try {
                serverSocket = ServerSocket(PORT).apply { reuseAddress = true }
                Log.i(TAG, "Proxy HTTP de streaming iniciado en puerto $PORT")
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    thread(isDaemon = true, name = "ProxyClientWorker") {
                        handleClient(client)
                    }
                }
            } catch (e: Throwable) {
                if (isRunning) Log.e(TAG, "Error en proxy socket: ${e.message}")
            }
        }
    }

    fun startAndGetProxyUrl(context: Context, originalUrl: String, headers: Map<String, String>): String {
        start(context)
        val sid = Integer.toHexString(originalUrl.hashCode())
        val mutableHeaders = headers.toMutableMap()
        if (!mutableHeaders.containsKey("User-Agent")) mutableHeaders["User-Agent"] = DEFAULT_UA
        try {
            val cookie = CookieManager.getInstance().getCookie(originalUrl)
                ?: headers["Referer"]?.let { CookieManager.getInstance().getCookie(it) }
            if (!cookie.isNullOrBlank() && !mutableHeaders.containsKey("Cookie")) {
                mutableHeaders["Cookie"] = cookie
            }
        } catch (_: Throwable) {}

        sessions[sid] = ProxySession(originalUrl, mutableHeaders)
        val ip = LocalImageServer.getLocalIpAddress()
        val isHls = originalUrl.contains(".m3u8", ignoreCase = true) || originalUrl.contains("/hls", ignoreCase = true)
        val endpoint = if (isHls) "master.m3u8" else "video.mp4"
        return "http://$ip:$PORT/$endpoint?sid=$sid"
    }

    fun getOriginalUrl(proxyUrl: String): String? {
        val sid = proxyUrl.substringAfter("sid=", "").substringBefore("&")
        return sessions[sid]?.originalUrl
    }

    private fun handleClient(client: Socket) {
        try {
            client.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                val reqLine = reader.readLine() ?: return
                val parts = reqLine.split(" ")
                if (parts.size < 2) return
                val pathAndQuery = parts[1]
                var rangeHeader: String? = null
                var headerLine = reader.readLine()
                while (!headerLine.isNullOrEmpty()) {
                    if (headerLine.startsWith("Range:", ignoreCase = true)) {
                        rangeHeader = headerLine.substringAfter(":", "").trim()
                    }
                    headerLine = reader.readLine()
                }

                val out = s.getOutputStream()
                val qIndex = pathAndQuery.indexOf('?')
                val path = if (qIndex != -1) pathAndQuery.substring(0, qIndex) else pathAndQuery
                val query = if (qIndex != -1) pathAndQuery.substring(qIndex + 1) else ""
                val params = parseQueryParams(query)

                when {
                    path.endsWith(".m3u8", ignoreCase = true) -> serveManifest(params["sid"], out)
                    path.startsWith("/chunk") -> serveChunk(params["url"], params["sid"], rangeHeader, out)
                    path.startsWith("/video.mp4") -> serveDirectMedia(params["sid"], rangeHeader, out)
                    else -> out.write("HTTP/1.1 404 Not Found\r\n\r\n".toByteArray())
                }
            }
        } catch (_: Throwable) {}
    }

    private fun serveManifest(sid: String?, out: OutputStream) {
        val session = sid?.let { sessions[it] } ?: run {
            out.write("HTTP/1.1 404 Session Not Found\r\n\r\n".toByteArray())
            return
        }

        try {
            val conn = (URL(session.originalUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000; readTimeout = 12000; requestMethod = "GET"
                session.headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            if (conn.responseCode !in 200..299) {
                out.write("HTTP/1.1 ${conn.responseCode} Upstream Error\r\n\r\n".toByteArray())
                conn.disconnect()
                return
            }

            val localIp = LocalImageServer.getLocalIpAddress()
            val baseUri = URI(session.originalUrl)
            val lines = conn.inputStream.bufferedReader().readLines()
            conn.disconnect()

            val rewritten = StringBuilder()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) {
                    rewritten.append("\n")
                } else if (trimmed.startsWith("#EXT-X-KEY:") && trimmed.contains("URI=\"")) {
                    val keyUri = trimmed.substringAfter("URI=\"").substringBefore("\"")
                    val absKey = try { baseUri.resolve(keyUri).toString() } catch (_: Throwable) { keyUri }
                    val proxyKey = "http://$localIp:$PORT/chunk?sid=$sid&url=${URLEncoder.encode(absKey, "UTF-8")}"
                    rewritten.append(trimmed.replace("URI=\"$keyUri\"", "URI=\"$proxyKey\"")).append("\n")
                } else if (!trimmed.startsWith("#")) {
                    val absUrl = try { baseUri.resolve(trimmed).toString() } catch (_: Throwable) { trimmed }
                    val proxyUrl = "http://$localIp:$PORT/chunk?sid=$sid&url=${URLEncoder.encode(absUrl, "UTF-8")}"
                    rewritten.append(proxyUrl).append("\n")
                } else {
                    rewritten.append(line).append("\n")
                }
            }

            val bytes = rewritten.toString().toByteArray(Charsets.UTF_8)
            val header = "HTTP/1.1 200 OK\r\nContent-Type: application/vnd.apple.mpegurl\r\nContent-Length: ${bytes.size}\r\nAccess-Control-Allow-Origin: *\r\nConnection: close\r\n\r\n"
            out.write(header.toByteArray()); out.write(bytes); out.flush()
        } catch (e: Throwable) {
            out.write("HTTP/1.1 502 Bad Gateway\r\n\r\n".toByteArray())
        }
    }

    private fun serveChunk(rawUrl: String?, sid: String?, range: String?, out: OutputStream) {
        val targetUrl = rawUrl?.let { try { URLDecoder.decode(it, "UTF-8") } catch (_: Throwable) { it } }
        if (targetUrl.isNullOrBlank()) {
            out.write("HTTP/1.1 400 Bad Request\r\n\r\n".toByteArray()); return
        }
        val session = sid?.let { sessions[it] }

        var conn: HttpURLConnection? = null
        try {
            conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000; readTimeout = 15000; requestMethod = "GET"
                session?.headers?.forEach { (k, v) -> setRequestProperty(k, v) }
                if (!range.isNullOrBlank()) setRequestProperty("Range", range)
            }
            val code = conn.responseCode
            val length = conn.contentLengthLong
            val mime = conn.contentType ?: "video/mp2t"
            val statusLine = if (code == 206) "HTTP/1.1 206 Partial Content" else "HTTP/1.1 200 OK"
            val rangeResp = conn.getHeaderField("Content-Range")?.let { "Content-Range: $it\r\n" } ?: ""

            val header = "$statusLine\r\nContent-Type: $mime\r\nContent-Length: $length\r\n${rangeResp}Accept-Ranges: bytes\r\nAccess-Control-Allow-Origin: *\r\nConnection: close\r\n\r\n"
            out.write(header.toByteArray())

            conn.inputStream.use { input ->
                val buf = ByteArray(64 * 1024)
                var read: Int
                while (input.read(buf).also { read = it } != -1) {
                    out.write(buf, 0, read)
                }
            }
            out.flush()
        } catch (_: Throwable) {
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
    }

    private fun serveDirectMedia(sid: String?, range: String?, out: OutputStream) {
        val session = sid?.let { sessions[it] } ?: run {
            out.write("HTTP/1.1 404 Not Found\r\n\r\n".toByteArray()); return
        }
        serveChunk(session.originalUrl, sid, range, out)
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split("&").mapNotNull {
            val idx = it.indexOf('=')
            if (idx > 0) it.substring(0, idx) to it.substring(idx + 1) else null
        }.toMap()
    }

    fun stop() {
        isRunning = false
        try { serverSocket?.close() } catch (_: Throwable) {}
        serverSocket = null
        sessions.clear()
    }
}
