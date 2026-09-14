package com.omnicast.sender.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.omnicast.core.domain.models.DeviceTarget
import com.omnicast.core.domain.models.DeviceType
import com.omnicast.core.domain.ports.IServiceDiscovery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Escáner mDNS/NSD para detectar automáticamente TV Boxes en la red local.
 */
class NsdScannerManager(context: Context) : IServiceDiscovery {

    companion object {
        private const val TAG = "NsdScannerManager"
        const val SERVICE_TYPE = "_omnicast._tcp."
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val _discoveredDevices = MutableStateFlow<List<DeviceTarget>>(emptyList())
    override val discoveredDevices: StateFlow<List<DeviceTarget>> = _discoveredDevices.asStateFlow()

    private var isScanning = false

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Escaneo mDNS iniciado: $regType")
            isScanning = true
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d(TAG, "Servicio detectado: ${service.serviceName}")
            if (service.serviceType.contains("omnicast")) {
                try {
                    nsdManager?.resolveService(service, createResolveListener())
                } catch (e: Throwable) {
                    Log.e(TAG, "Error resolviendo servicio NSD", e)
                }
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.d(TAG, "Servicio perdido: ${service.serviceName}")
            val current = _discoveredDevices.value.toMutableList()
            current.removeAll { it.name == service.serviceName }
            _discoveredDevices.value = current
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.d(TAG, "Escaneo detenido")
            isScanning = false
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Fallo al iniciar escaneo: $errorCode")
            stopDiscovery()
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Fallo al detener escaneo: $errorCode")
        }
    }

    private fun createResolveListener() = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Fallo resolviendo servicio: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            val host = serviceInfo.host?.hostAddress ?: return
            val port = serviceInfo.port
            val device = DeviceTarget(
                id = "${serviceInfo.serviceName}_$host",
                name = serviceInfo.serviceName,
                ipAddress = host,
                port = port,
                deviceType = DeviceType.ANDROID_TV
            )
            val current = _discoveredDevices.value.toMutableList()
            if (current.none { it.ipAddress == host && it.port == port }) {
                current.add(device)
                _discoveredDevices.value = current
                Log.i(TAG, "Dispositivo resuelto: ${device.name} en $host:$port")
            }
        }
    }

    override fun startDiscovery() {
        if (!isScanning) {
            try {
                nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            } catch (e: Throwable) {
                Log.e(TAG, "Error iniciando descubrimiento", e)
            }
        }
    }

    override fun stopDiscovery() {
        if (isScanning) {
            try {
                nsdManager?.stopServiceDiscovery(discoveryListener)
            } catch (e: Throwable) {
                Log.e(TAG, "Error deteniendo descubrimiento", e)
            }
        }
    }
}
