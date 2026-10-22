package com.omnicast.sender.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnicast.sender.history.NavigationTrackManager

data class SpeedDialBookmark(val title: String, val url: String, val icon: String, val category: String)

/**
 * Pantalla de inicio y marcadores rápidos del navegador web Cyber-Dark.
 */
@Composable
fun BrowserHomeScreen(
    onSelectUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val recentLinks = remember { NavigationTrackManager.getRecentVisitedSteps(context, 3) }

    val shortcuts = listOf(
        SpeedDialBookmark("Yandex", "https://yandex.com", "🔴", "Búsqueda"),
        SpeedDialBookmark("Fútbol Libre", "https://www.google.com/search?q=futbol+libre+en+vivo", "⚽", "Deportes"),
        SpeedDialBookmark("Rojadirecta", "https://www.google.com/search?q=rojadirecta+online+en+vivo", "🏆", "Deportes"),
        SpeedDialBookmark("Win Sports", "https://www.google.com/search?q=win+sports+online+en+vivo", "⚡", "Deportes"),
        SpeedDialBookmark("Películas", "https://www.google.com/search?q=ver+peliculas+online+gratis", "🎯", "Cine"),
        SpeedDialBookmark("YouTube", "https://m.youtube.com", "▶", "Video"),
        SpeedDialBookmark("Dailymotion", "https://www.dailymotion.com", "📹", "Video"),
        SpeedDialBookmark("Twitch TV", "https://m.twitch.tv", "💬", "Streams"),
        SpeedDialBookmark("Google", "https://www.google.com", "🔍", "Búsqueda")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D111A))
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = "OmniCast Web Browser",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = "Navega en cualquier sitio y transmite con calidad nativa a tu TV Box",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
        )

        // Card Bloqueo de Publicidad Activo
        Surface(
            color = Color(0xFF101B2B),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E354F)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF1B2A3E), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💡", fontSize = 20.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bloqueo de Publicidad Activo",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    Surface(
                        color = Color(0xFF09291D),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), androidx.compose.foundation.shape.CircleShape))
                            Text("Protegido", color = Color(0xFF6EE7B7), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Los anuncios invasivos y redirecciones se bloquean automáticamente.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // Fila Marcadores y Accesos Rápidos
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Marcadores y Accesos Rápidos",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Gestionar",
                color = Color(0xFF38BDF8),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        shortcuts.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectUrl(item.url) }
                            .padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color(0xFF162030), RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0xFF24334C), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.icon, fontSize = 22.sp)
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = item.title,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                repeat(4 - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        // Sección Inferior: Últimos 3 Enlaces Visitados
        if (recentLinks.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🕒", fontSize = 15.sp)
                Text(
                    text = "Últimos enlaces visitados",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentLinks.forEach { step ->
                    val host = try {
                        val h = java.net.URI(step.url).host?.removePrefix("www.")
                        if (!h.isNullOrBlank()) h else step.url
                    } catch (_: Throwable) { step.url }

                    val displayTitle = if (step.title.isNotBlank() && !step.title.startsWith("http") && step.title != "Cargando...") {
                        step.title
                    } else host

                    Surface(
                        color = Color(0xFF101B2B),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2E42)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectUrl(step.url) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1B2A3E), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFF24334C), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🌐", fontSize = 16.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayTitle,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = step.url,
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "Abrir enlace",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
