package io.github.litaog.dailyrecord.ui

import android.content.Context

/** Persists the user's explicit choice to use the app without signing in. */
internal class LocalModePreference(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    val isEnabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "daily_record_local_mode"
        const val KEY_ENABLED = "enabled"
    }
}
