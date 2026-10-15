package com.omnicast.sender.network

import android.content.Context
import com.omnicast.core.domain.models.DeviceTarget
import com.omnicast.core.protocol.SignalingMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    /**
     * Envía un mensaje de señalización o reproducción a múltiples receptores en la red local.
     */
    fun broadcastToDevices(devices: List<DeviceTarget>, msg: SignalingMessage) {
        if (devices.isEmpty()) return
        val scope = CoroutineScope(Dispatchers.IO)
        devices.forEach { dev ->
            scope.launch {
                try {
                    val client = SignalingClient()
                    client.connect(dev.ipAddress, dev.port, {
                        client.sendMessage(msg)
                    }, {})
                } catch (_: Throwable) {}
            }
        }
    }
}
