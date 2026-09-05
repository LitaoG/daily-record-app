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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.account.color
import io.github.litaog.dailyrecord.ui.account.label
import io.github.litaog.dailyrecord.ui.components.BrandIcon
import io.github.litaog.dailyrecord.ui.components.BrandIconAsset
import io.github.litaog.dailyrecord.ui.components.brandIconTheme
import io.github.litaog.dailyrecord.ui.components.DailyRecordDialog
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
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

@Composable
internal fun SettingsScreen(
    versionName: String,
    accountEmail: String?,
    syncStatus: SyncStatus,
    moduleColors: RecordModuleColorTokens,
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onSignIn: (() -> Unit)?,
) {
    BackHandler(onBack = onBack)

    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
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
                    }
                }
                item {
                    SettingsSectionTitle(
                        text = AppCopy.Settings.dataSection,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                item {
                    SettingsCard(contentSpacing = 0.dp) {
                        SettingsInfoRow(
                            icon = BrandIconAsset.Lock,
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
            LanguageOptionRow(
                label = languageDisplayName(language),
                selected = language == currentLanguage,
                tint = tint,
                onClick = { onSelected(language) },
            )
        }
    }
}

@Composable
private fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("settings_language_option")
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
