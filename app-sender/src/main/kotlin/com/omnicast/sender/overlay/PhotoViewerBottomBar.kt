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
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xE61D1730),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFBE185D)),
        modifier = modifier.padding(bottom = 36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFF2B1F47), CircleShape)
                    .border(1.dp, Color(0xFF8B5CF6), CircleShape)
                    .clickable(enabled = totalCount > 0) { onPrev() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "◀",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFA78BFA)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (folderName.isNotBlank()) {
                    Text(
                        text = "📁 $folderName",
                        color = Color(0xFFF43F5E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 170.dp)
                    )
                }
                Text(
                    text = if (totalCount > 0) "${currentIndex + 1} / $totalCount  ↻" else "1 / 1",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFF2B1F47), CircleShape)
                    .border(1.dp, Color(0xFF8B5CF6), CircleShape)
                    .clickable(enabled = totalCount > 0) { onNext() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFA78BFA)
                )
            }
        }
    }
}
