package com.omnicast.sender.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Cápsula inferior Cyber-Dark para navegación circular de fotos en el contexto de su carpeta: ◀ Carpeta • 1/5 ▶
 */
@Composable
fun PhotoViewerBottomBar(
    currentIndex: Int,
    totalCount: Int,
    folderName: String = "",
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xD91D1730),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.7f)),
        modifier = modifier.padding(bottom = 34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (folderName.isNotBlank()) {
                Text(
                    text = "📁 $folderName",
                    color = Color(0xFFF43F5E),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp)
                )
                Text("•", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
            Text(
                text = if (totalCount > 0) "${currentIndex + 1} / $totalCount" else "1 / 1",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
