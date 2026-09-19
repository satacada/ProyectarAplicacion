package com.omnicast.sender.playlist

import com.omnicast.sender.history.HistoryItem
import kotlinx.serialization.Serializable

/**
 * Representa una lista de reproducción o carpeta de marcadores guardados.
 * Permite a los usuarios organizar transmisiones y películas bajo nombres personalizados,
 * similar a las listas de reproducción de YouTube.
 */
@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val items: List<HistoryItem> = emptyList(),
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
