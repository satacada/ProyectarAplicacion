package com.omnicast.receiver.webrtc

import android.content.Context
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
    var currentMode: String = "FIT"; private set
    private var currentZoom = 1.0f
    private var currentPanY = 0.0f
    private var currentPanX = 0.0f
    var lastFrameW: Int = 0; private set
    var lastFrameH: Int = 0; private set
    private var lastRot = 0

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
            setEnableHardwareScaler(true)
            setZOrderMediaOverlay(true)
        }
    }

    private fun applyDisplayTransform() {
        val renderer = surfaceViewRenderer ?: return
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val isRotated = (lastRot == 90 || lastRot == 270)
            val frameW = if (isRotated) lastFrameH else lastFrameW
            val frameH = if (isRotated) lastFrameW else lastFrameH
            if (frameW <= 0 || frameH <= 0) return@post

            val tvMetrics = context.resources.displayMetrics
            val tvW = tvMetrics.widthPixels.toFloat().coerceAtLeast(1280f)
            val tvH = tvMetrics.heightPixels.toFloat().coerceAtLeast(720f)
            val tvAspect = tvW / tvH
            val frameAspect = frameW.toFloat() / frameH.toFloat()
            val isLandscape = frameW > frameH

            // Si el frame es horizontal o tiene relación de aspecto apaisada (ej. 20:9 vs 16:9),
            // calculamos el factor de llenado para expandir la imagen y eliminar márgenes negros
            val aspectFillFactor = if (isLandscape) {
                if (frameAspect > tvAspect) (frameAspect / tvAspect) else (tvAspect / frameAspect)
            } else {
                1.0f
            }

            val targetScalingType = if (!isLandscape) {
                RendererCommon.ScalingType.SCALE_ASPECT_FIT
            } else {
                currentScalingType
            }
            renderer.setScalingType(targetScalingType)

            when (currentMode) {
                "ZOOM" -> {
                    val scale = if (isLandscape && aspectFillFactor > 1.0f) currentZoom * aspectFillFactor else currentZoom
                    renderer.scaleX = scale
                    renderer.scaleY = scale
                    renderer.translationX = currentPanX
                    renderer.translationY = currentPanY
                    ReceiverLogger.log(TAG, "🔍 Zoom gradual TV: ${String.format(java.util.Locale.US, "%.2f", currentZoom)}x -> escala $scale (panX=$currentPanX, panY=$currentPanY, frame=${frameW}x${frameH})")
                }
                "FILL", "16:9", "EXTEND_16_9" -> {
                    val factor = if (isLandscape) aspectFillFactor else 1.0f
                    renderer.scaleX = factor
                    renderer.scaleY = factor
                    renderer.translationX = 0f
                    renderer.translationY = 0f
                    ReceiverLogger.log(TAG, "📺 Modo 16:9 (FILL): factor=$factor frame=${frameW}x${frameH} rot=$lastRot")
                }
                else -> {
                    val baseScale = if (currentZoom > 0f) currentZoom else 1.0f
                    val scale = if (isLandscape && currentZoom <= 1.0f && aspectFillFactor > 1.0f) {
                        aspectFillFactor
                    } else if (isLandscape && aspectFillFactor > 1.0f && currentZoom > 1.0f) {
                        baseScale * aspectFillFactor
                    } else {
                        baseScale
                    }
                    renderer.scaleX = scale
                    renderer.scaleY = scale
                    renderer.translationX = 0f
                    renderer.translationY = currentPanY
                    ReceiverLogger.log(TAG, "📱 Modo FIT: escala $scale (frame=${frameW}x${frameH} rot=$lastRot)")
                }
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
        currentScalingType = if (currentScalingType == RendererCommon.ScalingType.SCALE_ASPECT_FIT) {
            RendererCommon.ScalingType.SCALE_ASPECT_FILL
        } else {
            RendererCommon.ScalingType.SCALE_ASPECT_FIT
        }
        applyDisplayTransform()
        return currentScalingType
    }

    fun setDisplayMode(mode: String, zoom: Float = 1f, panY: Float = 0f, panX: Float = 0f) {
        currentMode = mode.uppercase()
        currentZoom = zoom
        currentPanY = panY
        currentPanX = panX
        when (currentMode) {
            "FILL", "16:9", "EXTEND_16_9" -> {
                currentScalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL
            }
            "FIT", "ORIGINAL_FIT" -> {
                currentScalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT
                currentZoom = 1.0f
                currentPanY = 0.0f
                currentPanX = 0.0f
            }
            "ZOOM" -> {
                currentScalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT
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
