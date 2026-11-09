package io.github.litaog.dailyrecord.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.TopDestination
import io.github.litaog.dailyrecord.ui.account.AccountTopBar
import io.github.litaog.dailyrecord.ui.account.LocalAccountTopBar
import io.github.litaog.dailyrecord.ui.components.DailyRecordBottomBar
import io.github.litaog.dailyrecord.ui.components.RecordModuleSelector
import io.github.litaog.dailyrecord.ui.lock.AppLockContent
import io.github.litaog.dailyrecord.ui.uiSpec

/**
 * Milestone 2 Compose Previews: App Shell, Navigation, Chrome & App Lock (Feature 24).
 * Covers ModuleSelector, DailyRecordBottomBar, AccountTopBar, and AppLockScreen across
 * [PreviewModules], [PreviewThemes], and [PreviewFontScales].
 */

// ==========================================
// 1. Dual-Module Switcher Previews
// ==========================================

@Preview(name = "Module Selector - Solo Active", group = "App Shell")
@Composable
fun ModuleSelectorSoloPreview() {
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { _ ->
        val specs = listOf(RecordModule.HandBrew.uiSpec(), RecordModule.Sex.uiSpec())
        RecordModuleSelector(
            selected = RecordModule.HandBrew,
            specs = specs,
            onSelected = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Module Selector - Duo Active", group = "App Shell")
@Composable
fun ModuleSelectorDuoPreview() {
    DailyRecordPreviewTheme(module = RecordModule.Sex) { _ ->
        val specs = listOf(RecordModule.HandBrew.uiSpec(), RecordModule.Sex.uiSpec())
        RecordModuleSelector(
            selected = RecordModule.Sex,
            specs = specs,
            onSelected = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@PreviewThemes
@Composable
fun ModuleSelectorThemesPreview() {
    DailyRecordPreviewTheme { _ ->
        val specs = listOf(RecordModule.HandBrew.uiSpec(), RecordModule.Sex.uiSpec())
        RecordModuleSelector(
            selected = RecordModule.HandBrew,
            specs = specs,
            onSelected = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@PreviewFontScales
@Composable
fun ModuleSelectorFontScalesPreview() {
    DailyRecordPreviewTheme { _ ->
        val specs = listOf(RecordModule.HandBrew.uiSpec(), RecordModule.Sex.uiSpec())
        RecordModuleSelector(
            selected = RecordModule.HandBrew,
            specs = specs,
            onSelected = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

// ==========================================
// 2. Primary 2-Destination Bottom Bar Previews
// ==========================================

@Preview(name = "Bottom Bar - Calendar (Solo)", group = "App Shell")
@Composable
fun BottomBarCalendarSoloPreview() {
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        DailyRecordBottomBar(
            selected = TopDestination.Calendar,
            colors = spec.colors,
            onSelected = {},
        )
    }
}

@Preview(name = "Bottom Bar - Statistics (Duo)", group = "App Shell")
@Composable
fun BottomBarStatisticsDuoPreview() {
    DailyRecordPreviewTheme(module = RecordModule.Sex) { spec ->
        DailyRecordBottomBar(
            selected = TopDestination.Statistics,
            colors = spec.colors,
            onSelected = {},
        )
    }
}

@PreviewModules
@Composable
fun BottomBarModulesPreview() {
    DailyRecordPreviewTheme { spec ->
        DailyRecordBottomBar(
            selected = TopDestination.Calendar,
            colors = spec.colors,
            onSelected = {},
        )
    }
}

@PreviewThemes
@Composable
fun BottomBarThemesPreview() {
    DailyRecordPreviewTheme { spec ->
        DailyRecordBottomBar(
            selected = TopDestination.Calendar,
            colors = spec.colors,
            onSelected = {},
        )
    }
}

@PreviewFontScales
@Composable
fun BottomBarFontScalesPreview() {
    DailyRecordPreviewTheme { spec ->
        DailyRecordBottomBar(
            selected = TopDestination.Calendar,
            colors = spec.colors,
            onSelected = {},
        )
    }
}

// ==========================================
// 3. Account Top Bar & Sync Status Previews
// ==========================================

@Preview(name = "Account Top Bar - Sync Status Matrix", group = "App Shell")
@Composable
fun AccountTopBarSyncMatrixPreview() {
    DailyRecordPreviewTheme { spec ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccountTopBar(status = SyncStatus.UpToDate, colors = spec.colors, onClick = {}, onSettings = {})
            AccountTopBar(status = SyncStatus.Syncing, colors = spec.colors, onClick = {}, onSettings = {})
            AccountTopBar(status = SyncStatus.Pending(3), colors = spec.colors, onClick = {}, onSettings = {})
            AccountTopBar(status = SyncStatus.Offline, colors = spec.colors, onClick = {}, onSettings = {})
            AccountTopBar(
                status = SyncStatus.Failed("Connection lost"),
                colors = spec.colors,
                onClick = {},
                onSettings = {},
            )
        }
    }
}

@Preview(name = "Account Top Bar - Local Mode", group = "App Shell")
@Composable
fun LocalAccountTopBarPreview() {
    DailyRecordPreviewTheme { spec ->
        LocalAccountTopBar(
            colors = spec.colors,
            onSignIn = {},
            onSettings = {},
        )
    }
}

@PreviewFontScales
@Composable
fun AccountTopBarFontScalesPreview() {
    DailyRecordPreviewTheme { spec ->
        AccountTopBar(
            status = SyncStatus.Pending(2),
            colors = spec.colors,
            onClick = {},
            onSettings = {},
        )
    }
}

// ==========================================
// 4. Biometric App Lock Screen Previews
// ==========================================

@Preview(name = "App Lock - Default", group = "App Shell")
@Composable
fun AppLockDefaultPreview() {
    DailyRecordPreviewTheme(showBackdrop = false) { _ ->
        AppLockContent(
            attemptFailed = false,
            onUnlock = {},
        )
    }
}

@Preview(name = "App Lock - Attempt Failed", group = "App Shell")
@Composable
fun AppLockAttemptFailedPreview() {
    DailyRecordPreviewTheme(showBackdrop = false) { _ ->
        AppLockContent(
            attemptFailed = true,
            onUnlock = {},
        )
    }
}

@PreviewThemes
@Composable
fun AppLockThemesPreview() {
    DailyRecordPreviewTheme(showBackdrop = false) { _ ->
        AppLockContent(
            attemptFailed = false,
            onUnlock = {},
        )
    }
}

@PreviewFontScales
@Composable
fun AppLockFontScalesPreview() {
    DailyRecordPreviewTheme(showBackdrop = false) { _ ->
        AppLockContent(
            attemptFailed = false,
            onUnlock = {},
        )
    }
}
