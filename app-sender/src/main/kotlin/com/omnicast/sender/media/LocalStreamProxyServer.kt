package com.omnicast.sender.media

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import com.omnicast.sender.ui.browser.BrowserUrlHelper
import com.omnicast.sender.ui.browser.MediaPlatformResolver
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.util.thread.QueuedThreadPool
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.concurrent.thread

/**
 * Servidor Proxy HTTP local impulsado por Eclipse Jetty 9.4 y OkHttp Connection Pooling.
 * Provee streaming persistente (Keep-Alive), soporte de rangos parciales (HTTP 206)
 * y reenvío fiel de cookies/cabeceras de sesión para sortear protecciones CDN (DropCDN, Fastream, Turnstile).
 */
object LocalStreamProxyServer {
    private const val TAG = "StreamProxyJetty"
    const val PORT = 8092
    private const val DEFAULT_UA = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private var jettyServer: Server? = null
    @Volatile private var isRunning = false
    private val sessions = ConcurrentHashMap<String, ProxySession>()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    data class ProxySession(
        val originalUrl: String,
        val headers: Map<String, String>,
        val createdAt: Long = System.currentTimeMillis()
    )

    @Synchronized
    fun start(context: Context? = null) {
        if (isRunning && jettyServer?.isStarted == true) return
        try {
            val pool = QueuedThreadPool(32, 8, 60000).apply { name = "JettyWorker" }
            val server = Server(pool)
            val connector = ServerConnector(server).apply {
                port = PORT
                reuseAddress = true
                idleTimeout = 30000
            }
            server.addConnector(connector)
            server.handler = JettyProxyHandler()
            server.start()
            jettyServer = server
            isRunning = true
            Log.i(TAG, "🟢 Servidor Eclipse Jetty 9.4 iniciado en puerto $PORT con Keep-Alive")
        } catch (e: Throwable) {
            isRunning = false
            Log.e(TAG, "❌ Error al iniciar Eclipse Jetty: ${e.message}", e)
        }
    }

    fun enrichProxyHeaders(originalUrl: String, headers: Map<String, String>): Map<String, String> {
        val cleanUrl = BrowserUrlHelper.sanitizeMediaUrl(originalUrl)
        val mutableHeaders = headers.toMutableMap()
        if (!mutableHeaders.containsKey("User-Agent")) mutableHeaders["User-Agent"] = DEFAULT_UA
        val id = MediaPlatformResolver.extractFastreamId(cleanUrl)
        val isDrop = cleanUrl.contains("dropcdn.io", true) || cleanUrl.contains("dr0pstream.com", true)
        val isFastream = cleanUrl.contains("fastream.to", true)
        if (isDrop && id != null) {
            mutableHeaders["Referer"] = "https://dr0pstream.com/e/$id"
            mutableHeaders["User-Agent"] = MediaPlatformResolver.UA_FASTREAM
            mutableHeaders.remove("Origin")
        } else if (isFastream && id != null) {
            mutableHeaders["Referer"] = "https://fastream.to/embed-$id.html"
            mutableHeaders["User-Agent"] = MediaPlatformResolver.UA_FASTREAM
            mutableHeaders.remove("Origin")
        } else {
            val ref = mutableHeaders["Referer"] ?: headers["Referer"]
            if (!ref.isNullOrBlank() && !mutableHeaders.containsKey("Origin")) {
                try { val u = URI(ref); mutableHeaders["Origin"] = "${u.scheme}://${u.authority}" } catch (_: Throwable) {}
            }
        }
        if (!mutableHeaders.containsKey("Sec-Fetch-Mode")) mutableHeaders["Sec-Fetch-Mode"] = "cors"
        if (!mutableHeaders.containsKey("Sec-Fetch-Site")) mutableHeaders["Sec-Fetch-Site"] = "cross-site"
        if (!mutableHeaders.containsKey("Sec-Fetch-Dest")) mutableHeaders["Sec-Fetch-Dest"] = "empty"
        if (!mutableHeaders.containsKey("Accept")) mutableHeaders["Accept"] = "*/*"
        try {
            // Por qué: Solo enviar la cookie que pertenezca al host de la petición para evitar Nginx 494
            val cm = CookieManager.getInstance()
            val specificCookie = cm?.getCookie(cleanUrl)?.takeIf { it.isNotBlank() } ?: mutableHeaders["Cookie"]
            if (!specificCookie.isNullOrBlank()) mutableHeaders["Cookie"] = specificCookie else mutableHeaders.remove("Cookie")
        } catch (_: Throwable) {}
        return mutableHeaders
    }

