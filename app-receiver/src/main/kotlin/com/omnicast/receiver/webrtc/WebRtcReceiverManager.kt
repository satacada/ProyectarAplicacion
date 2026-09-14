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

    fun initialize() {
        ReceiverLogger.log(TAG, "Inicializando decodificador WebRTC por hardware en TV Box")
        val initOptions = PeerConnectionFactory.InitializationOptions.builder(context).setEnableInternalTracer(true).createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, false, false))
            .createPeerConnectionFactory()

        surfaceViewRenderer = SurfaceViewRenderer(context).apply {
            init(eglBase.eglBaseContext, object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() { ReceiverLogger.log(TAG, "🎉 ¡PRIMER FRAME RENDERIZADO EN TV!") }
                override fun onFrameResolutionChanged(w: Int, h: Int, r: Int) { ReceiverLogger.log(TAG, "📐 Frame TV: ${w}x${h} rot:$r") }
            })
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            setEnableHardwareScaler(true)
            setFpsReduction(0f)
            setZOrderMediaOverlay(true)
        }
        createPeerConnection()
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
        currentScalingType = if (currentScalingType == RendererCommon.ScalingType.SCALE_ASPECT_FIT) RendererCommon.ScalingType.SCALE_ASPECT_FILL else RendererCommon.ScalingType.SCALE_ASPECT_FIT
        surfaceViewRenderer?.setScalingType(currentScalingType)
        return currentScalingType
    }

    fun setDisplayMode(mode: String, zoom: Float = 1f, panY: Float = 0f) {
        when (mode.uppercase()) {
            "FILL", "16:9" -> {
                currentScalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL
                surfaceViewRenderer?.setScalingType(currentScalingType)
                surfaceViewRenderer?.scaleX = 1f; surfaceViewRenderer?.scaleY = 1f; surfaceViewRenderer?.translationY = 0f
            }
            "FIT" -> {
                currentScalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT
                surfaceViewRenderer?.setScalingType(currentScalingType)
                surfaceViewRenderer?.scaleX = 1f; surfaceViewRenderer?.scaleY = 1f; surfaceViewRenderer?.translationY = 0f
            }
            "TOGGLE" -> toggleScalingType()
            "ZOOM" -> {
                surfaceViewRenderer?.scaleX = zoom; surfaceViewRenderer?.scaleY = zoom
                surfaceViewRenderer?.translationY = panY
            }
        }
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
