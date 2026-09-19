package com.omnicast.sender.webrtc

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.util.Log
import com.omnicast.core.domain.models.StreamProfile
import com.omnicast.core.domain.ports.IP2PTransport
import com.omnicast.core.domain.ports.P2PConnectionState
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
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
    var onIceConnectionClosed: (() -> Unit)? = null
    var activeProfile: StreamProfile? = null

    private var eglBase: EglBase? = null; private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null; private var peerConnection: PeerConnection? = null
    private var screenCapturer: ScreenCapturerAndroid? = null
    private var videoTrack: VideoTrack? = null; private var videoSource: VideoSource? = null
    private val queuedRemoteCandidates = mutableListOf<IceCandidate>()
    private var isRemoteDescriptionSet = false
    private var displayListener: DisplayManager.DisplayListener? = null
    private var lastCaptureWidth = 0
    private var lastCaptureHeight = 0

    var tvScaleMode: String = "EXTEND_16_9"

    override fun initialize() {
        Log.i(TAG, "🚀 OmniCast Sender v3.25 Inicializado (Universal WebRTC)")
        setupDisplayListener()
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true).createInitializationOptions()
        )
        eglBase = EglBase.create()
        val eglContext = eglBase!!.eglBaseContext
        val encoderFactory = DefaultVideoEncoderFactory(eglContext, true, false)
        val decoderFactory = DefaultVideoDecoderFactory(eglContext)
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        screenCapturer = ScreenCapturerAndroid(mediaProjectionPermissionData, object : MediaProjection.Callback() {
            override fun onStop() { Log.w(TAG, "MediaProjection detenido por el sistema"); close() }
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
                    PeerConnection.IceConnectionState.DISCONNECTED, PeerConnection.IceConnectionState.CLOSED -> {
                        _connectionState.value = P2PConnectionState.DISCONNECTED
                        onIceConnectionClosed?.invoke()
                    }
                    PeerConnection.IceConnectionState.FAILED -> {
                        _connectionState.value = P2PConnectionState.FAILED
                        onIceConnectionClosed?.invoke()
                    }
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
        _connectionState.value = P2PConnectionState.CONNECTING
    }

    private fun setupDisplayListener() {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return
        displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(displayId: Int) {
                if (displayId == Display.DEFAULT_DISPLAY) {
                    checkAndApplyOrientationChange()
                }
            }
        }
        dm.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
    }

    fun checkAndApplyOrientationChange() {
        val capturer = screenCapturer ?: return
        val dims = calcCaptureDimensions()
        val w = dims.first
        val h = dims.second
        if (w != lastCaptureWidth || h != lastCaptureHeight) {
            val fps = (activeProfile?.frameRate ?: 30).coerceIn(15, 60)
            Log.i(TAG, "🔄 Orientación adaptativa detectada: ${w}x${h} @ ${fps}fps (previo: ${lastCaptureWidth}x${lastCaptureHeight})")
            lastCaptureWidth = w
            lastCaptureHeight = h
            try {
                capturer.changeCaptureFormat(w, h, fps)
            } catch (e: Throwable) {
                Log.w(TAG, "Error cambiando formato de captura: ${e.message}")
            }
        }
    }

    /** Inicia la captura real de pantalla DESPUÉS de haber enviado la oferta SDP (v3.11 / v3.4 estable). */
    fun beginCapture() {
        val dims = calcCaptureDimensions()
        lastCaptureWidth = dims.first
        lastCaptureHeight = dims.second
        val fps = (activeProfile?.frameRate ?: 30).coerceIn(15, 60)
        Log.i(TAG, "🎬 Iniciando captura: ${dims.first}x${dims.second} @ ${fps}fps (modo: $tvScaleMode, perfil: ${activeProfile?.label})")
        screenCapturer?.startCapture(dims.first, dims.second, fps)
    }

    fun calcCaptureDimensions(): Pair<Int, Int> {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val defaultDisplay = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        defaultDisplay?.getRealMetrics(metrics)
        val screenW = if (metrics.widthPixels > 0) metrics.widthPixels else context.resources.displayMetrics.widthPixels
        val screenH = if (metrics.heightPixels > 0) metrics.heightPixels else context.resources.displayMetrics.heightPixels

        val isLandscape = screenW > screenH
        val maxDim = (if (isLandscape) activeProfile?.width ?: 1920 else activeProfile?.height ?: 1280).toFloat()
        val curMax = (if (isLandscape) screenW else screenH).toFloat().coerceAtLeast(1f)
        val scale = minOf(1.0f, maxDim / curMax)
        val capW = (((screenW * scale).toInt() / 16) * 16).coerceAtLeast(320)
        val capH = (((screenH * scale).toInt() / 16) * 16).coerceAtLeast(480)
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
                setBitrate(activeProfile?.targetBitrateKbps ?: 4500)
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
                    it.minBitrateBps = 500_000
                    it.maxBitrateBps = (targetBitrateKbps * 1000).coerceIn(1_000_000, 5_000_000)
                    it.maxFramerate = (activeProfile?.frameRate ?: 30).coerceIn(15, 60)
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
            peerConnectionFactory = null
            displayListener?.let {
                val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                dm?.unregisterDisplayListener(it)
                displayListener = null
            }
            eglBase?.release(); eglBase = null
            isRemoteDescriptionSet = false; queuedRemoteCandidates.clear()
        } catch (e: Exception) { Log.e(TAG, "Error cerrando WebRtcSenderManager", e) }
        _connectionState.value = P2PConnectionState.DISCONNECTED
    }
}
