package com.omnicast.receiver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun TvPlayerSettingsDialog(
    title: String,
    isSaved: Boolean,
    currentQuality: String = "Auto",
    onCycleQuality: () -> String = { "Auto" },
    currentAudioTrack: String = "Original",
    onCycleAudio: () -> String = { "Original" },
    subtitlesEnabled: Boolean = true,
    onToggleSubtitles: () -> Boolean = { true },
    onSaveToWatchLater: () -> Unit,
    onExitPlayback: () -> Unit,
    onDismiss: () -> Unit
) {
    var quality by remember { mutableStateOf(currentQuality) }
    var audioTrack by remember { mutableStateOf(currentAudioTrack) }
    var subsEnabled by remember { mutableStateOf(subtitlesEnabled) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E182A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(420.dp)
                .wrapContentHeight()
                .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "⚙️ Opciones de Reproducción",
                    color = Color(0xFF38BDF8),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = title.ifBlank { "Transmisión en TV" },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                TvSettingsButton(
                    text = "📺 Calidad de Video: $quality",
                    isPrimary = true,
                    onClick = {
                        quality = onCycleQuality()
                    }
                )

                TvSettingsButton(
                    text = "🔊 Pista de Audio: $audioTrack",
                    isPrimary = false,
                    onClick = {
                        audioTrack = onCycleAudio()
                    }
                )

                TvSettingsButton(
                    text = if (subsEnabled) "💬 Subtítulos: Activados" else "💬 Subtítulos: Desactivados",
                    isPrimary = false,
                    onClick = {
                        subsEnabled = onToggleSubtitles()
                    }
                )

                TvSettingsButton(
                    text = if (isSaved) "✅ Película ya guardada en Ver más tarde" else "🔖 Guardar en \"Ver más tarde\"",
                    isPrimary = !isSaved,
                    onClick = {
                        onSaveToWatchLater()
                        onDismiss()
                    }
                )

                TvSettingsButton(
                    text = "🚪 Salir a la pantalla principal",
                    isDanger = true,
                    onClick = {
                        onDismiss()
                        onExitPlayback()
                    }
                )

                TvSettingsButton(
                    text = "▶ Continuar viendo película",
                    isPrimary = false,
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun TvSettingsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isDanger: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }

    val bgColor = when {
        isFocused -> if (isDanger) Color(0xFFEF4444) else Color(0xFF38BDF8)
        isDanger -> Color(0xFF7F1D1D)
        isPrimary -> Color(0xFF0284C7)
        else -> Color(0xFF2D243F)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else Color(0xFF3E3255),
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (isFocused && !isDanger) Color.Black else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
