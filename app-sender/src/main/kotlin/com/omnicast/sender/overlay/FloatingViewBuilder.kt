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
            setColor(Color.parseColor("#E60F172A")); cornerRadius = dpToPx(20f).toFloat(); setStroke(dpToPx(1f), Color.parseColor("#38BDF8"))
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

    fun buildMenu(
        statusFeedbackText: TextView,
        onDetectVideo: () -> Unit,
        onStartScreenMirror: () -> Unit,
        onCastGallery: () -> Unit,
        onToggleTvScale: () -> Unit,
        onOpenOmniCast: () -> Unit,
        onStopCast: () -> Unit
    ): LinearLayout {
        val menuDrawable = GradientDrawable().apply {
            setColor(Color.parseColor("#F80F172A")); cornerRadius = dpToPx(14f).toFloat(); setStroke(dpToPx(1f), Color.parseColor("#334155"))
        }
        val menuLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; background = menuDrawable
            setPadding(dpToPx(14f), dpToPx(14f), dpToPx(14f), dpToPx(14f)); visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(dpToPx(240f), LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dpToPx(8f) }
        }
        val menuTitle = TextView(context).apply {
            text = "OmniCast Asistente"; setTextColor(Color.parseColor("#38BDF8")); setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); typeface = Typeface.DEFAULT_BOLD
        }
        menuLayout.addView(menuTitle); menuLayout.addView(statusFeedbackText)
        menuLayout.addView(createBtn("🔍 Detectar Flujo de Video", "#0284C7", onDetectVideo))
        menuLayout.addView(createBtn("🖼️ Enviar Foto con Zoom Táctil", "#059669", onCastGallery))
        menuLayout.addView(createBtn("📺 Cambiar Modo TV (16:9 / Fit)", "#8B5CF6", onToggleTvScale))
        menuLayout.addView(createBtn("📱 Proyectar Pantalla (Toda)", "#2563EB", onStartScreenMirror))
        menuLayout.addView(createBtn("⚙️ Abrir OmniCast", "#334155", onOpenOmniCast))
        menuLayout.addView(createBtn("🛑 Detener Transmisión", "#DC2626", onStopCast))
        return menuLayout
    }

    fun buildExitConfirmation(onConfirmClose: () -> Unit, onCancel: () -> Unit): LinearLayout {
        val bg = GradientDrawable().apply {
            setColor(Color.parseColor("#F80F172A")); cornerRadius = dpToPx(14f).toFloat(); setStroke(dpToPx(1f), Color.parseColor("#EF4444"))
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; background = bg; setPadding(dpToPx(14f), dpToPx(14f), dpToPx(14f), dpToPx(14f)); visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(dpToPx(240f), LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dpToPx(8f) }
            addView(TextView(context).apply { text = "¿Cerrar Asistente?"; setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); typeface = Typeface.DEFAULT_BOLD })
            addView(createBtn("❌ Sí, Cerrar", "#DC2626", onConfirmClose))
            addView(createBtn("↩ Continuar", "#334155", onCancel))
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
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (initialTouchX - event.rawX).toInt(); val deltaY = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isMoving = true
                        runnable?.let { handler.removeCallbacks(it) }
                        layoutParams.x = initialX + deltaX; layoutParams.y = initialY + deltaY
                        windowManager?.updateViewLayout(rootLayout, layoutParams)
                        true
                    } else false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    runnable?.let { handler.removeCallbacks(it) }
                    if (isMoving) true else v.performClick()
                }
                else -> false
            }
        }
    }
}
