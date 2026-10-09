package com.omnicast.sender.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Icono moderno de marcador / guardar en lista (Bookmark Outline).
 * Inspirado en diseño estándar limpio y minimalista.
 */
@Composable
fun BookmarkOutlineIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.15f)
            lineTo(w * 0.78f, h * 0.15f)
            lineTo(w * 0.78f, h * 0.85f)
            lineTo(w * 0.50f, h * 0.64f)
            lineTo(w * 0.22f, h * 0.85f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Icono moderno de papelera / eliminar (Trash Can).
 */
@Composable
fun ModernTrashIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()

        // Tapa superior
        drawLine(
            color = color,
            start = Offset(w * 0.18f, h * 0.28f),
            end = Offset(w * 0.82f, h * 0.28f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Manija de la tapa
        drawLine(
            color = color,
            start = Offset(w * 0.40f, h * 0.18f),
            end = Offset(w * 0.60f, h * 0.18f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Cuerpo del cesto
        val body = Path().apply {
            moveTo(w * 0.27f, h * 0.28f)
            lineTo(w * 0.31f, h * 0.84f)
            lineTo(w * 0.69f, h * 0.84f)
            lineTo(w * 0.73f, h * 0.28f)
        }
        drawPath(
            path = body,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Ranuras verticales interiores
        drawLine(
            color = color,
            start = Offset(w * 0.43f, h * 0.38f),
            end = Offset(w * 0.43f, h * 0.74f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.57f, h * 0.38f),
            end = Offset(w * 0.57f, h * 0.74f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Icono moderno de casteo a pantalla / TV (Cast Streaming).
 */
@Composable
fun CastTvIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        // Contorno de pantalla TV con hueco en la esquina inferior izquierda para las ondas
        val tvPath = Path().apply {
            moveTo(w * 0.12f, h * 0.54f)
            lineTo(w * 0.12f, h * 0.22f)
            lineTo(w * 0.88f, h * 0.22f)
            lineTo(w * 0.88f, h * 0.78f)
            lineTo(w * 0.54f, h * 0.78f)
        }
        drawPath(
            path = tvPath,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Punto emisor de casteo
        drawCircle(
            color = color,
            radius = stroke * 0.9f,
            center = Offset(w * 0.18f, h * 0.75f)
        )

        // Primer arco de onda
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.08f, h * 0.52f),
            size = Size(w * 0.36f, h * 0.36f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // Segundo arco de onda
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(-w * 0.02f, h * 0.32f),
            size = Size(w * 0.60f, h * 0.60f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

/**
 * Icono de Rayo con cruz roja superpuesta (⚡✕ Reset / Recapturar Flujos).
 */
@Composable
fun RayCrossIcon(
    modifier: Modifier = Modifier,
    rayColor: Color = Color(0xFFFBBF24),
    crossColor: Color = Color(0xFFEF4444)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        // Rayo principal
        val rayPath = Path().apply {
            moveTo(w * 0.52f, h * 0.10f)
            lineTo(w * 0.22f, h * 0.52f)
            lineTo(w * 0.48f, h * 0.52f)
            lineTo(w * 0.36f, h * 0.90f)
            lineTo(w * 0.76f, h * 0.44f)
            lineTo(w * 0.52f, h * 0.44f)
            close()
        }
        drawPath(path = rayPath, color = rayColor)

        // Cruz roja distintiva en la esquina inferior derecha
        val cx = w * 0.68f
        val cy = h * 0.68f
        val r = w * 0.22f
        drawCircle(color = Color(0xFF1E1520), radius = r + stroke, center = Offset(cx, cy))
        drawLine(crossColor, Offset(cx - r * 0.7f, cy - r * 0.7f), Offset(cx + r * 0.7f, cy + r * 0.7f), strokeWidth = stroke * 1.3f, cap = StrokeCap.Round)
        drawLine(crossColor, Offset(cx + r * 0.7f, cy - r * 0.7f), Offset(cx - r * 0.7f, cy + r * 0.7f), strokeWidth = stroke * 1.3f, cap = StrokeCap.Round)
    }
}

/**
 * Icono de Marcador con cruz roja superpuesta (🔖✕ Quitar de lista / guardados).
 */
@Composable
fun BookmarkRemoveIcon(
    modifier: Modifier = Modifier,
    bookmarkColor: Color = Color(0xFF38BDF8),
    crossColor: Color = Color(0xFFEF4444)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        // Contorno de marcador
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.15f)
            lineTo(w * 0.78f, h * 0.15f)
            lineTo(w * 0.78f, h * 0.85f)
            lineTo(w * 0.50f, h * 0.64f)
            lineTo(w * 0.22f, h * 0.85f)
            close()
        }
        drawPath(
            path = path,
            color = bookmarkColor,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Cruz roja en la esquina inferior derecha
        val cx = w * 0.68f
        val cy = h * 0.68f
        val r = w * 0.22f
        drawCircle(color = Color(0xFF1E1520), radius = r + stroke, center = Offset(cx, cy))
        drawLine(crossColor, Offset(cx - r * 0.7f, cy - r * 0.7f), Offset(cx + r * 0.7f, cy + r * 0.7f), strokeWidth = stroke * 1.3f, cap = StrokeCap.Round)
        drawLine(crossColor, Offset(cx + r * 0.7f, cy - r * 0.7f), Offset(cx - r * 0.7f, cy + r * 0.7f), strokeWidth = stroke * 1.3f, cap = StrokeCap.Round)
    }
}
