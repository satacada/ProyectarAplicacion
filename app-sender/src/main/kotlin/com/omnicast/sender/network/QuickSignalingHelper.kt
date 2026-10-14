package com.omnicast.sender.network

import android.content.Context
import com.omnicast.core.protocol.SignalingMessage

object QuickSignalingHelper {
    fun sendQuickMessage(context: Context, msg: SignalingMessage, disconnect: Boolean = false) {
        val prefs = context.getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
        val ip = prefs.getString("saved_tv_ip", null)?.takeIf { it.isNotBlank() } ?: return
        val port = prefs.getInt("saved_tv_port", 8088)
        val client = SignalingClientHolder.get()
        if (client.isConnected) {
            client.sendMessage(msg)
        } else {
            client.connect(ip, port, { client.sendMessage(msg) }, {})
        }
    }
}
