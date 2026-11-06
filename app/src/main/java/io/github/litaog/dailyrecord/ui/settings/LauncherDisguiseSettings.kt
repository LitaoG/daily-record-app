package io.github.litaog.dailyrecord.ui.settings

import androidx.compose.runtime.Immutable
import io.github.litaog.dailyrecord.core.security.LauncherIdentity

/**
 * What the settings page shows and changes for the home screen disguise
 * (ADR-024). MainActivity owns the stored choice and the launcher aliases;
 * settings only reads this snapshot and reports the user's choice back.
 * Public because the public [io.github.litaog.dailyrecord.ui.DailyRecordApp]
 * takes it as a parameter.
 */
@Immutable
data class LauncherDisguiseSettings(
    val identity: LauncherIdentity,
    val onIdentityChange: (LauncherIdentity) -> Unit,
)
