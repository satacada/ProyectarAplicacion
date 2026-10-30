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

    val AVAILABLE_LANGUAGES = com.omnicast.core.domain.i18n.CasteraI18n.SUPPORTED_LANGUAGES

    val currentLanguageFlow = kotlinx.coroutines.flow.MutableStateFlow("system")

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANG, "system") ?: "system"
        if (currentLanguageFlow.value != saved) currentLanguageFlow.value = saved
        return saved
    }

    fun t(context: Context, key: String): String = com.omnicast.core.domain.i18n.CasteraI18n.t(key, getSavedLanguage(context))

    fun setLanguage(context: Context, langCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, langCode)
            .apply()
        currentLanguageFlow.value = langCode
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
