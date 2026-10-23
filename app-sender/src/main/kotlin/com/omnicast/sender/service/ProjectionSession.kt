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
    private var tvScaleMode: String = "FIT",
    private val smoothBuffer: Boolean = true
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webRtcManager: WebRtcSenderManager? = null
    private var audioSender: DigitalAudioSender? = null
    private var audioCaptureManager: AudioCaptureManager? = null
    private var signalingClient: SignalingClient? = null
    var isDeviceLandscape: Boolean = false; private set

    fun setTvScaleMode(mode: String) {
        tvScaleMode = mode
        webRtcManager?.tvScaleMode = mode
        val preset = TvZoomManager.findPreset(mode)
        signalingClient?.sendMessage(TvZoomManager.toSignalingMessage(preset, isLandscape = isDeviceLandscape))
    }

    fun setDisplayMode(mode: String, zoom: Float = 1.0f, panY: Float = 0.0f, panX: Float = 0.0f) {
        tvScaleMode = mode
        webRtcManager?.tvScaleMode = mode
        signalingClient?.sendMessage(SignalingMessage.SetDisplayMode(mode, zoom, panY, panX, isLandscape = isDeviceLandscape))
    }

    fun toggleTvScaleMode(): String {
        val next = if (tvScaleMode == "FILL_16_9" || tvScaleMode == "FILL") "FIT" else "FILL_16_9"
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
                    val initOrientation = context.resources.configuration.orientation
                    isDeviceLandscape = (initOrientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE)
                    val isLandscape = if (tvScaleMode == "APP") null else isDeviceLandscape
                    val initPreset = TvZoomManager.findPreset(tvScaleMode)
                    client.sendMessage(TvZoomManager.toSignalingMessage(initPreset, isLandscape = isLandscape))
                }
            },
            onError = { e -> com.omnicast.sender.log.SenderLogger.e("ProjectionSession", "Error conectando TV ($targetIp:$targetPort): ${e.message}", e) }
        )

        val configCallback = object : android.content.ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
                if (tvScaleMode == "APP") return
                val isLand = (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE)
                if (isLand != isDeviceLandscape) {
                    isDeviceLandscape = isLand
                    val targetMode = if (isLand) "FILL" else "FIT"
                    Log.i("ProjectionSession", "📱 Giro real de imagen en celular: modo $targetMode (landscape=$isLand)")
                    signalingClient?.sendMessage(SignalingMessage.SetDisplayMode(targetMode, isLandscape = isLand))
                }
            }
            override fun onLowMemory() = Unit
        }
        context.registerComponentCallbacks(configCallback)
        this.componentCallbacks = configCallback
    }

    private var componentCallbacks: android.content.ComponentCallbacks? = null

    fun stop() {
        try {
            componentCallbacks?.let { context.unregisterComponentCallbacks(it) }
            componentCallbacks = null
            audioCaptureManager?.stopAudioCapture()
            audioCaptureManager = null
            audioSender?.close()
            audioSender = null

            signalingClient?.sendMessage(SignalingMessage.PlayControl("STOP"))
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
