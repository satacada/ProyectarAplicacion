package com.omnicast.sender.ui.browser

import android.content.Context
import android.net.Uri
import java.net.URI

/**
 * Gestor dinámico de lista blanca (whitelist) de sitios web y dominios para el bloqueador de anuncios.
 * Permite al usuario autorizar sitios completos (como yandex.com, yandex.ru, etc.) sin requerir recompilar la aplicación.
 */
object AdBlockWhitelistManager {

    private const val PREFS_NAME = "omnicast_adblock_whitelist_prefs"
    private const val KEY_WHITELIST_SET = "whitelisted_domains"

    internal val DEFAULT_WHITELIST = setOf(
        "yandex.com",
        "yandex.ru",
        "ya.ru",
        "dzen.ru",
        "yastatic.net",
        "dailymotion.com",
        "geo.dailymotion.com",
        "dmcdn.net",
        "static1.dmcdn.net",
        "youtube.com",
        "ytimg.com",
        "googlevideo.com"
    )

    private val cachedDomains = mutableSetOf<String>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(KEY_WHITELIST_SET, null)
        synchronized(cachedDomains) {
            cachedDomains.clear()
            if (saved != null) {
                cachedDomains.addAll(saved)
            } else {
                cachedDomains.addAll(DEFAULT_WHITELIST)
                prefs.edit().putStringSet(KEY_WHITELIST_SET, DEFAULT_WHITELIST).apply()
            }
        }
        isInitialized = true
    }

    fun getDomains(): Set<String> = synchronized(cachedDomains) {
        cachedDomains.toSet()
    }

    fun addDomain(context: Context, domainInput: String): Boolean {
        val cleanDomain = extractDomain(domainInput) ?: return false
        synchronized(cachedDomains) {
            cachedDomains.add(cleanDomain)
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putStringSet(KEY_WHITELIST_SET, HashSet(cachedDomains)).apply()
        }
        return true
    }

    fun removeDomain(context: Context, domainInput: String): Boolean {
        val cleanDomain = extractDomain(domainInput) ?: domainInput.trim().lowercase()
        synchronized(cachedDomains) {
            val removed = cachedDomains.remove(cleanDomain)
            if (removed) {
                val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putStringSet(KEY_WHITELIST_SET, HashSet(cachedDomains)).apply()
            }
            return removed
        }
    }

    fun extractSiteKey(rawUrl: String): String {
        val trimmed = rawUrl.trim().lowercase()
        if (trimmed.isBlank()) return ""
        return try {
            val formatted = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) "https://$trimmed" else trimmed
            val uri = URI(formatted)
            val host = (uri.host ?: Uri.parse(formatted).host)?.removePrefix("www.")?.lowercase() ?: return trimmed
            val path = (uri.path ?: Uri.parse(formatted).path ?: "").trimEnd('/')
            if (path.contains("/en-vivo") || path.contains("/canal") || path.contains("/watch") || path.contains("/live")) {
                "$host$path"
            } else {
                host
            }
        } catch (_: Throwable) {
            extractDomain(trimmed) ?: trimmed
        }
    }

    private fun isDefaultPlatform(host: String): Boolean =
        host.contains("yandex.") || host.contains("dailymotion.") || host.contains("youtube.") || host.contains("google.")

    fun isWhitelisted(url: String): Boolean {
        val host = try {
            val uri = URI(url)
            uri.host?.lowercase() ?: Uri.parse(url).host?.lowercase()
        } catch (_: Throwable) {
            null
        } ?: extractDomain(url) ?: return false

        val siteKey = extractSiteKey(url)
        val path = try { URI(url).path?.lowercase() } catch (_: Throwable) { null } ?: ""
        val isPlaybackChannel = path.contains("/en-vivo") || path.contains("/canal") || path.contains("/watch") || path.contains("/live")

        synchronized(cachedDomains) {
            if (cachedDomains.contains(siteKey)) return true
            if (isPlaybackChannel && !isDefaultPlatform(host)) return false
            return isWhitelistedHost(host)
        }
    }

    fun isSiteProtected(url: String): Boolean {
        if (url.isBlank() || url.startsWith("about:")) return true
        return !isWhitelisted(url)
    }

    fun toggleSiteProtection(context: Context, url: String): Boolean {
        val key = extractSiteKey(url).ifBlank { extractDomain(url) } ?: return true
        val currentlyWhitelisted = isWhitelisted(url)
        return if (currentlyWhitelisted) {
            removeDomain(context, key)
            val host = extractDomain(url)
            if (host != null && host != key) removeDomain(context, host)
            true // Ahora está protegido (verde)
        } else {
            addDomain(context, key)
            false // Ahora está desprotegido / en lista blanca (gris)
        }
    }

    fun isWhitelistedHost(host: String): Boolean {
        val clean = host.trim().lowercase()
        synchronized(cachedDomains) {
            for (domain in cachedDomains) {
                if (clean == domain || clean.endsWith(".$domain") || domain.endsWith(".$clean")) {
                    return true
                }
            }
        }
        return false
    }

    fun extractDomain(input: String): String? {
        val trimmed = input.trim().lowercase()
        if (trimmed.isBlank()) return null
        return try {
            val formatted = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
            val uri = URI(formatted)
            val host = uri.host ?: Uri.parse(formatted).host
            host?.removePrefix("www.")?.lowercase()
        } catch (_: Throwable) {
            trimmed.removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore("/")
        }
    }
}
