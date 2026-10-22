package com.omnicast.receiver.ui.components

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.media3.ui.PlayerView
import com.omnicast.receiver.R

/**
 * Estilizador dinámico para controles multimedia de reproducción en Android TV.
 * Garantiza que todo botón adquiera contorno, fondo cian de alta visibilidad y
 * escala ampliada (1.18x) al ser navegado mediante el mando a distancia (D-Pad).
 */
object TvMediaControlsStyler {

    fun applyTvFocusHighlight(playerView: PlayerView) {
        val context = playerView.context
        val selectorDrawable = ContextCompat.getDrawable(context, R.drawable.tv_media_control_button_selector)

        fun enhanceViewHierarchy(view: View) {
            if (view is ImageButton || view is Button) {
                view.isFocusable = true
                view.isFocusableInTouchMode = true
                if (selectorDrawable != null) {
                    view.background = selectorDrawable.constantState?.newDrawable()?.mutate() ?: selectorDrawable
                }
                val existingListener = view.onFocusChangeListener
                view.setOnFocusChangeListener { v, hasFocus ->
                    existingListener?.onFocusChange(v, hasFocus)
                    val targetScale = if (hasFocus) 1.18f else 1.0f
                    v.animate()
                        .scaleX(targetScale)
                        .scaleY(targetScale)
                        .setDuration(150)
                        .start()
                    v.elevation = if (hasFocus) 10f else 0f
                }
            } else if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    enhanceViewHierarchy(view.getChildAt(i))
                }
            }
        }

        playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
            if (visibility == View.VISIBLE) {
                playerView.post { enhanceViewHierarchy(playerView) }
            }
        })
        playerView.post { enhanceViewHierarchy(playerView) }
    }
}
