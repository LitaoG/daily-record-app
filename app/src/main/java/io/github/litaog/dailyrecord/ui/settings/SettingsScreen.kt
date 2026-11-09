package io.github.litaog.dailyrecord.ui.settings

import io.github.litaog.dailyrecord.ui.components.predictiveBackPreview
import io.github.litaog.dailyrecord.ui.components.rememberPredictiveBackState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.core.common.TerminologyStyle
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.components.brandIconTheme
import io.github.litaog.dailyrecord.ui.lock.AppLockSettings
import io.github.litaog.dailyrecord.ui.theme.DailyRecordColorTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordThemeState
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.dailyRecordBackdropBrush
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlassBackground

@Composable
internal fun SettingsScreen(
    versionName: String,
    accountEmail: String?,
    syncStatus: SyncStatus,
    moduleColors: RecordModuleColorTokens,
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    colorTheme: DailyRecordColorTheme = DailyRecordThemeState.current,
    onColorThemeSelected: (DailyRecordColorTheme) -> Unit = {},
    terminologyStyle: TerminologyStyle = TerminologyStyle.ELEGANT,
    onTerminologyStyleSelected: (TerminologyStyle) -> Unit = {},
    appLockSettings: AppLockSettings? = null,
    launcherDisguiseSettings: LauncherDisguiseSettings? = null,
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onSignIn: (() -> Unit)?,
) {
    val backState = rememberPredictiveBackState(onBack = onBack)

    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showColorThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showTerminologyStyleDialog by rememberSaveable { mutableStateOf(false) }
    var showAppLockDelayDialog by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val backdropBrush = remember(moduleColors) { dailyRecordBackdropBrush(moduleColors) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .predictiveBackPreview(backState)
            .background(backdropBrush),
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_screen"),
            containerColor = Color.Transparent,
            topBar = {
                SettingsTopBar(
                    onBack = onBack,
                    theme = moduleColors.brandIconTheme,
                )
            },
        ) { contentPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    horizontal = DailyRecordSpacing.ScreenHorizontal,
                    vertical = DailyRecordSpacing.ScreenVertical,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SettingsSectionTitle(AppCopy.Settings.accountSection)
                }
                item {
                    AccountSettingsCard(
                        accountEmail = accountEmail,
                        syncStatus = syncStatus,
                        moduleColors = moduleColors,
                        onOpenAccount = onOpenAccount,
                        onSignIn = onSignIn,
                    )
                }
                item {
                    SettingsSectionTitle(
                        text = AppCopy.Settings.generalSection,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                item {
                    SettingsCard(contentSpacing = 0.dp) {
                        LanguageSettingsRow(
                            language = language,
                            theme = moduleColors.brandIconTheme,
                            tint = moduleColors.primary,
                            onClick = { showLanguageDialog = true },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = DailyRecordDivider,
                        )
                        TerminologyStyleSettingsRow(
                            terminologyStyle = terminologyStyle,
                            theme = moduleColors.brandIconTheme,
                            tint = moduleColors.primary,
                            onClick = { showTerminologyStyleDialog = true },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = DailyRecordDivider,
                        )
                        ColorThemeSettingsRow(
                            colorTheme = colorTheme,
                            theme = moduleColors.brandIconTheme,
                            onClick = { showColorThemeDialog = true },
                        )
                    }
                }
                item {
                    SettingsSectionTitle(
                        text = AppCopy.Settings.dataSection,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (appLockSettings != null || launcherDisguiseSettings != null) {
                    item {
                        // The section's controls share one card, above the
                        // cards that only explain.
                        SettingsCard(contentSpacing = 0.dp) {
                            if (appLockSettings != null) {
                                AppLockSettingsRow(
                                    settings = appLockSettings,
                                    theme = moduleColors.brandIconTheme,
                                    tint = moduleColors.primary,
                                )
                                if (appLockSettings.enabled) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 52.dp),
                                        color = DailyRecordDivider,
                                    )
                                    AppLockDelayRow(
                                        delay = appLockSettings.delay,
                                        theme = moduleColors.brandIconTheme,
                                        tint = moduleColors.primary,
                                        onClick = { showAppLockDelayDialog = true },
                                    )
                                }
                            }
                            if (launcherDisguiseSettings != null) {
                                if (appLockSettings != null) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 52.dp),
                                        color = DailyRecordDivider,
                                    )
                                }
                                LauncherDisguiseRow(
                                    settings = launcherDisguiseSettings,
                                    theme = moduleColors.brandIconTheme,
                                    tint = moduleColors.primary,
                                )
                            }
                        }
                    }
                }
                item {
                    SettingsCard(contentSpacing = 0.dp) {
                        SettingsInfoRow(
                            // The lock symbol now belongs to the app lock row.
                            icon = BrandIconAsset.Device,
                            theme = moduleColors.brandIconTheme,
                            title = AppCopy.Settings.localFirstTitle,
                            summary = AppCopy.Settings.localFirstSummary,
                            tint = moduleColors.primary,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = DailyRecordDivider,
                        )
                        SettingsInfoRow(
                            icon = BrandIconAsset.Shield,
                            theme = moduleColors.brandIconTheme,
                            title = AppCopy.Settings.privacyTitle,
                            summary = AppCopy.Settings.privacySummary,
                            tint = moduleColors.primary,
                        )
                    }
                }
                item {
                    SettingsSectionTitle(
                        text = AppCopy.Settings.aboutSection,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                item {
                    SettingsCard(contentSpacing = 0.dp) {
                        AboutRow(
                            label = AppCopy.Settings.version,
                            value = versionName,
                            testTag = "settings_version",
                        )
                        HorizontalDivider(color = DailyRecordDivider)
                        AboutRow(
                            label = AppCopy.Settings.license,
                            value = AppCopy.Settings.licenseValue,
                        )
                        HorizontalDivider(color = DailyRecordDivider)
                        AboutLinkRow(
                            label = AppCopy.Settings.privacyPolicy,
                            actionDescription = AppCopy.Settings.privacyPolicyAction,
                            theme = moduleColors.brandIconTheme,
                            testTag = "settings_privacy_policy",
                            onClick = {
                                // Open the notice in the language the app shows.
                                val url = when (language) {
                                    AppLanguage.ZH -> PRIVACY_POLICY_URL_ZH
                                    AppLanguage.EN -> PRIVACY_POLICY_URL_EN
                                }
                                // Only a device without any browser fails here;
                                // the row then simply does nothing.
                                runCatching { uriHandler.openUri(url) }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = language,
            tint = moduleColors.primary,
            onDismiss = { showLanguageDialog = false },
            onSelected = { selected ->
                // Settle the dialog state before requesting the language
                // change: the callback may recreate the activity, and the
                // restored dialog must not reopen.
                showLanguageDialog = false
                onLanguageSelected(selected)
            },
        )
    }

    if (showColorThemeDialog) {
        ColorThemeSelectionDialog(
            currentTheme = colorTheme,
            tint = moduleColors.primary,
            onDismiss = { showColorThemeDialog = false },
            onSelected = { selected ->
                // Same rule as language: the theme change recreates the
                // activity, so the restored dialog must already be closed.
                showColorThemeDialog = false
                onColorThemeSelected(selected)
            },
        )
    }

    if (showTerminologyStyleDialog) {
        TerminologyStyleSelectionDialog(
            currentStyle = terminologyStyle,
            tint = moduleColors.primary,
            onDismiss = { showTerminologyStyleDialog = false },
            onSelected = { selected ->
                showTerminologyStyleDialog = false
                onTerminologyStyleSelected(selected)
            },
        )
    }

    if (showAppLockDelayDialog && appLockSettings?.enabled == true) {
        AppLockDelaySelectionDialog(
            currentDelay = appLockSettings.delay,
            tint = moduleColors.primary,
            onDismiss = { showAppLockDelayDialog = false },
            onSelected = { selected ->
                showAppLockDelayDialog = false
                appLockSettings.onDelayChange(selected)
            },
        )
    }
}

@Composable
private fun SettingsTopBar(
    onBack: () -> Unit,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
) {
    Surface(
        modifier = Modifier.dailyRecordGlassBackground(level = DailyRecordGlassLevel.Muted),
        color = Color.Transparent,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 64.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(DailyRecordSizes.MinimumTouchTarget),
            ) {
                BrandIcon(
                    asset = BrandIconAsset.Return,
                    theme = theme,
                    contentDescription = AppCopy.Settings.back,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = AppCopy.Settings.title,
                color = DailyRecordText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
