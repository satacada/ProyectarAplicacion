package com.omnicast.sender.language

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Gestor de idioma de la aplicación (HU-014).
 * Permite detección automática del sistema en instalación o selección manual persistente.
 */
object AppLanguageManager {
    private const val PREFS_NAME = "omnicast_language_prefs"
    private const val KEY_LANG = "app_language_preference"

    val AVAILABLE_LANGUAGES = listOf(
        "system" to "Automático (Sistema)",
        "es" to "Español",
        "en" to "English",
        "pt" to "Português",
        "ru" to "Русский",
        "it" to "Italiano"
    )

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, "system") ?: "system"
    }

    fun setLanguage(context: Context, langCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, langCode)
            .apply()
        applyLanguage(context, langCode)
    }

    fun applyLanguage(context: Context, langCode: String = getSavedLanguage(context)): Context {
        val locale = if (langCode == "system" || langCode.isBlank()) {
            Locale.getDefault()
        } else {
            Locale(langCode)
        }
        Locale.setDefault(locale)

        val res = context.resources
        val config = Configuration(res.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            context.createConfigurationContext(config)
        } else {
            context
        }
    }
}
