package com.omnicast.core.domain.models

/**
 * Representa un dispositivo receptor (TV Box o pantalla) descubierto en la red local.
 */
data class DeviceTarget(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val deviceType: DeviceType = DeviceType.ANDROID_TV,
    val isAvailable: Boolean = true
)

enum class DeviceType {
    ANDROID_TV,
    FIRE_TV,
    AOSP_TV_BOX,
    WEB_RECEIVER,
    UNKNOWN
}
