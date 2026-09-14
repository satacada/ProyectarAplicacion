package com.omnicast.sender.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
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
 * Pantalla de inicio y marcadores rápidos del navegador web estilo Web Video Caster.
 */
@Composable
fun BrowserHomeScreen(
    onSelectUrl: (String) -> Unit
) {
    val shortcuts = listOf(
        SpeedDialBookmark("Fútbol Libre", "https://www.google.com/search?q=futbol+libre+en+vivo", "⚽", "Deportes"),
        SpeedDialBookmark("Rojadirecta", "https://www.google.com/search?q=rojadirecta+online+en+vivo", "🏆", "Deportes"),
        SpeedDialBookmark("Win Sports", "https://www.google.com/search?q=win+sports+online+en+vivo", "🏅", "Deportes"),
        SpeedDialBookmark("YouTube Web", "https://m.youtube.com", "🎬", "Video"),
        SpeedDialBookmark("Dailymotion", "https://www.dailymotion.com", "🎥", "Video"),
        SpeedDialBookmark("Películas HD", "https://www.google.com/search?q=ver+peliculas+online+gratis", "🍿", "Cine"),
        SpeedDialBookmark("Twitch TV", "https://m.twitch.tv", "🎮", "Streams"),
        SpeedDialBookmark("Google", "https://www.google.com", "🔍", "Búsqueda")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "OmniCast Web Browser",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Navega en cualquier sitio y transmite con calidad nativa a tu TV Box",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("💡", fontSize = 28.sp)
                Column {
                    Text("Bloqueo de Publicidad Activo", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Los anuncios invasivos y redirecciones se bloquean automáticamente.", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }

        Text(
            "Marcadores y Accesos Rápidos",
            color = Color(0xFFE2E8F0),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(shortcuts) { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelectUrl(item.url) }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.icon, fontSize = 26.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        item.title,
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
