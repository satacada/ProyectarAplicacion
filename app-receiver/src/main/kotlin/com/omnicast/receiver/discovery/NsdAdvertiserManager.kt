package com.omnicast.receiver.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log

/**
 * Publicador mDNS/NSD en la TV Box.
 * Anuncia el servicio _omnicast._tcp para que cualquier móvil en la red local lo descubra al instante.
 */
class NsdAdvertiserManager(context: Context) {

    companion object {
        private const val TAG = "NsdAdvertiserManager"
        const val SERVICE_TYPE = "_omnicast._tcp."
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var isRegistered = false

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            isRegistered = true
            Log.i(TAG, "Servicio mDNS registrado con éxito: ${serviceInfo.serviceName}")
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            isRegistered = false
            Log.e(TAG, "Fallo al registrar servicio mDNS: $errorCode")
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
            isRegistered = false
            Log.i(TAG, "Servicio mDNS desregistrado")
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Fallo al desregistrar servicio mDNS: $errorCode")
        }
    }

    fun registerService(port: Int = 8088, customName: String? = null) {
        if (isRegistered) return

        val deviceName = customName ?: "OmniCast-${Build.MODEL.replace(" ", "-")}"
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = deviceName
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando servicio mDNS", e)
        }
    }

    fun unregisterService() {
        if (isRegistered) {
            try {
                nsdManager.unregisterService(registrationListener)
            } catch (e: Exception) {
                Log.e(TAG, "Error desregistrando servicio", e)
            }
        }
    }
}
