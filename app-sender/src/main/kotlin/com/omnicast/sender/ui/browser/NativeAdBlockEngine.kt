package com.omnicast.sender.ui.browser

import android.content.Context
import android.util.Log
import io.github.edsuns.adblockclient.AdBlockClient
import io.github.edsuns.adblockclient.ResourceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Motor C++ nativo de AdBlock basado en libadblock-client.so.
 * Evalúa reglas en microsegundos y bloquea anuncios, redirecciones y popups maliciosos.
 */
object NativeAdBlockEngine {
    private const val TAG = "NativeAdBlockEngine"
    private var client: AdBlockClient? = null
    private var isReady = false
    private val scope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) {
        if (isReady) return
        scope.launch {
            try {
                val c = AdBlockClient("omnicast_adblock")
                val customFile = File(context.filesDir, "adblock/easylist.txt")
                val rulesBytes = if (customFile.exists() && customFile.length() >= 500_000L) {
                    customFile.readBytes()
                } else {
                    if (customFile.exists()) customFile.delete()
                    try {
                        context.assets.open("adblock/easylist_min.txt").use { it.readBytes() }
                    } catch (_: Throwable) {
                        buildFallbackRules().toByteArray(Charsets.UTF_8)
                    }
                }
                c.loadBasicData(rulesBytes, false)
                client = c
                isReady = true
                Log.i(TAG, "Motor nativo C++ libadblock-client.so inicializado con éxito (${rulesBytes.size} bytes de reglas)")
            } catch (e: Throwable) {
                Log.e(TAG, "Error iniciando libadblock-client nativo: ${e.message}", e)
            }
        }
    }

    fun reload(context: Context) {
        scope.launch {
            try {
                val customFile = File(context.filesDir, "adblock/easylist.txt")
                if (customFile.exists() && customFile.length() >= 500_000L) {
                    val c = AdBlockClient("omnicast_adblock")
                    val rulesBytes = customFile.readBytes()
                    c.loadBasicData(rulesBytes, false)
                    client = c
                    isReady = true
                    Log.i(TAG, "Reglas de EasyList recargadas dinámicamente en caliente (${rulesBytes.size} bytes)")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error recargando reglas dinámicas en C++: ${e.message}", e)
            }
        }
    }

    fun shouldBlock(url: String, pageUrl: String? = null): Boolean {
        if (AdBlocker.isVerificationOrCaptcha(url) || (pageUrl != null && AdBlocker.isVerificationOrCaptcha(pageUrl))) return false
        val isPageSearch = pageUrl != null && AdBlocker.isSearchEngine(pageUrl)
        val isUrlSearch = AdBlocker.isSearchEngine(url)
        if (AdBlockWhitelistManager.isWhitelisted(url) || (pageUrl != null && AdBlockWhitelistManager.isWhitelisted(pageUrl))) {
            val lower = url.lowercase()
            if (!isPageSearch && !isUrlSearch && (lower.contains("an.yandex.") || lower.contains("mc.yandex."))) return true
            return false
        }
        val lower = url.lowercase()
        if (lower.contains("yandex.") || lower.contains("ya.ru") || lower.contains("dzen.ru") || lower.contains("yastatic.net")) {
            if (!isPageSearch && !isUrlSearch && (lower.contains("an.yandex.") || lower.contains("mc.yandex."))) return true
            return false
        }
        if (AdBlocker.isAd(url)) return true
        val c = client ?: return false
        if (!isReady) return false
        return try {
            val docUrl = pageUrl ?: url
            val result = c.matches(url, docUrl, ResourceType.UNKNOWN)
            result.shouldBlock
        } catch (e: Throwable) {
            false
        }
    }

    private fun buildFallbackRules(): String = """
        ||doubleclick.net^
        ||googleads.g.doubleclick.net^
        ||googlesyndication.com^
        ||adservice.google.com^
        ||adnxs.com^
        ||popads.net^
        ||propellerads.com^
        ||exoclick.com^
        ||onclickmega.com^
        ||onclickalgo.com^
        ||monetag.com^
        ||adsco.re^
        ||adsterra.com^
        ||cat-bolt.com^
        ||imasdk.googleapis.com^
        ||whocares.club^
        ||pemsrv.com^
        ||gdtcdn.com^
        ||luugy.com^
        ||deloton.com^
        ||dolohen.com^
        ||adkeeper.com^
        ||trafficjunky.com^
    """.trimIndent()
}
