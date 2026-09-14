package com.omnicast.sender.webrtc

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.util.Log
import com.omnicast.core.domain.models.StreamProfile
import com.omnicast.core.domain.ports.IP2PTransport
import com.omnicast.core.domain.ports.P2PConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*

class WebRtcSenderManager(
    private val context: Context,
    private val mediaProjectionPermissionData: Intent
) : IP2PTransport {

    companion object { private const val TAG = "WebRtcSenderManager" }

    private val _connectionState = MutableStateFlow(P2PConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<P2PConnectionState> = _connectionState.asStateFlow()
    var onLocalIceCandidate: ((IceCandidate) -> Unit)? = null

    private var eglBase: EglBase? = null; private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null; private var peerConnection: PeerConnection? = null
    private var screenCapturer: ScreenCapturerAndroid? = null
    private var videoTrack: VideoTrack? = null; private var videoSource: VideoSource? = null
    private var activeProfile: StreamProfile? = null
    private val queuedRemoteCandidates = mutableListOf<IceCandidate>()
    private var isRemoteDescriptionSet = false

    override fun initialize() {
        Log.i(TAG, "Inicializando WebRTC con H.264 High Profile por hardware y modo fluido para deportes")
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context).setEnableInternalTracer(true).createInitializationOptions())
        eglBase = EglBase.create()
        val eglContext = eglBase!!.eglBaseContext
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext)).createPeerConnectionFactory()

        screenCapturer = ScreenCapturerAndroid(mediaProjectionPermissionData, object : MediaProjection.Callback() {
            override fun onStop() { Log.w(TAG, "MediaProjection detenido"); close() }
        })
        surfaceTextureHelper = SurfaceTextureHelper.create("ScreenCaptureThread", eglContext)
        videoSource = peerConnectionFactory?.createVideoSource(true)
        screenCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
    }

    fun startScreenStreaming(profile: StreamProfile, iceServers: List<PeerConnection.IceServer> = emptyList()) {
        activeProfile = profile
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceCandidatePoolSize = 2
        }
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                Log.i(TAG, "ICE Emisor: $newState")
                when (newState) {
                    PeerConnection.IceConnectionState.CONNECTED -> _connectionState.value = P2PConnectionState.CONNECTED
                    PeerConnection.IceConnectionState.DISCONNECTED, PeerConnection.IceConnectionState.CLOSED -> _connectionState.value = P2PConnectionState.DISCONNECTED
                    PeerConnection.IceConnectionState.FAILED -> _connectionState.value = P2PConnectionState.FAILED
                    else -> Unit
                }
            }
            override fun onIceCandidate(c: IceCandidate?) { c?.let { onLocalIceCandidate?.invoke(it) } }
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) = Unit
            override fun onIceConnectionReceivingChange(p0: Boolean) = Unit
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) = Unit
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) = Unit
            override fun onAddStream(p0: MediaStream?) = Unit; override fun onRemoveStream(p0: MediaStream?) = Unit
            override fun onDataChannel(p0: DataChannel?) = Unit; override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) = Unit
        })

        videoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", videoSource)?.apply { setEnabled(true) }
        videoTrack?.let { peerConnection?.addTrack(it, listOf("ARDAMS")) }

        val dims = calcCaptureDimensions()
        val fps = profile.frameRate.coerceIn(30, 45)
        Log.i(TAG, "Iniciando Captura de Pantalla: ${dims.first}x${dims.second} @ ${fps}fps")
        screenCapturer?.startCapture(dims.first, dims.second, fps)
        _connectionState.value = P2PConnectionState.CONNECTING
    }

    private fun calcCaptureDimensions(): Pair<Int, Int> {
        val dm = context.resources.displayMetrics
        val maxScreen = maxOf(dm.widthPixels, dm.heightPixels).toFloat().coerceAtLeast(1f)
        val scale = minOf(1.0f, 1280f / maxScreen)
        val capW = (((dm.widthPixels * scale).toInt() / 16) * 16).coerceIn(480, 1280)
        val capH = (((dm.heightPixels * scale).toInt() / 16) * 16).coerceIn(480, 1280)
        return Pair(capW, capH)
    }

    private fun preferH264(sdp: String): String {
        val lines = sdp.split("\r\n").toMutableList()
        val idx = lines.indexOfFirst { it.startsWith("m=video") }
        if (idx == -1) return sdp
        val pt = lines.firstOrNull { it.contains("H264/90000") }?.substringAfter("a=rtpmap:")?.substringBefore(" ") ?: return sdp
        val parts = lines[idx].split(" ").toMutableList()
        if (parts.size > 3 && parts.remove(pt)) {
            parts.add(3, pt)
            lines[idx] = parts.joinToString(" ")
        }
        return lines.joinToString("\r\n")
    }

    fun getMediaProjection(): MediaProjection? = try {
        val f = ScreenCapturerAndroid::class.java.getDeclaredField("mediaProjection").apply { isAccessible = true }
        f.get(screenCapturer) as? MediaProjection
    } catch (_: Throwable) { null }

    override fun createOffer(onSdpCreated: (String) -> Unit) {
        val constraints = MediaConstraints().apply {
            listOf("OfferToReceiveVideo", "OfferToReceiveAudio").forEach { mandatory.add(MediaConstraints.KeyValuePair(it, "false")) }
        }
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let {
                    val sdp = preferH264(it.description)
                    peerConnection?.setLocalDescription(this, SessionDescription(it.type, sdp))
                    onSdpCreated(sdp)
                }
            }
            override fun onSetSuccess() = Unit; override fun onSetFailure(e: String?) = Unit
            override fun onCreateFailure(e: String?) { Log.e(TAG, "Error Offer: $e") }
        }, constraints)
    }

    override fun handleAnswer(sdpAnswer: String) {
        val desc = SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                isRemoteDescriptionSet = true
                queuedRemoteCandidates.forEach { peerConnection?.addIceCandidate(it) }
                queuedRemoteCandidates.clear()
                setBitrate(activeProfile?.targetBitrateKbps ?: 10000)
            }
            override fun onSetFailure(e: String?) = Unit
            override fun onCreateSuccess(d: SessionDescription?) = Unit; override fun onCreateFailure(e: String?) = Unit
        }, desc)
    }

    override fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        val c = IceCandidate(sdpMid, sdpMLineIndex, candidate)
        if (isRemoteDescriptionSet) peerConnection?.addIceCandidate(c) else queuedRemoteCandidates.add(c)
    }

    override fun setBitrate(targetBitrateKbps: Int) {
        try {
            peerConnection?.senders?.filter { it.track() is VideoTrack }?.forEach { sender ->
                val p = sender.parameters
                p.encodings?.firstOrNull()?.let {
                    it.minBitrateBps = 1_500_000
                    it.maxBitrateBps = 4_500_000
                    it.maxFramerate = 45
                }
                sender.parameters = p
            }
        } catch (e: Throwable) { Log.w(TAG, "No se pudo actualizar bitrate: ${e.message}") }
    }

    override fun close() {
        try {
            screenCapturer?.stopCapture(); screenCapturer?.dispose(); screenCapturer = null
            videoTrack?.dispose(); videoTrack = null; videoSource?.dispose(); videoSource = null
            surfaceTextureHelper?.dispose(); surfaceTextureHelper = null
            peerConnection?.close(); peerConnection = null
            peerConnectionFactory?.dispose(); peerConnectionFactory = null
            eglBase?.release(); eglBase = null
            isRemoteDescriptionSet = false; queuedRemoteCandidates.clear()
        } catch (e: Exception) { Log.e(TAG, "Error cerrando WebRtcSenderManager", e) }
        _connectionState.value = P2PConnectionState.DISCONNECTED
    }
}