    fun fetchManifestContent(targetUrl: String, headers: Map<String, String>): String? {
        val cleanUrl = BrowserUrlHelper.sanitizeMediaUrl(targetUrl)
        val enriched = enrichProxyHeaders(cleanUrl, headers)
        val reqBuilder = OkHttpRequest.Builder().url(cleanUrl).get()
        enriched.forEach { (k, v) -> reqBuilder.header(k, v) }
        return try {
            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } catch (_: Throwable) { null }
    }

    fun startAndGetProxyUrl(context: Context, originalUrl: String, headers: Map<String, String>): String {
        start(context)
        val cleanUrl = BrowserUrlHelper.sanitizeMediaUrl(originalUrl)
        val sid = Integer.toHexString(cleanUrl.hashCode())
        val enriched = enrichProxyHeaders(cleanUrl, headers)
        sessions[sid] = ProxySession(cleanUrl, enriched)
        val ip = LocalImageServer.getLocalIpAddress()
        val isHls = cleanUrl.contains(".m3u8", ignoreCase = true) || cleanUrl.contains("/hls", ignoreCase = true)
        val endpoint = if (isHls) "master.m3u8" else "video.mp4"
        return "http://$ip:$PORT/$endpoint?sid=$sid"
    }

    fun getOriginalUrl(proxyUrl: String): String? {
        val sid = proxyUrl.substringAfter("sid=", "").substringBefore("&")
        return sessions[sid]?.originalUrl
    }

    private class JettyProxyHandler : AbstractHandler() {
        override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
            baseRequest.isHandled = true
            response.setHeader("Access-Control-Allow-Origin", "*")
            response.setHeader("Access-Control-Allow-Headers", "*")
            response.setHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
            if (request.method.equals("OPTIONS", ignoreCase = true)) {
                response.status = HttpServletResponse.SC_OK
                return
            }

            val path = target.substringBefore('?')
            val rawUrl = request.getParameter("url")
            val sid = request.getParameter("sid")
            val session = sid?.let { sessions[it] }

            when {
                path.endsWith(".m3u8", ignoreCase = true) || path.contains("manifest", ignoreCase = true) ->
                    serveManifest(rawUrl, session, sid, response)
                path.contains("/chunk", ignoreCase = true) ->
                    serveChunk(rawUrl, session, request.getHeader("Range"), response)
                path.startsWith("/video.mp4", ignoreCase = true) ->
                    serveChunk(session?.originalUrl, session, request.getHeader("Range"), response)
                else -> response.sendError(HttpServletResponse.SC_NOT_FOUND)
            }
        }

