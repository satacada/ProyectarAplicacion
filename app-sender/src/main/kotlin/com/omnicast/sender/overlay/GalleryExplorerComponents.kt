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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.sender.gallery.*

@Composable
fun GalleryExplorerHeaderBar(
    routeStack: List<GalleryNavNode>,
    currentNode: GalleryNavNode,
    prevNode: GalleryNavNode?,
    subtitle: String,
    mimeFilter: String,
    allFolders: List<GalleryFolderInfo>,
    onStepBack: () -> Unit,
    onOpenSystemPicker: () -> Unit,
    onExitToMain: () -> Unit,
    onFilterChange: (String) -> Unit,
    onNavigateToFolder: (String, String) -> Unit
) {
    Surface(color = Color(0xFF131926), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.clickable { onStepBack() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("←", color = Color(0xFF38BDF8), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = prevNode?.name ?: "Salir",
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 95.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentNode.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                        modifier = Modifier.clickable { onOpenSystemPicker() }
                    ) {
                        Text("📁 Sistema", color = Color(0xFFBAE6FD), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                    }
                    if (routeStack.size > 1) {
                        Surface(
                            color = Color(0xFF311022),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBE185D)),
                            modifier = Modifier.clickable { onExitToMain() }
                        ) {
                            Text("✕", color = Color(0xFFF43F5E), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                        }
                    }
                }
            }

            val breadcrumbScroll = rememberScrollState()
            LaunchedEffect(routeStack.size) { breadcrumbScroll.animateScrollTo(breadcrumbScroll.maxValue) }
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF0F1522), RoundedCornerShape(8.dp)).horizontalScroll(breadcrumbScroll).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Ruta:", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                routeStack.forEachIndexed { idx, node ->
                    val isLast = idx == routeStack.lastIndex
                    Text(
                        text = node.name,
                        color = if (isLast) Color(0xFFF43F5E) else Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = if (isLast) FontWeight.ExtraBold else FontWeight.Medium,
                        modifier = Modifier.clickable { if (!isLast) onNavigateToFolder(node.path, node.name) }
                    )
                    if (!isLast) Text("▸", color = Color(0xFF475569), fontSize = 11.sp)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChipButton("📷 Fotos", mimeFilter.startsWith("image/")) { onFilterChange("image/*") }
                FilterChipButton("🎬 Videos", mimeFilter.startsWith("video/")) { onFilterChange("video/*") }
                FilterChipButton("Todo", mimeFilter == "*/*") { onFilterChange("*/*") }
            }

            if (currentNode.path.isNotEmpty() && allFolders.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ir a:", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    allFolders.forEach { folder ->
                        val isCurrent = folder.path.equals(currentNode.path, ignoreCase = true)
                        Surface(
                            color = if (isCurrent) Color(0xFF831843) else Color(0xFF1A2234),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurrent) Color(0xFFF43F5E) else Color(0xFF2E3A52)),
                            modifier = Modifier.clickable { if (!isCurrent) onNavigateToFolder(folder.path, folder.name) }
                        ) {
                            Text("📁 ${folder.name} (${folder.totalCount})", color = if (isCurrent) Color.White else Color(0xFFCBD5E1), fontSize = 11.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) Color(0xFFE11D48) else Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Color(0xFFF43F5E) else Color(0xFF334155)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun GalleryFolderGridCard(folder: GalleryFolderInfo, showFullRouteSubtitle: Boolean = false, onClick: () -> Unit) {
    val context = LocalContext.current
    var coverBitmap by remember(folder.path, folder.coverItem?.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(folder.coverItem?.id, folder.coverItem?.uri) {
        folder.coverItem?.let { coverBitmap = GalleryMediaHelper.getThumbnail(context, it, 220) }
    }

    Box(
        modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF161F30)).border(1.dp, Color(0xFF2E3A52), RoundedCornerShape(12.dp)).clickable { onClick() }
    ) {
        coverBitmap?.let { bmp ->
            Image(bitmap = bmp.asImageBitmap(), contentDescription = folder.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.55f)
        }
        Box(modifier = Modifier.fillMaxSize().background(Color(0x660B0F17)).padding(8.dp)) {
            Surface(color = Color(0xCC1E293B), shape = RoundedCornerShape(6.dp), modifier = Modifier.align(Alignment.TopStart)) {
                Text("📁 ${folder.totalCount}", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
                Text(folder.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = if (showFullRouteSubtitle && folder.path.contains('/')) folder.path else buildList {
                    if (folder.photoCount > 0) add("${folder.photoCount} fotos")
                    if (folder.videoCount > 0) add("${folder.videoCount} videos")
                }.joinToString(" • ")
                Text(sub, color = if (showFullRouteSubtitle) Color(0xFFCBD5E1) else Color(0xFF94A3B8), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1A2234)).border(if (isLastActive) 2.dp else 1.dp, if (isLastActive) Color(0xFFF43F5E) else Color(0xFF28354E), RoundedCornerShape(10.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val bmp = thumb
        if (bmp != null) {
            Image(bitmap = bmp.asImageBitmap(), contentDescription = item.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(if (item.isVideo) "🎬" else "🖼️", fontSize = 24.sp)
        }
        if (item.isVideo) {
            Box(modifier = Modifier.size(32.dp).background(Color(0xAA000000), CircleShape).border(1.dp, Color(0xFF38BDF8), CircleShape), contentAlignment = Alignment.Center) {
                Text("▶", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (item.durationMs > 0L) {
                val sec = (item.durationMs / 1000L).coerceAtLeast(0L)
                Surface(color = Color(0xCC0B0F17), shape = RoundedCornerShape(6.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)) {
                    Text("%02d:%02d".format(sec / 60L, sec % 60L), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                }
            }
        }
    }
}
