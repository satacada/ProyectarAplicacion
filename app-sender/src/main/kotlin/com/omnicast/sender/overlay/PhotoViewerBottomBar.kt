package com.omnicast.sender.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
 * Cápsula inferior flotante para navegación de fotos dentro del contexto de su carpeta: < Carpeta • 1/10 >
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
        color = Color(0xD91E2433),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier.padding(bottom = 36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clickable(enabled = totalCount > 1) { onPrev() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "‹",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (totalCount > 1) Color.White else Color(0xFF64748B)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (folderName.isNotBlank()) {
                    Text(
                        text = "📁 $folderName",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 160.dp)
                    )
                }
                Text(
                    text = if (totalCount > 0) "${currentIndex + 1}/$totalCount" else "1/1",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clickable(enabled = totalCount > 1) { onNext() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "›",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (totalCount > 1) Color.White else Color(0xFF64748B)
                )
            }
        }
    }
}