        private fun serveManifest(rawUrl: String?, session: ProxySession?, sid: String?, response: HttpServletResponse) {
            if (session == null && rawUrl.isNullOrBlank()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Session not found"); return
            }
            val targetUrl = BrowserUrlHelper.sanitizeMediaUrl(rawUrl?.let { decode(it) } ?: session?.originalUrl ?: "")
            val reqBuilder = OkHttpRequest.Builder().url(targetUrl).get()
            session?.headers?.forEach { (k, v) -> reqBuilder.header(k, v) }

            try {
                httpClient.newCall(reqBuilder.build()).execute().use { upstream ->
                    if (!upstream.isSuccessful) {
                        response.sendError(upstream.code, "Upstream error"); return
                    }
                    val body = upstream.body?.string() ?: ""
                    val localIp = LocalImageServer.getLocalIpAddress()
                    val baseUri = URI(targetUrl)
                    val lines = body.lineSequence()
                    val rewritten = StringBuilder()

                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) {
                            rewritten.append("\n")
                        } else if (trimmed.startsWith("#EXT-X-KEY:") && trimmed.contains("URI=\"")) {
                            val keyUri = trimmed.substringAfter("URI=\"").substringBefore("\"")
                            val absKey = try { baseUri.resolve(keyUri).toString() } catch (_: Throwable) { keyUri }
                            val proxyKey = "http://$localIp:$PORT/chunk.ts?sid=$sid&url=${encode(absKey)}"
                            rewritten.append(trimmed.replace("URI=\"$keyUri\"", "URI=\"$proxyKey\"")).append("\n")
                        } else if (trimmed.startsWith("#EXT-X-MEDIA:") && trimmed.contains("URI=\"")) {
                            val mediaUri = trimmed.substringAfter("URI=\"").substringBefore("\"")
                            val absMedia = try { baseUri.resolve(mediaUri).toString() } catch (_: Throwable) { mediaUri }
                            val isSub = absMedia.contains(".m3u8", ignoreCase = true)
                            val ep = if (isSub) "playlist.m3u8" else "chunk.ts"
                            val proxyMedia = "http://$localIp:$PORT/$ep?sid=$sid&url=${encode(absMedia)}"
                            rewritten.append(trimmed.replace("URI=\"$mediaUri\"", "URI=\"$proxyMedia\"")).append("\n")
                        } else if (!trimmed.startsWith("#")) {
                            val absUrl = try { baseUri.resolve(trimmed).toString() } catch (_: Throwable) { trimmed }
                            val isSub = absUrl.contains(".m3u8", ignoreCase = true)
                            val ep = if (isSub) "playlist.m3u8" else "chunk.ts"
                            val proxyUrl = "http://$localIp:$PORT/$ep?sid=$sid&url=${encode(absUrl)}"
                            rewritten.append(proxyUrl).append("\n")
                        } else {
                            rewritten.append(line).append("\n")
                        }
                    }

                    val bytes = rewritten.toString().toByteArray(Charsets.UTF_8)
                    response.status = HttpServletResponse.SC_OK
                    response.contentType = "application/vnd.apple.mpegurl"
                    response.setContentLength(bytes.size)
                    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                    response.outputStream.write(bytes)
                    response.outputStream.flush()
                }
            } catch (e: Throwable) {
                try { response.sendError(HttpServletResponse.SC_BAD_GATEWAY) } catch (_: Throwable) {}
            }
        }

        private fun serveChunk(rawUrl: String?, session: ProxySession?, range: String?, response: HttpServletResponse) {
            val targetUrl = BrowserUrlHelper.sanitizeMediaUrl(rawUrl?.let { decode(it) } ?: "")
            if (targetUrl.isBlank()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST); return
            }

            val reqBuilder = OkHttpRequest.Builder().url(targetUrl).get()
            session?.headers?.forEach { (k, v) -> reqBuilder.header(k, v) }
            if (!range.isNullOrBlank()) reqBuilder.header("Range", range)

            try {
                httpClient.newCall(reqBuilder.build()).execute().use { upstream ->
                    response.status = upstream.code
                    response.contentType = upstream.header("Content-Type") ?: "video/mp2t"
                    upstream.header("Content-Length")?.toLongOrNull()?.let { response.setContentLengthLong(it) }
                    upstream.header("Content-Range")?.let { response.setHeader("Content-Range", it) }
                    response.setHeader("Accept-Ranges", "bytes")

                    upstream.body?.byteStream()?.use { input ->
                        val buf = ByteArray(64 * 1024)
                        var read: Int
                        val out = response.outputStream
                        while (input.read(buf).also { read = it } != -1) {
                            out.write(buf, 0, read)
                        }
                        out.flush()
                    }
                }
            } catch (_: IOException) {
                // Client disconnection or seek (Broken pipe / Connection reset): ignore gracefully
            } catch (e: Throwable) {
                try { response.sendError(HttpServletResponse.SC_BAD_GATEWAY) } catch (_: Throwable) {}
            }
        }

        private fun decode(s: String): String = try { URLDecoder.decode(s, "UTF-8") } catch (_: Throwable) { s }
        private fun encode(s: String): String = try { URLEncoder.encode(s, "UTF-8") } catch (_: Throwable) { s }
    }

    @Synchronized
    fun stop() {
        isRunning = false
        try {
            jettyServer?.stop()
            jettyServer?.destroy()
        } catch (_: Throwable) {}
        jettyServer = null
        sessions.clear()
        Log.i(TAG, "⏹️ Servidor Eclipse Jetty detenido")
    }
}
