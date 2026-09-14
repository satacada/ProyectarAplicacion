package com.omnicast.receiver.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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
    panY: Float = 0f
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
                com.omnicast.receiver.log.ReceiverLogger.log("TvImageScreen", "Error cargando imagen: ${e.message}", e)
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
                    scaleX = scale; scaleY = scale; translationX = panX; translationY = panY
                },
                contentScale = ContentScale.Fit
            )
            var showBanner by remember(imageUrl) { mutableStateOf(true) }
            LaunchedEffect(imageUrl) { kotlinx.coroutines.delay(3000); showBanner = false }
            androidx.compose.animation.AnimatedVisibility(
                visible = showBanner,
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 24.dp)
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🖼️", fontSize = 14.sp)
                        Text(title.ifBlank { "Foto de Galería" }, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
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
