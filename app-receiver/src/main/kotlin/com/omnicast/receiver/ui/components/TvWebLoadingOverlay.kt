package com.omnicast.receiver.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Superposición moderna de carga y aviso de fallo para TvWebStreamScreen.
 * Principio "Poner y Sacar" (AGENTS.md):
 * Oculta el WebView de Chromium y su icono nativo de Play hasta que el video empiece a reproducirse.
 * Si ocurre un error fatal o timeout, despliega un aviso amigable y claro antes de retornar a Standby.
 */
@Composable
fun TvWebLoadingOverlay(
    title: String,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .fillMaxWidth(0.75f)
        ) {
            if (errorMessage == null) {
                // Estado de Carga
                CircularProgressIndicator(
                    color = Color(0xFF6366F1),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = title.ifBlank { "Cargando contenido..." },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .background(Color(0x226366F1), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🌐 Conectando con el servidor multimedia...",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Estado de Error / Fallo
                Text(
                    text = "⚠️",
                    fontSize = 44.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Servidor no disponible",
                    color = Color(0xFFF87171),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "El servidor de este video no responde o el enlace está inactivo.\nPor favor, selecciona otro servidor o capítulo en tu celular.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .background(Color(0x33EF4444), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Volviendo a la pantalla principal...",
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
