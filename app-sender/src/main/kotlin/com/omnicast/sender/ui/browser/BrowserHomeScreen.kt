package com.omnicast.sender.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SpeedDialBookmark(val title: String, val url: String, val icon: String, val category: String)

/**
 * Pantalla de inicio y marcadores rápidos del navegador web Cyber-Dark.
 */
@Composable
fun BrowserHomeScreen(
    onSelectUrl: (String) -> Unit
) {
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
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

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
            modifier = Modifier.padding(top = 4.dp, bottom = 22.dp)
        )

        // Card Bloqueo de Publicidad Activo
        Surface(
            color = Color(0xFF101B2B),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E354F)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
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

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(shortcuts) { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelectUrl(item.url) }
                        .padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xFF162030), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFF24334C), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.icon, fontSize = 22.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = item.title,
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
