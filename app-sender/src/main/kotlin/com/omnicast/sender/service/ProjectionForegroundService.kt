package com.omnicast.sender.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import android.content.res.Configuration
import com.omnicast.core.domain.models.QualityMode
import com.omnicast.core.domain.models.StreamProfile
import com.omnicast.sender.ui.SenderMainActivity

/**
 * Foreground Service mandatorio en Android 10+ para autorizar y mantener viva
 * la sesión de MediaProjection mientras el usuario navega en cualquier otra app.
 */
class ProjectionForegroundService : Service() {

    companion object {
        private const val TAG = "ProjectionForegroundService"
        const val CHANNEL_ID = "OmniCast_Projection_Channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.omnicast.sender.ACTION_START_PROJECTION"
        const val ACTION_STOP = "com.omnicast.sender.ACTION_STOP_PROJECTION"

        var isProjecting = false
            private set

        private var currentSession: ProjectionSession? = null

        fun startProjection(
            context: Context,
            targetIp: String,
            targetPort: Int,
            profile: StreamProfile,
            permissionData: Intent,
            tvScaleMode: String = "EXTEND_16_9",
            smoothBuffer: Boolean = true
        ) {
            val intent = Intent(context, ProjectionForegroundService::class.java).apply {
                action = ACTION_START
                putExtra("EXTRA_IP", targetIp)
                putExtra("EXTRA_PORT", targetPort)
                putExtra("EXTRA_WIDTH", profile.width)
                putExtra("EXTRA_HEIGHT", profile.height)
                putExtra("EXTRA_FPS", profile.frameRate)
                putExtra("EXTRA_BITRATE", profile.targetBitrateKbps)
                putExtra("EXTRA_MODE", profile.mode.name)
                putExtra("EXTRA_TV_SCALE_MODE", tvScaleMode)
                putExtra("EXTRA_SMOOTH_BUFFER", smoothBuffer)
                putExtra("EXTRA_PERMISSION_DATA", permissionData)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun setTvScaleMode(mode: String) {
            currentSession?.setTvScaleMode(mode)
        }

        fun setDisplayMode(mode: String, zoom: Float = 1.0f, panY: Float = 0.0f, panX: Float = 0.0f) {
            currentSession?.setDisplayMode(mode, zoom, panY, panX)
        }

        fun toggleTvScaleMode(): String {
            return currentSession?.toggleTvScaleMode() ?: "EXTEND_16_9"
        }

        fun getTvScaleMode(): String {
            return currentSession?.getTvScaleMode() ?: "EXTEND_16_9"
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ProjectionForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "Deteniendo servicio de proyección")
                currentSession?.stop()
                currentSession = null
                isProjecting = false
                CastSessionState.setProjecting(false)
                com.omnicast.sender.overlay.FloatingIndicatorService.updateMode()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val notification = buildForegroundNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                val prefs = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
                val ip = intent.getStringExtra("EXTRA_IP") ?: prefs.getString("saved_tv_ip", "") ?: ""
                if (ip.isBlank()) { stopSelf(); return START_NOT_STICKY }
                val port = intent.getIntExtra("EXTRA_PORT", prefs.getInt("saved_tv_port", 8088))
                val width = intent.getIntExtra("EXTRA_WIDTH", 1920)
                val height = intent.getIntExtra("EXTRA_HEIGHT", 1280)
                val fps = intent.getIntExtra("EXTRA_FPS", 30)
                val bitrate = intent.getIntExtra("EXTRA_BITRATE", 3500)
                val modeStr = intent.getStringExtra("EXTRA_MODE")
                val mode = try { QualityMode.valueOf(modeStr ?: "") } catch (_: Throwable) { QualityMode.BALANCED_30FPS }
                val permData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("EXTRA_PERMISSION_DATA", Intent::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra("EXTRA_PERMISSION_DATA")
                }
                val tvScaleMode = intent.getStringExtra("EXTRA_TV_SCALE_MODE") ?: prefs.getString("saved_tv_scale_mode", "FIT") ?: "FIT"
                val smoothBuffer = intent.getBooleanExtra("EXTRA_SMOOTH_BUFFER", prefs.getBoolean("saved_smooth_buffer", true))
                Log.i(TAG, "ACTION_START recibido: ip=$ip, port=$port, mode=$mode, fps=$fps, tvScaleMode=$tvScaleMode, smoothBuffer=$smoothBuffer")
                if (permData != null) {
                    currentSession?.stop()
                    val profile = StreamProfile.ALL_PROFILES.firstOrNull { it.mode == mode }
                        ?: StreamProfile(mode, width, height, fps, bitrate, 65)
                    currentSession = ProjectionSession(this, ip, port, profile, permData, tvScaleMode, smoothBuffer).apply {
                        start()
                    }
                    isProjecting = true
                    CastSessionState.setProjecting(true)
                    com.omnicast.sender.overlay.FloatingIndicatorService.start(this)
                    com.omnicast.sender.overlay.FloatingIndicatorService.updateMode()
                } else {
                    Log.e(TAG, "EXTRA_PERMISSION_DATA es null. No se puede iniciar captura.")
                }
            }
        }
        return START_STICKY
    }

    private var lastReportedLandscape: Boolean? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val isLandscape = (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
        if (lastReportedLandscape != isLandscape) {
            lastReportedLandscape = isLandscape
            val mode = if (isLandscape) "FILL" else "FIT"
            Log.i(TAG, "🔄 Orientación de sistema Android cambiada: landscape=$isLandscape -> modo TV: $mode")
            currentSession?.setDisplayMode(mode)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lastReportedLandscape = null
        currentSession?.stop()
        currentSession = null
        isProjecting = false
        CastSessionState.setProjecting(false)
        com.omnicast.sender.overlay.FloatingIndicatorService.updateMode()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OmniCast Proyección Activa",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente requerida durante la transmisión de pantalla"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, SenderMainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, ProjectionForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OmniCast Proyectando")
            .setContentText("Transmitiendo pantalla y audio a la TV Box")
            .setSmallIcon(android.R.drawable.ic_menu_slideshow)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
