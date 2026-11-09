package io.github.litaog.dailyrecord.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.core.common.TerminologyStyle
import io.github.litaog.dailyrecord.core.security.AppLockDelay
import io.github.litaog.dailyrecord.core.security.LauncherIdentity
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.lock.AppLockSettings
import io.github.litaog.dailyrecord.ui.settings.ColorThemeSelectionDialog
import io.github.litaog.dailyrecord.ui.settings.LauncherDisguiseSettings
import io.github.litaog.dailyrecord.ui.settings.RichColorThemeCard
import io.github.litaog.dailyrecord.ui.settings.SettingsScreen
import io.github.litaog.dailyrecord.ui.theme.DailyRecordColorTheme

private fun sampleAppLockSettings(enabled: Boolean = true) = AppLockSettings(
    enabled = enabled,
    delay = AppLockDelay.Immediately,
    deviceSecure = true,
    blocksScreenshots = false,
    onEnabledChange = {},
    onDelayChange = {},
)

private fun sampleLauncherDisguiseSettings(disguised: Boolean = false) = LauncherDisguiseSettings(
    identity = if (disguised) LauncherIdentity.Journal else LauncherIdentity.Default,
    onIdentityChange = {},
)

// ==========================================
// 1. Full Settings Screen Previews
// ==========================================

@Preview(name = "Settings Screen - Local Account", group = "Settings Screen")
@Composable
fun SettingsScreenLocalAccountPreview() {
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        SettingsScreen(
            versionName = "1.0.0 (Preview)",
            accountEmail = null,
            syncStatus = SyncStatus.Offline,
            moduleColors = spec.colors,
            language = AppLanguage.ZH,
            onLanguageSelected = {},
            colorTheme = DailyRecordColorTheme.Aurora,
            onColorThemeSelected = {},
            terminologyStyle = TerminologyStyle.ELEGANT,
            onTerminologyStyleSelected = {},
            appLockSettings = sampleAppLockSettings(enabled = false),
            launcherDisguiseSettings = sampleLauncherDisguiseSettings(disguised = false),
            onBack = {},
            onOpenAccount = {},
            onSignIn = {},
        )
    }
}

@Preview(name = "Settings Screen - Signed In & Syncing", group = "Settings Screen")
@Composable
fun SettingsScreenSignedInPreview() {
    DailyRecordPreviewTheme(module = RecordModule.Sex) { spec ->
        SettingsScreen(
            versionName = "1.0.0 (Preview)",
            accountEmail = "user@example.com",
            syncStatus = SyncStatus.UpToDate,
            moduleColors = spec.colors,
            language = AppLanguage.ZH,
            onLanguageSelected = {},
            colorTheme = DailyRecordColorTheme.GlacialCoral,
            onColorThemeSelected = {},
            terminologyStyle = TerminologyStyle.DIRECT,
            onTerminologyStyleSelected = {},
            appLockSettings = sampleAppLockSettings(enabled = true),
            launcherDisguiseSettings = sampleLauncherDisguiseSettings(disguised = true),
            onBack = {},
            onOpenAccount = {},
            onSignIn = null,
        )
    }
}

@PreviewThemes
@Composable
fun SettingsScreenThemesPreview() {
    DailyRecordPreviewTheme { spec ->
        SettingsScreen(
            versionName = "1.0.0 (Preview)",
            accountEmail = "preview@example.com",
            syncStatus = SyncStatus.UpToDate,
            moduleColors = spec.colors,
            language = AppLanguage.ZH,
            onLanguageSelected = {},
            colorTheme = DailyRecordColorTheme.Aurora,
            onColorThemeSelected = {},
            terminologyStyle = TerminologyStyle.ELEGANT,
            onTerminologyStyleSelected = {},
            appLockSettings = sampleAppLockSettings(enabled = true),
            launcherDisguiseSettings = sampleLauncherDisguiseSettings(),
            onBack = {},
            onOpenAccount = {},
            onSignIn = null,
        )
    }
}

@PreviewFontScales
@Composable
fun SettingsScreenFontScalesPreview() {
    DailyRecordPreviewTheme { spec ->
        SettingsScreen(
            versionName = "1.0.0 (Preview)",
            accountEmail = "fontscale@example.com",
            syncStatus = SyncStatus.UpToDate,
            moduleColors = spec.colors,
            language = AppLanguage.ZH,
            onLanguageSelected = {},
            colorTheme = DailyRecordColorTheme.Aurora,
            onColorThemeSelected = {},
            terminologyStyle = TerminologyStyle.ELEGANT,
            onTerminologyStyleSelected = {},
            appLockSettings = sampleAppLockSettings(enabled = true),
            launcherDisguiseSettings = sampleLauncherDisguiseSettings(),
            onBack = {},
            onOpenAccount = {},
            onSignIn = null,
        )
    }
}

// ==========================================
// 2. Isolated Dialog & Card Previews
// ==========================================

@Preview(name = "Theme Selection Dialog", group = "Settings Dialogs")
@Composable
fun ColorThemeSelectionDialogPreview() {
    DailyRecordPreviewTheme(theme = DailyRecordColorTheme.Aurora) { spec ->
        ColorThemeSelectionDialog(
            currentTheme = DailyRecordColorTheme.Aurora,
            tint = spec.colors.primary,
            onDismiss = {},
            onSelected = {},
        )
    }
}

@Preview(name = "Rich Theme Cards (Selected vs Unselected)", group = "Settings Dialogs")
@Composable
fun RichThemeCardsPreview() {
    DailyRecordPreviewTheme { spec ->
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RichColorThemeCard(
                colorTheme = DailyRecordColorTheme.Aurora,
                selected = true,
                tint = spec.colors.primary,
                onClick = {},
            )
            RichColorThemeCard(
                colorTheme = DailyRecordColorTheme.EmeraldBerry,
                selected = false,
                tint = spec.colors.primary,
                onClick = {},
            )
            RichColorThemeCard(
                colorTheme = DailyRecordColorTheme.TwilightLilac,
                selected = false,
                tint = spec.colors.primary,
                onClick = {},
            )
        }
    }
}
