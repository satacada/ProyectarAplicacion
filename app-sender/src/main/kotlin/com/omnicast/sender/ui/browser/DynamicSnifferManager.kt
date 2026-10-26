package com.omnicast.sender.ui.browser

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Gestor dinámico del sniffer JavaScript inspirado en Seekee (resourceAnalysis.html).
 * Permite cargar el script actualizado desde el almacenamiento interno sin requerir
 * recompilación del APK, con fallback inmediato a los assets incluidos.
 */
object DynamicSnifferManager {

    private const val TAG = "DynamicSnifferManager"
    private const val SCRIPT_FILE_NAME = "universal_sniffer.js"
    private var cachedScript: String? = null

    fun getInjectionScript(context: Context): String {
        cachedScript?.let { return it }

        val dynamicFile = File(context.filesDir, "sniffer/$SCRIPT_FILE_NAME")
        val scriptContent = if (dynamicFile.exists() && dynamicFile.length() > 0) {
            val text = try { dynamicFile.readText(Charsets.UTF_8) } catch (_: Throwable) { "" }
            if (text.contains("triggerPointerSequence") || text.contains("autoSolveTurnstile") || !text.contains("OMNICAST_STREAM")) {
                runCatching { dynamicFile.delete() }
                loadFromAssets(context)
            } else {
                text
            }
        } else {
            loadFromAssets(context)
        }

        val wvcFallback = try {
            context.assets.open("wvc_sniffer.js").bufferedReader().use { it.readText() }
        } catch (_: Throwable) { "" }

        val combined = "$wvcFallback\n$scriptContent"
        cachedScript = combined
        return combined
    }

    private fun loadFromAssets(context: Context): String {
        return try {
            context.assets.open("sniffer/$SCRIPT_FILE_NAME").bufferedReader().use { it.readText() }
        } catch (e: Throwable) {
            Log.e(TAG, "Error leyendo sniffer de assets: ${e.message}")
            ""
        }
    }

    fun updateDynamicScript(context: Context, newScript: String): Boolean {
        if (newScript.isBlank() || !newScript.contains("OMNICAST_STREAM")) return false
        return try {
            val dir = File(context.filesDir, "sniffer").apply { if (!exists()) mkdirs() }
            val file = File(dir, SCRIPT_FILE_NAME)
            file.writeText(newScript, Charsets.UTF_8)
            cachedScript = null
            Log.i(TAG, "Script de sniffer actualizado exitosamente (${file.length()} bytes)")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Error guardando script dinámico: ${e.message}", e)
            false
        }
    }

    fun clearCache() {
        cachedScript = null
    }
}
