package com.omnicast.sender.webrtc

/**
 * Optimizador de SDP para WebRTC enfocado en deportes y video en vivo.
 * Prioriza H.264 High Profile e inyecta parámetros de tasa de bits inmediata (BWE).
 */
object WebRtcSdpTuner {

    fun tuneForFluidMotion(sdpDescription: String, targetBitrateKbps: Int = 10000): String {
        val lines = sdpDescription.split("\r\n").toMutableList()
        val videoLineIdx = lines.indexOfFirst { it.startsWith("m=video") }
        if (videoLineIdx == -1) return sdpDescription

        val h264Pt = lines.firstOrNull { it.startsWith("a=rtpmap:") && it.contains("H264/90000") }
            ?.substringAfter("a=rtpmap:")?.substringBefore(" ")?.trim()

        if (h264Pt != null) {
            val parts = lines[videoLineIdx].split(" ").filter { it.isNotEmpty() }.toMutableList()
            if (parts.size > 3 && parts.remove(h264Pt)) {
                parts.add(3, h264Pt)
                lines[videoLineIdx] = parts.joinToString(" ")
            }
        }

        val minBitrate = 600
        val startBitrate = (targetBitrateKbps / 3).coerceAtLeast(1500)
        val maxBitrate = targetBitrateKbps

        val tunedLines = mutableListOf<String>()
        for (line in lines) {
            if (h264Pt != null && line.startsWith("a=fmtp:$h264Pt")) {
                continue
            }
            if (line.startsWith("b=AS:")) {
                continue
            }

            tunedLines.add(line)

            if (h264Pt != null && line.startsWith("a=rtpmap:$h264Pt") && line.contains("H264/90000")) {
                tunedLines.add("a=fmtp:$h264Pt level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=640c28;x-google-min-bitrate=$minBitrate;x-google-start-bitrate=$startBitrate;x-google-max-bitrate=$maxBitrate")
            }
            if (line.startsWith("c=IN IP4")) {
                tunedLines.add("b=AS:$targetBitrateKbps")
            }
        }
        return tunedLines.joinToString("\r\n")
    }
}
