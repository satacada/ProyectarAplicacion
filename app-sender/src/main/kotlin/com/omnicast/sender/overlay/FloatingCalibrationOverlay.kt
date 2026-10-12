package com.omnicast.sender.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.omnicast.sender.service.ProjectionForegroundService

/**
 * Lienzo táctil transparente flotante para calibrar la proyección en la TV.
 * Permite usar 2 dedos (pellizcar/expandir) para Zoom y 1 dedo (arrastre) para Pan/Mover en tiempo real.
 */
object FloatingCalibrationOverlay {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var currentZoom = 1.0f
    private var currentPanX = 0.0f
    private var currentPanY = 0.0f

    private fun dpToPx(context: Context, dp: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()

    @SuppressLint("ClickableViewAccessibility")
    fun show(context: Context) {
        if (overlayView != null) return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        windowManager = wm

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.argb(80, 15, 23, 42)) // 30% slate transparente para ver debajo
        }

        // Tarjeta flotante superior con instrucciones y controles
        val cardDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#F00F172A"))
            cornerRadius = dpToPx(context, 16f).toFloat()
            setStroke(dpToPx(context, 1.2f), Color.parseColor("#38BDF8"))
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = cardDrawable
            setPadding(dpToPx(context, 16f), dpToPx(context, 14f), dpToPx(context, 16f), dpToPx(context, 14f))
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(context, 320f),
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = dpToPx(context, 48f)
            }
        }

        val title = TextView(context).apply {
            text = "🎛️ Calibrar Pantalla en TV"
            setTextColor(Color.parseColor("#38BDF8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.DEFAULT_BOLD
        }
        val instructions = TextView(context).apply {
            text = "• Pellizca con 2 dedos para Zoom\n• Arrastra con 1 dedo para Mover"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dpToPx(context, 6f), 0, dpToPx(context, 6f))
        }
        val statusText = TextView(context).apply {
            text = "Zoom: 100% | X: 0 | Y: 0"
            setTextColor(Color.parseColor("#10B981"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.MONOSPACE
        }

        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(context, 10f), 0, 0)
        }

        fun createBtn(label: String, colorHex: String, onClick: () -> Unit): Button {
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor(colorHex))
                cornerRadius = dpToPx(context, 10f).toFloat()
            }
            return Button(context).apply {
                text = label
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.DEFAULT_BOLD
                background = bg
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dpToPx(context, 38f)
                ).apply { marginEnd = dpToPx(context, 8f) }
                setOnClickListener { onClick() }
            }
        }

        val btnReset = createBtn("🔄 Restablecer", "#475569") {
            currentZoom = 1.0f
            currentPanX = 0.0f
            currentPanY = 0.0f
            statusText.text = "Zoom: 100% | X: 0 | Y: 0"
            sendTransform("FIT", 1.0f, 0f, 0f, context)
        }

        val btnDone = createBtn("✅ Listo", "#10B981") {
            dismiss()
        }

        btnRow.addView(btnReset)
        btnRow.addView(btnDone)

        card.addView(title)
        card.addView(instructions)
        card.addView(statusText)
        card.addView(btnRow)
        root.addView(card)

        // Detección de gestos táctiles (Scale + Pan)
        var lastTouchX = 0f
        var lastTouchY = 0f
        var isDragging = false

        val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                currentZoom = (currentZoom * detector.scaleFactor).coerceIn(0.6f, 3.0f)
                statusText.text = String.format(java.util.Locale.US, "Zoom: %.0f%% | X: %.0f | Y: %.0f", currentZoom * 100f, currentPanX, currentPanY)
                sendTransform("ZOOM", currentZoom, currentPanY, currentPanX, context)
                return true
            }
        })

        root.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    isDragging = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging && !scaleDetector.isInProgress && event.pointerCount == 1) {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        lastTouchX = event.x
                        lastTouchY = event.y
                        currentPanX += dx * 0.8f
                        currentPanY += dy * 0.8f
                        statusText.text = String.format(java.util.Locale.US, "Zoom: %.0f%% | X: %.0f | Y: %.0f", currentZoom * 100f, currentPanX, currentPanY)
                        sendTransform("ZOOM", currentZoom, currentPanY, currentPanX, context)
                    }
                }
                MotionEvent.ACTION_POINTER_DOWN -> isDragging = false
                MotionEvent.ACTION_POINTER_UP -> {
                    val remainingIdx = if (event.actionIndex == 0) 1 else 0
                    if (event.pointerCount > 1) {
                        lastTouchX = event.getX(remainingIdx)
                        lastTouchY = event.getY(remainingIdx)
                        isDragging = true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isDragging = false
            }
            true
        }

        try {
            wm.addView(root, params)
            overlayView = root
        } catch (e: Throwable) {
            android.util.Log.e("FloatingCalibration", "Error mostrando calibrador", e)
        }
    }

    private fun sendTransform(mode: String, zoom: Float, panY: Float, panX: Float, context: Context) {
        if (ProjectionForegroundService.isProjecting) {
            ProjectionForegroundService.setDisplayMode(mode, zoom, panY, panX)
        } else {
            val msg = com.omnicast.core.protocol.SignalingMessage.SetDisplayMode(mode, zoom, panY, panX)
            com.omnicast.sender.network.QuickSignalingHelper.sendQuickMessage(context, msg)
        }
    }

    fun dismiss() {
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (_: Throwable) {}
        }
        overlayView = null
        windowManager = null
    }
}
