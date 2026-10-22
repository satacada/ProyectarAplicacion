package com.omnicast.receiver.webrtc

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.omnicast.receiver.log.ReceiverLogger
import org.webrtc.*

class WebRtcReceiverManager(private val context: Context) {

    companion object { private const val TAG = "WebRtcReceiverManager" }

    private val eglBase = EglBase.create()
    val eglContext: EglBase.Context get() = eglBase.eglBaseContext

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    var surfaceViewRenderer: SurfaceViewRenderer? = null; private set
    var currentVideoTrack: VideoTrack? = null; private set

    var onLocalIceCandidate: ((IceCandidate) -> Unit)? = null
    var onVideoTrackReceived: ((VideoTrack) -> Unit)? = null
    var onConnectionClosed: (() -> Unit)? = null
    private val queuedRemoteCandidates = mutableListOf<IceCandidate>()
    private var isRemoteDescriptionSet = false
    private var isResetting = false
    private var currentScalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT
    var currentMode: String by mutableStateOf("FIT"); private set
    var currentZoom: Float by mutableStateOf(1.0f); private set
    private var currentPanY = 0.0f
    private var currentPanX = 0.0f
    var lastFrameW: Int by mutableIntStateOf(0); private set
    var lastFrameH: Int by mutableIntStateOf(0); private set
    var lastRot: Int by mutableIntStateOf(0); private set
    var isPhoneLandscape: Boolean by mutableStateOf(false); private set

    val isRotated: Boolean get() = (lastRot == 90 || lastRot == 270)
    val effectiveFrameW: Int get() = if (isRotated) lastFrameH else lastFrameW
    val effectiveFrameH: Int get() = if (isRotated) lastFrameW else lastFrameH

    val isLandscape: Boolean
        get() = isPhoneLandscape || (effectiveFrameW > 0 && effectiveFrameH > 0 && effectiveFrameW > effectiveFrameH)

    fun isVertical(res: String = ""): Boolean {
        if (isLandscape) return false
        if (effectiveFrameW > 0 && effectiveFrameH > 0) return effectiveFrameW < effectiveFrameH
        val p = res.split("x"); val w = p.getOrNull(0)?.toIntOrNull() ?: 0; val h = p.getOrNull(1)?.toIntOrNull() ?: 0
        return w in 1..<h
    }

    fun streamAspect(res: String = ""): Float {
        if (effectiveFrameW > 0 && effectiveFrameH > 0) return effectiveFrameW.toFloat() / effectiveFrameH.toFloat()
        val p = res.split("x"); val h = p.getOrNull(1)?.toFloatOrNull() ?: 16f
        return if (h > 0f) (p.getOrNull(0)?.toFloatOrNull() ?: 9f) / h else (9f / 16f)
    }

