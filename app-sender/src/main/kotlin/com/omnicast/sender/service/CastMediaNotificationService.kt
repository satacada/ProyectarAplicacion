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
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.omnicast.sender.stream.ActiveStreamHolder
import com.omnicast.sender.ui.SenderMainActivity
import kotlinx.coroutines.*

/**
 * Foreground Service que publica la sesión multimedia (MediaSessionCompat) y
 * la notificación interactiva en la pantalla de bloqueo (MediaStyle) idéntica a Web Video Caster.
 */
class CastMediaNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "OmniCast_Media_Channel"
        const val NOTIFICATION_ID = 2002

        const val ACTION_START = "com.omnicast.sender.ACTION_START_CAST_SESSION"
        const val ACTION_STOP = "com.omnicast.sender.ACTION_STOP_CAST_SESSION"
        const val ACTION_PLAY_PAUSE = "com.omnicast.sender.ACTION_PLAY_PAUSE"
        const val ACTION_REWIND = "com.omnicast.sender.ACTION_REWIND_10"
        const val ACTION_FAST_FORWARD = "com.omnicast.sender.ACTION_FORWARD_10"

        fun start(context: Context) {
            val intent = Intent(context, CastMediaNotificationService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            runCatching {
                val intent = Intent(context, CastMediaNotificationService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            }
        }
    }

    private var mediaSession: MediaSessionCompat? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastTitle: String = ""
    private var lastIsPlaying: Boolean = true
    private var lastPos: Long = 0L
    private var lastDur: Long = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initMediaSession()
        observeActiveStream()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                ActiveStreamHolder.stopCasting()
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_PLAY_PAUSE -> handlePlayPause()
            ACTION_REWIND -> handleSeekDelta(-10_000L)
            ACTION_FAST_FORWARD -> handleSeekDelta(10_000L)
            ACTION_START -> {
                val notification = buildMediaNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_STICKY
    }

    private fun handlePlayPause() {
        val currentPlaying = ActiveStreamHolder.isPlaying.value
        val nextAction = if (currentPlaying) "PAUSE" else "RESUME"
        ActiveStreamHolder.onSendPlayControl?.invoke(nextAction, ActiveStreamHolder.positionMs.value, 1f, null)
        ActiveStreamHolder.updatePlayback(!currentPlaying, ActiveStreamHolder.positionMs.value, ActiveStreamHolder.durationMs.value)
    }

    private fun handleSeekDelta(deltaMs: Long) {
        val curPos = ActiveStreamHolder.positionMs.value
        val maxDur = ActiveStreamHolder.durationMs.value.coerceAtLeast(1L)
        val newPos = (curPos + deltaMs).coerceIn(0L, maxDur)
        ActiveStreamHolder.onSendPlayControl?.invoke("SEEK", newPos, 1f, null)
        ActiveStreamHolder.updatePlayback(ActiveStreamHolder.isPlaying.value, newPos, ActiveStreamHolder.durationMs.value)
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "OmniCastMediaSession").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { handlePlayPause() }
                override fun onPause() { handlePlayPause() }
                override fun onStop() { ActiveStreamHolder.stopCasting() }
                override fun onSeekTo(pos: Long) {
                    ActiveStreamHolder.onSendPlayControl?.invoke("SEEK", pos, 1f, null)
                    ActiveStreamHolder.updatePlayback(ActiveStreamHolder.isPlaying.value, pos, ActiveStreamHolder.durationMs.value)
                }
                override fun onFastForward() { handleSeekDelta(10_000L) }
                override fun onRewind() { handleSeekDelta(-10_000L) }
            })
            isActive = true
        }
    }

    private fun observeActiveStream() {
        serviceScope.launch {
            launch {
                ActiveStreamHolder.isCasting.collect { casting ->
                    if (!casting) stopForegroundService()
                }
            }
            launch {
                ActiveStreamHolder.isPlaying.collect { playing ->
                    if (lastIsPlaying != playing) {
                        lastIsPlaying = playing
                        updateSessionPlaybackState()
                        updateNotification()
                    }
                }
            }
            launch {
                ActiveStreamHolder.currentMediaTitle.collect { title ->
                    if (lastTitle != title && title.isNotBlank()) {
                        lastTitle = title
                        updateSessionMetadata()
                        updateNotification()
                    }
                }
            }
            launch {
                ActiveStreamHolder.positionMs.collect { pos ->
                    lastPos = pos
                    updateSessionPlaybackState()
                }
            }
            launch {
                ActiveStreamHolder.durationMs.collect { dur ->
                    if (lastDur != dur) {
                        lastDur = dur
                        updateSessionMetadata()
                        updateSessionPlaybackState()
                    }
                }
            }
        }
    }

    private fun updateSessionMetadata() {
        val title = lastTitle.ifBlank { "Transmisión Web" }
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "OmniCast Caster")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, lastDur)
            .build()
        mediaSession?.setMetadata(metadata)
    }

    private fun updateSessionPlaybackState() {
        val state = if (lastIsPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_FAST_FORWARD or
                PlaybackStateCompat.ACTION_REWIND

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, lastPos, 1.0f)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    private fun buildMediaNotification(): Notification {
        val session = mediaSession ?: return NotificationCompat.Builder(this, CHANNEL_ID).build()
        val title = lastTitle.ifBlank { "Transmisión Web" }
        val prefs = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
        val tvIp = prefs.getString("saved_tv_ip", "TV Box") ?: "TV Box"

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, SenderMainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = createActionPendingIntent(ACTION_STOP, 1)
        val rewIntent = createActionPendingIntent(ACTION_REWIND, 2)
        val playPauseIntent = createActionPendingIntent(ACTION_PLAY_PAUSE, 3)
        val fwdIntent = createActionPendingIntent(ACTION_FAST_FORWARD, 4)

        val playPauseIcon = if (lastIsPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText("Enviando a $tvIp")
            .setSubText("OmniCast")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopIntent)
            .addAction(android.R.drawable.ic_media_rew, "-10s", rewIntent)
            .addAction(playPauseIcon, if (lastIsPlaying) "Pausar" else "Reproducir", playPauseIntent)
            .addAction(android.R.drawable.ic_media_ff, "+10s", fwdIntent)
            .setStyle(
                MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(1, 2, 3)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(stopIntent)
            )
            .build()
    }

    private fun createActionPendingIntent(action: String, reqCode: Int): PendingIntent {
        val intent = Intent(this, CastMediaNotificationService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, buildMediaNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OmniCast Reproducción Multimedia",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reproducción en pantalla de bloqueo y barra de estado"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundService() {
        serviceScope.cancel()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    override fun onDestroy() {
        stopForegroundService()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
