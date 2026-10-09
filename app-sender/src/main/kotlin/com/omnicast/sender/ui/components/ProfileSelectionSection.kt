package com.omnicast.sender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.core.domain.models.QualityMode
import com.omnicast.core.domain.models.StreamProfile

@Composable
fun ProfileSelectionSection(
    tempProfile: StreamProfile,
    onSelectProfile: (StreamProfile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("⚡", fontSize = 12.sp)
                Text(
                    text = "PERFIL DE TRANSMISIÓN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCBD5E1),
                    letterSpacing = 0.6.sp
                )
            }
            Text("Resolución • FPS", fontSize = 11.sp, color = Color(0xFF64748B))
        }

        PROFILE_OPTIONS.forEach { opt ->
            val isSelected = tempProfile.mode == opt.profile.mode ||
                    (opt.profile.mode == QualityMode.BALANCED_30FPS && tempProfile.mode == QualityMode.BALANCED)
            Surface(
                color = if (isSelected) Color(0xFF13223A) else Color(0xFF0F1420),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.2.dp,
                    if (isSelected) Color(0xFF3B82F6) else Color(0xFF222B3D)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectProfile(opt.profile) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(
                                    color = if (isSelected) Color(0xFF3B82F6) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .border(
                                    1.5.dp,
                                    if (isSelected) Color(0xFF93C5FD) else Color(0xFF475569),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(modifier = Modifier.size(6.dp).background(Color.White, CircleShape))
                            }
                        }
                        Text(
                            text = opt.resLabel,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                        )
                    }

                    Surface(
                        color = when {
                            opt.badgeIsWarning -> Color(0xFF332009)
                            opt.badgeIsPrimary -> Color(0xFF1E2D4E)
                            else -> Color(0xFF1A2232)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when {
                                opt.badgeIsWarning -> Color(0xFFD97706)
                                opt.badgeIsPrimary -> Color(0xFF3B82F6)
                                else -> Color(0xFF334155)
                            }
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = opt.badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                opt.badgeIsWarning -> Color(0xFFFBBF24)
                                opt.badgeIsPrimary -> Color(0xFF60A5FA)
                                else -> Color(0xFF94A3B8)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
