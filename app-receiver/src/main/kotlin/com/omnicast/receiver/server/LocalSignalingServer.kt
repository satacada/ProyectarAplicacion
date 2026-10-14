package com.omnicast.receiver.server

import android.util.Log
import com.omnicast.core.protocol.SignalingMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress

sealed class ClientStatus {
    object Disconnected : ClientStatus()
    data class Connected(val clientAddress: String) : ClientStatus()
}

/**
 * Servidor WebSocket local embebido en la TV Box basado en Java-WebSocket.
 * Robusto, ultraligero (100 KB) con reporte de estado de conexión en vivo.
 */
class LocalSignalingServer(private val port: Int = 8088) {

    companion object {
        private const val TAG = "LocalSignalingServer"
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _incomingMessages = MutableSharedFlow<SignalingMessage>(replay = 32, extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<SignalingMessage> = _incomingMessages.asSharedFlow()

    private val _clientStatus = MutableStateFlow<ClientStatus>(ClientStatus.Disconnected)
    val clientStatus: StateFlow<ClientStatus> = _clientStatus.asStateFlow()

    private var activeConnection: WebSocket? = null
    private var internalServer: InternalWebSocketServer? = null

    fun start() {
        if (internalServer != null) return

        try {
            val address = InetSocketAddress("0.0.0.0", port)
            internalServer = InternalWebSocketServer(address).apply {
                isReuseAddr = true
                isTcpNoDelay = true
                connectionLostTimeout = 10
                start()
            }
            Log.i(TAG, "Servidor de señalización Java-WebSocket iniciado en 0.0.0.0:$port")
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando servidor en puerto $port", e)
        }
    }

    suspend fun sendMessage(message: SignalingMessage) {
        val text = json.encodeToString(message)
        val currentConn = activeConnection
        var sent = false
        if (currentConn != null && currentConn.isOpen) {
            try {
                currentConn.send(text)
                sent = true
                Log.d(TAG, "Mensaje enviado a cliente activo: ${message::class.simpleName}")
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando a cliente activo", e)
            }
        }
        internalServer?.connections?.filter { it.isOpen && it != currentConn }?.forEach { conn ->
            try {
                conn.send(text)
                sent = true
                Log.d(TAG, "Mensaje broadcast a conexión secundaria: ${message::class.simpleName}")
            } catch (e: Exception) {
                Log.w(TAG, "Error broadcast", e)
            }
        }
        if (!sent) {
            Log.w(TAG, "No hay cliente móvil conectado para enviar mensaje")
        }
    }

    fun stop() {
        try {
            activeConnection?.close()
            activeConnection = null
            _clientStatus.value = ClientStatus.Disconnected
            internalServer?.stop(1000)
            internalServer = null
            Log.i(TAG, "Servidor de señalización detenido")
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo servidor", e)
        }
    }

    private inner class InternalWebSocketServer(address: InetSocketAddress) : WebSocketServer(address) {

        override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
            activeConnection = conn
            val addr = conn.remoteSocketAddress.toString().removePrefix("/")
            _clientStatus.value = ClientStatus.Connected(addr)
            Log.i(TAG, "Cliente móvil conectado desde: $addr")
        }

        override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
            if (activeConnection == conn) {
                activeConnection = connections.firstOrNull { it.isOpen && it != conn }
            }
            if (activeConnection == null) {
                _clientStatus.value = ClientStatus.Disconnected
            }
            Log.i(TAG, "Cliente móvil desconectado (código: $code, razón: $reason)")
        }

        override fun onMessage(conn: WebSocket, message: String) {
            activeConnection = conn
            if (_clientStatus.value is ClientStatus.Disconnected) {
                val addr = conn.remoteSocketAddress.toString().removePrefix("/")
                _clientStatus.value = ClientStatus.Connected(addr)
            }
            try {
                val signalingMessage = json.decodeFromString<SignalingMessage>(message)
                _incomingMessages.tryEmit(signalingMessage)
                Log.d(TAG, "Mensaje recibido: ${signalingMessage::class.simpleName}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deserializando mensaje: $message", e)
            }
        }

        override fun onError(conn: WebSocket?, ex: Exception) {
            Log.e(TAG, "Error en WebSocketServer: ${ex.message}", ex)
        }

        override fun onStart() {
            Log.i(TAG, "WebSocketServer listo y escuchando en puerto $port")
        }
    }
}
