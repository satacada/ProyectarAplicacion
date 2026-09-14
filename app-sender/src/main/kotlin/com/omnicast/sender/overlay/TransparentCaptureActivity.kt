package com.omnicast.sender.overlay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.omnicast.core.domain.models.DeviceTarget
import com.omnicast.core.domain.models.StreamProfile
import com.omnicast.sender.service.ProjectionForegroundService

/**
 * Actividad invisible y transparente que solicita permiso de proyección sin interrumpir
 * ni tapar la aplicación o juego que el usuario está utilizando.
 * En Android 14+, utiliza createConfigForDefaultDisplay para evitar el selector de apps.
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

            ProjectionForegroundService.startProjection(
                this,
                target.ipAddress,
                target.port,
                StreamProfile.BALANCED_1080P,
                result.data!!
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
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (mpm == null) {
            finish()
            return
        }

        val captureIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val config = MediaProjectionConfig.createConfigForDefaultDisplay()
            mpm.createScreenCaptureIntent(config)
        } else {
            mpm.createScreenCaptureIntent()
        }
        captureLauncher.launch(captureIntent)
    }
}
