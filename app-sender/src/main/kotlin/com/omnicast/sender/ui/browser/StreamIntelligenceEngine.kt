package com.omnicast.sender.ui.browser

import android.net.Uri
import android.util.Log

/**
 * Clasificador determinista y constructor de cabeceras reales de flujo multimedia.
 * Cero dominios en duro: respeta estrictamente las cabeceras entregadas por Chromium
 * y el origen real del iframe reproductor que solicitó el recurso.
 */
object StreamIntelligenceEngine {

    private const val TAG = "StreamIntelligence"


    fun resolveDynamicHeaders(
        streamUrl: String,
        sourceFrameOrPageUrl: String,
        rawHeaders: Map<String, String>? = null
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        rawHeaders?.forEach { (k, v) ->
            if (v.isNotBlank()) {
                when (k.lowercase()) {
                    "referer" -> result["Referer"] = v
                    "origin" -> result["Origin"] = v
                    "user-agent" -> result["User-Agent"] = v
                    "cookie" -> result["Cookie"] = v
                    "authorization" -> result["Authorization"] = v
                }
            }
        }

        // Preservar siempre el Referer real detectado de la petición. Solo usar sourceFrameOrPageUrl si no hay Referer o si sourceFrame es un iframe embed explícito
        val curRef = result["Referer"]
        if (sourceFrameOrPageUrl.startsWith("http")) {
            val isSourceEmbed = sourceFrameOrPageUrl.contains("/e/") || sourceFrameOrPageUrl.contains("embed")
            val isCurEmbed = curRef?.let { it.contains("/e/") || it.contains("embed") } ?: false
            if (curRef.isNullOrBlank() || (isSourceEmbed && !isCurEmbed)) {
                result["Referer"] = sourceFrameOrPageUrl
            }
        }

        val existingReferer = result["Referer"]
        if (!existingReferer.isNullOrBlank() && !result.containsKey("Origin")) {
            try {
                val rUri = Uri.parse(existingReferer)
                if (!rUri.scheme.isNullOrBlank() && !rUri.host.isNullOrBlank()) {
                    result["Origin"] = "${rUri.scheme}://${rUri.host}"
                }
            } catch (_: Throwable) {}
        }

        if (!result.containsKey("User-Agent")) result["User-Agent"] = UA_MOBILE
        return result
    }
}
