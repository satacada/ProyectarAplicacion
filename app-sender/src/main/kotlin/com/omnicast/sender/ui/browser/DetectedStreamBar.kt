package com.omnicast.sender.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
 * Barra inferior prominente que aparece inmediatamente al detectar videos en la web.
 * Permite transmitir a la TV con 1 solo toque o desplegar opciones de calidad.
 */
@Composable
fun DetectedStreamBar(
    streamCount: Int,
    title: String,
    onTransmitDirect: () -> Unit,
    onOpenQualities: () -> Unit,
    onClearStreams: () -> Unit = {},
    onSaveToPlaylist: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = streamCount > 0,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🎬", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "¡Video Detectado! ($streamCount)",
                                color = Color(0xFF38BDF8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = title.ifBlank { "Transmisión Web" },
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (onSaveToPlaylist != null) {
                            IconButton(
                                onClick = onSaveToPlaylist,
                                modifier = Modifier.size(36.dp).background(Color(0xFF38BDF8).copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Text("💾", fontSize = 16.sp)
                            }
                        }
                        IconButton(
                            onClick = onClearStreams,
                            modifier = Modifier.size(36.dp).background(Color(0xFFEF4444).copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.CircleShape)
                        ) {
                            Text("🗑️", fontSize = 16.sp)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTransmitDirect,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("▶ Transmitir a TV", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onOpenQualities,
                        modifier = Modifier.height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Text("Calidades", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onClearStreams,
                        modifier = Modifier.height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("🗑️ Borrar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
