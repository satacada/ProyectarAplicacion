package com.omnicast.sender.overlay

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.sender.gallery.*
import com.omnicast.sender.ui.components.VectorArrowRightIcon
import com.omnicast.sender.ui.components.VectorImageIcon
import com.omnicast.sender.ui.components.VectorTvIcon

@Composable
fun GalleryExplorerHeaderBar(
    routeStack: List<GalleryNavNode>,
    currentNode: GalleryNavNode,
    prevNode: GalleryNavNode?,
    subtitle: String,
    mimeFilter: String,
    onStepBack: () -> Unit,
    onOpenSystemPicker: () -> Unit,
    onExitToMain: () -> Unit,
    onJumpToBreadcrumbIndex: (Int) -> Unit = {}
) {
    val isVideoMode = mimeFilter.startsWith("video/")
    val rootTitle = if (isVideoMode) "Videos" else "Fotos"
    val displayTitle = if (currentNode.path.isEmpty() && currentNode.customItems == null) rootTitle else currentNode.name

    Surface(
        color = Color(0xFF1A1121),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9D174D).copy(alpha = 0.65f)),
        shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Surface(
                        color = Color(0xFF2B1F47),
                        shape = RoundedCornerShape(11.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF8B5CF6)),
                        modifier = Modifier.clickable { onStepBack() }
                    ) {
                        val backLabel = when {
                            prevNode == null -> "Inicio"
                            prevNode.path.isEmpty() -> rootTitle
                            else -> prevNode.name
                        }
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("◀", color = Color(0xFFA78BFA), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            Text(backLabel, color = Color(0xFFF1F5F9), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 92.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.background(Color(0xFF38142C), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFBE185D), RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        VectorImageIcon(modifier = Modifier.size(13.dp), color = Color(0xFFF43F5E))
                        VectorArrowRightIcon(modifier = Modifier.size(11.dp), color = Color(0xFFF43F5E))
                        VectorTvIcon(modifier = Modifier.size(13.dp), color = Color(0xFFF43F5E))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(displayTitle, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(subtitle, color = Color(0xFFFBCFE8), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF7C3AED),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA78BFA)),
                        modifier = Modifier.clickable { onOpenSystemPicker() }
                    ) {
                        Text("📁 Colecciones", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp))
                    }
                    if (routeStack.size > 1) {
                        Surface(
                            color = Color(0xFFDC2626),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.clickable { onExitToMain() }
                        ) {
                            Text("✕", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp))
                        }
                    }
                }
            }

            if (routeStack.size > 1) {
                val breadcrumbScroll = rememberScrollState()
                LaunchedEffect(routeStack.size) { breadcrumbScroll.animateScrollTo(breadcrumbScroll.maxValue) }
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF120C18), RoundedCornerShape(10.dp)).border(1.dp, Color(0xFF3B1D38), RoundedCornerShape(10.dp)).horizontalScroll(breadcrumbScroll).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("Ruta:", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    routeStack.forEachIndexed { idx, node ->
                        val isLast = idx == routeStack.lastIndex
                        val nodeLabel = if (node.path.isEmpty() && node.customItems == null) rootTitle else node.name
                        Surface(
                            color = if (isLast) Color(0xFFE11D48) else Color(0xFF281834),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { if (!isLast) onJumpToBreadcrumbIndex(idx) }
                        ) {
                            Text(nodeLabel, color = if (isLast) Color.White else Color(0xFFDDD6FE), fontSize = 10.sp, fontWeight = if (isLast) FontWeight.ExtraBold else FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                        }
                        if (!isLast) Text("▸", color = Color(0xFFF43F5E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryFolderGridCard(folder: GalleryFolderInfo, showFullRouteSubtitle: Boolean = false, onClick: () -> Unit) {
    val context = LocalContext.current
    var coverBitmap by remember(folder.path, folder.coverItem?.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(folder.coverItem?.id, folder.coverItem?.uri) {
        folder.coverItem?.let { coverBitmap = GalleryMediaHelper.getThumbnail(context, it, 220) }
    }

    val isVideoFolder = folder.videoCount > 0 && folder.photoCount == 0
    val borderColor = if (isVideoFolder) Color(0xFF8B5CF6) else Color(0xFFBE185D)

    Box(
        modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF211328)).border(1.2.dp, borderColor, RoundedCornerShape(14.dp)).clickable { onClick() }
    ) {
        coverBitmap?.let { bmp ->
            Image(bitmap = bmp.asImageBitmap(), contentDescription = folder.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.72f)
        }
        Box(
            modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x330B0F17), Color(0xE6140B1C)))).padding(8.dp)
        ) {
            Surface(
                color = if (isVideoFolder) Color(0xDD4C1D95) else Color(0xDD881337),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isVideoFolder) Color(0xFFA78BFA) else Color(0xFFF43F5E)),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(if (isVideoFolder) "🎬 ${folder.videoCount}" else "📁 ${folder.totalCount}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
                Text(folder.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = buildList {
                    if (folder.photoCount > 0) add("${folder.photoCount} foto${if (folder.photoCount != 1) "s" else ""}")
                    if (folder.videoCount > 0) add("${folder.videoCount} video${if (folder.videoCount != 1) "s" else ""}")
                }.joinToString(" • ").ifBlank { if (showFullRouteSubtitle) folder.path else "Carpeta" }
                Text(sub, color = Color(0xFFFBCFE8), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun GalleryMediaGridCell(item: GalleryPhotoItem, onClick: () -> Unit) {
    val context = LocalContext.current
    var thumb by remember(item.id, item.uri) { mutableStateOf<Bitmap?>(null) }
    val isLastActive = GallerySessionHolder.getActiveUri() == item.uri ||
        GallerySessionHolder.activeVideos.getOrNull(GallerySessionHolder.activeVideoIndex)?.uri == item.uri

    LaunchedEffect(item.id, item.uri) { thumb = GalleryMediaHelper.getThumbnail(context, item, 240) }

    Box(
        modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1D1528)).border(if (isLastActive) 2.2.dp else 1.dp, if (isLastActive) Color(0xFFF43F5E) else Color(0xFF4C1D95), RoundedCornerShape(12.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val bmp = thumb
        if (bmp != null) {
            Image(bitmap = bmp.asImageBitmap(), contentDescription = item.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(if (item.isVideo) "🎬" else "🖼️", fontSize = 24.sp)
        }
        if (item.isVideo) {
            Box(modifier = Modifier.size(34.dp).background(Color(0xCC2B1F47), CircleShape).border(1.2.dp, Color(0xFFA78BFA), CircleShape), contentAlignment = Alignment.Center) {
                Text("▶", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (item.durationMs > 0L) {
                val sec = (item.durationMs / 1000L).coerceAtLeast(0L)
                Surface(color = Color(0xDD1A1121), shape = RoundedCornerShape(6.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)) {
                    Text("%02d:%02d".format(sec / 60L, sec % 60L), color = Color(0xFFFBCFE8), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                }
            }
        }
    }
}
