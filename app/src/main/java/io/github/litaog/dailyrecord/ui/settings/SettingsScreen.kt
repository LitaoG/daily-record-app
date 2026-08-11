package io.github.litaog.dailyrecord.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.account.color
import io.github.litaog.dailyrecord.ui.account.label
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
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onSignIn: (() -> Unit)?,
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dailyRecordBackdropBrush(moduleColors)),
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_screen"),
            containerColor = Color.Transparent,
            topBar = {
                SettingsTopBar(onBack = onBack)
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
                        text = AppCopy.Settings.dataSection,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                item {
                    SettingsCard(contentSpacing = 0.dp) {
                        SettingsInfoRow(
                            icon = Icons.Outlined.Lock,
                            title = AppCopy.Settings.localFirstTitle,
                            summary = AppCopy.Settings.localFirstSummary,
                            tint = moduleColors.primary,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = DailyRecordDivider,
                        )
                        SettingsInfoRow(
                            icon = Icons.Outlined.Shield,
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
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
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
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = AppCopy.Settings.back,
                    tint = DailyRecordText,
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
                imageVector = Icons.Outlined.CloudSync,
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
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = DailyRecordTextMuted,
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
    icon: ImageVector,
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
        SettingsIconBadge(imageVector = icon, tint = tint)
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
    imageVector: ImageVector,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(DailyRecordShapes.Compact)
            .background(tint.copy(alpha = .10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = tint,
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
                contentDescription = "$label，$value"
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

