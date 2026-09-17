package com.omnicast.sender.network

import android.util.Log
import com.omnicast.core.protocol.SignalingMessage
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Cliente WebSocket para el emisor móvil.
 * Conecta directamente a la TV Box en la red local (ws://192.168.0.4:8088/ws)
 */
class SignalingClient {

    companion object {
        private const val TAG = "SignalingClient"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp) {
        install(WebSockets)
    }

    private var session: DefaultClientWebSocketSession? = null
    private var clientScope: CoroutineScope? = null

    val isConnected: Boolean
        get() = session != null

    private val _messages = MutableSharedFlow<SignalingMessage>(replay = 0, extraBufferCapacity = 64)
    val messages: SharedFlow<SignalingMessage> = _messages.asSharedFlow()

    fun connect(ipAddress: String, port: Int, onConnected: () -> Unit, onError: (Throwable) -> Unit) {
        clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        clientScope?.launch {
            try {
                val urlString = "ws://$ipAddress:$port/ws"
                Log.i(TAG, "Conectando al WebSocket de la TV Box: $urlString")
                client.webSocket(urlString = urlString) {
                    session = this
                    Log.i(TAG, "Conectado exitosamente con la TV Box")
                    withContext(Dispatchers.Main) { onConnected() }

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            try {
                                val message = json.decodeFromString<SignalingMessage>(text)
                                _messages.emit(message)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deserializando mensaje recibido: $text", e)
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error en la conexión WebSocket con la TV Box", e)
                withContext(Dispatchers.Main) { onError(e) }
            } finally {
                session = null
            }
        }
    }

    fun sendMessage(message: SignalingMessage) {
        clientScope?.launch {
            val s = session
            if (s != null) {
                try {
                    val text = json.encodeToString(message)
                    s.send(Frame.Text(text))
                    Log.d(TAG, "Mensaje enviado a TV: ${message::class.simpleName}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error enviando mensaje por WebSocket", e)
                }
            } else {
                Log.w(TAG, "No hay sesión WebSocket activa para enviar mensaje")
            }
        }
    }

    fun disconnect() {
        clientScope?.launch {
            try {
                session?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error cerrando sesión WebSocket", e)
            } finally {
                session = null
                clientScope?.cancel()
            }
        }
    }
}
