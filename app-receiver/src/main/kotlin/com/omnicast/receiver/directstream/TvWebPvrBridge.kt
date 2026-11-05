package com.omnicast.receiver.directstream

import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference

/**
 * Puente Hexagonal de Grabación/Descarga PVR desde el motor Chromium de TvWebStreamScreen.
 * Se activa EXCLUSIVAMENTE para flujos Variante 1 (DropCDN, Fastream con TLS Lock / Token HMAC)
 * que se reproducen en TvWebStreamScreen, reutilizando la sesión BoringSSL y manifiesto en RAM de Hls.js.
 */
object TvWebPvrBridge {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var webViewRef: WeakReference<WebView>? = null

    @Volatile
    var activeStreamUrl: String = ""
        private set

    @Volatile
    var activeDurationMs: Long = 0L

    @Volatile
    private var activeSession: BridgeDownloadSession? = null

    private class BridgeDownloadSession(
        val outFile: File,
        val expectedBytes: Long,
        val onProgress: (TvBridgeDownloadProgress) -> Unit,
        val completion: CompletableDeferred<Boolean>
    ) {
        var fileOutput: FileOutputStream? = null
        var downloadedBytes: Long = 0L
        var lastError: String? = null

        fun writeBytes(bytes: ByteArray, currentSeg: Int, totalSegs: Int) {
            if (bytes.isEmpty()) return
            val fos = fileOutput ?: FileOutputStream(outFile, false).also { fileOutput = it }
            fos.write(bytes)
            downloadedBytes += bytes.size
            val pct = if (totalSegs > 0) ((currentSeg * 100) / totalSegs).coerceIn(1, 99) else 50
            val total = expectedBytes.coerceAtLeast(downloadedBytes + 1024L)
            onProgress(TvBridgeDownloadProgress(true, false, downloadedBytes, total, pct, outFile.absolutePath))
        }

        fun closeCleanly() {
            runCatching { fileOutput?.flush() }
            runCatching { fileOutput?.close() }
            fileOutput = null
            if (outFile.exists() && outFile.length() == 0L) {
                runCatching { outFile.delete() }
            }
        }
    }

    fun attach(webView: WebView, streamUrl: String) {
        webViewRef = WeakReference(webView)
        activeStreamUrl = streamUrl
        activeDurationMs = 0L
    }

    fun detach(webView: WebView) {
        if (webViewRef?.get() == webView) {
            abortActiveDownload()
            webViewRef = null
            activeStreamUrl = ""
            activeDurationMs = 0L
        }
    }

    fun canHandleWebStream(url: String): Boolean {
        val wv = webViewRef?.get() ?: return false
        return activeStreamUrl.isNotBlank() && (activeStreamUrl == url || wv.isAttachedToWindow)
    }

    fun abortActiveDownload() {
        val wv = webViewRef?.get()
        if (wv != null) {
            mainHandler.post {
                runCatching { wv.evaluateJavascript("window.omniPvrAbort = true;", null) }
            }
        }
        activeSession?.let { session ->
            session.closeCleanly()
            if (!session.completion.isCompleted) session.completion.complete(false)
        }
        activeSession = null
    }

