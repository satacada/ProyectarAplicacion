package com.omnicast.sender.gallery

import android.net.Uri

data class GalleryPhotoItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val albumName: String,
    val dateAdded: Long
)
