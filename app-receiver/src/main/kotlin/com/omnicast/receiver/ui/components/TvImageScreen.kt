package com.omnicast.receiver.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun TvImageScreen(
    imageUrl: String,
    title: String,
    scale: Float = 1f,
    panX: Float = 0f,
    panY: Float = 0f,
    rotation: Float = 0f,
    onPrevClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
    onRotateClick: () -> Unit = {}
) {
    var imageBitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(imageUrl) { mutableStateOf(true) }

    LaunchedEffect(imageUrl) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val bmp = if (imageUrl.startsWith("data:image")) {
                    val base64Data = imageUrl.substringAfter("base64,")
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } else {
                    URL(imageUrl).openStream().use { BitmapFactory.decodeStream(it) }
                }
                imageBitmap = bmp?.asImageBitmap()
            } catch (e: Throwable) {
                com.omnicast.receiver.log.ReceiverLogger.log("TvImageScreen", "Error cargando: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val bmp = imageBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = title,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = panX; translationY = panY; rotationZ = rotation
                },
                contentScale = ContentScale.Fit
            )

            // Botón en pantalla pequeño y no invasivo para regresar a la principal de OmniCast
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 24.dp)
                    .size(38.dp)
                    .background(Color(0xBB0F172A), RoundedCornerShape(10.dp))
            ) {
                Text("🚪", fontSize = 18.sp)
            }

            // Botón en pantalla pequeño y no invasivo para rotar la foto
            IconButton(
                onClick = onRotateClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 24.dp, top = 24.dp)
                    .size(38.dp)
                    .background(Color(0xBB0284C7), RoundedCornerShape(10.dp))
            ) {
                Text("⟳", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            // Flecha Izquierda
            IconButton(
                onClick = onPrevClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 28.dp)
                    .size(58.dp)
                    .background(Color(0x66000000), shape = CircleShape)
            ) {
                Text("◀", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            // Flecha Derecha
            IconButton(
                onClick = onNextClick,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 28.dp)
                    .size(58.dp)
                    .background(Color(0x66000000), shape = CircleShape)
            ) {
                Text("▶", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            // Banner inferior con título, contador y ayuda de control remoto
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xBB0F172A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🖼️", fontSize = 14.sp)
                    Text(title.ifBlank { "Foto de Galería" }, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("• ◀ / ▶ navegar • OK / ⟳ Rotar 90°", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
        } else if (isLoading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
                Text("Cargando imagen en alta resolución...", color = Color.LightGray, fontSize = 16.sp)
            }
        } else {
            Text("No se pudo cargar la imagen", color = Color(0xFFEF4444), fontSize = 18.sp)
        }
    }
}
