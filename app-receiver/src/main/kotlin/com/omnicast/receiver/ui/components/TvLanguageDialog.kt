package com.omnicast.receiver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.core.domain.i18n.CasteraI18n
import com.omnicast.receiver.language.TvLanguageManager

@Composable
fun TvLanguageDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val currentLang = remember { TvLanguageManager.getSavedLanguage(context) }
    var focusedCode by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E182A),
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🌐", fontSize = 22.sp)
                Text(
                    text = TvLanguageManager.t(context, "dialog_select_language"),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(CasteraI18n.SUPPORTED_LANGUAGES) { (code, label) ->
                    val isSelected = currentLang == code
                    val isFocused = focusedCode == code
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isFocused -> Color(0xFF38BDF8)
                                isSelected -> Color(0xFF2E2442)
                                else -> Color(0xFF161224)
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (it.isFocused) focusedCode = code }
                            .focusable()
                            .clickable {
                                TvLanguageManager.setLanguage(context, code)
                                onLanguageSelected(code)
                                onDismiss()
                            }
                            .border(
                                width = if (isFocused) 2.dp else if (isSelected) 1.dp else 0.5.dp,
                                color = if (isFocused) Color.White else if (isSelected) Color(0xFF38BDF8) else Color(0xFF2D223F),
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                color = if (isFocused) Color(0xFF0F172A) else if (isSelected) Color(0xFF38BDF8) else Color.White,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    color = if (isFocused) Color(0xFF0F172A) else Color(0xFF38BDF8),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            var closeFocused by remember { mutableStateOf(false) }
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (closeFocused) Color(0xFF38BDF8) else Color(0xFF2E2442)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .onFocusChanged { closeFocused = it.isFocused }
                    .focusable()
            ) {
                Text(
                    text = "OK",
                    color = if (closeFocused) Color(0xFF0F172A) else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
