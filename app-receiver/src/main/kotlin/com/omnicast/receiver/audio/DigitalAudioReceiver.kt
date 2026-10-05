package com.omnicast.receiver.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket

/**
 * Receptor de audio digital interno de ultra-baja latencia (<25ms) para la TV Box.
 * Recibe flujos PCM de 48kHz estéreo vía UDP en el puerto 8089 y los reproduce con AudioTrack nativo.
 */
class DigitalAudioReceiver(private val port: Int = 8089) {

    companion object {
        private const val TAG = "DigitalAudioReceiver"
        private const val SAMPLE_RATE = 48000
    }

    @Volatile
    private var isRunning = false
    private var socket: DatagramSocket? = null
    private var audioTrack: AudioTrack? = null
    private var receiverThread: Thread? = null

    fun start() {
        if (isRunning) return
        isRunning = true

        try {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufSize = (minBuf * 2).coerceAtLeast(4096)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build().apply { play() }

            receiverThread = Thread {
                try {
                    socket = DatagramSocket(port)
                    Log.i(TAG, "🟢 Receptor de audio digital escuchando en UDP :$port")
                    val buffer = ByteArray(2048)
                    while (isRunning && !Thread.currentThread().isInterrupted) {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket?.receive(packet)
                        if (packet.length > 0) {
                            audioTrack?.write(packet.data, packet.offset, packet.length)
                        }
                    }
                } catch (e: Exception) {
                    if (isRunning) Log.w(TAG, "Excepción socket UDP audio: ${e.message}")
                }
            }.apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error iniciando DigitalAudioReceiver", e)
        }
    }

    fun stop() {
        isRunning = false
        receiverThread?.interrupt()
        receiverThread = null
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        Log.i(TAG, "Receptor de audio digital detenido")
    }
}
