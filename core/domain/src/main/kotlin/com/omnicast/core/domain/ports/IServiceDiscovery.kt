package com.omnicast.core.domain.ports

import com.omnicast.core.domain.models.DeviceTarget
import kotlinx.coroutines.flow.StateFlow

/**
 * Puerto de descubrimiento de receptores en red local (mDNS / NSD).
 */
interface IServiceDiscovery {
    val discoveredDevices: StateFlow<List<DeviceTarget>>
    
    fun startDiscovery()
    fun stopDiscovery()
}
