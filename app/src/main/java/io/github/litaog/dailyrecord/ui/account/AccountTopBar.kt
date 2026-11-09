package io.github.litaog.dailyrecord.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.R
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.components.DailyRecordHaptics
import io.github.litaog.dailyrecord.ui.components.DailyRecordTextAction
import io.github.litaog.dailyrecord.ui.components.PrivateSettingsIcon
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.LiquidGlassElevation
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.appleLiquidGlass
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlassBackground

@Composable
internal fun AccountTopBar(
    status: SyncStatus,
    colors: RecordModuleColorTokens,
    onClick: () -> Unit,
    onSettings: () -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.4f
    Surface(
        modifier = Modifier.dailyRecordGlassBackground(level = DailyRecordGlassLevel.Muted),
        color = androidx.compose.ui.graphics.Color.Transparent,
        shadowElevation = 0.dp,
    ) {
        if (largeText) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AccountTitle(AppCopy.privateRecordSubtitle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    SyncStatusChip(
                        status = status,
                        onClick = onClick,
                    )
                    SettingsButton(onClick = onSettings, moduleTint = colors.primary)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .heightIn(min = 58.dp)
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AccountTitle(
                    subtitle = AppCopy.privateRecordSubtitle,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SyncStatusChip(status = status, onClick = onClick)
                    SettingsButton(onClick = onSettings, moduleTint = colors.primary)
                }
            }
        }
    }
}

@Composable
internal fun LocalAccountTopBar(
    colors: RecordModuleColorTokens,
    onSignIn: (() -> Unit)?,
    onSettings: () -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.4f
    Surface(
        modifier = Modifier.dailyRecordGlassBackground(level = DailyRecordGlassLevel.Muted),
        color = androidx.compose.ui.graphics.Color.Transparent,
        shadowElevation = 0.dp,
    ) {
        if (largeText) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AccountTitle(AppCopy.offlineSubtitle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onSignIn != null) {
                        DailyRecordTextAction(
                            label = AppCopy.Account.signInSync,
                            onClick = onSignIn,
                            accessibilityLabel = AppCopy.Account.signInSyncAccessibility,
                        )
                    }
                    SettingsButton(onClick = onSettings, moduleTint = colors.primary)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .heightIn(min = 58.dp)
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AccountTitle(
                    subtitle = AppCopy.offlineSubtitle,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (onSignIn != null) {
                        DailyRecordTextAction(
                            label = AppCopy.Account.signInSync,
                            onClick = onSignIn,
                            accessibilityLabel = AppCopy.Account.signInSyncAccessibility,
                        )
                    }
                    SettingsButton(onClick = onSettings, moduleTint = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun SettingsButton(
    onClick: () -> Unit,
    moduleTint: Color,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(DailyRecordSizes.SettingsButton)
            .testTag("home_settings_button"),
    ) {
        PrivateSettingsIcon(
            contentDescription = AppCopy.Settings.open,
            moduleTint = moduleTint,
        )
    }
}

@Composable
private fun AccountTitle(
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            stringResource(R.string.app_name),
            color = DailyRecordText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(subtitle, color = DailyRecordTextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SyncStatusChip(
    status: SyncStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    // The interactive layer and the visual pill are separate nodes: touch and
    // TalkBack get the 48dp contract target while the visible pill keeps the
    // 38dp height (#261, #278, #287). Painting the pill on the expanded layer
    // instead was the visual bloat #278 fixed.
    Box(
        modifier = modifier
            .testTag("sync_status_chip")
            .clickable(role = Role.Button) {
                DailyRecordHaptics.tick(haptic)
                onClick()
            }
            .minimumInteractiveComponentSize()
            .semantics {
                role = Role.Button
                contentDescription = AppCopy.Account.syncChipDescription(status.label())
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .testTag("sync_status_chip_pill")
                .height(DailyRecordSizes.SyncChipHeight)
                .appleLiquidGlass(
                    shape = DailyRecordShapes.Full,
                    glowColor = status.color(),
                    elevation = LiquidGlassElevation.Subtle,
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        ) {
            androidx.compose.foundation.Canvas(Modifier.size(8.dp)) {
                drawCircle(status.color())
            }
            Text(status.shortLabel(), color = DailyRecordText, style = MaterialTheme.typography.labelMedium)
        }
    }
}
