package com.omnicast.sender.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
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
 * Barra inferior Cyber-Dark que aparece al detectar streams de video en la web.
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
            color = Color(0xF8111624),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF28354E)),
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF132F26), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "Flujos detectados ($streamCount)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = title.ifBlank { "HLS • m3u8 stream directo" },
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (onSaveToPlaylist != null) {
                            IconButton(
                                onClick = onSaveToPlaylist,
                                modifier = Modifier.size(34.dp).background(Color(0xFF1E283D), CircleShape)
                            ) {
                                Icon(Icons.Rounded.BookmarkAdd, contentDescription = "Guardar", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            }
                        }
                        IconButton(
                            onClick = onClearStreams,
                            modifier = Modifier.size(34.dp).background(Color(0xFF2A1B24), CircleShape)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Limpiar", tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTransmitDirect,
                        modifier = Modifier.weight(1.5f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text("Transmitir a TV", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Surface(
                        color = Color(0xFF162030),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF28354E)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        TextButton(onClick = onOpenQualities) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(16.dp))
                                Text("Calidad", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
