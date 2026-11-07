package io.github.litaog.dailyrecord.ui.lock

import androidx.compose.runtime.Immutable
import io.github.litaog.dailyrecord.core.security.AppLockDelay

/**
 * What the settings page shows and changes for the app lock (ADR-023).
 * MainActivity owns the preference and the session; settings only reads this
 * snapshot and reports the user's choice back. Public because the public
 * [io.github.litaog.dailyrecord.ui.DailyRecordApp] takes it as a parameter.
 */
@Immutable
data class AppLockSettings(
    val enabled: Boolean,
    val delay: AppLockDelay,
    /** False when the device has no PIN, pattern or password screen lock. */
    val deviceSecure: Boolean,
    /** True before Android 13, where hiding the recents preview also blocks screenshots. */
    val blocksScreenshots: Boolean,
    val onEnabledChange: (Boolean) -> Unit,
    val onDelayChange: (AppLockDelay) -> Unit,
)
