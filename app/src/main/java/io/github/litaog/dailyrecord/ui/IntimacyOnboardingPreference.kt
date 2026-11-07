package io.github.litaog.dailyrecord.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Persists whether the user has dismissed the first-launch intimacy
 * terminology onboarding explanatory card (ADR-025 / GOAL-INTIMACY-TERMINOLOGY).
 */
internal class IntimacyOnboardingPreference(
    private val preferences: SharedPreferences,
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ),
    )

    private val _dismissed = MutableStateFlow(isDismissed)

    val dismissed: StateFlow<Boolean> = _dismissed

    val isDismissed: Boolean
        get() = preferences.getBoolean(KEY_DISMISSED, false)

    fun dismiss() {
        if (isDismissed) return
        preferences.edit { putBoolean(KEY_DISMISSED, true) }
        _dismissed.value = true
    }

    companion object {
        const val PREFERENCES_NAME = "daily_record_onboarding"
        const val KEY_DISMISSED = "has_dismissed_intimacy_onboarding"
    }
}
