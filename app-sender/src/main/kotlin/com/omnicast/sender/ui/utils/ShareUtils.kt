package com.omnicast.sender.ui.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast

object ShareUtils {
    fun getShareableUrl(url: String, pageUrl: String? = null): String? {
        val direct = url.trim()
        val page = pageUrl?.trim()
        return when {
            direct.startsWith("http://") || direct.startsWith("https://") -> direct
            page?.startsWith("http://") == true || page?.startsWith("https://") == true -> page
            else -> null
        }
    }

    fun canShare(url: String, pageUrl: String? = null): Boolean {
        return getShareableUrl(url, pageUrl) != null
    }

    fun shareVideo(context: Context, title: String, url: String, pageUrl: String? = null) {
        val shareUrl = getShareableUrl(url, pageUrl) ?: return
        try {
            val shareText = if (!pageUrl.isNullOrBlank() && pageUrl.trim() != shareUrl) {
                if (title.isNotBlank()) "$title\n$shareUrl\nWeb: $pageUrl" else "$shareUrl\nWeb: $pageUrl"
            } else {
                if (title.isNotBlank()) "$title\n$shareUrl" else shareUrl
            }
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "Video" })
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            val chooser = Intent.createChooser(sendIntent, "Compartir enlace del video")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Throwable) {
            Toast.makeText(context, "No se pudo compartir el video", Toast.LENGTH_SHORT).show()
        }
    }
}
