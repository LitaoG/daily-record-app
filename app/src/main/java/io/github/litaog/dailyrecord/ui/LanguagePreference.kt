package io.github.litaog.dailyrecord.ui

import android.content.Context
import androidx.core.content.edit
import io.github.litaog.dailyrecord.core.common.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Persists the user-selected display language; ZH is the default. */
internal class LanguagePreference(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val _language = MutableStateFlow(current)

    val language: StateFlow<AppLanguage> = _language

    val current: AppLanguage
        get() = preferences.getString(KEY_LANGUAGE, null)
            ?.let { stored -> AppLanguage.entries.firstOrNull { it.name == stored } }
            ?: AppLanguage.ZH

    fun setLanguage(language: AppLanguage) {
        if (language == current) return
        preferences.edit { putString(KEY_LANGUAGE, language.name) }
        _language.value = language
    }

    private companion object {
        const val PREFERENCES_NAME = "daily_record_language"
        const val KEY_LANGUAGE = "app_language"
    }
}
