package com.omnicast.receiver.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Insignia OSD flotante de grabación PVR y descarga de películas para Android TV.
 * Muestra el clásico punto rojo parpadeante "🔴 REC" o "🔴 GUARDANDO XX%" y el tiempo transcurrido.
 */
@Composable
fun TvRecFloatingBadge(
    isRecording: Boolean,
    durationSeconds: Long,
    remainingSeconds: Long = -1L,
    isMovieDownload: Boolean = false,
    progressPct: Int = 0,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = isRecording,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "RecBlink")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(650, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "RedDotAlpha"
        )

        val clickableMod = if (onClick != null) {
            Modifier.androidxClickable(onClick)
        } else Modifier

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xDD0B132B))
                .border(1.5.dp, Color(0xFFDC2626), RoundedCornerShape(10.dp))
                .then(clickableMod)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Punto rojo parpadeante
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDC2626).copy(alpha = alpha))
            )

            // Texto REC o GUARDANDO
            Text(
                text = if (isMovieDownload) "GUARDANDO" else "REC",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            if (isMovieDownload) {
                Text(
                    text = "$progressPct%",
                    color = Color(0xFF38BDF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Text(
                text = formatDuration(durationSeconds * 1000L),
                color = Color(0xFFFCA5A5),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Cuenta regresiva si hay temporizador
            if (remainingSeconds > 0L) {
                Text(
                    text = "(-${formatDuration(remainingSeconds * 1000L)})",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun Modifier.androidxClickable(onClick: () -> Unit): Modifier =
    this.clickable { onClick() }

