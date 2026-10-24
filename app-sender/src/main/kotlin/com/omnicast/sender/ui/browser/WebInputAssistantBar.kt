package com.omnicast.sender.ui.browser

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.lang.ref.WeakReference

/**
 * Gestor y caja de texto nativa temporal para entrada visible y asistida en páginas web.
 * Permite al usuario ver, tipear y corregir texto en Jetpack Compose nativo con Gboard,
 * inyectando y enviando el texto sincronizado a la página web (Dailymotion/Buscadores).
 */
object WebInputAssistantManager {
    val isVisible = mutableStateOf(false)
    val text = mutableStateOf("")
    val placeholder = mutableStateOf("Escribe tu búsqueda aquí...")
    private var webViewRef: WeakReference<WebView>? = null

    fun attachWebView(wv: WebView?) {
        webViewRef = if (wv != null) WeakReference(wv) else null
    }

    fun open(initialText: String, ph: String, wv: WebView? = null) {
        if (wv != null) webViewRef = WeakReference(wv)
        Handler(Looper.getMainLooper()).post {
            text.value = initialText
            placeholder.value = if (ph.isNotBlank()) ph else "Escribe tu búsqueda aquí..."
            isVisible.value = true
        }
    }

    fun close() {
        Handler(Looper.getMainLooper()).post {
            isVisible.value = false
        }
    }

    fun updateText(newText: String) {
        text.value = newText
        InputCaretStabilizer.injectText(webViewRef?.get(), newText)
    }

    fun submit(query: String) {
        val q = query.trim()
        close()
        InputCaretStabilizer.submitInput(webViewRef?.get(), q)
    }
}

@Composable
fun WebInputAssistantBar(
    modifier: Modifier = Modifier
) {
    val visible by WebInputAssistantManager.isVisible
    var currentText by WebInputAssistantManager.text
    val currentPlaceholder by WebInputAssistantManager.placeholder
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            delay(120)
            try { focusRequester.requestFocus() } catch (_: Throwable) {}
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (currentText.isEmpty()) {
                        Text(
                            text = currentPlaceholder,
                            color = Color(0xFF94A3B8),
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                    }
                    BasicTextField(
                        value = currentText,
                        onValueChange = { WebInputAssistantManager.updateText(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFF38BDF8)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { WebInputAssistantManager.submit(currentText) })
                    )
                }

                if (currentText.isNotEmpty()) {
                    IconButton(
                        onClick = { WebInputAssistantManager.updateText("") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Borrar",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { WebInputAssistantManager.submit(currentText) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Enviar",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = { WebInputAssistantManager.close() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Ocultar",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
