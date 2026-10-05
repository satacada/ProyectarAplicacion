package com.omnicast.sender.media

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.net.Socket

/**
 * Manejador HTTP para servir archivos multimedia locales con soporte de chunks y rangos (HTTP 206).
 */
object LocalMediaStreamHandler {
    private const val TAG = "LocalMediaStreamHandler"

    fun handleClient(
        client: Socket,
        file: File?,
        uri: Uri?,
        mime: String,
        context: Context?
    ) {
        try {
            client.use { s ->
                val input = s.getInputStream().bufferedReader()
                val reqLine = input.readLine() ?: return
                val isHead = reqLine.startsWith("HEAD", ignoreCase = true)
                var rangeHeader: String? = null
                var line = input.readLine()
                while (!line.isNullOrEmpty()) {
                    if (line.startsWith("Range:", ignoreCase = true)) {
                        rangeHeader = line.substringAfter(":", "").trim()
                    }
                    line = input.readLine()
                }

                val out = s.getOutputStream()
                val targetMime = mime.ifBlank { "application/octet-stream" }

                if (file != null && file.exists() && file.length() > 0) {
                    streamFromFile(file, out, isHead, rangeHeader, targetMime)
                } else if (uri != null && context != null) {
                    streamFromUri(uri, context, out, isHead, rangeHeader, targetMime)
                } else {
                    out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n".toByteArray())
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error atendiendo stream: ${e.message}")
        }
    }

    private fun streamFromFile(
        file: File,
        out: java.io.OutputStream,
        isHead: Boolean,
        rangeHeader: String?,
        mime: String
    ) {
        val size = file.length()
        val (start, end) = parseRange(rangeHeader, size)
        val contentLength = end - start + 1
        val isRange = rangeHeader != null && rangeHeader.startsWith("bytes=")
        val statusLine = if (isRange) "HTTP/1.1 206 Partial Content" else "HTTP/1.1 200 OK"
        val rangeResp = if (isRange) "Content-Range: bytes $start-$end/$size\r\n" else ""
        val header = "$statusLine\r\nContent-Type: $mime\r\nContent-Length: $contentLength\r\n${rangeResp}Accept-Ranges: bytes\r\nAccess-Control-Allow-Origin: *\r\nConnection: close\r\n\r\n"
        out.write(header.toByteArray())

        if (!isHead) {
            FileInputStream(file).use { fis ->
                if (start > 0) fis.channel.position(start)
                copyStreamWithLimit(fis, out, contentLength)
            }
        }
        out.flush()
    }

    private fun streamFromUri(
        uri: Uri,
        context: Context,
        out: java.io.OutputStream,
        isHead: Boolean,
        rangeHeader: String?,
        mime: String
    ) {
        val pfd = try { context.contentResolver.openFileDescriptor(uri, "r") } catch (_: Throwable) { null }
        val size = pfd?.statSize ?: -1L
        if (pfd == null || size <= 0) {
            val header = "HTTP/1.1 200 OK\r\nContent-Type: $mime\r\nTransfer-Encoding: chunked\r\nAccess-Control-Allow-Origin: *\r\nConnection: close\r\n\r\n"
            out.write(header.toByteArray())
            if (!isHead) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        out.write("${Integer.toHexString(read)}\r\n".toByteArray())
                        out.write(buffer, 0, read)
                        out.write("\r\n".toByteArray())
                        out.flush()
                    }
                    out.write("0\r\n\r\n".toByteArray())
                    out.flush()
                }
            }
            return
        }

        val (start, end) = parseRange(rangeHeader, size)
        val contentLength = end - start + 1
        val isRange = rangeHeader != null && rangeHeader.startsWith("bytes=")
        val statusLine = if (isRange) "HTTP/1.1 206 Partial Content" else "HTTP/1.1 200 OK"
        val rangeResp = if (isRange) "Content-Range: bytes $start-$end/$size\r\n" else ""
        val header = "$statusLine\r\nContent-Type: $mime\r\nContent-Length: $contentLength\r\n${rangeResp}Accept-Ranges: bytes\r\nAccess-Control-Allow-Origin: *\r\nConnection: close\r\n\r\n"
        out.write(header.toByteArray())

        if (!isHead) {
            pfd.use { fd ->
                FileInputStream(fd.fileDescriptor).use { fis ->
                    if (start > 0) fis.channel.position(start)
                    copyStreamWithLimit(fis, out, contentLength)
                }
            }
        }
        out.flush()
    }

    private fun parseRange(rangeHeader: String?, size: Long): Pair<Long, Long> {
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            val parts = rangeHeader.substringAfter("bytes=").trim().split("-")
            val s = parts.getOrNull(0)?.toLongOrNull() ?: 0L
            val e = parts.getOrNull(1)?.toLongOrNull() ?: (size - 1)
            return Pair(s.coerceAtLeast(0L), e.coerceAtMost(size - 1))
        }
        return Pair(0L, size - 1)
    }

    private fun copyStreamWithLimit(input: java.io.InputStream, output: java.io.OutputStream, limit: Long) {
        val buffer = ByteArray(64 * 1024)
        var remaining = limit
        while (remaining > 0) {
            val toRead = minOf(buffer.size.toLong(), remaining).toInt()
            val read = input.read(buffer, 0, toRead)
            if (read == -1) break
            output.write(buffer, 0, read)
            remaining -= read
        }
    }
}
