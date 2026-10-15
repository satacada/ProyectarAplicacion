package com.omnicast.receiver.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Icono vectorial moderno de salida (marco abierto con flecha saliente a la derecha).
 * Reemplaza a los emojis antiguos de puerta de forma limpia y estilizada.
 */
@Composable
fun ModernExitIcon(
    modifier: Modifier = Modifier.size(18.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.12f

        // Marco moderno rectangular abierto hacia la derecha
        val framePath = Path().apply {
            moveTo(w * 0.52f, h * 0.18f)
            lineTo(w * 0.20f, h * 0.18f)
            lineTo(w * 0.20f, h * 0.82f)
            lineTo(w * 0.52f, h * 0.82f)
        }
        drawPath(
            path = framePath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Flecha estilizada saliendo hacia la derecha
        drawLine(
            color = tint,
            start = Offset(w * 0.38f, h * 0.50f),
            end = Offset(w * 0.86f, h * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.66f, h * 0.30f),
            end = Offset(w * 0.86f, h * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.66f, h * 0.70f),
            end = Offset(w * 0.86f, h * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