    suspend fun downloadViaChromium(
        outFile: File,
        expectedBytes: Long,
        onProgress: (TvBridgeDownloadProgress) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val wv = webViewRef?.get() ?: return@withContext false
        val deferred = CompletableDeferred<Boolean>()
        val session = BridgeDownloadSession(outFile, expectedBytes, onProgress, deferred)
        activeSession = session

        val jsCode = """
            (async function() {
              try {
                window.omniPvrAbort = false;
                var urls = [];
                if (window.hls && window.hls.levels && window.hls.levels.length > 0) {
                  var lvlIdx = window.hls.currentLevel >= 0 ? window.hls.currentLevel : 0;
                  var details = window.hls.levels[lvlIdx] && window.hls.levels[lvlIdx].details;
                  if (!details) {
                    for (var i = 0; i < window.hls.levels.length; i++) {
                      if (window.hls.levels[i].details) { details = window.hls.levels[i].details; break; }
                    }
                  }
                  if (details && details.fragments && details.fragments.length > 0) {
                    if (details.initSegment && details.initSegment.url) {
                      urls.push(details.initSegment.url);
                    }
                    for (var j = 0; j < details.fragments.length; j++) {
                      if (details.fragments[j].url) urls.push(details.fragments[j].url);
                    }
                  }
                }
                if (urls.length === 0 && typeof streamUrl === 'string' && streamUrl.length > 0) {
                  var r1 = await fetch(streamUrl);
                  if (!r1.ok) throw new Error('HTTP_' + r1.status);
                  var txt = await r1.text();
                  var lines = txt.split(/\r?\n/).map(function(l){ return l.trim(); }).filter(function(l){ return l.length > 0 && !l.startsWith('#'); });
                  var firstUrl = new URL(lines[0], streamUrl).href;
                  if (firstUrl.indexOf('.m3u8') !== -1 || firstUrl.indexOf('.txt') !== -1 || txt.indexOf('#EXT-X-STREAM-INF') !== -1) {
                    var r2 = await fetch(firstUrl);
                    if (!r2.ok) throw new Error('HTTP_' + r2.status);
                    var txt2 = await r2.text();
                    var lines2 = txt2.split(/\r?\n/).map(function(l){ return l.trim(); }).filter(function(l){ return l.length > 0 && !l.startsWith('#'); });
                    urls = lines2.map(function(u){ return new URL(u, firstUrl).href; });
                  } else {
                    urls = lines.map(function(u){ return new URL(u, streamUrl).href; });
                  }
                }
                if (urls.length === 0) throw new Error('NO_SEGMENTS_FOUND');
                function bufToB64(buffer) {
                  var binary = '';
                  var bytes = new Uint8Array(buffer);
                  var chunk = 32768;
                  for (var k = 0; k < bytes.length; k += chunk) {
                    binary += String.fromCharCode.apply(null, bytes.subarray(k, Math.min(k + chunk, bytes.length)));
                  }
                  return window.btoa(binary);
                }
                for (var idx = 0; idx < urls.length; idx++) {
                  if (window.omniPvrAbort) { window.OmniPvrBridge.onDownloadAborted(); return; }
                  var segResp = await fetch(urls[idx]);
                  if (!segResp.ok) throw new Error('SEG_HTTP_' + segResp.status);
                  var ab = await segResp.arrayBuffer();
                  if (ab.byteLength > 0) {
                    window.OmniPvrBridge.onSegmentBase64(bufToB64(ab), idx + 1, urls.length);
                  }
                }
                window.OmniPvrBridge.onDownloadComplete();
              } catch (err) {
                window.OmniPvrBridge.onDownloadError(err && err.message ? err.message : 'WEB_FETCH_ERROR');
              }
            })();
        """.trimIndent()

        mainHandler.post {
            runCatching { wv.evaluateJavascript(jsCode, null) }.onFailure {
                if (!deferred.isCompleted) deferred.complete(false)
            }
        }

        try {
            val ok = deferred.await()
            if (ok && session.downloadedBytes > 0L) {
                session.closeCleanly()
                onProgress(TvBridgeDownloadProgress(false, true, session.downloadedBytes, session.downloadedBytes, 100, outFile.absolutePath))
                true
            } else {
                session.closeCleanly()
                false
            }
        } finally {
            if (!coroutineContext.isActive) abortActiveDownload()
            activeSession = null
        }
    }

    @JavascriptInterface
    fun onSegmentBase64(b64: String, currentSeg: Int, totalSegs: Int) {
        val session = activeSession ?: return
        try {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            session.writeBytes(bytes, currentSeg, totalSegs)
        } catch (e: Throwable) {
            session.lastError = e.message
        }
    }

    @JavascriptInterface
    fun onDownloadComplete() {
        val session = activeSession ?: return
        session.closeCleanly()
        if (!session.completion.isCompleted) {
            session.completion.complete(session.downloadedBytes > 0L)
        }
    }

    @JavascriptInterface
    fun onDownloadAborted() {
        val session = activeSession ?: return
        session.closeCleanly()
        if (!session.completion.isCompleted) {
            session.completion.complete(session.downloadedBytes > 0L)
        }
    }

    @JavascriptInterface
    fun onDownloadError(msg: String) {
        val session = activeSession ?: return
        com.omnicast.receiver.log.ReceiverLogger.log("TvWebPvrBridge", "⚠️ Error en descarga Chromium PVR: $msg")
        session.lastError = msg
        session.closeCleanly()
        if (!session.completion.isCompleted) {
            session.completion.complete(false)
        }
    }
}
