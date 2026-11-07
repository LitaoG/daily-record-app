package io.github.litaog.dailyrecord.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.litaog.dailyrecord.core.common.TerminologyStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Persists the user-selected terminology style; ELEGANT is the default.
 */
internal class TerminologyStylePreference(
    private val preferences: SharedPreferences,
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ),
    )

    private val _style = MutableStateFlow(current)

    val style: StateFlow<TerminologyStyle> = _style

    val current: TerminologyStyle
        get() = preferences.getString(KEY_TERMINOLOGY_STYLE, null)
            ?.let { stored ->
                TerminologyStyle.entries.firstOrNull {
                    it.name.equals(stored, ignoreCase = true)
                }
            } ?: TerminologyStyle.ELEGANT

    fun setStyle(style: TerminologyStyle) {
        if (style == current) return
        preferences.edit { putString(KEY_TERMINOLOGY_STYLE, style.name) }
        _style.value = style
    }

    companion object {
        const val PREFERENCES_NAME = "daily_record_terminology_style"
        const val KEY_TERMINOLOGY_STYLE = "terminology_style"
    }
}
