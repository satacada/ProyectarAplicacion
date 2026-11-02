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
 * Diálogo de confirmación D-Pad al detener una grabación PVR en TV Box.
 * Ofrece la decisión de [Conservar Grabación], [Descartar y Borrar] o [Continuar Grabando].
 */
@Composable
fun TvConfirmStopRecordDialog(
    isOpen: Boolean,
    recordedDurationSec: Long,
    onKeep: () -> Unit,
    onDiscard: () -> Unit,
    onContinue: () -> Unit
) {
    if (!isOpen) return

    val keepFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        keepFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onContinue) {
        Card(
            modifier = Modifier.width(440.dp).padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF00F172A)),
            border = BorderStroke(2.dp, Color(0xFFDC2626))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⏹️ Detener Grabación PVR",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = "Tiempo grabado: ${formatDuration(recordedDurationSec * 1000L)}. ¿Deseas conservar el archivo?",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 18.dp)
                )

                TvConfirmActionBtn(
                    text = "💾 Conservar Grabación",
                    bgColor = Color(0xFF0284C7),
                    focusColor = Color(0xFF38BDF8),
                    focusRequester = keepFocusRequester,
                    onClick = onKeep
                )

                Spacer(modifier = Modifier.height(10.dp))

                TvConfirmActionBtn(
                    text = "🗑️ Descartar y Borrar Archivo",
                    bgColor = Color(0xFF991B1B),
                    focusColor = Color(0xFFEF4444),
                    onClick = onDiscard
                )

                Spacer(modifier = Modifier.height(10.dp))

                TvConfirmActionBtn(
                    text = "↩️ Continuar Grabando",
                    bgColor = Color(0x33475569),
                    focusColor = Color.White,
                    onClick = onContinue
                )
            }
        }
    }
}

@Composable
private fun TvConfirmActionBtn(
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) focusColor else bgColor)
            .border(if (isFocused) 2.dp else 1.dp, if (isFocused) Color.White else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isFocused && focusColor == Color(0xFF38BDF8)) Color.Black else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
