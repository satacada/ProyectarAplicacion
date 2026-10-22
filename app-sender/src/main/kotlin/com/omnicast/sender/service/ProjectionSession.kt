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
    private val permissionData: Intent,
    private var tvScaleMode: String = "EXTEND_16_9",
    private val smoothBuffer: Boolean = true
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webRtcManager: WebRtcSenderManager? = null
    private var audioSender: DigitalAudioSender? = null
    private var audioCaptureManager: AudioCaptureManager? = null
    private var signalingClient: SignalingClient? = null

    fun setTvScaleMode(mode: String) {
        tvScaleMode = mode
        webRtcManager?.tvScaleMode = mode
        val preset = TvZoomManager.PRESETS.firstOrNull { it.id == mode || it.mode == mode }
        if (preset != null) {
            signalingClient?.sendMessage(TvZoomManager.toSignalingMessage(preset))
        } else {
            signalingClient?.sendMessage(SignalingMessage.SetDisplayMode(if (mode == "EXTEND_16_9" || mode == "FILL") "FILL" else "FIT"))
        }
    }

    fun setDisplayMode(mode: String, zoom: Float = 1.0f, panY: Float = 0.0f, panX: Float = 0.0f) {
        tvScaleMode = mode
        webRtcManager?.tvScaleMode = mode
        signalingClient?.sendMessage(SignalingMessage.SetDisplayMode(mode, zoom, panY, panX))
    }

    fun toggleTvScaleMode(): String {
        val next = if (tvScaleMode == "EXTEND_16_9") "ORIGINAL_FIT" else "EXTEND_16_9"
        setTvScaleMode(next)
        return next
    }

    fun getTvScaleMode(): String = tvScaleMode

    fun start() {
        com.omnicast.sender.log.SenderLogger.log("INFO", "PROJECTION_START", "Iniciando proyección a $targetIp:$targetPort (modo: $tvScaleMode)")
        webRtcManager = WebRtcSenderManager(context, permissionData).apply {
            this.tvScaleMode = this@ProjectionSession.tvScaleMode
            initialize()
            startScreenStreaming(profile)
            onIceConnectionClosed = {
                com.omnicast.sender.log.SenderLogger.w("ProjectionSession", "Conexión WebRTC cerrada o fallida en red. Finalizando servicio.")
                ProjectionForegroundService.stopService(context)
            }
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
        val client = SignalingClient()
        signalingClient = client

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

        client.connect(
            ipAddress = targetIp, port = targetPort,
            onConnected = {
                Log.i("ProjectionSession", "Conexión señalización OK con TV. Creando oferta SDP...")
                webRtcManager?.createOffer { offerSdp ->
                    val dims = webRtcManager?.calcCaptureDimensions() ?: Pair(profile.width, profile.height)
                    client.sendMessage(SignalingMessage.Offer(offerSdp, dims.first, dims.second, profile.frameRate, profile.targetBitrateKbps, smoothBuffer))
                    offerSent = true
                    pendingCandidates.forEach { client.sendMessage(it) }
                    pendingCandidates.clear()
                    Log.i("ProjectionSession", "🟢 Oferta SDP y candidatos enviados a TV Box")
                    // CRÍTICO: iniciar captura DESPUÉS de enviar la oferta, no antes
                    // El encoder ya está negociado y puede recibir frames inmediatamente
                    webRtcManager?.beginCapture()
                    val initPreset = TvZoomManager.PRESETS.firstOrNull { it.id == tvScaleMode || it.mode == tvScaleMode }
                    if (initPreset != null) {
                        client.sendMessage(TvZoomManager.toSignalingMessage(initPreset))
                    } else {
                        client.sendMessage(SignalingMessage.SetDisplayMode(if (tvScaleMode == "EXTEND_16_9" || tvScaleMode == "FILL") "FILL" else "FIT"))
                    }
                }
            },
            onError = { e -> com.omnicast.sender.log.SenderLogger.e("ProjectionSession", "Error conectando TV ($targetIp:$targetPort): ${e.message}", e) }
        )

        val oListener = object : android.view.OrientationEventListener(context) {
            private var lastIsLandscape: Boolean? = null
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return
                val isLandscape = (orientation in 60..120 || orientation in 240..300)
                if (isLandscape != lastIsLandscape) {
                    lastIsLandscape = isLandscape
                    val targetMode = if (isLandscape) "FILL" else "FIT"
                    Log.i("ProjectionSession", "📱 Giro de teléfono detectado ($orientation°): enviando modo $targetMode a TV Box")
                    signalingClient?.sendMessage(SignalingMessage.SetDisplayMode(targetMode))
                }
            }
        }
        if (oListener.canDetectOrientation()) oListener.enable()
        this.orientationListener = oListener
    }

    private var orientationListener: android.view.OrientationEventListener? = null

    fun stop() {
        try {
            orientationListener?.disable()
            orientationListener = null
            audioCaptureManager?.stopAudioCapture()
            audioCaptureManager = null
            audioSender?.close()
            audioSender = null

            signalingClient?.sendMessage(SignalingMessage.Disconnect())
            signalingClient?.disconnect()
            signalingClient = null

            webRtcManager?.close()
            webRtcManager = null
            scope.cancel()
        } catch (e: Throwable) {
            Log.e("ProjectionSession", "Error cerrando sesión", e)
        }
    }
}
