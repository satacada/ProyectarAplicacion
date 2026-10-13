package com.omnicast.receiver.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.omnicast.receiver.log.ReceiverLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class TvUpdateState {
    data object Idle : TvUpdateState()
    data object Checking : TvUpdateState()
    data class UpdateAvailable(val versionName: String, val versionCode: Int, val downloadUrl: String, val changelog: String) : TvUpdateState()
    data object UpToDate : TvUpdateState()
    data class Downloading(val progressPercent: Int) : TvUpdateState()
    data class ReadyToInstall(val apkFile: File) : TvUpdateState()
    data class Error(val message: String) : TvUpdateState()
}

class TvUpdateManager(private val context: Context) {

    private val _updateState = MutableStateFlow<TvUpdateState>(TvUpdateState.Idle)
    val updateState: StateFlow<TvUpdateState> = _updateState.asStateFlow()

    companion object {
        private const val TAG = "TV_UPDATE"
        private const val VERSION_JSON_URL = "https://raw.githubusercontent.com/satacada/ProyectarAplicacion/main/version.json"
    }

    suspend fun checkForUpdates() = withContext(Dispatchers.IO) {
        _updateState.value = TvUpdateState.Checking
        try {
            ReceiverLogger.log(TAG, "🔍 Consultando actualizaciones en GitHub...")
            val url = URL(VERSION_JSON_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                useCaches = false
                setRequestProperty("User-Agent", "OmniCast-Receiver-OTA")
            }
            if (conn.responseCode !in 200..299) {
                _updateState.value = TvUpdateState.Error("Error HTTP ${conn.responseCode}")
                return@withContext
            }
            val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
            val obj = JSONObject(jsonText)
            val remoteReceiverCode = obj.optInt("versionCodeReceiver", 0)
            val remoteVerName = obj.optString("versionName", "")
            val receiverApkUrl = obj.optString("receiverApkUrl", "")
            val changelog = obj.optString("changelog", "")

            val currentCode = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            } catch (_: Throwable) { 0 }

            ReceiverLogger.log(TAG, "📦 Versión remota: $remoteVerName (code: $remoteReceiverCode) vs local: $currentCode")

            if (remoteReceiverCode > currentCode && receiverApkUrl.isNotBlank()) {
                _updateState.value = TvUpdateState.UpdateAvailable(
                    versionName = remoteVerName,
                    versionCode = remoteReceiverCode,
                    downloadUrl = receiverApkUrl,
                    changelog = changelog
                )
            } else {
                _updateState.value = TvUpdateState.UpToDate
            }
        } catch (e: Throwable) {
            ReceiverLogger.log(TAG, "❌ Error al verificar actualización: ${e.message}")
            _updateState.value = TvUpdateState.Error(e.message ?: "Error de red")
        }
    }

    suspend fun downloadAndInstall(downloadUrl: String) = withContext(Dispatchers.IO) {
        _updateState.value = TvUpdateState.Downloading(0)
        try {
            ReceiverLogger.log(TAG, "⬇️ Descargando APK desde: $downloadUrl")
            var currentUrl = downloadUrl
            var conn: HttpURLConnection
            var redirects = 0
            while (true) {
                val u = URL(currentUrl)
                conn = (u.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 20000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "OmniCast-Receiver-OTA")
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location") ?: break
                    currentUrl = loc
                    redirects++
                    if (redirects > 5) break
                } else {
                    break
                }
            }

            val totalBytes = conn.contentLengthLong
            val apkFile = File(context.cacheDir, "omnicast-receiver-update.apk")
            if (apkFile.exists()) apkFile.delete()

            conn.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var downloaded = 0L
                    var lastPercent = 0
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        if (totalBytes > 0) {
                            val percent = ((downloaded * 100) / totalBytes).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                _updateState.value = TvUpdateState.Downloading(percent)
                            }
                        }
                    }
                    output.flush()
                }
            }

            ReceiverLogger.log(TAG, "✅ Descarga completada: ${apkFile.absolutePath} (${apkFile.length()} bytes)")
            _updateState.value = TvUpdateState.ReadyToInstall(apkFile)
            triggerApkInstall(apkFile)
        } catch (e: Throwable) {
            ReceiverLogger.log(TAG, "❌ Error descargando APK: ${e.message}")
            _updateState.value = TvUpdateState.Error(e.message ?: "Fallo de descarga")
        }
    }

    fun triggerApkInstall(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            ReceiverLogger.log(TAG, "🚀 Instalador lanzado vía FileProvider")
        } catch (e: Throwable) {
            ReceiverLogger.log(TAG, "❌ Error al lanzar instalador: ${e.message}")
            _updateState.value = TvUpdateState.Error("Error al abrir instalador: ${e.message}")
        }
    }
}
