package com.omnicast.sender.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.sender.gallery.GalleryMediaHelper
import com.omnicast.sender.gallery.GalleryPhotoItem
import com.omnicast.sender.gallery.GallerySessionHolder
import com.omnicast.sender.overlay.PhotoCastViewerActivity
import kotlinx.coroutines.launch

@Composable
fun GallerySection(onPickExternalFile: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(GalleryMediaHelper.hasGalleryPermission(context)) }
    var photos by remember { mutableStateOf<List<GalleryPhotoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            isLoading = true
            scope.launch {
                photos = GalleryMediaHelper.loadGalleryPhotos(context)
                isLoading = false
            }
        } else {
            Toast.makeText(context, "Se necesita permiso para ver fotos de la galería", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && photos.isEmpty()) {
            isLoading = true
            photos = GalleryMediaHelper.loadGalleryPhotos(context)
            isLoading = false
        }
    }

    fun openPhotoAt(index: Int) {
        if (photos.isEmpty()) return
        GallerySessionHolder.setPhotos(photos, index)
        val intent = Intent(context, PhotoCastViewerActivity::class.java).apply {
            putExtra("EXTRA_START_INDEX", index)
            putExtra("IMAGE_URI", photos[index].uri.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Toast.makeText(context, "🖼️ Proyectando foto a la TV Box", Toast.LENGTH_SHORT).show()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "🖼️ Galería Local",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        if (photos.isNotEmpty()) "${photos.size} fotos • Toca para proyectar" else "Toca una foto para verla en la TV",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hasPermission) {
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    photos = GalleryMediaHelper.loadGalleryPhotos(context)
                                    isLoading = false
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("🔄", fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = onPickExternalFile,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) {
                        Text("📁 Otros", fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            if (!hasPermission) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("📷 Acceso a Fotos Requerido", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Concede acceso a tus fotos para explorarlas y transmitirlas con un toque a la TV Box.",
                            color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { permissionLauncher.launch(GalleryMediaHelper.getRequiredPermission()) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Text("Conceder Permiso de Galería", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Cargando fotos de tu dispositivo...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            } else if (photos.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("No se encontraron fotos en la galería", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            } else {
                val chunkedPhotos = remember(photos) { photos.chunked(3) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunkedPhotos.take(40).forEachIndexed { rowIndex, rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (colIndex in 0 until 3) {
                                val item = rowItems.getOrNull(colIndex)
                                if (item != null) {
                                    val globalIndex = rowIndex * 3 + colIndex
                                    GalleryGridThumbnail(
                                        item = item,
                                        modifier = Modifier.weight(1f).aspectRatio(1f),
                                        onClick = { openPhotoAt(globalIndex) }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    if (photos.size > 120) {
                        Text(
                            "Mostrando las 120 fotos más recientes (${photos.size} en total)",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryGridThumbnail(
    item: GalleryPhotoItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.id) {
        bitmap = GalleryMediaHelper.getThumbnail(context, item, 240)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF334155))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text("🖼️", fontSize = 20.sp)
        }
    }
}
