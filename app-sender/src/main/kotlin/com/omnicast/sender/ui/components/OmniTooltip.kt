package com.omnicast.sender.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Componente unificado de Tooltip para mostrar descripciones reactivas
 * al pasar el cursor (mouse hover) o pulsación prolongada sin saturar la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniTooltip(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!enabled || text.isBlank()) {
        content()
        return
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFF38BDF8))
            ) {
                Text(
                    text = text,
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        },
        state = rememberTooltipState(),
        modifier = modifier
    ) {
        content()
    }
}
