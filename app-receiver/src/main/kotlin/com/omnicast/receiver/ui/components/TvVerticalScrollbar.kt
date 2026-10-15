package com.omnicast.receiver.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Scrollbar vertical para pantallas Android TV.
 * Refleja reactivamente el avance del foco D-Pad en LazyColumn.
 */
@Composable
fun TvVerticalScrollbar(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    width: Dp = 6.dp,
    thumbColor: Color = Color(0xFF38BDF8),
    trackColor: Color = Color(0x332D223F)
) {
    val layoutInfo = lazyListState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo

    if (totalItems <= 1 || visibleItems.isEmpty()) return
    val visibleCount = visibleItems.size
    if (visibleCount >= totalItems) return

    val firstVisible = lazyListState.firstVisibleItemIndex
    val thumbHeightFraction = (visibleCount.toFloat() / totalItems.toFloat()).coerceIn(0.12f, 0.85f)
    val maxScrollOffset = 1f - thumbHeightFraction
    val thumbOffsetFraction = ((firstVisible.toFloat() / (totalItems - visibleCount).coerceAtLeast(1)) * maxScrollOffset)
        .coerceIn(0f, maxScrollOffset)

    val animatedOffset by animateFloatAsState(targetValue = thumbOffsetFraction, label = "tvThumbOffset")

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(width + 4.dp)
            .padding(vertical = 6.dp)
    ) {
        val totalHeight = maxHeight
        val thumbHeight = totalHeight * thumbHeightFraction
        val thumbOffset = totalHeight * animatedOffset

        // Pista de desplazamiento
        Box(
            modifier = Modifier
                .width(width)
                .fillMaxHeight()
                .background(trackColor, RoundedCornerShape(width / 2))
        )

        // Indicador de posición
        Box(
            modifier = Modifier
                .width(width)
                .height(thumbHeight)
                .offset(y = thumbOffset)
                .background(thumbColor, RoundedCornerShape(width / 2))
        )
    }
}
