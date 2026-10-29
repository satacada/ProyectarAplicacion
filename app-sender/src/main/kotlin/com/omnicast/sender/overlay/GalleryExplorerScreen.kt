package com.omnicast.sender.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.sender.gallery.*

@Composable
fun GalleryExplorerScreen(
    allItems: List<GalleryPhotoItem>,
    isLoading: Boolean,
    hasPermission: Boolean,
    mimeFilter: String,
    onFilterChange: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSystemPicker: () -> Unit,
    onSelectPhoto: (List<GalleryPhotoItem>, Int, String) -> Unit,
    onSelectVideo: (List<GalleryPhotoItem>, Int, String) -> Unit,
    onStepBack: () -> Unit,
    onExitToMain: () -> Unit
) {
    GallerySessionHolder.ensureRouteInitialized()
    val routeStack = GallerySessionHolder.routeStack
    val currentNode = routeStack.lastOrNull() ?: GalleryNavNode("", GallerySessionHolder.ROOT_NAME)
    val prevNode = if (routeStack.size > 1) routeStack[routeStack.size - 2] else null

    val filteredItems = remember(allItems, mimeFilter) {
        when {
            mimeFilter.startsWith("image/") -> allItems.filter { !it.isVideo }
            mimeFilter.startsWith("video/") -> allItems.filter { it.isVideo }
            else -> allItems
        }
    }

    val viewData = remember(filteredItems, currentNode.path) {
        GalleryMediaHelper.buildFolderViewData(filteredItems, currentNode.path)
    }

    val gridState = key(routeStack.size, currentNode.path) {
        rememberLazyGridState(
            initialFirstVisibleItemIndex = currentNode.firstVisibleIndex,
            initialFirstVisibleItemScrollOffset = currentNode.firstVisibleOffset
        )
    }

    LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
        GallerySessionHolder.updateCurrentScroll(
            gridState.firstVisibleItemIndex,
            gridState.firstVisibleItemScrollOffset
        )
    }

    val subtitle = if (currentNode.path.isEmpty()) {
        "${viewData.allFolders.size} carpetas • ${filteredItems.size} archivos"
    } else {
        "${viewData.directItems.size} en esta carpeta • ${viewData.subfolders.size} subcarpetas"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F17))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        GalleryExplorerHeaderBar(
            routeStack = routeStack,
            currentNode = currentNode,
            prevNode = prevNode,
            subtitle = subtitle,
            mimeFilter = mimeFilter,
            allFolders = viewData.allFolders,
            onStepBack = onStepBack,
            onOpenSystemPicker = onOpenSystemPicker,
            onExitToMain = onExitToMain,
            onFilterChange = onFilterChange,
            onNavigateToFolder = { targetPath, targetName ->
                GallerySessionHolder.navigateToFolder(
                    targetPath = targetPath,
                    targetName = targetName,
                    currentScrollIndex = gridState.firstVisibleItemIndex,
                    currentScrollOffset = gridState.firstVisibleItemScrollOffset,
                    expandAncestorsIfFromRoot = false
                )
            }
        )

        if (!hasPermission) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("📷 Permiso de Galería Requerido", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Concede permiso para explorar tus carpetas de fotos y videos manteniendo toda la ruta de navegación.",
                        color = Color(0xFF94A3B8), fontSize = 13.sp
                    )
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                    ) {
                        Text("Conceder Permiso", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (isLoading && allItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFF43F5E))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (viewData.subfolders.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            text = if (currentNode.path.isEmpty()) "📂 Carpetas Raíz del Dispositivo" else "📂 Subcarpetas en ${currentNode.name}",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    itemsIndexed(viewData.subfolders, key = { _, f -> "sub_${f.path}" }) { _, folder ->
                        GalleryFolderGridCard(
                            folder = folder,
                            onClick = {
                                GallerySessionHolder.navigateToFolder(
                                    targetPath = folder.path,
                                    targetName = folder.name,
                                    currentScrollIndex = gridState.firstVisibleItemIndex,
                                    currentScrollOffset = gridState.firstVisibleItemScrollOffset,
                                    expandAncestorsIfFromRoot = true
                                )
                            }
                        )
                    }
                }

                if (currentNode.path.isEmpty() && viewData.allFolders.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            text = "🖼️ Todas las Carpetas con Fotos / Videos (Ruta Completa)",
                            color = Color(0xFFF43F5E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }
                    itemsIndexed(viewData.allFolders, key = { _, f -> "album_${f.path}" }) { _, folder ->
                        GalleryFolderGridCard(
                            folder = folder,
                            showFullRouteSubtitle = true,
                            onClick = {
                                GallerySessionHolder.navigateToFolder(
                                    targetPath = folder.path,
                                    targetName = folder.name,
                                    currentScrollIndex = gridState.firstVisibleItemIndex,
                                    currentScrollOffset = gridState.firstVisibleItemScrollOffset,
                                    expandAncestorsIfFromRoot = true
                                )
                            }
                        )
                    }
                }

                if (viewData.directItems.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            text = "Archivos en ${currentNode.name} (${viewData.directItems.size})",
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }
                    itemsIndexed(viewData.directItems, key = { _, it -> "media_${it.isVideo}_${it.id}" }) { _, item ->
                        GalleryMediaGridCell(
                            item = item,
                            onClick = {
                                if (item.isVideo) {
                                    val folderVideos = viewData.directItems.filter { it.isVideo }
                                    val vIdx = folderVideos.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                                    onSelectVideo(folderVideos, vIdx, currentNode.name)
                                } else {
                                    val folderPhotos = viewData.directItems.filter { !it.isVideo }
                                    val pIdx = folderPhotos.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
                                    onSelectPhoto(folderPhotos, pIdx, currentNode.name)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
