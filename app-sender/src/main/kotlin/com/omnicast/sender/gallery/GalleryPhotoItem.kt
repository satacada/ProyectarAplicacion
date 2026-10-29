package com.omnicast.sender.gallery

import android.net.Uri

data class GalleryPhotoItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val albumName: String,
    val dateAdded: Long,
    val isVideo: Boolean = false,
    val mimeType: String = "image/jpeg",
    val folderPath: String = "",
    val durationMs: Long = 0L
)

data class GalleryFolderInfo(
    val path: String,
    val name: String,
    val photoCount: Int,
    val videoCount: Int,
    val coverItem: GalleryPhotoItem?,
    val isDirectAlbum: Boolean = false
) {
    val totalCount: Int get() = photoCount + videoCount
}

data class GalleryNavNode(
    val path: String,
    val name: String,
    val firstVisibleIndex: Int = 0,
    val firstVisibleOffset: Int = 0
)

data class FolderViewData(
    val subfolders: List<GalleryFolderInfo>,
    val directItems: List<GalleryPhotoItem>,
    val allFolders: List<GalleryFolderInfo>
)
