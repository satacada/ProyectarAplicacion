package com.omnicast.sender.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun StreamQualitySelectorDialog(
    pageTitle: String,
    options: List<StreamQualityOption>,
    onSelectAndPlay: (StreamQualityOption) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf(options.firstOrNull()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎬", fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Calidad de Transmisión", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(pageTitle.take(35), color = Color(0xFF94A3B8), fontSize = 12.sp, maxLines = 1)
                    }
                }

                Text("Selecciona la calidad deseada para reproducir en tu TV Box:", color = Color(0xFFCBD5E1), fontSize = 13.sp)

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(options) { opt ->
                        val isSelected = selectedOption == opt
                        val borderColor = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                        val bgColor = if (isSelected) Color(0xFF0C4A6E) else Color(0xFF0F172A)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor, RoundedCornerShape(12.dp))
                                .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                                .clickable { selectedOption = opt }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(opt.label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                if (opt.resolution != "Auto") {
                                    Text("Resolución: ${opt.resolution} | Tasa: ${opt.bitrate}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedOption = opt },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8), unselectedColor = Color.Gray)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) { Text("Cancelar") }

                    Button(
                        onClick = { selectedOption?.let { onSelectAndPlay(it) } },
                        modifier = Modifier.weight(1.5f),
                        enabled = selectedOption != null,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("🚀 Transmitir", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
