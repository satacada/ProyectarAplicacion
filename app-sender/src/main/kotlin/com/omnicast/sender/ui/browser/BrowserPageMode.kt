package com.omnicast.sender.ui.browser

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

enum class BrowserPageMode {
    OFFICIAL_PLATFORM, // Modo A: Guantes de Seda (YouTube, Dailymotion, OK.ru, buscadores). Cero inyecciones DOM, foco nativo
    MOVIE_STREAMING    // Modo B: Escaneo Total de Películas (PelisGO, Repelis, Cuevana). Pre-análisis de iframes y neutralización de click-shields
}

object BrowserPageClassifier {

    private const val PREFS_NAME = "omnicast_official_platforms_prefs"
    private const val KEY_DOMAINS = "official_domains_set"
    private const val KEY_LAST_SYNC = "last_official_sync_ms"
    private const val SYNC_INTERVAL_MS = 12 * 60 * 60 * 1000L // 12 horas

    // Lista viva oficial pública alojada en GitHub y sincronizada continuamente
    private const val GITHUB_OFFICIAL_RULES_URL =
        "https://raw.githubusercontent.com/satacada/ProyectarAplicacion/main/app-sender/src/main/assets/rules/official_platforms.json"

    private const val SUPABASE_LOGS_URL = "https://pfhjrplnfuupftgzdnop.supabase.co/rest/v1/omnicast_system_logs"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBmaGpycGxuZnV1cGZ0Z3pkbm9wIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg5NTc4NDEsImV4cCI6MjEwNDUzMzg0MX0.u2ysI-hTvT7LczA7QL2YS2mudW8pSI644Ig8gyvTs7o"

    private val DEFAULT_OFFICIAL_DOMAINS = hashSetOf(
        "dailymotion.com", "geo.dailymotion.com", "dmcdn.net", "static1.dmcdn.net",
        "youtube.com", "youtu.be", "ytimg.com", "googlevideo.com",
        "ok.ru", "odnoklassniki.ru", "okcdn.ru", "mycdn.me",
        "vk.com", "vk.ru", "vkvideo.ru", "userapi.com",
        "vimeo.com", "vimeocdn.com", "rumble.com",
        "twitch.tv", "ttvnw.net", "jtvnw.net",
        "canela.tv", "pluto.tv", "rtve.es", "atresplayer.com", "mitele.es",
        "tiktok.com", "byteoversea.com", "ibytedtos.com",
        "x.com", "twitter.com", "twimg.com", "t.co",
        "facebook.com", "fb.watch", "fbcdn.net", "instagram.com", "cdninstagram.com",
        "reddit.com", "redd.it", "bilibili.com", "archive.org",
        "google.com", "bing.com", "duckduckgo.com", "yahoo.com",
        "yandex.com", "yandex.ru", "ya.ru", "dzen.ru", "yastatic.net",
        "ecosia.org", "brave.com", "wikipedia.org", "wikimedia.org", "github.com",
        "disneyplus.com", "netflix.com", "primevideo.com", "max.com", "hbomax.com", "crunchyroll.com"
    )

    private val cachedDomains = ConcurrentHashMap.newKeySet<String>()
    @Volatile private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) {
        if (isInitialized) return
        cachedDomains.addAll(DEFAULT_OFFICIAL_DOMAINS)
        loadFromAssets(context)
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(KEY_DOMAINS, null)
        if (saved != null) cachedDomains.addAll(saved)
        isInitialized = true
        syncFromCloudIfNeeded(context.applicationContext)
    }

    private fun loadFromAssets(context: Context) {
        try {
            val jsonText = context.assets.open("rules/official_platforms.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonText)
            val arr = root.optJSONArray("official_domains") ?: return
            for (i in 0 until arr.length()) {
                val d = arr.optString(i).trim().lowercase().removePrefix("www.")
                if (d.isNotBlank()) cachedDomains.add(d)
            }
        } catch (_: Throwable) {}
    }

    fun classify(url: String?): BrowserPageMode {
        if (url.isNullOrBlank()) return BrowserPageMode.MOVIE_STREAMING
        if (isOfficialPlatform(url)) return BrowserPageMode.OFFICIAL_PLATFORM
        return BrowserPageMode.MOVIE_STREAMING
    }

    fun isOfficialPlatform(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val host = extractHost(url) ?: return false
        val cleanHost = host.removePrefix("www.")

        if (cachedDomains.isEmpty()) cachedDomains.addAll(DEFAULT_OFFICIAL_DOMAINS)
        for (domain in cachedDomains) {
            if (cleanHost == domain || cleanHost.endsWith(".$domain") || domain.endsWith(".$cleanHost")) return true
        }

        if (cleanHost.contains("google.") || cleanHost.contains("yandex.") || cleanHost.contains("bing.") ||
            cleanHost.contains("yahoo.") || cleanHost.contains("duckduckgo.")) return true

        if (AdBlocker.isVerificationOrCaptcha(cleanHost)) return true
        return false
    }

    fun addOfficialDomain(context: Context, domain: String) {
        val clean = domain.trim().lowercase().removePrefix("www.")
        if (clean.isBlank()) return
        cachedDomains.add(clean)
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_DOMAINS, cachedDomains.toSet()).apply()
    }

    fun syncFromCloudIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
        val now = System.currentTimeMillis()
        if (now - lastSync < SYNC_INTERVAL_MS) return

        scope.launch {
            var updated = false
            try {
                // 1. Sincronización desde lista viva en GitHub (uBlock / yt-dlp mirror)
                val ghUrl = URL(GITHUB_OFFICIAL_RULES_URL)
                val ghConn = (ghUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000; readTimeout = 6000; requestMethod = "GET"
                }
                if (ghConn.responseCode == 200) {
                    val body = ghConn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(body)
                    root.optJSONArray("official_domains")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val d = arr.optString(i).trim().lowercase().removePrefix("www.")
                            if (d.isNotBlank()) { cachedDomains.add(d); updated = true }
                        }
                    }
                }
            } catch (_: Throwable) {}

            try {
                // 2. Sincronización desde Supabase (Inteligencia Colectiva)
                val sbUrl = URL("$SUPABASE_LOGS_URL?select=message&event=eq.OFFICIAL_PLATFORM&order=created_at.desc&limit=50")
                val sbConn = (sbUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000; readTimeout = 6000; requestMethod = "GET"
                    setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    setRequestProperty("Accept", "application/json")
                }
                if (sbConn.responseCode == 200) {
                    val body = sbConn.inputStream.bufferedReader().use { it.readText() }
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val row = array.optJSONObject(i) ?: continue
                        val domain = row.optString("message").trim().lowercase().removePrefix("www.")
                        if (domain.isNotBlank() && !domain.startsWith("{")) {
                            cachedDomains.add(domain); updated = true
                        }
                    }
                }
            } catch (_: Throwable) {}

            if (updated) {
                prefs.edit().putStringSet(KEY_DOMAINS, cachedDomains.toSet()).putLong(KEY_LAST_SYNC, now).apply()
            }
        }
    }

    private fun extractHost(url: String): String? {
        return try {
            val uri = URI(url)
            uri.host?.lowercase() ?: Uri.parse(url).host?.lowercase()
        } catch (_: Throwable) {
            val clean = url.substringAfter("://").substringBefore("/").substringBefore("?").lowercase()
            clean.ifBlank { null }
        }
    }
}
