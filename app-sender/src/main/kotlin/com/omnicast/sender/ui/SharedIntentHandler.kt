package com.omnicast.sender.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.omnicast.sender.media.LocalImageServer
import com.omnicast.sender.stream.ClipboardStreamExtractor

object SharedIntentHandler {
    fun handle(
        context: Context,
        intent: Intent,
        clipboardExtractor: ClipboardStreamExtractor,
        onSendStream: (String, String) -> Unit,
        onSendImage: (String, String) -> Unit,
        onMoveToBack: () -> Unit
    ) {
        if (intent.action != Intent.ACTION_SEND) return
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
            val res = clipboardExtractor.extractStream(text)
            val url = res.mediaUrl
            if (res.isSuccess && url != null) {
                onSendStream(url, "Video Compartido")
                onMoveToBack()
                return
            }
        }
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        if (uri != null) {
            val mime = intent.type ?: ""
            val mediaUrl = LocalImageServer.serveMedia(uri, mime)
            if (mime.startsWith("image/")) {
                onSendImage(mediaUrl, "Foto de Galería")
                onMoveToBack()
            } else if (mime.startsWith("video/")) {
                onSendStream(mediaUrl, "Video de Galería")
                onMoveToBack()
            }
        }
    }
}
