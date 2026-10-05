package com.omnicast.sender.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PhotoViewerBottomBar(
    currentIndex: Int,
    totalCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onRotate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xDD0F172A)),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.padding(bottom = 32.dp, start = 8.dp, end = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onPrev,
                enabled = totalCount > 1,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1E293B))
            ) {
                Text("◀", fontSize = 14.sp, color = Color.White)
            }

            IconButton(
                onClick = onRotate,
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFF0284C7), CircleShape)
            ) {
                Text("⟳", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Text(
                text = if (totalCount > 0) "${currentIndex + 1}/$totalCount" else "0/0",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            FilledTonalButton(
                onClick = onNext,
                enabled = totalCount > 1,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1E293B))
            ) {
                Text("▶", fontSize = 14.sp, color = Color.White)
            }
        }
    }
}
