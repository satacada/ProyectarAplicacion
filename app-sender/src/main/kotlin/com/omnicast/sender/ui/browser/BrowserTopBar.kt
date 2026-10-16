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
import androidx.compose.ui.focus.onFocusChanged
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
    onForward: () -> Unit = {},
    canGoForward: Boolean = false,
    onReload: () -> Unit,
    isDesktopMode: Boolean,
    onToggleDesktopMode: () -> Unit,
    isAdBlockEnabled: Boolean,
    onToggleAdBlock: () -> Unit,
    onForceRescan: () -> Unit,
    onClearData: () -> Unit,
    detectedStreamsCount: Int = 0,
    onToggleBottomControls: () -> Unit = {},
    onOpenDetectedVideos: () -> Unit = {},
    isCasting: Boolean = false,
    isPlaying: Boolean = false,
    showBottomControls: Boolean = false,
    onOpenHistory: () -> Unit = {},
    onOpenPlaylists: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onOpenWhitelist: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    var showMenu by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(inputUrl, TextRange(inputUrl.length)))
    }

    LaunchedEffect(inputUrl) {
        if (!isFocused && inputUrl != textFieldValue.text) {
            textFieldValue = TextFieldValue(inputUrl, TextRange(inputUrl.length))
        }
    }

    Surface(color = Color(0xFF0D111A), tonalElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(onClick = onOpenDrawer, modifier = Modifier.size(28.dp)) {
                Text("☰", color = Color(0xFF94A3B8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
                Text("<", color = Color(0xFFCBD5E1), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onForward, enabled = canGoForward, modifier = Modifier.size(28.dp)) {
                Text(
                    ">",
                    color = if (canGoForward) Color(0xFFCBD5E1) else Color(0xFF475569),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(Color(0xFF141C2B), RoundedCornerShape(18.dp))
                    .border(1.dp, Color(0xFF24334C), RoundedCornerShape(18.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔒", fontSize = 10.sp)
                Spacer(modifier = Modifier.width(4.dp))
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        if (newValue.text != inputUrl) {
                            onInputUrlChange(newValue.text)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onFocusChanged { isFocused = it.isFocused },
                    singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF38BDF8)),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
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
                                Text("Buscar o escribir URL...", color = Color(0xFF64748B), fontSize = 12.sp)
                            }
                            innerTextField()
                        }
                    }
                )
                if (textFieldValue.text.isNotBlank()) {
                    IconButton(onClick = {
                        textFieldValue = TextFieldValue("")
                        onInputUrlChange("")
                    }, modifier = Modifier.size(22.dp)) {
                        Text("✕", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }
                IconButton(onClick = onReload, modifier = Modifier.size(22.dp)) {
                    Text("⟳", color = Color(0xFFCBD5E1), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Control multimedia toggle (solo visible cuando está transmitiendo Y reproduciendo activamente en la TV)
            if (isCasting && isPlaying) {
                IconButton(
                    onClick = onToggleBottomControls,
                    modifier = Modifier
                        .size(30.dp)
                        .background(if (showBottomControls) Color(0xFF0284C7) else Color(0xFF1E293B), CircleShape)
                        .border(1.dp, if (showBottomControls) Color(0xFF38BDF8) else Color(0xFF334155), CircleShape)
                ) {
                    Text("🎛️", fontSize = 13.sp)
                }
            }

            // Botón Naranja Circular de Transmisión con Insignia Contadora (Web Video Caster)
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(
                    onClick = onOpenDetectedVideos,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (detectedStreamsCount > 0) Color(0xFFFF7A00) else Color(0xFF334155),
                            CircleShape
                        )
                ) {
                    Text(
                        "▶",
                        color = if (detectedStreamsCount > 0) Color.White else Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (detectedStreamsCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = 3.dp, y = (-3).dp)
                            .background(Color(0xFFEF4444), CircleShape)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (detectedStreamsCount > 9) "9+" else detectedStreamsCount.toString(),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                    Text("⋮", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    DropdownMenuItem(
                        text = { Text("ℹ️ Detalle de Controles", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold) },
                        onClick = { showHelpDialog = true; showMenu = false }
                    )
                    HorizontalDivider(color = Color(0xFF334155))
                    DropdownMenuItem(
                        text = { Text(if (isAdBlockEnabled) "🛡️ Bloqueador (Activo)" else "🛡️ Bloquear Anuncios", color = Color.White) },
                        onClick = { onToggleAdBlock(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("✅ Sitios Permitidos (Lista Blanca)", color = Color(0xFF10B981)) },
                        onClick = { onOpenWhitelist(); showMenu = false }
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
                    DropdownMenuItem(
                        text = { Text("☰ Menú Lateral Completo", color = Color(0xFF38BDF8)) },
                        onClick = { onOpenDrawer(); showMenu = false }
                    )
                    HorizontalDivider(color = Color(0xFF334155))
                    DropdownMenuItem(
                        text = { Text("✕ Cerrar Navegador", color = Color(0xFFCBD5E1)) },
                        onClick = { onClose(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("🗑 Limpiar caché y datos", color = Color(0xFFF87171)) },
                        onClick = { onClearData(); showMenu = false }
                    )
                }
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("ℹ️ Detalle de Controles", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HelpItem("▶ Flujos", "Muestra la lista de videos capturados para emitir a la TV.")
                    HelpItem("🎛️ Control", "Muestra u oculta los controles de reproducción de la TV.")
                    HelpItem("⟳ Recargar", "Actualiza la página actual dentro del buscador.")
                    HelpItem("❚❚ / ▶", "Pausa o reanuda la reproducción en el televisor.")
                    HelpItem("↺ / ↻", "Adelanta o retrocede 10 segundos en la película.")
                    HelpItem("🔊 Volumen", "Regula el volumen de sonido en el televisor.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Entendido", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold) }
            }
        )
    }
}

@Composable
private fun HelpItem(title: String, desc: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(desc, color = Color(0xFFCBD5E1), fontSize = 11.sp)
    }
}
