package com.omnicast.sender.webrtc

/**
 * Optimizador de SDP para WebRTC enfocado en deportes y video en vivo.
 * Prioriza H.264 High Profile e inyecta parámetros de tasa de bits inmediata (BWE).
 */
object WebRtcSdpTuner {

    fun tuneForFluidMotion(sdp: String): String {
        val lines = sdp.split("\r\n").toMutableList()
        val videoLineIdx = lines.indexOfFirst { it.startsWith("m=video") }
        if (videoLineIdx == -1) return sdp

        val h264Pt = lines.firstOrNull { it.contains("H264/90000") }
            ?.substringAfter("a=rtpmap:")?.substringBefore(" ")

        if (h264Pt != null) {
            val parts = lines[videoLineIdx].split(" ").toMutableList()
            if (parts.size > 3 && parts.remove(h264Pt)) {
                parts.add(3, h264Pt)
                lines[videoLineIdx] = parts.joinToString(" ")
            }
        }

        val tunedLines = mutableListOf<String>()
        for (line in lines) {
            tunedLines.add(line)
            if (line.startsWith("a=rtpmap:") && line.contains("H264/90000") && h264Pt != null) {
                tunedLines.add("a=fmtp:$h264Pt level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e028;x-google-min-bitrate=6000;x-google-start-bitrate=12000;x-google-max-bitrate=20000")
            }
            if (line.startsWith("c=IN IP4")) {
                tunedLines.add("b=AS:18000")
            }
        }
        return tunedLines.joinToString("\r\n")
    }
}
