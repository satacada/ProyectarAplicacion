package com.omnicast.receiver.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Modal D-Pad nativo de configuración para reproductores web (HTML5 y YouTube) en TV Box.
 * Permite seleccionar Calidad, Subtítulos y Modo de Aspecto con navegación 100% de mando remoto.
 */
@Composable
fun TvWebSettingsModal(
    state: TvWebPlayerState,
    onSelectQuality: (String) -> Unit,
    onSelectSubtitle: (String) -> Unit,
    onToggleAspect: () -> Unit,
    onClose: () -> Unit
) {
    if (!state.isSettingsOpen) return

    val firstItemFocus = remember { FocusRequester() }
    LaunchedEffect(state.isSettingsOpen) {
        firstItemFocus.requestFocus()
    }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .width(420.dp)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF00B132B)),
            border = BorderStroke(2.dp, Color(0xFF38BDF8))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚙️ Ajustes de Reproducción",
                    color = Color(0xFF38BDF8),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Modo de Escala / Aspecto
                TvSettingsOptionRow(
                    label = "📺 Aspecto / Escala",
                    value = state.aspectRatio,
                    focusRequester = firstItemFocus,
                    onClick = onToggleAspect
                )

                // Selector de Calidad (solo si el flujo reporta más de 1 nivel real)
                if (state.availableQualities.size > 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TvSettingsMultiOption(
                        title = "🎥 Calidad de Video",
                        current = state.currentQuality,
                        options = state.availableQualities,
                        onSelect = onSelectQuality
                    )
                }

                // Selector de Subtítulos (solo si el flujo reporta subtítulos disponibles)
                if (state.availableSubtitles.size > 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TvSettingsMultiOption(
                        title = "💬 Subtítulos",
                        current = state.currentSubtitle,
                        options = state.availableSubtitles,
                        onSelect = onSelectSubtitle
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TvFocusableSettingsButton(
                    text = "Cerrar [ATRÁS]",
                    onClick = onClose
                )
            }
        }
    }
}

@Composable
private fun TvSettingsOptionRow(
    label: String,
    value: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable(interactionSource = interactionSource)
            .clickable { onClick() }
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color(0xFF0284C7) else Color(0x331E293B))
            .border(if (isFocused) 2.dp else 1.dp, if (isFocused) Color(0xFF38BDF8) else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = if (isFocused) Color.White else Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TvSettingsMultiOption(
    title: String,
    current: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.take(4).forEach { opt ->
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()
                val isSelected = opt.equals(current, ignoreCase = true)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .focusable(interactionSource = interactionSource)
                        .clickable { onSelect(opt) }
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isFocused -> Color(0xFF0284C7)
                                isSelected -> Color(0x660284C7)
                                else -> Color(0x221E293B)
                            }
                        )
                        .border(
                            width = if (isFocused) 2.dp else if (isSelected) 1.5.dp else 1.dp,
                            color = if (isFocused) Color(0xFF38BDF8) else if (isSelected) Color(0xFF0EA5E9) else Color(0x33FFFFFF),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        color = if (isFocused || isSelected) Color.White else Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TvFocusableSettingsButton(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusable(interactionSource = interactionSource)
            .clickable { onClick() }
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color(0xFFDC2626) else Color(0x44DC2626))
            .border(if (isFocused) 2.dp else 1.dp, if (isFocused) Color.White else Color(0x66DC2626), RoundedCornerShape(8.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
