package io.github.litaog.dailyrecord.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.security.AppLockDelay
import io.github.litaog.dailyrecord.core.security.LauncherIdentity
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.components.DailyRecordDialog
import io.github.litaog.dailyrecord.ui.lock.AppLockSettings
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary

/**
 * App lock switch (ADR-023). The whole row toggles; the switch only shows the
 * state, and its thumb position and size carry it besides the color.
 */
@Composable
internal fun AppLockSettingsRow(
    settings: AppLockSettings,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    tint: Color,
) {
    // Without a secure screen lock nothing could unlock the app, so the lock
    // can then only be turned off, never on.
    val available = settings.deviceSecure || settings.enabled
    val summary = if (settings.deviceSecure) {
        AppCopy.AppLock.settingSummary
    } else {
        AppCopy.AppLock.needsScreenLock
    }
    val note = if (settings.blocksScreenshots) {
        AppCopy.AppLock.recentsAndScreenshotNote
    } else {
        AppCopy.AppLock.recentsNote
    }
    val state = if (settings.enabled) AppCopy.AppLock.settingOn else AppCopy.AppLock.settingOff
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag("settings_app_lock_row")
            .toggleable(
                value = settings.enabled,
                enabled = available,
                role = Role.Switch,
                onValueChange = settings.onEnabledChange,
            )
            // App copy instead of the system "On" / "Off", which follow the
            // device language rather than the app language.
            .semantics { stateDescription = state }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBadge(icon = BrandIconAsset.Lock, theme = theme, tint = tint)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = AppCopy.AppLock.settingTitle,
                color = DailyRecordText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = summary,
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            if (available) {
                Text(
                    text = note,
                    color = DailyRecordTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = settings.enabled,
            // The row handles the tap and the semantics.
            onCheckedChange = null,
            enabled = available,
            colors = settingsSwitchColors(tint),
        )
    }
}

/**
 * Home screen disguise switch (ADR-024). On means the launcher shows the
 * neutral "Journal" identity; the whole row toggles, like the app lock row.
 */
@Composable
internal fun LauncherDisguiseRow(
    settings: LauncherDisguiseSettings,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    tint: Color,
) {
    val disguised = settings.identity == LauncherIdentity.Journal
    // The same on / off words as the app lock switch in this card.
    val state = if (disguised) AppCopy.AppLock.settingOn else AppCopy.AppLock.settingOff
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag("settings_launcher_disguise_row")
            .toggleable(
                value = disguised,
                role = Role.Switch,
                onValueChange = { on ->
                    settings.onIdentityChange(
                        if (on) LauncherIdentity.Journal else LauncherIdentity.Default,
                    )
                },
            )
            .semantics { stateDescription = state }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBadge(icon = BrandIconAsset.Note, theme = theme, tint = tint)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = AppCopy.Disguise.settingTitle,
                color = DailyRecordText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = AppCopy.Disguise.settingSummary,
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = AppCopy.Disguise.settingNote,
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = disguised,
            // The row handles the tap and the semantics.
            onCheckedChange = null,
            colors = settingsSwitchColors(tint),
        )
    }
}

@Composable
internal fun AppLockDelayRow(
    delay: AppLockDelay,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    tint: Color,
    onClick: () -> Unit,
) {
    val value = delay.displayName()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag("settings_app_lock_delay_row")
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = AppCopy.Components.joinSemantics(
                    AppCopy.AppLock.delayTitle,
                    value,
                )
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBadge(icon = BrandIconAsset.Clock, theme = theme, tint = tint)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = AppCopy.AppLock.delayTitle,
                color = DailyRecordText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        BrandIcon(
            asset = BrandIconAsset.Next,
            theme = theme,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun AppLockDelaySelectionDialog(
    currentDelay: AppLockDelay,
    tint: Color,
    onDismiss: () -> Unit,
    onSelected: (AppLockDelay) -> Unit,
) {
    DailyRecordDialog(
        title = AppCopy.AppLock.delayDialogTitle,
        testTag = "settings_app_lock_delay_dialog",
        onDismissRequest = onDismiss,
    ) {
        AppLockDelay.entries.forEach { option ->
            SettingsOptionRow(
                label = option.displayName(),
                selected = option == currentDelay,
                tint = tint,
                testTag = "settings_app_lock_delay_option_${option.id}",
                onClick = { onSelected(option) },
            )
        }
    }
}

private fun AppLockDelay.displayName(): String = when (this) {
    AppLockDelay.Immediately -> AppCopy.AppLock.delayImmediately
    AppLockDelay.OneMinute -> AppCopy.AppLock.delayOneMinute
    AppLockDelay.FiveMinutes -> AppCopy.AppLock.delayFiveMinutes
}
