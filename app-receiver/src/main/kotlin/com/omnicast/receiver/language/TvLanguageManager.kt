package com.omnicast.receiver.language

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

object TvLanguageManager {
    private const val PREFS_NAME = "castera_tv_language_prefs"
    private const val KEY_LANG = "tv_language_preference"

    val currentLanguageFlow = MutableStateFlow("system")

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANG, "system") ?: "system"
        if (currentLanguageFlow.value != saved) currentLanguageFlow.value = saved
        return saved
    }

    fun setLanguage(context: Context, langCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, langCode)
            .apply()
        currentLanguageFlow.value = langCode
    }

    fun t(context: Context, key: String, lang: String = getSavedLanguage(context)): String =
        com.omnicast.core.domain.i18n.CasteraI18n.t(key, lang)
}
