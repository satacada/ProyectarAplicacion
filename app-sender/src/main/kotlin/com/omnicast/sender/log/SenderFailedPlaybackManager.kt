package com.omnicast.sender.log

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

/**
 * Registro de Caja Negra de Fallos Totales de Reproducción en el Sender (Móvil).
 * Mantiene un historial persistente (FIFO, máximo 50 eventos) reportados
 * desde la TV Box o fallos locales irremediables.
 */
data class FailedPlaybackRecord(
    val timestamp: Long = System.currentTimeMillis(),
    val streamUrl: String,
    val host: String,
    val player: String,
    val errorCode: String,
    val title: String = "",
    val details: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("timestamp", timestamp)
        put("streamUrl", streamUrl)
        put("host", host)
        put("player", player)
        put("errorCode", errorCode)
        put("title", title)
        put("details", details)
    }

    companion object {
        fun fromJson(json: JSONObject): FailedPlaybackRecord = FailedPlaybackRecord(
            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
            streamUrl = json.optString("streamUrl", ""),
            host = json.optString("host", ""),
            player = json.optString("player", "UNKNOWN"),
            errorCode = json.optString("errorCode", "UNKNOWN"),
            title = json.optString("title", ""),
            details = json.optString("details", "")
        )
    }
}

object SenderFailedPlaybackManager {
    private const val TAG = "SenderFailedPlaybackMgr"
    private const val PREFS_NAME = "omnicast_sender_failures"
    private const val KEY_FAILURES = "failed_playbacks_json"
    private const val MAX_ENTRIES = 50

    fun recordFailure(
        context: Context,
        streamUrl: String,
        player: String,
        errorCode: String,
        title: String = "",
        details: String = ""
    ) {
        if (streamUrl.isBlank()) return
        try {
            val host = extractHost(streamUrl)
            val record = FailedPlaybackRecord(
                streamUrl = streamUrl,
                host = host,
                player = player,
                errorCode = errorCode,
                title = title,
                details = details
            )
            val currentList = getRecentFailures(context).toMutableList()
            currentList.add(0, record)
            if (currentList.size > MAX_ENTRIES) {
                currentList.subList(MAX_ENTRIES, currentList.size).clear()
            }
            saveList(context, currentList)
            SenderLogger.w(TAG, "❌ [CAJA NEGRA SENDER] Fallo persistido: $host ($player -> $errorCode: $details)")
        } catch (e: Throwable) {
            Log.e(TAG, "Error persistiendo fallo de reproducción en sender: ${e.message}", e)
        }
    }

    fun getRecentFailures(context: Context): List<FailedPlaybackRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_FAILURES, "[]") ?: "[]"
        val list = mutableListOf<FailedPlaybackRecord>()
        try {
            val arr = JSONArray(rawJson)
            for (i in 0 until arr.length()) {
                list.add(FailedPlaybackRecord.fromJson(arr.getJSONObject(i)))
            }
        } catch (_: Throwable) {}
        return list
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_FAILURES)
            .apply()
    }

    private fun saveList(context: Context, list: List<FailedPlaybackRecord>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FAILURES, arr.toString())
            .apply()
    }

    private fun extractHost(url: String): String {
        return try {
            val uri = URI(url)
            val h = uri.host?.lowercase() ?: ""
            if (h.startsWith("www.")) h.substring(4) else h
        } catch (_: Throwable) {
            ""
        }
    }
}
