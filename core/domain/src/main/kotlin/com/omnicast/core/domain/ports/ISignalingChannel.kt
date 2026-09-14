package com.omnicast.core.domain.ports

import kotlinx.coroutines.flow.SharedFlow

/**
 * Puerto para el canal de señalización local (WebSocket).
 */
interface ISignalingChannel {
    val incomingMessages: SharedFlow<String>
    
    suspend fun connect(targetIp: String, port: Int)
    suspend fun sendMessage(messageJson: String)
    suspend fun disconnect()
}
