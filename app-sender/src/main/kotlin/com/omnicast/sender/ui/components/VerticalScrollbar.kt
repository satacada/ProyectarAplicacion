package com.omnicast.sender.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Barra de desplazamiento vertical estilizada para LazyColumn.
 * Refleja visualmente la posición del scroll y permite navegación rápida.
 */
@Composable
fun VerticalScrollbar(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    width: Dp = 5.dp,
    thumbColor: Color = Color(0xFF38BDF8),
    trackColor: Color = Color(0x33334155)
) {
    val coroutineScope = rememberCoroutineScope()
    val layoutInfo = lazyListState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo

    if (totalItems <= 1 || visibleItems.isEmpty()) return
    val visibleCount = visibleItems.size
    if (visibleCount >= totalItems) return

    val firstVisible = lazyListState.firstVisibleItemIndex
    val thumbHeightFraction = (visibleCount.toFloat() / totalItems.toFloat()).coerceIn(0.12f, 0.9f)
    val maxScrollOffset = 1f - thumbHeightFraction
    val thumbOffsetFraction = ((firstVisible.toFloat() / (totalItems - visibleCount).coerceAtLeast(1)) * maxScrollOffset)
        .coerceIn(0f, maxScrollOffset)

    val animatedOffset by animateFloatAsState(targetValue = thumbOffsetFraction, label = "thumbOffset")

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(width + 4.dp)
            .padding(vertical = 4.dp)
            .pointerInput(totalItems) {
                detectVerticalDragGestures { change, _ ->
                    val yFraction = (change.position.y / size.height).coerceIn(0f, 1f)
                    val targetItem = (yFraction * totalItems).toInt().coerceIn(0, totalItems - 1)
                    coroutineScope.launch {
                        lazyListState.scrollToItem(targetItem)
                    }
                }
            }
    ) {
        val totalHeight = maxHeight
        val thumbHeight = totalHeight * thumbHeightFraction
        val thumbOffset = totalHeight * animatedOffset

        // Pista de fondo
        Box(
            modifier = Modifier
                .width(width)
                .fillMaxHeight()
                .background(trackColor, RoundedCornerShape(width / 2))
        )

        // Pulgar indicador de posición
        Box(
            modifier = Modifier
                .width(width)
                .height(thumbHeight)
                .offset(y = thumbOffset)
                .background(thumbColor, RoundedCornerShape(width / 2))
        )
    }
}
