package com.omnicast.sender.audio

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Transmisor UDP de audio digital interno de ultra-baja latencia (<5ms).
 * Envía paquetes PCM de 48kHz estéreo directamente al puerto 8089 de la TV Box.
 */
class DigitalAudioSender(
    private val targetIp: String,
    private val targetPort: Int = 8089
) {
    companion object {
        private const val TAG = "DigitalAudioSender"
    }

    private var socket: DatagramSocket? = null
    private var targetAddress: InetAddress? = null

    init {
        try {
            socket = DatagramSocket()
            targetAddress = InetAddress.getByName(targetIp)
            Log.i(TAG, "Transmisor de audio digital listo hacia $targetIp:$targetPort")
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando socket UDP de audio", e)
        }
    }

    fun sendAudioChunk(data: ByteArray, length: Int) {
        val dest = targetAddress ?: return
        val sock = socket ?: return
        try {
            val packet = DatagramPacket(data, length, dest, targetPort)
            sock.send(packet)
        } catch (e: Exception) {
            // Ignorar pérdidas esporádicas en Wi-Fi para no bloquear el hilo de captura
        }
    }

    fun close() {
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        targetAddress = null
        Log.i(TAG, "Transmisor de audio cerrado")
    }
}