    fun initialize() {
        ReceiverLogger.log(TAG, "Inicializando decodificador WebRTC por hardware en TV Box (v3.25)")
        setSmoothBuffer(true)
        val initOptions = PeerConnectionFactory.InitializationOptions.builder(context).setEnableInternalTracer(true).createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, false, false))
            .createPeerConnectionFactory()

        initSurfaceView()
        createPeerConnection()
    }

    private fun initSurfaceView() {
        surfaceViewRenderer = SurfaceViewRenderer(context).apply {
            init(eglBase.eglBaseContext, object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() { ReceiverLogger.log(TAG, "🎉 ¡PRIMER FRAME RENDERIZADO EN TV!") }
                override fun onFrameResolutionChanged(w: Int, h: Int, r: Int) {
                    ReceiverLogger.log(TAG, "📐 Frame TV: ${w}x${h} rot:$r")
                    lastFrameW = w; lastFrameH = h; lastRot = r
                    applyDisplayTransform()
                }
            })
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            setEnableHardwareScaler(false)
            setZOrderMediaOverlay(true)
        }
    }

    private fun applyDisplayTransform() {
        val renderer = surfaceViewRenderer ?: return
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val isRot = (lastRot == 90 || lastRot == 270)
            val frameW = if (isRot) lastFrameH else lastFrameW
            val frameH = if (isRot) lastFrameW else lastFrameH
            if (frameW <= 0 || frameH <= 0) return@post
            val land = isLandscape || (frameW > frameH)

            if (renderer.width > 0 && renderer.height > 0) {
                renderer.pivotX = renderer.width / 2f
                renderer.pivotY = renderer.height / 2f
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                renderer.resetPivot()
            }

            if (!land) {
                // Proyección Vertical: 100% altura TV, recorte fino de status/nav bar (1.075x) sin deformación lateral
                renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                val baseScale = 1.075f
                val scale = when (currentMode) {
                    "ZOOM" -> currentZoom * baseScale
                    "FILL", "16:9", "EXTEND_16_9" -> 1.35f
                    else -> baseScale // FIT: 100% altura TV, sin barra superior ni inferior
                }
                renderer.scaleX = scale; renderer.scaleY = scale
                renderer.translationX = if (currentMode == "ZOOM") currentPanX else 0f
                renderer.translationY = if (currentMode == "ZOOM") currentPanY else 0f
                ReceiverLogger.log(TAG, "📱 Vertical TV: modo=$currentMode escala=$scale centrado (frame=${frameW}x${frameH} rot=$lastRot)")
            } else {
                // Proyección Horizontal: 16:9 pantalla completa de borde a borde con SCALE_ASPECT_FILL
                renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                val scale = if (currentMode == "ZOOM") currentZoom else 1.0f
                renderer.scaleX = scale; renderer.scaleY = scale
                renderer.translationX = if (currentMode == "ZOOM") currentPanX else 0f
                renderer.translationY = if (currentMode == "ZOOM") currentPanY else 0f
                ReceiverLogger.log(TAG, "📺 Horizontal TV 16:9: modo=$currentMode escala=$scale (frame=${frameW}x${frameH} rot=$lastRot)")
            }
        }
    }

    private fun createPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(emptyList()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceCandidatePoolSize = 2
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                ReceiverLogger.log(TAG, "Estado de conexión ICE en TV: $newState")
                if (!isResetting && newState == PeerConnection.IceConnectionState.FAILED) {
                    onConnectionClosed?.invoke()
                }
            }
            override fun onIceCandidate(candidate: IceCandidate?) { candidate?.let { onLocalIceCandidate?.invoke(it) } }
            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                val track = receiver?.track()
                if (track is VideoTrack) {
                    ReceiverLogger.log(TAG, "🟢 VideoTrack recibido en TV Box (${track.id()}).")
                    track.setEnabled(true); currentVideoTrack = track
                    surfaceViewRenderer?.let { track.addSink(it) }
                    onVideoTrackReceived?.invoke(track)
                } else if (track is AudioTrack) {
                    track.setEnabled(true); track.setVolume(1.0)
                }
            }
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) = Unit
            override fun onIceConnectionReceivingChange(p0: Boolean) = Unit
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) = Unit
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) = Unit
            override fun onAddStream(p0: MediaStream?) = Unit; override fun onRemoveStream(p0: MediaStream?) = Unit
            override fun onDataChannel(p0: DataChannel?) = Unit; override fun onRenegotiationNeeded() = Unit
        })
    }

    fun handleOffer(offerSdp: String, onAnswerCreated: (String) -> Unit) {
        isRemoteDescriptionSet = false; queuedRemoteCandidates.clear()
        val remoteDesc = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                isRemoteDescriptionSet = true
                queuedRemoteCandidates.forEach { candidate -> peerConnection?.addIceCandidate(candidate) }
                queuedRemoteCandidates.clear()
                createAnswer(onAnswerCreated)
            }
            override fun onSetFailure(error: String?) { ReceiverLogger.log(TAG, "Error RemoteDescription: $error") }
            override fun onCreateSuccess(p0: SessionDescription?) = Unit; override fun onCreateFailure(p0: String?) = Unit
        }, remoteDesc)
    }

    private fun createAnswer(onAnswerCreated: (String) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() { onAnswerCreated(it.description) }
                        override fun onSetFailure(e: String?) { ReceiverLogger.log(TAG, "Error LocalDescription: $e") }
                        override fun onCreateSuccess(p0: SessionDescription?) = Unit; override fun onCreateFailure(p0: String?) = Unit
                    }, it)
                }
            }
            override fun onCreateFailure(error: String?) { ReceiverLogger.log(TAG, "Error Answer: $error") }
            override fun onSetSuccess() = Unit; override fun onSetFailure(p0: String?) = Unit
        }, constraints)
    }

    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
        if (isRemoteDescriptionSet) peerConnection?.addIceCandidate(iceCandidate) else queuedRemoteCandidates.add(iceCandidate)
    }

    fun toggleScalingType(): RendererCommon.ScalingType {
        val willBeFill = (currentScalingType != RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        currentScalingType = if (willBeFill) RendererCommon.ScalingType.SCALE_ASPECT_FILL else RendererCommon.ScalingType.SCALE_ASPECT_FIT
        currentMode = if (willBeFill) "FILL" else "FIT"
        currentZoom = 1.0f
        currentPanX = 0f
        currentPanY = 0f
        applyDisplayTransform()
        return currentScalingType
    }

    fun setDisplayMode(mode: String, zoom: Float = 1f, panY: Float = 0f, panX: Float = 0f, isLandscapeParam: Boolean? = null) {
        currentMode = mode.uppercase(); currentZoom = zoom; currentPanY = panY; currentPanX = panX
        if (isLandscapeParam != null) {
            isPhoneLandscape = isLandscapeParam
        }
        val land = isLandscape
        currentScalingType = if (land) RendererCommon.ScalingType.SCALE_ASPECT_FILL else RendererCommon.ScalingType.SCALE_ASPECT_FIT
        when (currentMode) {
            "FILL", "16:9", "EXTEND_16_9" -> {
                if (land) {
                    currentZoom = 1.0f; currentPanY = 0.0f; currentPanX = 0.0f
                }
            }
            "FIT", "ORIGINAL_FIT" -> {
                currentZoom = 1.0f; currentPanY = 0.0f; currentPanX = 0.0f
            }
            "TOGGLE" -> toggleScalingType()
        }
        applyDisplayTransform()
    }

    fun setSmoothBuffer(enabled: Boolean) {
        val trial = if (enabled) "WebRTC-ForcePlayoutDelay/min_ms:100,max_ms:250/" else "WebRTC-ForcePlayoutDelay/min_ms:0,max_ms:50/"
        try {
            PeerConnectionFactory.initializeFieldTrials(trial)
            ReceiverLogger.log(TAG, "🛡️ Buffer Anti-Tirones en TV: ${if (enabled) "ACTIVADO (100ms)" else "DESACTIVADO (0ms)"}")
        } catch (e: Throwable) { ReceiverLogger.log(TAG, "Buffer warning: ${e.message}") }
    }

    fun resetConnection() {
        isResetting = true
        try { peerConnection?.close() } catch (e: Exception) { ReceiverLogger.log(TAG, "Error reset", e) }
        currentVideoTrack = null; isRemoteDescriptionSet = false; queuedRemoteCandidates.clear()
        createPeerConnection()
        isResetting = false
    }

    fun release() {
        try {
            surfaceViewRenderer?.release(); surfaceViewRenderer = null
            currentVideoTrack = null
            peerConnection?.close(); peerConnection = null
            peerConnectionFactory?.dispose(); peerConnectionFactory = null
            eglBase.release()
            isRemoteDescriptionSet = false; queuedRemoteCandidates.clear()
        } catch (e: Exception) { ReceiverLogger.log(TAG, "Error liberando WebRtcReceiverManager", e) }
    }
}
