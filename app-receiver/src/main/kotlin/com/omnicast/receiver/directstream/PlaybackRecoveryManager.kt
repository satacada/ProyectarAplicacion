package com.omnicast.receiver.directstream

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer

/**
 * Gestor de resiliencia y recuperación de reproducción inspirado en PlaybackRecoveryDecision de Seekee.
 * Maneja silenciosamente BehindLiveWindowException, caídas de red y reintentos automáticos con backoff.
 */
class PlaybackRecoveryManager {

    companion object {
        private const val TAG = "PlaybackRecovery"
        private const val MAX_RETRIES = 3
    }

    private var retryCount = 0
    private var isRecovering = false
    private val mainHandler = Handler(Looper.getMainLooper())

    fun onPlaybackReady() {
        retryCount = 0
        isRecovering = false
    }

    fun handlePlaybackError(
        error: PlaybackException,
        player: ExoPlayer?,
        onRetryCleanStream: () -> Unit
    ): Boolean {
        if (player == null) return false
        if (isRecovering) {
            Log.w(TAG, "Ya hay un proceso de recuperación activo, omitiendo evento duplicado")
            return true
        }

        val code = error.errorCode
        Log.w(TAG, "Evaluando recuperación para error [${error.errorCodeName}] ($code): ${error.message}")

        // 1. Caso HLS Desfase de Ventana en Vivo (BehindLiveWindowException)
        if (code == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            Log.i(TAG, "Recuperando BehindLiveWindowException: reenganchando al vivo en posición por defecto")
            isRecovering = true
            mainHandler.post {
                try {
                    player.seekToDefaultPosition()
                    player.prepare()
                    player.playWhenReady = true
                } catch (t: Throwable) {
                    Log.e(TAG, "Fallo al reenganchar vivo: ${t.message}")
                } finally {
                    isRecovering = false
                }
            }
            return true
        }

        // 2. Errores de Conexión de Red Local o Remota (Timeout, Caída transitoria)
        val isNetworkTimeout = code == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                code == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT

        if (isNetworkTimeout && retryCount < MAX_RETRIES) {
            retryCount++
            val backoffMs = (1000L * retryCount).coerceAtMost(4000L)
            Log.i(TAG, "Reintentando conexión ($retryCount/$MAX_RETRIES) en ${backoffMs}ms...")
            isRecovering = true
            mainHandler.postDelayed({
                try {
                    val pos = player.currentPosition
                    player.prepare()
                    if (pos > 0) player.seekTo(pos)
                    player.playWhenReady = true
                } catch (t: Throwable) {
                    Log.e(TAG, "Fallo en reintento con backoff: ${t.message}")
                } finally {
                    isRecovering = false
                }
            }, backoffMs)
            return true
        }

        // 3. Fallo de Decodificador o Cabeceras Prohibidas (HTTP 403 / BAD_STATUS)
        if (code == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS || retryCount >= MAX_RETRIES || code == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED) {
            Log.i(TAG, "Activando resolución/curación inmediata de flujo (HTTP status / decoder)...")
            onRetryCleanStream()
            return true
        }

        return false
    }

    fun reset() {
        retryCount = 0
        isRecovering = false
        mainHandler.removeCallbacksAndMessages(null)
    }
}
