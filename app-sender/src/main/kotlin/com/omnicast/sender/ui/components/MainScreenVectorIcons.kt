package com.omnicast.sender.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun VectorTvIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF8B5CF6)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, h * 0.15f),
            size = Size(w * 0.76f, h * 0.58f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.38f, h * 0.85f),
            end = Offset(w * 0.62f, h * 0.85f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.73f),
            end = Offset(w * 0.50f, h * 0.85f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun VectorGlobeIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFBBF24)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()
        val cx = w * 0.5f
        val cy = h * 0.5f
        val r = w * 0.40f

        drawCircle(color = color, radius = r, center = Offset(cx, cy), style = Stroke(width = stroke))
        drawLine(color = color, start = Offset(cx - r, cy), end = Offset(cx + r, cy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawOval(
            color = color,
            topLeft = Offset(cx - r * 0.45f, cy - r),
            size = Size(r * 0.90f, r * 2.0f),
            style = Stroke(width = stroke)
        )
    }
}

@Composable
fun VectorArrowRightIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFBBF24)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()
        val cy = h * 0.5f

        drawLine(color = color, start = Offset(w * 0.15f, cy), end = Offset(w * 0.80f, cy), strokeWidth = stroke, cap = StrokeCap.Round)
        val arrowHead = Path().apply {
            moveTo(w * 0.55f, cy - h * 0.28f)
            lineTo(w * 0.82f, cy)
            lineTo(w * 0.55f, cy + h * 0.28f)
        }
        drawPath(path = arrowHead, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun VectorImageIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFF43F5E)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, h * 0.15f),
            size = Size(w * 0.76f, h * 0.70f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(color = color, radius = w * 0.08f, center = Offset(w * 0.35f, h * 0.38f))
        val mountain = Path().apply {
            moveTo(w * 0.20f, h * 0.75f)
            lineTo(w * 0.46f, h * 0.48f)
            lineTo(w * 0.62f, h * 0.62f)
            lineTo(w * 0.74f, h * 0.52f)
            lineTo(w * 0.84f, h * 0.75f)
        }
        drawPath(path = mountain, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun VectorGearIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFDDD6FE)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val stroke = 1.6.dp.toPx()

        drawCircle(color = color, radius = w * 0.22f, center = Offset(cx, cy), style = Stroke(width = stroke))
        drawCircle(color = color, radius = w * 0.38f, center = Offset(cx, cy), style = Stroke(width = stroke))
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60.0))
            val x1 = (cx + Math.cos(angle) * w * 0.30f).toFloat()
            val y1 = (cy + Math.sin(angle) * h * 0.30f).toFloat()
            val x2 = (cx + Math.cos(angle) * w * 0.46f).toFloat()
            val y2 = (cy + Math.sin(angle) * h * 0.46f).toFloat()
            drawLine(color = color, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = stroke * 1.5f, cap = StrokeCap.Round)
        }
    }
}
