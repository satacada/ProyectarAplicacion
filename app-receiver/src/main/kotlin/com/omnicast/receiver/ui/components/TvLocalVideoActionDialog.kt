package com.omnicast.receiver.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omnicast.receiver.directstream.TvDownloadedVideo

@Composable
fun TvLocalVideoActionDialog(
    video: TvDownloadedVideo?,
    onPlay: (TvDownloadedVideo) -> Unit,
    onDelete: (TvDownloadedVideo) -> Unit,
    onDismiss: () -> Unit
) {
    if (video == null) return

    val playFocusRequester = remember { FocusRequester() }
    LaunchedEffect(video) {
        playFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(460.dp).padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF50F172A)),
            border = BorderStroke(2.dp, Color(0xFF38BDF8))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "💾 Película en Disco Local",
                    color = Color(0xFF38BDF8),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("📁 Ubicación: ${video.filePath}", color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("📦 Tamaño: ${video.formattedSize} • 📅 Fecha: ${video.formattedDate}", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                TvModernDialogBtn(
                    text = "▶️ Reproducir desde Disco",
                    bgColor = Color(0xFF0284C7),
                    focusColor = Color(0xFF38BDF8),
                    focusRequester = playFocusRequester,
                    onClick = { onPlay(video); onDismiss() }
                )

                TvModernDialogBtn(
                    text = "🗑️ Borrar archivo (Liberar espacio)",
                    bgColor = Color(0xFF991B1B),
                    focusColor = Color(0xFFEF4444),
                    onClick = { onDelete(video); onDismiss() }
                )

                TvModernDialogBtn(
                    text = "✕ Cancelar",
                    bgColor = Color(0xFF334155),
                    focusColor = Color(0xFF64748B),
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun TvModernDialogBtn(
    text: String,
    bgColor: Color,
    focusColor: Color,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable(interactionSource = interactionSource)
            .clickable { onClick() }
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) focusColor else bgColor)
            .border(if (isFocused) 2.dp else 1.dp, if (isFocused) Color.White else Color(0x33FFFFFF), RoundedCornerShape(10.dp))
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isFocused && focusColor == Color(0xFF38BDF8)) Color.Black else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
