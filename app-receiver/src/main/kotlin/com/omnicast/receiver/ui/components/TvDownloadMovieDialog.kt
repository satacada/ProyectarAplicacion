package com.omnicast.receiver.ui.components

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omnicast.receiver.directstream.TvBridgeDownloadProgress
import com.omnicast.receiver.directstream.TvDownloadManager
import com.omnicast.receiver.directstream.TvPlaylistItem
import com.omnicast.receiver.directstream.TvStorageBridgeInfo
import kotlinx.coroutines.launch

@Composable
fun TvDownloadMovieDialog(
    item: TvPlaylistItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var storageInfo by remember { mutableStateOf<TvStorageBridgeInfo?>(null) }
    var progress by remember { mutableStateOf(TvBridgeDownloadProgress()) }

    LaunchedEffect(item.url) {
        storageInfo = TvDownloadManager.inspectStorageAndMovie(
            context = context,
            url = item.url,
            headers = item.headers,
            durationMs = item.durationMs
        )
    }

    Dialog(onDismissRequest = {
        if (!progress.isDownloading) onDismiss()
    }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1426)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .width(500.dp)
                .wrapContentHeight()
                .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "⚠️ Descarga en Modo Puente (Aviso de Disco)",
                    color = Color(0xFFFBBF24),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = item.title.ifBlank { "Película en Ver más tarde" },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Recuadro de advertencia obligatoria de saturación de disco y uso como puente
                Surface(
                    color = Color(0xFF2D161E),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "🚨 IMPORTANTE: Tu TV Box tiene almacenamiento limitado y se llenará su disco si acumulas películas.",
                            color = Color(0xFFFDA4AF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "• Usa esta descarga en la TV únicamente como PUENTE TEMPORAL.\n• Debes pasar la película a otro dispositivo (Memoria USB / Disco Externo / PC) y luego pulsar 'Limpiar Archivo Puente'.",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp
                        )
                    }
                }

                // Tarjeta de cálculo real de espacio disponible vs tamaño de película
                val info = storageInfo
                Surface(
                    color = Color(0xFF131926),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (info == null) {
                        Box(modifier = Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                            Text("Calculando espacio libre en TV y peso de la película...", color = Color(0xFF38BDF8), fontSize = 12.sp)
                        }
                    } else {
                        val sizePrefix = if (info.isExactMovieSize) "" else "~"
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("💾 Espacio libre actual en TV:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(
                                    text = "${TvDownloadManager.formatBytes(info.freeBytes)} libres (de ${TvDownloadManager.formatBytes(info.totalBytes)})",
                                    color = Color(0xFF34D399),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🎬 Espacio que ocupará la película:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(
                                    text = "$sizePrefix${TvDownloadManager.formatBytes(info.movieBytes)}",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("📊 Espacio libre tras descargar:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(
                                    text = "$sizePrefix${TvDownloadManager.formatBytes(info.remainingBytesAfterDownload)}",
                                    color = if (info.willExceedSafeSpace) Color(0xFFEF4444) else Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = if (info.hasUsbDrive) "✅ Unidad externa USB detectada: ${info.usbLabel}"
                                else "📂 Ruta puente en TV: ${info.bridgeDirPath}",
                                color = if (info.hasUsbDrive) Color(0xFF6EE7B7) else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Estado de progreso si está descargando o completado
                if (progress.isDownloading || progress.isCompleted || progress.errorMsg != null) {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            when {
                                progress.errorMsg != null -> Text("❌ ${progress.errorMsg}", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                progress.isCompleted -> Text(
                                    text = "✅ Descarga puente lista (${TvDownloadManager.formatBytes(progress.downloadedBytes)}). Pásala a tu USB/Dispositivo y luego límpiala.",
                                    color = Color(0xFF34D399),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                else -> Text(
                                    text = "⬇️ Descargando en puente: ${progress.percent}% (${TvDownloadManager.formatBytes(progress.downloadedBytes)})",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // Botones de acción
                if (!progress.isDownloading) {
                    val hasUsb = storageInfo?.hasUsbDrive == true
                    TvBridgeActionBtn(
                        text = if (hasUsb) "💾 Descargar directo a Memoria USB Externa" else "⬇️ Entendido: Descargar en TV como Puente",
                        bgBase = Color(0xFFD97706),
                        onClick = {
                            val est = storageInfo?.movieBytes ?: 800_000_000L
                            TvDownloadManager.activeJob = scope.launch {
                                TvDownloadManager.downloadMovieInBridgeMode(
                                    context = context,
                                    title = item.title,
                                    url = item.url,
                                    headers = item.headers,
                                    expectedBytes = est,
                                    useUsb = hasUsb,
                                    onProgress = { progress = it }
                                )
                                storageInfo = TvDownloadManager.inspectStorageAndMovie(context, item.url, item.headers, item.durationMs)
                            }
                        }
                    )

                    if ((storageInfo?.bridgeFolderBytes ?: 0L) > 0L || progress.isCompleted) {
                        TvBridgeActionBtn(
                            text = "🗑️ Limpiar Archivos Puente de la TV (${TvDownloadManager.formatBytes(storageInfo?.bridgeFolderBytes ?: progress.downloadedBytes)})",
                            bgBase = Color(0xFF7F1D1D),
                            onClick = {
                                val freed = TvDownloadManager.clearBridgeDownloads(context)
                                progress = TvBridgeDownloadProgress()
                                Toast.makeText(context, "🧹 Liberados ${TvDownloadManager.formatBytes(freed)} en la TV", Toast.LENGTH_SHORT).show()
                                scope.launch {
                                    storageInfo = TvDownloadManager.inspectStorageAndMovie(context, item.url, item.headers, item.durationMs)
                                }
                            }
                        )
                    }

                    TvBridgeActionBtn(
                        text = "✕ Cerrar",
                        bgBase = Color(0xFF2D243F),
                        onClick = onDismiss
                    )
                } else {
                    TvBridgeActionBtn(
                        text = "⏹ Cancelar descarga en curso",
                        bgBase = Color(0xFFDC2626),
                        onClick = {
                            TvDownloadManager.activeJob?.cancel()
                            TvDownloadManager.clearBridgeDownloads(context)
                            progress = TvBridgeDownloadProgress(errorMsg = "Descarga cancelada y espacio liberado")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvBridgeActionBtn(text: String, bgBase: Color, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        color = if (focused) Color(0xFF38BDF8) else bgBase,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0xFF475569), RoundedCornerShape(10.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = text, color = if (focused) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
