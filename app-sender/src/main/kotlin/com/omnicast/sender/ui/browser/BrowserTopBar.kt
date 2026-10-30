package com.omnicast.sender.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Tv
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.sender.ui.components.CastTvIcon

/**
 * Barra superior del navegador web con diseño moderno y minimalista.
 * Botones de navegación (Cerrar, Inicio, Atrás), barra de URL con favoritos y recargar,
 * botón de casteo con fondo naranja e icono Google Cast, y controles TV visibles al castear.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun BrowserTopBar(
    inputUrl: String,
    onInputUrlChange: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit,
    onHome: () -> Unit = {},
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
    isBookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenPlaylists: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onOpenWhitelist: () -> Unit = {},
    onExitApp: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeLang by com.omnicast.sender.language.AppLanguageManager.currentLanguageFlow.collectAsState()
    val focusManager = LocalFocusManager.current
    var showMenu by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showLangDialog by remember { mutableStateOf(false) }
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .background(Color(0xFF141C2B), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF24334C), RoundedCornerShape(16.dp))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = "Seguro", tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            if (newValue.text != inputUrl) onInputUrlChange(newValue.text)
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight().onFocusChanged { isFocused = it.isFocused },
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF38BDF8)),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal,
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { focusManager.clearFocus(); onNavigate(textFieldValue.text) }),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                                if (textFieldValue.text.isEmpty()) {
                                    Text(com.omnicast.sender.language.AppLanguageManager.t(context, "browser_search_hint"), color = Color(0xFF64748B), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (textFieldValue.text.isNotEmpty()) {
                        IconButton(
                            onClick = { textFieldValue = TextFieldValue(""); onInputUrlChange("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Borrar texto", tint = Color(0xFFCBD5E1), modifier = Modifier.size(17.dp))
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Opciones", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text(com.omnicast.sender.language.AppLanguageManager.t(context, "menu_controls"), color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold) },
                            onClick = { showHelpDialog = true; showMenu = false }
                        )
                        HorizontalDivider(color = Color(0xFF334155))
                        DropdownMenuItem(
                            text = { Text(com.omnicast.sender.language.AppLanguageManager.t(context, "menu_adblock"), color = Color.White) },
                            onClick = { onToggleAdBlock(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(com.omnicast.sender.language.AppLanguageManager.t(context, "menu_whitelist"), color = Color(0xFF10B981)) },
                            onClick = { onOpenWhitelist(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isDesktopMode) com.omnicast.sender.language.AppLanguageManager.t(context, "menu_mobile") else com.omnicast.sender.language.AppLanguageManager.t(context, "menu_desktop"), color = Color.White) },
                            onClick = { onToggleDesktopMode(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(com.omnicast.sender.language.AppLanguageManager.t(context, "menu_rescan"), color = Color(0xFF38BDF8)) },
                            onClick = { onForceRescan(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(com.omnicast.sender.language.AppLanguageManager.t(context, "menu_language"), color = Color(0xFF38BDF8)) },
                            onClick = { showLangDialog = true; showMenu = false }
                        )
                        HorizontalDivider(color = Color(0xFF334155))
                        DropdownMenuItem(
                            text = { Text(com.omnicast.sender.language.AppLanguageManager.t(context, "menu_exit"), color = Color(0xFFF87171), fontWeight = FontWeight.Bold) },
                            onClick = { onExitApp(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(com.omnicast.sender.language.AppLanguageManager.t(context, "menu_clear_data"), color = Color(0xFFF87171)) },
                            onClick = { onClearData(); showMenu = false }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onForward, enabled = canGoForward, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Adelante", tint = if (canGoForward) Color(0xFFCBD5E1) else Color(0xFF475569), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onReload, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Recargar", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onHome, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Home, contentDescription = "Inicio", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onToggleBookmark, modifier = Modifier.size(28.dp)) {
                    Icon(if (isBookmarked) Icons.Rounded.Star else Icons.Rounded.StarBorder, contentDescription = "Favoritos", tint = if (isBookmarked) Color(0xFFFFC107) else Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onOpenDrawer, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Menu, contentDescription = "Historial y Guardados", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                }
                if (isCasting) {
                    IconButton(onClick = onToggleBottomControls, modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Controles TV", tint = if (showBottomControls) Color(0xFF38BDF8) else Color(0xFF10B981), modifier = Modifier.size(19.dp))
                            Box(modifier = Modifier.size(5.dp).background(if (showBottomControls) Color(0xFF38BDF8) else Color(0xFF10B981), CircleShape))
                        }
                    }
                }
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(onClick = onOpenDetectedVideos, modifier = Modifier.size(28.dp)) {
                        CastTvIcon(modifier = Modifier.size(18.dp), color = if (detectedStreamsCount > 0) Color(0xFFFF9800) else Color(0xFF94A3B8))
                    }
                    if (detectedStreamsCount > 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = 3.dp, y = (-3).dp)
                                .background(Color(0xFFEF4444), CircleShape)
                                .padding(horizontal = 3.dp, vertical = 0.5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = if (detectedStreamsCount > 9) "9+" else detectedStreamsCount.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        BrowserHelpDialog(onDismiss = { showHelpDialog = false })
    }
    if (showLangDialog) {
        com.omnicast.sender.ui.components.AppLanguageDialog(
            onDismiss = { showLangDialog = false },
            onLanguageSelected = { showLangDialog = false }
        )
    }
}

