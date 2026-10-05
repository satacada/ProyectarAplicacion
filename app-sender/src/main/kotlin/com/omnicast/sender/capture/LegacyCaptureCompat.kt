package com.omnicast.sender.capture

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build

/**
 * Adaptador modular de compatibilidad para Android 9 (Pie - API 28) y versiones anteriores.
 * Aísla totalmente las diferencias de MediaProjection y ForegroundService sin alterar Android 10+.
 */
object LegacyCaptureCompat {

    fun isLegacy(): Boolean = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P

    fun createCaptureIntent(context: Context): Intent? {
        val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            ?: return null
        return mpm.createScreenCaptureIntent()
    }

    fun startProjectionService(context: Context, serviceIntent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
