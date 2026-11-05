package io.github.litaog.dailyrecord.ui

import android.content.Context
import androidx.core.content.edit
import io.github.litaog.dailyrecord.ui.theme.DailyRecordColorTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Persists the user-selected color theme (ADR-022); Aurora is the default.
 * Stores only the theme id, never record data.
 */
internal class ColorThemePreference(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val _theme = MutableStateFlow(current)

    val theme: StateFlow<DailyRecordColorTheme> = _theme

    val current: DailyRecordColorTheme
        get() = DailyRecordColorTheme.fromId(preferences.getString(KEY_THEME, null))

    fun setTheme(theme: DailyRecordColorTheme) {
        if (theme == current) return
        preferences.edit { putString(KEY_THEME, theme.id) }
        _theme.value = theme
    }

    private companion object {
        const val PREFERENCES_NAME = "daily_record_color_theme"
        const val KEY_THEME = "color_theme"
    }
}
