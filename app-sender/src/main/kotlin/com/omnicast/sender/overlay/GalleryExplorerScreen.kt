package com.omnicast.sender.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
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

    val viewData = remember(filteredItems, currentNode.path, currentNode.customItems) {
        val custom = currentNode.customItems
        if (custom != null) {
            val allView = GalleryMediaHelper.buildFolderViewData(filteredItems, "")
            FolderViewData(
                subfolders = emptyList(),
                directItems = custom,
                allFolders = allView.allFolders
            )
        } else {
            GalleryMediaHelper.buildFolderViewData(filteredItems, currentNode.path)
        }
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

    val isVideoTab = mimeFilter.startsWith("video/")
    val subtitle = when {
        currentNode.customItems != null -> "${currentNode.customItems.size} archivos en esta selección • Solo bucle local"
        currentNode.path.isEmpty() -> "${viewData.allFolders.size} agrupaciones • ${filteredItems.size} ${if (isVideoTab) "videos" else "archivos"}"
        else -> "${viewData.directItems.size} en este nivel • ${viewData.subfolders.size} subcarpetas"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
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
            onJumpToBreadcrumbIndex = { targetIdx ->
                GallerySessionHolder.jumpToRouteIndex(targetIdx)
            },
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
                Surface(
                    color = Color(0xFF24121F),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFBE185D))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("📷 Permiso de Galería y Videos", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Concede acceso a Fotos y Videos para mostrar tus carpetas y agrupaciones manteniendo la ruta exacta al retroceder.",
                            color = Color(0xFFCBD5E1), fontSize = 13.sp
                        )
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Conceder Permiso", color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        } else if (isLoading && allItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFF43F5E))
            }
        } else if (filteredItems.isEmpty() && currentNode.customItems == null) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Surface(
                    color = Color(0xFF1D1730),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6D28D9))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isVideoTab) "🎬 No se detectaron videos aún" else "🖼️ No se encontraron archivos en esta vista",
                            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Si tienes videos o colecciones en tu teléfono, pulsa 'Autorizar Videos' o abre 'Colecciones / Sistema'.",
                            color = Color(0xFF94A3B8), fontSize = 12.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onRequestPermission,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Autorizar Videos/Fotos", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onOpenSystemPicker,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Abrir Sistema", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (currentNode.path.isEmpty() && currentNode.customItems == null && viewData.allFolders.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            text = if (isVideoTab) "🎬 Agrupaciones de Videos por Carpeta (${viewData.allFolders.size})"
                            else if (mimeFilter.startsWith("image/")) "📷 Agrupaciones de Fotos por Carpeta (${viewData.allFolders.size})"
                            else "📂 Todas las Carpetas Multimedia (${viewData.allFolders.size})",
                            color = Color(0xFFF43F5E),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(vertical = 2.dp)
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
                                    expandAncestorsIfFromRoot = false
                                )
                            }
                        )
                    }
                }

                if (viewData.subfolders.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            text = if (currentNode.path.isEmpty()) "📁 Directorios Raíz del Dispositivo" else "📁 Subcarpetas en ${currentNode.name}",
                            color = Color(0xFFA78BFA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
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
                                    expandAncestorsIfFromRoot = false
                                )
                            }
                        )
                    }
                }

                if (viewData.directItems.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            text = "Archivos en ${currentNode.name} (${viewData.directItems.size} en bucle circular)",
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }
                    itemsIndexed(viewData.directItems, key = { idx, it -> "media_${it.isVideo}_${it.id}_$idx" }) { _, item ->
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
