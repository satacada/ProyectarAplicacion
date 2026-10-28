package com.omnicast.sender.overlay

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.omnicast.core.domain.models.DeviceTarget
import com.omnicast.core.domain.models.StreamProfile
import android.media.projection.MediaProjectionManager
import com.omnicast.sender.service.ProjectionForegroundService

/**
 * Actividad invisible y transparente que solicita permiso de proyección sin interrumpir
 * ni tapar la aplicación o juego que el usuario está utilizando.
 * Compatible al 100% con Android 9 (Pie) y Android 10-14.
 */
class TransparentCaptureActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PROJECTION_MODE = "EXTRA_PROJECTION_MODE"
    }

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val mode = intent.getStringExtra(EXTRA_PROJECTION_MODE) ?: "FULL_SCREEN"
            val prefs = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
            val ip = prefs.getString("saved_tv_ip", "")?.takeIf { it.isNotBlank() && it != "192.168.1.100" }
            if (ip == null) {
                Toast.makeText(this, "⚠️ Conecta la TV Box antes de proyectar", Toast.LENGTH_SHORT).show()
                finish(); return@registerForActivityResult
            }
            val port = prefs.getInt("saved_tv_port", 8088)
            val target = DeviceTarget("direct-tv", "OmniCast TV Box", ip, port)

            val savedProfileName = prefs.getString("saved_stream_profile", StreamProfile.BALANCED_30FPS.mode.name)
            val profile = StreamProfile.ALL_PROFILES.firstOrNull { it.mode.name == savedProfileName } ?: StreamProfile.BALANCED_30FPS
            prefs.edit().putString("saved_tv_scale_mode", mode).apply()
            com.omnicast.sender.service.TvZoomManager.resetToFit()
            val smoothBuffer = prefs.getBoolean("saved_smooth_buffer", true)

            ProjectionForegroundService.startProjection(
                this,
                target.ipAddress,
                target.port,
                profile,
                result.data!!,
                mode,
                smoothBuffer
            )
            val msg = if (mode == "APP") "🟢 Transmitiendo aplicación a la TV" else "🟢 Transmitiendo pantalla a la TV"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Transmisión cancelada", Toast.LENGTH_SHORT).show()
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(EXTRA_PROJECTION_MODE) ?: "FULL_SCREEN"
        val isFull = mode != "APP"
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        val captureIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val cfg = if (isFull) {
                android.media.projection.MediaProjectionConfig.createConfigForDefaultDisplay()
            } else {
                android.media.projection.MediaProjectionConfig.createConfigForUserChoice()
            }
            mpm?.createScreenCaptureIntent(cfg)
        } else {
            mpm?.createScreenCaptureIntent()
        }
        if (captureIntent == null) {
            finish()
            return
        }
        if (!isFull) {
            Toast.makeText(this, "🔒 Modo Por Aplicación: mantén seleccionada 'Una sola app'", Toast.LENGTH_LONG).show()
        }
        captureLauncher.launch(captureIntent)
    }
}
