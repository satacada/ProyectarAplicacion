package com.omnicast.core.domain.ports

import com.omnicast.core.domain.models.StreamProfile
import kotlinx.coroutines.flow.StateFlow

/**
 * Puerto para el canal de transporte WebRTC P2P en red local.
 */
interface IP2PTransport {
    val connectionState: StateFlow<P2PConnectionState>
    
    fun initialize()
    fun createOffer(onSdpCreated: (String) -> Unit)
    fun handleAnswer(sdpAnswer: String)
    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidate: String)
    fun setBitrate(targetBitrateKbps: Int)
    fun close()
}

enum class P2PConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED
}
