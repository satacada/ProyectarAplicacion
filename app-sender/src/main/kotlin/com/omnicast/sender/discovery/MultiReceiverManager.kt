package com.omnicast.sender.discovery

import android.content.Context
import com.omnicast.core.domain.models.DeviceTarget
import com.omnicast.core.protocol.SignalingMessage
import com.omnicast.sender.network.QuickSignalingHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestiona la selección y emisión hacia receptores secundarios seleccionados por el usuario.
 * Por defecto solo existe un único receptor principal; los secundarios se eligen a demanda.
 */
object MultiReceiverManager {
    private const val PREF_KEY = "saved_secondary_tv_ips"
    private val _secondaryIps = MutableStateFlow<Set<String>>(emptySet())
    val secondaryIps: StateFlow<Set<String>> = _secondaryIps.asStateFlow()

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(PREF_KEY, emptySet()) ?: emptySet()
        _secondaryIps.value = saved
    }

    fun toggleIp(context: Context, ip: String) {
        val current = _secondaryIps.value.toMutableSet()
        if (current.contains(ip)) current.remove(ip) else current.add(ip)
        _secondaryIps.value = current
        val prefs = context.getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet(PREF_KEY, current).apply()
    }

    fun clear(context: Context) {
        _secondaryIps.value = emptySet()
        val prefs = context.getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove(PREF_KEY).apply()
    }

    fun broadcastToSecondary(msg: SignalingMessage, discoveredDevices: List<DeviceTarget>) {
        val targets = _secondaryIps.value
        if (targets.isEmpty()) return
        val primaryIp = appContext?.getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)?.getString("saved_tv_ip", "") ?: ""
        val filtered = targets.filter { it.isNotBlank() && it != primaryIp }
        if (filtered.isEmpty()) return
        val list = filtered.map { ip ->
            discoveredDevices.firstOrNull { it.ipAddress == ip }
                ?: DeviceTarget("secondary-$ip", "TV $ip", ip, 8088)
        }
        QuickSignalingHelper.broadcastToDevices(list, msg)
    }
}
