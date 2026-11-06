package io.github.litaog.dailyrecord.core.security

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Persists the app lock choice (ADR-023): whether the lock is on and how long
 * the app may stay in the background. It stores no credential and no record
 * data; the system always performs the authentication.
 */
internal class AppLockPreference(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val _enabled = MutableStateFlow(enabled)
    private val _delay = MutableStateFlow(delay)

    val enabledState: StateFlow<Boolean> = _enabled
    val delayState: StateFlow<AppLockDelay> = _delay

    /** Off until the user turns it on in settings. */
    val enabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, false)

    val delay: AppLockDelay
        get() = AppLockDelay.fromId(preferences.getString(KEY_DELAY, null))

    fun setEnabled(value: Boolean) {
        if (value == enabled) return
        preferences.edit { putBoolean(KEY_ENABLED, value) }
        _enabled.value = value
    }

    fun setDelay(value: AppLockDelay) {
        if (value == delay) return
        preferences.edit { putString(KEY_DELAY, value.id) }
        _delay.value = value
    }

    private companion object {
        const val PREFERENCES_NAME = "daily_record_app_lock"
        const val KEY_ENABLED = "enabled"
        const val KEY_DELAY = "relock_delay"
    }
}
