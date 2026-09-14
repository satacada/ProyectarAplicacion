package com.omnicast.sender.network

import android.content.Context
import com.omnicast.core.protocol.SignalingMessage

object QuickSignalingHelper {
    fun sendQuickMessage(context: Context, msg: SignalingMessage, disconnect: Boolean = false) {
        val prefs = context.getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
        val ip = prefs.getString("saved_tv_ip", "192.168.0.136") ?: "192.168.0.136"
        val port = prefs.getInt("saved_tv_port", 8088)
        SignalingClient().apply {
            connect(ip, port, {
                sendMessage(msg)
                if (disconnect) disconnect()
            }, {})
        }
    }
}
