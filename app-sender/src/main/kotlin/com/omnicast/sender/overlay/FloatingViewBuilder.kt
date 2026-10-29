package com.omnicast.sender.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class FloatingViewBuilder(private val context: Context) {

    fun dpToPx(dp: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()

    fun buildPill(pillText: TextView): LinearLayout {
        val pillDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#E60D111A")); cornerRadius = dpToPx(20f).toFloat(); setStroke(dpToPx(1.2f), Color.parseColor("#10B981"))
        }
        val dotView = View(context).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#10B981")); setSize(dpToPx(10f), dpToPx(10f)) }
            layoutParams = LinearLayout.LayoutParams(dpToPx(10f), dpToPx(10f)).apply { marginEnd = dpToPx(8f) }
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = pillDrawable
            setPadding(dpToPx(12f), dpToPx(8f), dpToPx(14f), dpToPx(8f)); addView(dotView); addView(pillText)
        }
    }

    fun buildDotView(): View {
        val container = android.widget.FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(44f), dpToPx(44f))
        }
        val dotDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#10B981"))
            setStroke(dpToPx(2f), Color.parseColor("#34D399"))
        }
        val innerDot = View(context).apply {
            background = dotDrawable
            layoutParams = android.widget.FrameLayout.LayoutParams(dpToPx(20f), dpToPx(20f)).apply {
                gravity = Gravity.CENTER
            }
            elevation = dpToPx(4f).toFloat()
        }
        container.addView(innerDot)
        return container
    }

    class FloatingMenuController(
        val rootView: View,
        private val applyModeFn: (com.omnicast.sender.service.ActiveCastMode, String) -> Unit,
        private val updateZoomLabelFn: (String) -> Unit
    ) {
        fun applyMode(mode: com.omnicast.sender.service.ActiveCastMode, zoomLabel: String) {
            applyModeFn(mode, zoomLabel)
        }
        fun updateZoomLabel(label: String) {
            updateZoomLabelFn(label)
        }
    }

    fun buildMenu(
        statusFeedbackText: TextView,
        onDetectVideo: () -> Unit,
        onStartFullScreen: () -> Unit,
        onStartAppProjection: () -> Unit,
        onCastGallery: () -> Unit,
        onOpenBrowser: () -> Unit,
        onToggleTvScale: () -> Unit,
        onCalibrateTv: () -> Unit = {},
        onOpenOmniCast: () -> Unit,
        onStopCast: () -> Unit,
        onDisableOverlay: () -> Unit
    ): FloatingMenuController {
        val menuDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#F80D111A")); cornerRadius = dpToPx(16f).toFloat(); setStroke(dpToPx(1.2f), Color.parseColor("#28354E"))
        }
        val innerMenu = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(14f), dpToPx(14f), dpToPx(14f), dpToPx(14f))
        }
        val menuTitle = TextView(context).apply {
            text = "OmniCast Asistente"; setTextColor(Color.parseColor("#38BDF8")); setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); typeface = Typeface.DEFAULT_BOLD
        }
        innerMenu.addView(menuTitle)

        val btnStopCast = createBtn("🛑 Detener", "#DC2626", onStopCast)
        val btnZoomTv = createBtn("🔍 Zoom TV", "#6D28D9", onToggleTvScale)
        val btnCalibrate = createBtn("🤏 Calibrar Pantalla (Zoom/Arrastre)", "#0284C7", onCalibrateTv)
        val btnDetectVideo = createBtn("🔍 Detectar Flujo de Video", "#00B4D8", onDetectVideo)
        val btnOpenOmni = createBtn("🏠 Proyectar", "#1E2738", onOpenOmniCast)

        val rowMirror = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpToPx(2f); bottomMargin = dpToPx(2f)
            }
        }
        val btnMirrorFull = Button(context).apply {
            text = "📱 Pantalla"; setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f); setBackgroundColor(Color.parseColor("#7C3AED"))
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(38f), 1f).apply { marginEnd = dpToPx(3f) }
            setOnClickListener { onStartFullScreen() }
        }
        val btnMirrorApp = Button(context).apply {
            text = "📱 Por App"; setTextColor(Color.parseColor("#C7D2FE")); setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f); setBackgroundColor(Color.parseColor("#4F46E5"))
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(38f), 1f).apply { marginStart = dpToPx(3f) }
            setOnClickListener { onStartAppProjection() }
        }
        rowMirror.addView(btnMirrorFull); rowMirror.addView(btnMirrorApp)

        val btnGallery = createBtn("🖼️ Galería (Fotos)", "#E11D48", onCastGallery)
        val btnOpenBrowser = createBtn("🌐 Captura Web", "#D97706", onOpenBrowser)
        val btnDisableOverlay = createBtn("❌ Desactivar Asistente", "#334155", onDisableOverlay)

        innerMenu.addView(btnStopCast)
        innerMenu.addView(btnZoomTv)
        innerMenu.addView(btnCalibrate)
        innerMenu.addView(btnDetectVideo)
        innerMenu.addView(btnOpenOmni)
        innerMenu.addView(rowMirror)
        innerMenu.addView(btnGallery)
        innerMenu.addView(btnOpenBrowser)
        innerMenu.addView(btnDisableOverlay)
        innerMenu.addView(statusFeedbackText)

        val scrollView = object : android.widget.ScrollView(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val maxH = dpToPx(380f)
                val customHeightSpec = MeasureSpec.makeMeasureSpec(maxH, MeasureSpec.AT_MOST)
                super.onMeasure(widthMeasureSpec, customHeightSpec)
            }
        }.apply {
            isVerticalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
            verticalScrollbarPosition = View.SCROLLBAR_POSITION_LEFT
            background = menuDrawable; visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(dpToPx(240f), LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dpToPx(8f) }
            addView(innerMenu)
        }

        fun applyMode(mode: com.omnicast.sender.service.ActiveCastMode, zoomLabel: String) {
            btnZoomTv.text = "🔍 Zoom TV: $zoomLabel"
            btnZoomTv.visibility = View.VISIBLE
            btnStopCast.text = "🛑 Detener"
            btnOpenOmni.text = if (mode == com.omnicast.sender.service.ActiveCastMode.SCREEN_MIRROR) "⚙️ Configuración Proyectar" else "🏠 Proyectar"
            btnCalibrate.visibility = if (mode == com.omnicast.sender.service.ActiveCastMode.SCREEN_MIRROR) View.VISIBLE else View.GONE
            when (mode) {
                com.omnicast.sender.service.ActiveCastMode.SCREEN_MIRROR -> {
                    btnStopCast.visibility = View.VISIBLE
                    btnOpenOmni.visibility = View.VISIBLE
                    btnDisableOverlay.visibility = View.VISIBLE
                    btnDetectVideo.visibility = View.GONE
                    rowMirror.visibility = View.GONE
                    btnGallery.visibility = View.GONE
                    btnOpenBrowser.visibility = View.GONE
                }
                com.omnicast.sender.service.ActiveCastMode.GALLERY -> {
                    btnStopCast.visibility = View.VISIBLE
                    btnOpenOmni.visibility = View.VISIBLE
                    btnDisableOverlay.visibility = View.VISIBLE
                    btnDetectVideo.visibility = View.GONE
                    rowMirror.visibility = View.GONE
                    btnGallery.visibility = View.GONE
                    btnOpenBrowser.visibility = View.GONE
                }
                com.omnicast.sender.service.ActiveCastMode.WEB_STREAM -> {
                    btnStopCast.visibility = View.VISIBLE
                    btnDetectVideo.visibility = View.VISIBLE
                    btnOpenOmni.visibility = View.VISIBLE
                    btnDisableOverlay.visibility = View.VISIBLE
                    rowMirror.visibility = View.GONE
                    btnGallery.visibility = View.GONE
                    btnOpenBrowser.visibility = View.GONE
                }
                com.omnicast.sender.service.ActiveCastMode.IDLE -> {
                    val casting = com.omnicast.sender.stream.ActiveStreamHolder.isCasting.value || !com.omnicast.sender.stream.ActiveStreamHolder.streamUrl.isNullOrBlank()
                    btnStopCast.visibility = if (casting) View.VISIBLE else View.GONE
                    btnDetectVideo.visibility = View.VISIBLE
                    btnOpenOmni.visibility = View.VISIBLE
                    rowMirror.visibility = View.VISIBLE
                    btnOpenBrowser.visibility = View.VISIBLE
                    btnGallery.visibility = View.VISIBLE
                    btnDisableOverlay.visibility = View.VISIBLE
                }
            }
        }

        return FloatingMenuController(
            rootView = scrollView,
            applyModeFn = ::applyMode,
            updateZoomLabelFn = { label -> btnZoomTv.text = "🔍 Zoom TV: $label" }
        )
    }

    fun buildExitConfirmation(
        onConfirmHideOverlay: () -> Unit,
        onExitCompletely: () -> Unit,
        onCancel: () -> Unit
    ): LinearLayout {
        val bg = GradientDrawable().apply {
            setColor(Color.parseColor("#F80D111A")); cornerRadius = dpToPx(14f).toFloat(); setStroke(dpToPx(1.2f), Color.parseColor("#EF4444"))
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; background = bg; setPadding(dpToPx(14f), dpToPx(14f), dpToPx(14f), dpToPx(14f)); visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(dpToPx(240f), LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dpToPx(8f) }
            addView(TextView(context).apply { text = "¿Opciones de Cierre?"; setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); typeface = Typeface.DEFAULT_BOLD })
            addView(createBtn("❌ Solo Ocultar Asistente", "#334155", onConfirmHideOverlay))
            addView(createBtn("⏹ Salir de la App", "#DC2626", onExitCompletely))
            addView(createBtn("↩ Continuar", "#1E293B", onCancel))
        }
    }

    private fun createBtn(txt: String, colorHex: String, onClick: () -> Unit) = Button(context).apply {
        text = txt; setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        setBackgroundColor(Color.parseColor(colorHex)); setOnClickListener { onClick() }
    }

    fun attachDragToMove(
        view: View,
        layoutParams: WindowManager.LayoutParams,
        windowManager: WindowManager?,
        rootLayout: View?,
        onLongPress2s: (() -> Unit)? = null
    ) {
        var initialX = 0; var initialY = 0; var initialTouchX = 0f; var initialTouchY = 0f; var isMoving = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var runnable: Runnable? = null

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x; initialY = layoutParams.y; initialTouchX = event.rawX; initialTouchY = event.rawY; isMoving = false
                    runnable = Runnable { if (!isMoving) onLongPress2s?.invoke() }
                    handler.postDelayed(runnable!!, 2000)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (initialTouchX - event.rawX).toInt(); val deltaY = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(deltaX) > dpToPx(4f) || Math.abs(deltaY) > dpToPx(4f)) {
                        isMoving = true
                        runnable?.let { handler.removeCallbacks(it) }
                        layoutParams.x = initialX + deltaX; layoutParams.y = initialY + deltaY
                        try { windowManager?.updateViewLayout(rootLayout, layoutParams) } catch (_: Throwable) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    runnable?.let { handler.removeCallbacks(it) }
                    if (!isMoving) { v.performClick() }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    runnable?.let { handler.removeCallbacks(it) }
                    true
                }
                else -> false
            }
        }
    }
}
