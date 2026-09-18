package com.omnicast.sender.overlay

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.omnicast.core.domain.models.DeviceTarget
import com.omnicast.core.domain.models.StreamProfile
import com.omnicast.sender.capture.LegacyCaptureCompat
import com.omnicast.sender.service.ProjectionForegroundService

/**
 * Actividad invisible y transparente que solicita permiso de proyección sin interrumpir
 * ni tapar la aplicación o juego que el usuario está utilizando.
 * Compatible al 100% con Android 9 (Pie) y Android 10-14.
 */
class TransparentCaptureActivity : ComponentActivity() {

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val prefs = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
            val ip = prefs.getString("saved_tv_ip", "192.168.0.136")?.takeIf { it.isNotBlank() && it != "192.168.1.100" } ?: "192.168.0.136"
            val port = prefs.getInt("saved_tv_port", 8088)
            val target = DeviceTarget("direct-tv", "OmniCast TV Box", ip, port)

            val savedProfileName = prefs.getString("saved_stream_profile", StreamProfile.BALANCED_30FPS.mode.name)
            val profile = StreamProfile.ALL_PROFILES.firstOrNull { it.mode.name == savedProfileName } ?: StreamProfile.BALANCED_30FPS
            val scaleMode = prefs.getString("saved_tv_scale_mode", "EXTEND_16_9") ?: "EXTEND_16_9"
            val smoothBuffer = prefs.getBoolean("saved_smooth_buffer", true)

            ProjectionForegroundService.startProjection(
                this,
                target.ipAddress,
                target.port,
                profile,
                result.data!!,
                scaleMode,
                smoothBuffer
            )
            Toast.makeText(this, "🟢 Transmitiendo pantalla a la TV", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Transmisión cancelada", Toast.LENGTH_SHORT).show()
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val captureIntent = LegacyCaptureCompat.createCaptureIntent(this)
        if (captureIntent == null) {
            finish()
            return
        }
        captureLauncher.launch(captureIntent)
    }
}
