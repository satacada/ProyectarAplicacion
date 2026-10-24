package com.omnicast.sender.ui.browser

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gestor y caja de texto nativa temporal con soporte para sugerencias dinámicas.
 * Envía el texto letra por letra a la página web y despliega chips con sugerencias
 * de Dailymotion / Buscadores para autocompletar con un solo toque.
 */
object WebInputAssistantManager {
    val isVisible = mutableStateOf(false)
    val text = mutableStateOf("")
    val placeholder = mutableStateOf("Escribe tu búsqueda aquí...")
    val suggestions = mutableStateListOf<String>()
    private var webViewRef: WeakReference<WebView>? = null
    private var suggestJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun attachWebView(wv: WebView?) {
        webViewRef = if (wv != null) WeakReference(wv) else null
    }

    fun getWebView(): WebView? = webViewRef?.get()

    fun open(initialText: String, ph: String, wv: WebView? = null) {
        if (wv != null) webViewRef = WeakReference(wv)
        Handler(Looper.getMainLooper()).post {
            text.value = initialText
            placeholder.value = if (ph.isNotBlank()) ph else "Escribe tu búsqueda aquí..."
            suggestions.clear()
            isVisible.value = true
        }
    }

    fun close() {
        suggestJob?.cancel()
        Handler(Looper.getMainLooper()).post {
            isVisible.value = false
            suggestions.clear()
        }
    }

    fun setSuggestions(list: List<String>) {
        Handler(Looper.getMainLooper()).post {
            suggestions.clear()
            suggestions.addAll(list.distinct().take(6))
        }
    }

    fun updateText(newText: String) {
        text.value = newText
        val wv = webViewRef?.get()
        InputCaretStabilizer.injectText(wv, newText)
        fetchSuggestionsDebounced(newText, wv?.url ?: "")
    }

    private fun fetchSuggestionsDebounced(query: String, pageUrl: String) {
        suggestJob?.cancel()
        val q = query.trim()
        if (q.length < 2) {
            Handler(Looper.getMainLooper()).post { suggestions.clear() }
            return
        }
        suggestJob = scope.launch {
            delay(160)
            try {
                val list = mutableListOf<String>()
                if (pageUrl.contains("dailymotion.com")) {
                    val urlStr = "https://api.dailymotion.com/videos?fields=title&limit=6&search=" + android.net.Uri.encode(q)
                    val conn = URL(urlStr).openConnection() as HttpURLConnection
                    conn.connectTimeout = 1500; conn.readTimeout = 1500
                    val json = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONObject(json).optJSONArray("list") ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i).optString("title").trim()
                        if (item.isNotBlank() && !list.contains(item)) list.add(item)
                    }
                } else {
                    val urlStr = "https://suggestqueries.google.com/complete/search?client=firefox&q=" + android.net.Uri.encode(q)
                    val conn = URL(urlStr).openConnection() as HttpURLConnection
                    conn.connectTimeout = 1500; conn.readTimeout = 1500
                    val json = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONArray(json).optJSONArray(1) ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val item = arr.optString(i).trim()
                        if (item.isNotBlank() && !list.contains(item)) list.add(item)
                    }
                }
                if (list.isNotEmpty()) setSuggestions(list)
            } catch (_: Throwable) {}
        }
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
    val currentSuggestions = WebInputAssistantManager.suggestions
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                if (currentSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(currentSuggestions) { suggestion ->
                            Surface(
                                onClick = { WebInputAssistantManager.submit(suggestion) },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = suggestion,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
