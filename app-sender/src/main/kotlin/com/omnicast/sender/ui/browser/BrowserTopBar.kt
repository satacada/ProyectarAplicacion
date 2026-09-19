package com.omnicast.sender.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Barra superior del navegador web con diseño idéntico a Web Video Caster.
 * Contiene el icónico botón amarillo de reproducción con insignia de conteo de videos detectados.
 */
@Composable
fun BrowserTopBar(
    inputUrl: String,
    onInputUrlChange: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit,
    onBack: () -> Unit,
    onReload: () -> Unit,
    isDesktopMode: Boolean,
    onToggleDesktopMode: () -> Unit,
    isAdBlockEnabled: Boolean,
    onToggleAdBlock: () -> Unit,
    onForceRescan: () -> Unit,
    onClearData: () -> Unit,
    detectedStreamsCount: Int = 0,
    onOpenVideoList: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenPlaylists: () -> Unit = {},
    isTvConnected: Boolean = true
) {
    val focusManager = LocalFocusManager.current
    var showMenu by remember { mutableStateOf(false) }
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(inputUrl, TextRange(inputUrl.length)))
    }

    LaunchedEffect(inputUrl) {
        if (inputUrl != textFieldValue.text) {
            textFieldValue = TextFieldValue(inputUrl, TextRange(inputUrl.length))
        }
    }

    Surface(color = Color(0xFF0F172A), tonalElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Text("✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Text("◀", color = Color(0xFFCBD5E1), fontSize = 15.sp)
            }
            IconButton(onClick = onReload, modifier = Modifier.size(32.dp)) {
                Text("↻", color = Color(0xFFCBD5E1), fontSize = 16.sp)
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔒", fontSize = 11.sp)
                Spacer(modifier = Modifier.width(6.dp))
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        if (newValue.text != inputUrl) {
                            onInputUrlChange(newValue.text)
                        }
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF38BDF8)),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        focusManager.clearFocus()
                        onNavigate(textFieldValue.text)
                    }),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                            if (textFieldValue.text.isEmpty()) {
                                Text("Buscar o escribir URL...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                            innerTextField()
                        }
                    }
                )
                if (textFieldValue.text.isNotBlank()) {
                    IconButton(onClick = {
                        textFieldValue = TextFieldValue("")
                        onInputUrlChange("")
                    }, modifier = Modifier.size(24.dp)) {
                        Text("✕", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }

            // Botón Amarillo Icónico Web Video Caster con Insignia Contadora
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(
                    onClick = onOpenVideoList,
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (detectedStreamsCount > 0) Color(0xFFF59E0B) else Color(0xFF334155),
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Text(
                        "▶",
                        color = if (detectedStreamsCount > 0) Color.Black else Color(0xFF94A3B8),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (detectedStreamsCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = (-4).dp)
                            .background(Color(0xFFEF4444), CircleShape)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (detectedStreamsCount > 9) "9+" else detectedStreamsCount.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Indicador de Cast a la TV Box
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        if (isTvConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                        CircleShape
                    )
            )

            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Text("⋮", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isAdBlockEnabled) "🛡️ Bloqueador (Activo)" else "🛡️ Bloquear Anuncios", color = Color.White) },
                        onClick = { onToggleAdBlock(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isDesktopMode) "💻 Modo Móvil" else "💻 Sitio Escritorio", color = Color.White) },
                        onClick = { onToggleDesktopMode(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("🔍 Re-escanear videos", color = Color(0xFF38BDF8)) },
                        onClick = { onForceRescan(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("📜 Historial de Películas", color = Color(0xFFF59E0B)) },
                        onClick = { onOpenHistory(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("📂 Listas Guardadas", color = Color(0xFF10B981)) },
                        onClick = { onOpenPlaylists(); showMenu = false }
                    )
                    HorizontalDivider(color = Color(0xFF334155))
                    DropdownMenuItem(
                        text = { Text("🗑 Limpiar caché y datos", color = Color(0xFFF87171)) },
                        onClick = { onClearData(); showMenu = false }
                    )
                }
            }
        }
    }
}
