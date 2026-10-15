package com.omnicast.receiver.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omnicast.receiver.directstream.TvPlaylistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder

@Composable
fun TvRenewQrDialog(
    item: TvPlaylistItem,
    onDismiss: () -> Unit
) {
    val renewUrl = remember(item) {
        val page = item.pageUrl?.trim()
        val direct = item.url.trim()
        when {
            page?.startsWith("http://") == true || page?.startsWith("https://") == true -> page
            direct.startsWith("http://") || direct.startsWith("https://") -> direct
            else -> ""
        }
    }

    var qrBitmap by remember(renewUrl) { mutableStateOf<Bitmap?>(null) }
    var isLoadingQr by remember(renewUrl) { mutableStateOf(true) }

    val posLabel = remember(item.lastPositionMs) {
        if (item.lastPositionMs >= 5000L) {
            val totalSec = item.lastPositionMs / 1000
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
        } else null
    }

    LaunchedEffect(renewUrl) {
        if (renewUrl.isNotBlank()) {
            isLoadingQr = true
            withContext(Dispatchers.IO) {
                try {
                    val encoded = URLEncoder.encode(renewUrl, "UTF-8")
                    val qrApiUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=$encoded"
                    val bmp = URL(qrApiUrl).openStream().use { BitmapFactory.decodeStream(it) }
                    qrBitmap = bmp
                } catch (_: Throwable) {
                    qrBitmap = null
                } finally {
                    isLoadingQr = false
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E182A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(460.dp)
                .wrapContentHeight()
                .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🔄 Enlace de Video Caducado",
                    color = Color(0xFFF59E0B),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = item.title.ifBlank { "Película guardada" },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )

                if (posLabel != null) {
                    Surface(
                        color = Color(0xFF064E3B),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "▶ Te quedaste en: $posLabel",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "El enlace temporal caducó en el servidor web.\nEscanea este código con tu celular para abrirlo y transmitirlo de nuevo.\nLa TV guardará el nuevo enlace activo.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoadingQr -> CircularProgressIndicator(color = Color(0xFFF59E0B), strokeWidth = 3.dp)
                        qrBitmap != null -> Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR para renovar video",
                            modifier = Modifier.fillMaxSize()
                        )
                        else -> Text("⚠️ No se pudo generar QR", color = Color.Red, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                var isCloseFocused by remember { mutableStateOf(false) }
                Surface(
                    color = if (isCloseFocused) Color(0xFF38BDF8) else Color(0xFF2D243F),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .border(
                            width = if (isCloseFocused) 2.dp else 1.dp,
                            color = if (isCloseFocused) Color.White else Color(0xFF3E3255),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .onFocusChanged { isCloseFocused = it.isFocused }
                        .focusable()
                        .clickable(onClick = onDismiss)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "✕ Volver a la lista",
                            color = if (isCloseFocused) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
