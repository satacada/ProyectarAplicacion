package com.omnicast.sender.network

/**
 * Gestor Singleton del cliente de señalización WebSocket móvil.
 * Garantiza una única conexión compartida y estable con la TV Box,
 * evitando colisiones entre SenderMainActivity y ProjectionSession.
 */
object SignalingClientHolder {
    private var instance: SignalingClient? = null

    @Synchronized
    fun get(): SignalingClient {
        return instance ?: SignalingClient().also { instance = it }
    }

    @Synchronized
    fun reset() {
        instance?.disconnect()
        instance = null
    }
}
