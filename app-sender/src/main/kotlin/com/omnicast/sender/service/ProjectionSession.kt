package com.omnicast.sender.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.omnicast.core.domain.models.StreamProfile
import com.omnicast.core.protocol.SignalingMessage
import com.omnicast.sender.audio.DigitalAudioSender
import com.omnicast.sender.capture.AudioCaptureManager
import com.omnicast.sender.network.SignalingClient
import com.omnicast.sender.network.SignalingClientHolder
import com.omnicast.sender.webrtc.WebRtcSenderManager
import kotlinx.coroutines.*

/**
 * Sesión activa de proyección WebRTC con video por hardware y audio digital interno UDP (<25ms).
 * Cero micrófono, cero acoplamiento acústico, música y diálogos en estéreo 48kHz.
 */
class ProjectionSession(
    private val context: Context,
    private val targetIp: String,
    private val targetPort: Int,
    private val profile: StreamProfile,
    private val permissionData: Intent
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webRtcManager: WebRtcSenderManager? = null
    private var audioSender: DigitalAudioSender? = null
    private var audioCaptureManager: AudioCaptureManager? = null

    fun start() {
        Log.i("ProjectionSession", "Iniciando proyección en segundo plano a $targetIp:$targetPort")
        webRtcManager = WebRtcSenderManager(context, permissionData).apply {
            initialize()
            startScreenStreaming(profile)
        }

        audioSender = DigitalAudioSender(targetIp, 8089)
        scope.launch {
            delay(600)
            val mp = webRtcManager?.getMediaProjection()
            if (mp != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    audioCaptureManager = AudioCaptureManager(mp) { data, len ->
                        audioSender?.sendAudioChunk(data, len)
                    }.apply {
                        startAudioCapture(48000, true)
                    }
                    Log.i("ProjectionSession", "🟢 Captura de audio digital interno iniciada a 48kHz estéreo")
                } catch (e: Throwable) {
                    Log.w("ProjectionSession", "AudioCapture: ${e.message}")
                }
            }
        }

        var offerSent = false
        val pendingCandidates = mutableListOf<SignalingMessage.IceCandidate>()
        val client = SignalingClientHolder.get()

        webRtcManager?.onLocalIceCandidate = { c ->
            val msg = SignalingMessage.IceCandidate(c.sdpMid, c.sdpMLineIndex, c.sdp)
            if (client.isConnected && offerSent) {
                client.sendMessage(msg)
            } else {
                pendingCandidates.add(msg)
            }
        }

        scope.launch {
            client.messages.collect { msg ->
                when (msg) {
                    is SignalingMessage.Answer -> {
                        Log.i("ProjectionSession", "🟢 Answer SDP recibido de TV Box. Conectando WebRTC...")
                        webRtcManager?.handleAnswer(msg.sdp)
                    }
                    is SignalingMessage.IceCandidate -> webRtcManager?.addRemoteIceCandidate(msg.sdpMid, msg.sdpMLineIndex, msg.candidate)
                    else -> Unit
                }
            }
        }

        fun doCreateOffer() {
            Log.i("ProjectionSession", "Creando oferta SDP para TV...")
            webRtcManager?.createOffer { offerSdp ->
                client.sendMessage(SignalingMessage.Offer(offerSdp, profile.width, profile.height, profile.frameRate, profile.targetBitrateKbps))
                offerSent = true
                pendingCandidates.forEach { client.sendMessage(it) }
                pendingCandidates.clear()
                Log.i("ProjectionSession", "🟢 Oferta SDP y candidatos enviados a TV Box")
            }
        }

        if (client.isConnected) {
            doCreateOffer()
        } else {
            client.connect(
                ipAddress = targetIp, port = targetPort,
                onConnected = { doCreateOffer() },
                onError = { e -> Log.e("ProjectionSession", "Error conectando TV ($targetIp:$targetPort): ${e.message}") }
            )
        }
    }

    fun stop() {
        try {
            audioCaptureManager?.stopAudioCapture()
            audioCaptureManager = null
            audioSender?.close()
            audioSender = null

            SignalingClientHolder.get().sendMessage(SignalingMessage.Disconnect())
            webRtcManager?.close()
            webRtcManager = null
            scope.cancel()
        } catch (e: Throwable) {
            Log.e("ProjectionSession", "Error cerrando sesión", e)
        }
    }
}
