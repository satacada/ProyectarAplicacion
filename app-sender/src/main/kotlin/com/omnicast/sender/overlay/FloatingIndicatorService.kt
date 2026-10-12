package com.omnicast.sender.overlay

import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.omnicast.core.protocol.SignalingMessage
import com.omnicast.sender.service.ProjectionForegroundService
import com.omnicast.sender.stream.ClipboardStreamExtractor
import com.omnicast.sender.ui.SenderMainActivity

class FloatingIndicatorService : Service() {

    companion object {
        private const val TAG = "FloatingIndicatorService"
        var isRunning = false; private set
        private var instance: FloatingIndicatorService? = null

        fun updateMode() { instance?.let { it.handler.post { it.refreshDisplayMode() } } }

        fun start(context: Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)) {
                    context.startService(Intent(context, FloatingIndicatorService::class.java))
                }
            } catch (e: Throwable) { Log.e(TAG, "Error start", e) }
        }
        fun stop(context: Context) {
            try { context.stopService(Intent(context, FloatingIndicatorService::class.java)) } catch (e: Throwable) { Log.e(TAG, "Error stop", e) }
        }
    }

    private var windowManager: WindowManager? = null
    private var rootLayout: LinearLayout? = null
    private var pillLayoutRef: View? = null
    private var dotViewRef: View? = null
    private var menuControllerRef: FloatingViewBuilder.FloatingMenuController? = null
    private var layoutParamsRef: WindowManager.LayoutParams? = null
    private var isExpanded = false
    private lateinit var clipboardExtractor: ClipboardStreamExtractor
    private val handler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null

    fun refreshDisplayMode() {
        val pill = pillLayoutRef ?: return
        val dot = dotViewRef ?: return
        var changed = false
        if (pill.visibility != View.VISIBLE) { pill.visibility = View.VISIBLE; changed = true }
        if (dot.visibility != View.GONE) { dot.visibility = View.GONE; changed = true }
        if (changed) {
            try { windowManager?.updateViewLayout(rootLayout, layoutParamsRef) } catch (_: Throwable) {}
        }
        menuControllerRef?.applyMode(
            com.omnicast.sender.service.CastSessionState.getCurrentMode(),
            com.omnicast.sender.service.TvZoomManager.getCurrent().label
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true
        clipboardExtractor = ClipboardStreamExtractor(this)
        setupFloatingView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun setupFloatingView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) { stopSelf(); return }
        val builder = FloatingViewBuilder(this)
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager ?: run { stopSelf(); return }

        val windowLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = builder.dpToPx(16f); y = builder.dpToPx(120f) }

        rootLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.END }
        val ver = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (_: Throwable) { "3.32" }
        val pillText = TextView(this).apply { text = "OmniCast v$ver"; setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); typeface = Typeface.DEFAULT_BOLD }
        val prefs = getSharedPreferences("omnicast_prefs", Context.MODE_PRIVATE)
        val tvIp = prefs.getString("saved_tv_ip", "192.168.0.136") ?: "192.168.0.136"
        val statusFeedbackText = TextView(this).apply {
            text = "TV: $tvIp"
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, builder.dpToPx(6f), 0, builder.dpToPx(4f))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val pillLayout = builder.buildPill(pillText)
        val dotView = builder.buildDotView().apply { visibility = View.GONE }
        var menuLayoutRef: View? = null
        var exitConfirmRef: LinearLayout? = null
        val exitConfirmLayout = builder.buildExitConfirmation(
            onConfirmHideOverlay = { stopSelf() },
            onExitCompletely = {
                SenderMainActivity.exitCompletely(this)
                stopSelf()
            },
            onCancel = { exitConfirmRef?.visibility = View.GONE }
        )
        exitConfirmRef = exitConfirmLayout

        val menuController = builder.buildMenu(
            statusFeedbackText = statusFeedbackText,
            onDetectVideo = {
                val actUrl = com.omnicast.sender.stream.ActiveStreamHolder.streamUrl
                val actTitle = com.omnicast.sender.stream.ActiveStreamHolder.title ?: "Video Detectado"
                val actHdrs = com.omnicast.sender.stream.ActiveStreamHolder.headers
                if (!actUrl.isNullOrBlank()) {
                    if (ProjectionForegroundService.isProjecting) ProjectionForegroundService.stopService(this)
                    statusFeedbackText.text = "✅ Transmitiendo: ${actTitle.take(20)}"; statusFeedbackText.setTextColor(Color.parseColor("#10B981"))
                    SenderMainActivity.sendDirectStreamStatic(this, actUrl, actTitle, actHdrs)
                    Toast.makeText(this, "▶ Transmitiendo: ${actTitle.take(22)}", Toast.LENGTH_SHORT).show()
                } else {
                    statusFeedbackText.text = "🔍 Escaneando reproductor..."
                    statusFeedbackText.setTextColor(Color.parseColor("#38BDF8"))
                    SenderMainActivity.requestBrowserRescan(this)
                    handler.postDelayed({
                        val retryUrl = com.omnicast.sender.stream.ActiveStreamHolder.streamUrl
                        val retryTitle = com.omnicast.sender.stream.ActiveStreamHolder.title ?: "Video Detectado"
                        val retryHdrs = com.omnicast.sender.stream.ActiveStreamHolder.headers
                        if (!retryUrl.isNullOrBlank()) {
                            if (ProjectionForegroundService.isProjecting) ProjectionForegroundService.stopService(this)
                            statusFeedbackText.text = "✅ Transmitiendo: ${retryTitle.take(20)}"
                            statusFeedbackText.setTextColor(Color.parseColor("#10B981"))
                            SenderMainActivity.sendDirectStreamStatic(this, retryUrl, retryTitle, retryHdrs)
                            Toast.makeText(this, "▶ Transmitiendo: ${retryTitle.take(22)}", Toast.LENGTH_SHORT).show()
                        } else {
                            val res = clipboardExtractor.extractFromClipboard()
                            val clipUrl = res.mediaUrl
                            if (res.isSuccess && clipUrl != null) {
                                if (ProjectionForegroundService.isProjecting) ProjectionForegroundService.stopService(this)
                                statusFeedbackText.text = "✅ ¡Flujo Detectado! Transmitiendo..."
                                statusFeedbackText.setTextColor(Color.parseColor("#10B981"))
                                SenderMainActivity.sendDirectStreamStatic(this, clipUrl, "Video Capturado")
                            } else {
                                statusFeedbackText.text = "⚠️ Reproduce el video o pulsa 'Transmitir'."
                                statusFeedbackText.setTextColor(Color.parseColor("#F59E0B"))
                                Toast.makeText(this, "⚠️ Dale Play al video en la página para capturar el flujo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, 800)
                }
            },
            onStartScreenMirror = {
                menuLayoutRef?.visibility = View.GONE; isExpanded = false
                SenderMainActivity.sendPlayControlStatic(this, "STOP")
                com.omnicast.sender.stream.ActiveStreamHolder.clear()
                if (ProjectionForegroundService.isProjecting) {
                    statusFeedbackText.text = "🟢 Ya estás proyectando."; statusFeedbackText.setTextColor(Color.parseColor("#10B981"))
                } else {
                    launchIntentSafely(Intent(this, TransparentCaptureActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }, 103)
                }
            },
            onCastGallery = {
                menuLayoutRef?.visibility = View.GONE; isExpanded = false
                if (ProjectionForegroundService.isProjecting) ProjectionForegroundService.stopService(this)
                SenderMainActivity.sendPlayControlStatic(this, "STOP")
                com.omnicast.sender.stream.ActiveStreamHolder.clear()
                launchIntentSafely(Intent(this, GalleryPickerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }, 104)
            },
            onOpenBrowser = {
                menuLayoutRef?.visibility = View.GONE; isExpanded = false
                launchIntentSafely(Intent(this, SenderMainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("EXTRA_OPEN_BROWSER", true)
                }, 105)
            },
            onToggleTvScale = {
                val preset = com.omnicast.sender.service.TvZoomManager.cycleNext()
                val msg = com.omnicast.sender.service.TvZoomManager.toSignalingMessage(preset)
                prefs.edit().putString("saved_tv_scale_mode", preset.mode).apply()
                if (ProjectionForegroundService.isProjecting) {
                    ProjectionForegroundService.setDisplayMode(preset.mode, preset.zoom, preset.panY)
                } else {
                    com.omnicast.sender.network.QuickSignalingHelper.sendQuickMessage(this, msg)
                }
                statusFeedbackText.text = preset.label
                statusFeedbackText.setTextColor(Color.parseColor("#38BDF8"))
                menuControllerRef?.updateZoomLabel(preset.label)
                Toast.makeText(this, preset.label, Toast.LENGTH_SHORT).show()
            },
            onOpenOmniCast = {
                menuLayoutRef?.visibility = View.GONE; isExpanded = false
                launchIntentSafely(Intent(this, SenderMainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP) }, 102)
            },
            onStopCast = {
                menuLayoutRef?.visibility = View.GONE; isExpanded = false
                ProjectionForegroundService.stopService(this)
                SenderMainActivity.sendPlayControlStatic(this, "STOP")
                com.omnicast.sender.service.CastMediaNotificationService.stop(this)
                com.omnicast.sender.stream.ActiveStreamHolder.stopCasting()
                com.omnicast.sender.stream.ActiveStreamHolder.clear()
                pillText.text = "OmniCast"; statusFeedbackText.text = "🛑 Transmisión detenida."
                statusFeedbackText.setTextColor(Color.parseColor("#EF4444"))
                Toast.makeText(this, "🛑 Transmisión detenida", Toast.LENGTH_SHORT).show()
            },
            onDisableOverlay = {
                menuLayoutRef?.visibility = View.GONE; isExpanded = false
                exitConfirmRef?.visibility = View.VISIBLE
            }
        )
        menuControllerRef = menuController
        val menuLayout = menuController.rootView
        menuLayoutRef = menuLayout

        pillLayoutRef = pillLayout; dotViewRef = dotView; layoutParamsRef = windowLayoutParams
        rootLayout?.addView(dotView); rootLayout?.addView(pillLayout); rootLayout?.addView(menuLayout); rootLayout?.addView(exitConfirmLayout)

        val toggleMenu = {
            if (exitConfirmLayout.visibility == View.VISIBLE) exitConfirmLayout.visibility = View.GONE
            else {
                isExpanded = !isExpanded
                if (isExpanded) {
                    menuController.applyMode(
                        com.omnicast.sender.service.CastSessionState.getCurrentMode(),
                        com.omnicast.sender.service.TvZoomManager.getCurrent().label
                    )
                }
                menuLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
            }
        }
        pillLayout.setOnClickListener { toggleMenu() }
        dotView.setOnClickListener { toggleMenu() }

        builder.attachDragToMove(pillLayout, windowLayoutParams, windowManager, rootLayout) { menuLayout.visibility = View.GONE; exitConfirmLayout.visibility = View.VISIBLE }
        builder.attachDragToMove(dotView, windowLayoutParams, windowManager, rootLayout) { menuLayout.visibility = View.GONE; exitConfirmLayout.visibility = View.VISIBLE }

        pollRunnable = Runnable {
            refreshDisplayMode()
            if (isExpanded) {
                if (com.omnicast.sender.stream.ActiveStreamHolder.isCasting.value) {
                    val t = com.omnicast.sender.stream.ActiveStreamHolder.currentMediaTitle.value
                    if (t.isNotBlank()) {
                        statusFeedbackText.text = "✅ Transmitiendo: ${t.take(20)}"
                        statusFeedbackText.setTextColor(Color.parseColor("#10B981"))
                    }
                } else if (!com.omnicast.sender.stream.ActiveStreamHolder.hasStream() && statusFeedbackText.text.startsWith("✅ Transmitiendo:")) {
                    statusFeedbackText.text = "TV: $tvIp"
                    statusFeedbackText.setTextColor(Color.parseColor("#94A3B8"))
                }
            }
            handler.postDelayed(pollRunnable!!, 1000)
        }
        handler.post(pollRunnable!!)

        try { windowManager?.addView(rootLayout, windowLayoutParams); Log.i(TAG, "Asistente flotante agregado") } catch (e: Throwable) { Log.e(TAG, "Error vista flotante", e) }
    }

    private fun launchIntentSafely(intent: Intent, reqCode: Int) {
        try {
            val bundle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ActivityOptions.makeBasic().apply { setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED) }.toBundle()
            } else null
            PendingIntent.getActivity(this, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE, bundle).send()
        } catch (_: Throwable) { try { startActivity(intent) } catch (t: Throwable) { Log.e(TAG, "Error launch", t) } }
    }

    override fun onDestroy() {
        super.onDestroy(); isRunning = false; instance = null
        pollRunnable?.let { handler.removeCallbacks(it) }
        try { rootLayout?.let { windowManager?.removeView(it) } } catch (e: Throwable) { Log.e(TAG, "Error remove", e) }
        rootLayout = null; pillLayoutRef = null; dotViewRef = null; menuControllerRef = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
