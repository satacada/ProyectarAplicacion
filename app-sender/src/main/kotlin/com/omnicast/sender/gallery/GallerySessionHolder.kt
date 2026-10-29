package com.omnicast.sender.gallery

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf

/**
 * Almacén en memoria para la sesión activa de navegación de galería (Fotos y Videos).
 * Mantiene el contexto exacto de cada carpeta y la pila de rutas LIFO (ej: 1 -> 3 -> 2 -> 4 -> 5)
 * para que al retroceder con el botón Atrás se recree toda la secuencia inversa (5 -> 4 -> 2 -> 3 -> 1)
 * sin saltar ningún paso ni perder la posición de scroll.
 */
object GallerySessionHolder {
    const val ROOT_NAME = "Carpetas"

    var activePhotos: List<GalleryPhotoItem> = emptyList()
    var activeIndex: Int = 0
    var hasExplicitContext: Boolean = false
    var activeFolderName: String = ""

    var activeVideos: List<GalleryPhotoItem> = emptyList()
    var activeVideoIndex: Int = 0
    var activeVideoFolderName: String = ""

    var cachedAllItems: List<GalleryPhotoItem> = emptyList()
    val routeStack = mutableStateListOf<GalleryNavNode>()

    fun getActiveUri(): Uri? = activePhotos.getOrNull(activeIndex)?.uri

    fun setPhotos(
        photos: List<GalleryPhotoItem>,
        startIndex: Int = 0,
        folderName: String = "",
        explicitContext: Boolean = false
    ) {
        activePhotos = photos
        activeIndex = startIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0))
        hasExplicitContext = explicitContext
        activeFolderName = folderName.ifBlank {
            photos.firstOrNull()?.albumName ?: ""
        }
    }

    fun setVideos(
        videos: List<GalleryPhotoItem>,
        startIndex: Int = 0,
        folderName: String = ""
    ) {
        activeVideos = videos
        activeVideoIndex = startIndex.coerceIn(0, (videos.size - 1).coerceAtLeast(0))
        activeVideoFolderName = folderName.ifBlank {
            videos.firstOrNull()?.albumName ?: ""
        }
    }

    fun ensureRouteInitialized() {
        if (routeStack.isEmpty()) {
            routeStack.add(GalleryNavNode(path = "", name = ROOT_NAME))
        }
    }

    fun currentRouteNode(): GalleryNavNode {
        ensureRouteInitialized()
        return routeStack.last()
    }

    fun updateCurrentScroll(firstVisibleIndex: Int, firstVisibleOffset: Int) {
        if (routeStack.isEmpty()) return
        val lastIdx = routeStack.lastIndex
        val current = routeStack[lastIdx]
        if (current.firstVisibleIndex != firstVisibleIndex || current.firstVisibleOffset != firstVisibleOffset) {
            routeStack[lastIdx] = current.copy(
                firstVisibleIndex = firstVisibleIndex,
                firstVisibleOffset = firstVisibleOffset
            )
        }
    }

    /**
     * Registra el avance hacia una carpeta.
     * - Si el usuario navega carpeta por carpeta (ej: 1 -> 3 -> 2 -> 4 -> 5), apila cada paso en orden exacto.
     * - Si desde la raíz salta directamente a una ruta anidada (ej: "1/3/2/4/5"), expande y apila todos los
     *   niveles intermedios para que el botón Atrás recorra 5 -> 4 -> 2 -> 3 -> 1 -> Raíz sin saltar ninguno.
     */
    fun navigateToFolder(
        targetPath: String,
        targetName: String,
        currentScrollIndex: Int = 0,
        currentScrollOffset: Int = 0,
        expandAncestorsIfFromRoot: Boolean = true
    ) {
        ensureRouteInitialized()
        updateCurrentScroll(currentScrollIndex, currentScrollOffset)
        val cleanTarget = targetPath.trim('/')
        val currentTop = routeStack.last()
        if (currentTop.path.equals(cleanTarget, ignoreCase = true)) return

        if (expandAncestorsIfFromRoot && currentTop.path.isEmpty() && cleanTarget.contains('/')) {
            val segments = cleanTarget.split('/').filter { it.isNotBlank() }
            var acc = ""
            for (seg in segments) {
                acc = if (acc.isEmpty()) seg else "$acc/$seg"
                if (!routeStack.last().path.equals(acc, ignoreCase = true)) {
                    routeStack.add(GalleryNavNode(path = acc, name = seg))
                }
            }
            return
        }

        if (cleanTarget.startsWith("${currentTop.path}/", ignoreCase = true) && currentTop.path.isNotEmpty()) {
            val remainder = cleanTarget.substring(currentTop.path.length + 1)
            val segments = remainder.split('/').filter { it.isNotBlank() }
            var acc = currentTop.path
            for (seg in segments) {
                acc = "$acc/$seg"
                if (!routeStack.last().path.equals(acc, ignoreCase = true)) {
                    routeStack.add(GalleryNavNode(path = acc, name = seg))
                }
            }
            return
        }

        val displayName = targetName.ifBlank { cleanTarget.substringAfterLast('/').ifBlank { ROOT_NAME } }
        routeStack.add(GalleryNavNode(path = cleanTarget, name = displayName))
    }

    /**
     * Reconstruye la ruta completa de carpetas desde la raíz hasta [folderPath] cuando un archivo
     * proviene del selector del sistema y la pila aún estaba en la raíz.
     */
    fun reconstructRouteIfNeeded(folderPath: String) {
        val clean = folderPath.trim('/')
        if (clean.isEmpty()) {
            ensureRouteInitialized()
            return
        }
        ensureRouteInitialized()
        if (routeStack.last().path.equals(clean, ignoreCase = true)) return
        if (routeStack.size <= 1) {
            routeStack.clear()
            routeStack.add(GalleryNavNode(path = "", name = ROOT_NAME))
            val segments = clean.split('/').filter { it.isNotBlank() }
            var acc = ""
            for (seg in segments) {
                acc = if (acc.isEmpty()) seg else "$acc/$seg"
                routeStack.add(GalleryNavNode(path = acc, name = seg))
            }
        } else {
            navigateToFolder(clean, clean.substringAfterLast('/'), expandAncestorsIfFromRoot = false)
        }
    }

    fun canGoBackFolder(): Boolean = routeStack.size > 1

    fun popPreviousFolder(): GalleryNavNode? {
        if (routeStack.size <= 1) return null
        routeStack.removeAt(routeStack.lastIndex)
        return routeStack.lastOrNull()
    }

    fun clearRoute() {
        routeStack.clear()
    }

    fun clear() {
        activePhotos = emptyList()
        activeIndex = 0
        hasExplicitContext = false
        activeFolderName = ""
        activeVideos = emptyList()
        activeVideoIndex = 0
        activeVideoFolderName = ""
        routeStack.clear()
    }
}
