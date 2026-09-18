package com.omnicast.sender.gallery

import android.net.Uri

/**
 * Almacén en memoria para la sesión activa de navegación de galería.
 * Evita TransactionTooLargeException al transferir cientos o miles de URIs entre Activities.
 */
object GallerySessionHolder {
    var activePhotos: List<GalleryPhotoItem> = emptyList()
    var activeIndex: Int = 0

    fun getActiveUri(): Uri? = activePhotos.getOrNull(activeIndex)?.uri

    fun setPhotos(photos: List<GalleryPhotoItem>, startIndex: Int = 0) {
        activePhotos = photos
        activeIndex = startIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0))
    }

    fun clear() {
        activePhotos = emptyList()
        activeIndex = 0
    }
}
