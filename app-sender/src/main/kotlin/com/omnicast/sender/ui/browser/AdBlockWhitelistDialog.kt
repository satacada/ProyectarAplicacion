package com.omnicast.sender.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Diálogo interactivo para gestionar la lista blanca de sitios permitidos en el navegador de OmniCast.
 * Permite agregar dominios a demanda (como yandex.com, ok.ru, etc.) sin tener que recompilar la aplicación.
 */
@Composable
fun AdBlockWhitelistDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onWhitelistUpdated: () -> Unit = {}
) {
    val context = LocalContext.current
    var domainsList by remember { mutableStateOf(AdBlockWhitelistManager.getDomains().toList().sorted()) }
    var inputDomain by remember { mutableStateOf("") }
    val currentHost = remember(currentUrl) { AdBlockWhitelistManager.extractDomain(currentUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛡️ Sitios Permitidos",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text("✕", color = Color(0xFF94A3B8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Los sitios aquí listados nunca serán bloqueados por el AdBlocker ni sus redirecciones serán canceladas.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Botón rápido para autorizar el sitio web actual
                if (!currentHost.isNullOrBlank() && !domainsList.contains(currentHost) && currentHost != "google.com") {
                    Button(
                        onClick = {
                            if (AdBlockWhitelistManager.addDomain(context, currentHost)) {
                                domainsList = AdBlockWhitelistManager.getDomains().toList().sorted()
                                onWhitelistUpdated()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text("➕ Permitir sitio actual: $currentHost", color = Color.White, fontSize = 13.sp)
                    }
                }

                // Campo para ingresar un nuevo dominio manualmente
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputDomain,
                        onValueChange = { inputDomain = it },
                        placeholder = { Text("ej. yandex.com o reddit.com", color = Color(0xFF64748B), fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (inputDomain.isNotBlank() && AdBlockWhitelistManager.addDomain(context, inputDomain)) {
                                inputDomain = ""
                                domainsList = AdBlockWhitelistManager.getDomains().toList().sorted()
                                onWhitelistUpdated()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = inputDomain.isNotBlank()
                    ) {
                        Text("Agregar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Dominios activos (${domainsList.size}):",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    if (domainsList.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("No hay dominios en la lista blanca.", color = Color(0xFF64748B), fontSize = 12.sp)
                            }
                        }
                    } else {
                        items(domainsList, key = { it }) { domain ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🌐 $domain",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        AdBlockWhitelistManager.removeDomain(context, domain)
                                        domainsList = AdBlockWhitelistManager.getDomains().toList().sorted()
                                        onWhitelistUpdated()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("✕", color = Color(0xFFEF4444), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cerrar", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
