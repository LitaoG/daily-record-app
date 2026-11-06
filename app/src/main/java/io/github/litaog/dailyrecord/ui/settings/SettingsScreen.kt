package io.github.litaog.dailyrecord.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.core.security.AppLockDelay
import io.github.litaog.dailyrecord.core.security.LauncherIdentity
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.account.color
import io.github.litaog.dailyrecord.ui.account.label
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.components.brandIconTheme
import io.github.litaog.dailyrecord.ui.components.DailyRecordDialog
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.lock.AppLockSettings
import io.github.litaog.dailyrecord.ui.theme.DailyRecordColorTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordThemeState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.dailyRecordBackdropBrush
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlass
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlassBackground

private val SettingsCardShape = RoundedCornerShape(18.dp)

/** Public privacy notices of this open-source repository (Play listing requirement). */
private const val PRIVACY_POLICY_URL_ZH = "https://github.com/LitaoG/daily-record-app/blob/main/PRIVACY.md"
private const val PRIVACY_POLICY_URL_EN = "https://github.com/LitaoG/daily-record-app/blob/main/PRIVACY.en.md"

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
    appLockSettings: AppLockSettings? = null,
    launcherDisguiseSettings: LauncherDisguiseSettings? = null,
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onSignIn: (() -> Unit)?,
) {
    BackHandler(onBack = onBack)

    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showColorThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showAppLockDelayDialog by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val backdropBrush = remember(moduleColors) { dailyRecordBackdropBrush(moduleColors) }
    Box(
        modifier = Modifier
            .fillMaxSize()
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

@Composable
private fun AccountSettingsCard(
    accountEmail: String?,
    syncStatus: SyncStatus,
    moduleColors: RecordModuleColorTokens,
    onOpenAccount: () -> Unit,
    onSignIn: (() -> Unit)?,
) {
    val signedIn = accountEmail != null
    val title = accountEmail ?: AppCopy.Settings.localAccountTitle
    val status = if (signedIn) syncStatus.label() else AppCopy.Settings.localAccountSummary

    SettingsCard(
        modifier = Modifier
            .testTag("settings_account_card")
            .then(
                if (signedIn) {
                    Modifier
                        .clickable(role = Role.Button, onClick = onOpenAccount)
                        .semantics {
                            role = Role.Button
                            contentDescription = AppCopy.Settings.accountDescription(title, status)
                        }
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIconBadge(
                icon = BrandIconAsset.CloudSync,
                theme = moduleColors.brandIconTheme,
                tint = moduleColors.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    color = DailyRecordText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (signedIn) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(syncStatus.color()),
                        )
                        Text(
                            text = status,
                            color = DailyRecordTextSecondary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            if (signedIn) {
                BrandIcon(
                    asset = BrandIconAsset.Next,
                    theme = moduleColors.brandIconTheme,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Text(
            text = if (signedIn) {
                AppCopy.Settings.signedInAccountSummary
            } else {
                AppCopy.Settings.localAccountSummary
            },
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.bodyMedium,
        )

        if (!signedIn && onSignIn != null) {
            PrimaryActionButton(
                label = AppCopy.Account.signInSync,
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                accent = moduleColors.primary,
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = DailyRecordTextSecondary,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .semantics { heading() },
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    contentSpacing: androidx.compose.ui.unit.Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .dailyRecordGlass(
                shape = SettingsCardShape,
                level = DailyRecordGlassLevel.Base,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(contentSpacing),
        content = content,
    )
}

@Composable
private fun SettingsInfoRow(
    icon: BrandIconAsset,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    title: String,
    summary: String,
    tint: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBadge(icon = icon, theme = theme, tint = tint)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                color = DailyRecordText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = summary,
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SettingsIconBadge(
    icon: BrandIconAsset,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(DailyRecordShapes.Compact)
            .background(tint.copy(alpha = .10f)),
        contentAlignment = Alignment.Center,
    ) {
        BrandIcon(
            asset = icon,
            theme = theme,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun AboutRow(
    label: String,
    value: String,
    testTag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .semantics(mergeDescendants = true) {
                contentDescription = AppCopy.Components.joinSemantics(label, value)
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = DailyRecordText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** About row that opens an external page; the chevron marks it as tappable. */
@Composable
private fun AboutLinkRow(
    label: String,
    actionDescription: String,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag(testTag)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = AppCopy.Components.joinSemantics(label, actionDescription)
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = DailyRecordText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        BrandIcon(
            asset = BrandIconAsset.Next,
            theme = theme,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun LanguageSettingsRow(
    language: AppLanguage,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag("settings_language_row")
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = AppCopy.Components.joinSemantics(
                    AppCopy.Settings.languageTitle,
                    languageDisplayName(language),
                )
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBadge(
            icon = BrandIconAsset.Edit,
            theme = theme,
            tint = tint,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = AppCopy.Settings.languageTitle,
            color = DailyRecordText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = languageDisplayName(language),
            color = DailyRecordTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        BrandIcon(
            asset = BrandIconAsset.Next,
            theme = theme,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    tint: Color,
    onDismiss: () -> Unit,
    onSelected: (AppLanguage) -> Unit,
) {
    DailyRecordDialog(
        title = AppCopy.Settings.languageDialogTitle,
        testTag = "settings_language_dialog",
        onDismissRequest = onDismiss,
    ) {
        AppLanguage.entries.forEach { language ->
            SettingsOptionRow(
                label = languageDisplayName(language),
                selected = language == currentLanguage,
                tint = tint,
                testTag = "settings_language_option",
                onClick = { onSelected(language) },
            )
        }
    }
}

/** One choice in a single-choice dialog (language, relock delay). */
@Composable
private fun SettingsOptionRow(
    label: String,
    selected: Boolean,
    tint: Color,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag(testTag)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.RadioButton
                contentDescription = AppCopy.selectedState(label, selected)
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = if (selected) tint else DailyRecordText,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(
                    if (selected) tint else Color.Transparent,
                )
                .border(
                    width = 1.dp,
                    color = if (selected) tint else DailyRecordDivider,
                    shape = CircleShape,
                ),
        )
    }
}

private fun languageDisplayName(language: AppLanguage): String = when (language) {
    AppLanguage.ZH -> AppCopy.Settings.languageZh
    AppLanguage.EN -> AppCopy.Settings.languageEn
}

@Composable
private fun ColorThemeSettingsRow(
    colorTheme: DailyRecordColorTheme,
    theme: io.github.litaog.dailyrecord.ui.components.BrandIconTheme,
    onClick: () -> Unit,
) {
    val name = colorTheme.displayName()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag("settings_color_theme_row")
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = AppCopy.Components.joinSemantics(
                    AppCopy.Settings.themeTitle,
                    name,
                )
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorThemeSwatch(colorTheme = colorTheme, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        // Theme names can be long in English; keep the value on its own line
        // so 200% font wraps instead of squeezing the title.
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = AppCopy.Settings.themeTitle,
                color = DailyRecordText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = name,
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
private fun ColorThemeSelectionDialog(
    currentTheme: DailyRecordColorTheme,
    tint: Color,
    onDismiss: () -> Unit,
    onSelected: (DailyRecordColorTheme) -> Unit,
) {
    DailyRecordDialog(
        title = AppCopy.Settings.themeDialogTitle,
        testTag = "settings_color_theme_dialog",
        onDismissRequest = onDismiss,
    ) {
        // Nine options do not fit short screens at 200% font; the bounded
        // height keeps the nested scroll measurable inside the dialog.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            DailyRecordColorTheme.entries.forEach { option ->
                ColorThemeOptionRow(
                    colorTheme = option,
                    selected = option == currentTheme,
                    tint = tint,
                    onClick = { onSelected(option) },
                )
            }
        }
    }
}

@Composable
private fun ColorThemeOptionRow(
    colorTheme: DailyRecordColorTheme,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit,
) {
    val name = colorTheme.displayName()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("settings_color_theme_option_${colorTheme.id}")
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.RadioButton
                contentDescription = AppCopy.selectedState(name, selected)
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorThemeSwatch(colorTheme = colorTheme, size = 32.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            color = if (selected) tint else DailyRecordText,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (selected) tint else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (selected) tint else DailyRecordDivider,
                    shape = CircleShape,
                ),
        )
    }
}

/**
 * Decorative swatch: a circle split between the masturbation accent (left)
 * and the sex accent (right) of the theme, flat colors only. The enclosing
 * row carries the semantics.
 */
@Composable
private fun ColorThemeSwatch(colorTheme: DailyRecordColorTheme, size: Dp) {
    val palette = colorTheme.palette
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .drawBehind {
                drawArc(
                    color = palette.solo.primary,
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = true,
                )
                drawArc(
                    color = palette.duo.primary,
                    startAngle = 270f,
                    sweepAngle = 180f,
                    useCenter = true,
                )
            }
            .border(1.dp, Color.White.copy(alpha = .8f), CircleShape),
    )
}

internal fun DailyRecordColorTheme.displayName(): String = when (this) {
    DailyRecordColorTheme.Aurora -> AppCopy.Settings.themeAurora
    DailyRecordColorTheme.GlacialCoral -> AppCopy.Settings.themeGlacialCoral
    DailyRecordColorTheme.TwilightLilac -> AppCopy.Settings.themeTwilightLilac
    DailyRecordColorTheme.EmeraldBerry -> AppCopy.Settings.themeEmeraldBerry
    DailyRecordColorTheme.TitaniumChampagne -> AppCopy.Settings.themeTitaniumChampagne
    DailyRecordColorTheme.CreamLavender -> AppCopy.Settings.themeCreamLavender
    DailyRecordColorTheme.SpringSky -> AppCopy.Settings.themeSpringSky
    DailyRecordColorTheme.BuffGlacier -> AppCopy.Settings.themeBuffGlacier
    DailyRecordColorTheme.DreamBlush -> AppCopy.Settings.themeDreamBlush
}

/**
 * App lock switch (ADR-023). The whole row toggles; the switch only shows the
 * state, and its thumb position and size carry it besides the color.
 */
@Composable
private fun AppLockSettingsRow(
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

/** Flat module color when on, a muted outline when off; no gradients (ADR-022). */
@Composable
private fun settingsSwitchColors(tint: Color) = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = tint,
    checkedBorderColor = tint,
    uncheckedThumbColor = DailyRecordTextMuted,
    uncheckedTrackColor = Color.Transparent,
    uncheckedBorderColor = DailyRecordTextMuted,
)

/**
 * Home screen disguise switch (ADR-024). On means the launcher shows the
 * neutral "Journal" identity; the whole row toggles, like the app lock row.
 */
@Composable
private fun LauncherDisguiseRow(
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
private fun AppLockDelayRow(
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
private fun AppLockDelaySelectionDialog(
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
