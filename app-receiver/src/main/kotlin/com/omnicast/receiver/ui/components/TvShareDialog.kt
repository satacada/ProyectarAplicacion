package com.omnicast.receiver.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
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

object TvShareHelper {
    fun getShareableUrl(item: TvPlaylistItem): String? {
        val direct = item.url.trim()
        val page = item.pageUrl?.trim()
        return when {
            direct.startsWith("http://") || direct.startsWith("https://") -> direct
            page?.startsWith("http://") == true || page?.startsWith("https://") == true -> page
            else -> null
        }
    }

    fun canShare(item: TvPlaylistItem): Boolean = getShareableUrl(item) != null
}

@Composable
fun TvShareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF0284C7) else Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .width(42.dp)
            .height(78.dp)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color(0xFF38BDF8) else Color(0xFF334155),
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("🔗", fontSize = 16.sp)
        }
    }
}

@Composable
fun TvShareDialog(
    item: TvPlaylistItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val shareUrl = remember(item) { TvShareHelper.getShareableUrl(item) ?: "" }
    var qrBitmap by remember(shareUrl) { mutableStateOf<Bitmap?>(null) }
    var isLoadingQr by remember(shareUrl) { mutableStateOf(true) }

    LaunchedEffect(shareUrl) {
        if (shareUrl.isNotBlank()) {
            isLoadingQr = true
            withContext(Dispatchers.IO) {
                try {
                    val qrPayload = "omnicast://cast?url=${URLEncoder.encode(shareUrl, "UTF-8")}&title=${URLEncoder.encode(item.title.ifBlank { "Video Web" }, "UTF-8")}"
                    val encoded = URLEncoder.encode(qrPayload, "UTF-8")
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
                .width(440.dp)
                .wrapContentHeight()
                .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🔗 Compartir Video",
                    color = Color(0xFF38BDF8),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = item.title.ifBlank { "Video Web" },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingQr) {
                        CircularProgressIndicator(color = Color(0xFF0F172A), strokeWidth = 3.dp)
                    } else if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "Código QR",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "Escanea el enlace con tu celular",
                            color = Color.Black,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Text(
                    text = "Escanea con la cámara del celular para ver o compartir",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = shareUrl,
                        color = Color(0xFF7DD3FC),
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TvActionButton(
                        text = "📋 Copiar",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clip.setPrimaryClip(ClipData.newPlainText("URL Video", shareUrl))
                            Toast.makeText(context, "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        }
                    )
                    TvActionButton(
                        text = "📤 Apps",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            try {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, item.title)
                                    putExtra(Intent.EXTRA_TEXT, "${item.title}\n$shareUrl")
                                }
                                val chooser = Intent.createChooser(sendIntent, "Compartir enlace")
                                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooser)
                            } catch (_: Throwable) {
                                Toast.makeText(context, "No hay apps para compartir", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    TvActionButton(
                        text = "✕ Salir",
                        modifier = Modifier.weight(1f),
                        isPrimary = false,
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
fun TvActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        color = when {
            isFocused -> Color(0xFF38BDF8)
            isPrimary -> Color(0xFF2563EB)
            else -> Color(0xFF334155)
        },
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .height(36.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (isFocused) Color.Black else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
