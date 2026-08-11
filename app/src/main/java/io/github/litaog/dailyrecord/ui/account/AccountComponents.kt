package io.github.litaog.dailyrecord.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.litaog.dailyrecord.R
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.sync.SyncFailureKind
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.components.DangerActionButton
import io.github.litaog.dailyrecord.ui.components.DailyRecordDialog
import io.github.litaog.dailyrecord.ui.components.DailyRecordTextAction
import io.github.litaog.dailyrecord.ui.components.OutlineActionButton
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccentSoft
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDanger
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSuccess
import io.github.litaog.dailyrecord.ui.theme.DailyRecordWarning
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlassBackground

internal const val VPN_SYNC_DIALOG_MESSAGE =
    AppCopy.Account.syncDialogMessage

internal enum class SyncFailureAction {
    Retry,
    Reauthenticate,
}

internal data class SyncFailurePresentation(
    val title: String,
    val guidance: String,
    val actionLabel: String,
    val action: SyncFailureAction,
)

internal fun SyncFailureKind.presentation(): SyncFailurePresentation = when (this) {
    SyncFailureKind.Network -> SyncFailurePresentation(
        title = AppCopy.Account.networkFailureTitle,
        guidance = VPN_SYNC_DIALOG_MESSAGE,
        actionLabel = AppCopy.Account.syncNow,
        action = SyncFailureAction.Retry,
    )
    SyncFailureKind.Authentication -> SyncFailurePresentation(
        title = AppCopy.Account.authFailureTitle,
        guidance = AppCopy.Account.authFailureGuidance,
        actionLabel = AppCopy.Account.reSignIn,
        action = SyncFailureAction.Reauthenticate,
    )
    SyncFailureKind.Permission -> SyncFailurePresentation(
        title = AppCopy.Account.permissionFailureTitle,
        guidance = AppCopy.Account.permissionFailureGuidance,
        actionLabel = AppCopy.Account.reSignIn,
        action = SyncFailureAction.Reauthenticate,
    )
    SyncFailureKind.Quota -> SyncFailurePresentation(
        title = AppCopy.Account.quotaFailureTitle,
        guidance = AppCopy.Account.quotaFailureGuidance,
        actionLabel = AppCopy.Account.syncNow,
        action = SyncFailureAction.Retry,
    )
    SyncFailureKind.Service -> SyncFailurePresentation(
        title = AppCopy.Account.serviceFailureTitle,
        guidance = AppCopy.Account.serviceFailureGuidance,
        actionLabel = AppCopy.Account.syncNow,
        action = SyncFailureAction.Retry,
    )
    SyncFailureKind.Data -> SyncFailurePresentation(
        title = AppCopy.Account.dataFailureTitle,
        guidance = AppCopy.Account.dataFailureGuidance,
        actionLabel = AppCopy.Account.syncNow,
        action = SyncFailureAction.Retry,
    )
    SyncFailureKind.Unknown -> SyncFailurePresentation(
        title = AppCopy.Account.unknownFailureTitle,
        guidance = AppCopy.Account.unknownFailureGuidance,
        actionLabel = AppCopy.Account.syncNow,
        action = SyncFailureAction.Retry,
    )
}

@Composable
internal fun AccountTopBar(
    status: SyncStatus,
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SyncStatusChip(
                        status = status,
                        onClick = onClick,
                        modifier = Modifier.weight(1f),
                    )
                    SettingsButton(onClick = onSettings)
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
                    SettingsButton(onClick = onSettings)
                }
            }
        }
    }
}

@Composable
internal fun LocalAccountTopBar(
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
                    SettingsButton(onClick = onSettings)
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
                    SettingsButton(onClick = onSettings)
                }
            }
        }
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(DailyRecordSizes.MinimumTouchTarget)
            .testTag("home_settings_button"),
    ) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = AppCopy.Settings.open,
            tint = DailyRecordTextSecondary,
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
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(DailyRecordDefaultAccentSoft)
            .border(1.dp, DailyRecordDivider, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
            .semantics {
                role = Role.Button
                contentDescription = AppCopy.Account.syncChipDescription(status.label())
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(8.dp)) {
            drawCircle(status.color())
        }
        Text(status.shortLabel(), color = DailyRecordText, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun AccountDialog(
    email: String,
    status: SyncStatus,
    onSyncNow: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    val failurePresentation = (status as? SyncStatus.Failed)?.kind?.presentation()
    DailyRecordDialog(
        title = if (confirmSignOut) AppCopy.Account.signOutConfirm else AppCopy.Account.accountAndSync,
        subtitle = if (confirmSignOut) AppCopy.Account.cloudDataRetained else AppCopy.Account.restoreOnAnotherDevice,
        testTag = "account_sync_dialog",
        onDismissRequest = onDismiss,
    ) {
        if (confirmSignOut) {
            Text(
                AppCopy.Account.signOutMessage,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 18.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlineActionButton(AppCopy.Account.back, { confirmSignOut = false }, Modifier.weight(1f))
                DangerActionButton(AppCopy.Account.confirmSignOut, onSignOut, Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(
                        DailyRecordSurfaceMuted,
                        androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    )
                    .border(
                        1.dp,
                        DailyRecordDivider,
                        androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(email, color = DailyRecordText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.foundation.Canvas(Modifier.size(9.dp)) { drawCircle(status.color()) }
                    Text(status.label(), color = DailyRecordTextSecondary, style = MaterialTheme.typography.labelLarge)
                }
                if (status is SyncStatus.Failed && failurePresentation != null) {
                    Text(
                        status.message,
                        color = DailyRecordTextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        failurePresentation.guidance,
                        color = DailyRecordDefaultAccent,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 9.33.sp,
                            lineHeight = 13.33.sp,
                        ),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .testTag(
                                if (status.networkRelated) {
                                    "account_vpn_sync_guidance"
                                } else {
                                    "account_sync_failure_guidance"
                                },
                            )
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            Text(
                AppCopy.Account.syncDescription,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 14.dp),
            )
            PrimaryActionButton(
                label = when {
                    status is SyncStatus.Syncing -> AppCopy.Account.syncing
                    failurePresentation != null -> failurePresentation.actionLabel
                    else -> AppCopy.Account.syncNow
                },
                onClick = if (failurePresentation?.action == SyncFailureAction.Reauthenticate) {
                    onSignOut
                } else {
                    onSyncNow
                },
                enabled = status !is SyncStatus.Syncing,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
            OutlineActionButton(AppCopy.Account.close, onDismiss, Modifier.fillMaxWidth().padding(top = 10.dp))
            DailyRecordTextAction(
                label = AppCopy.Account.signOut,
                onClick = { confirmSignOut = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = DailyRecordSizes.MinimumTouchTarget),
                danger = true,
            )
            DailyRecordTextAction(
                label = AppCopy.Account.deleteAccount,
                onClick = onDeleteAccount,
                modifier = Modifier.fillMaxWidth().heightIn(min = DailyRecordSizes.MinimumTouchTarget),
                danger = true,
            )
        }
    }
}

internal fun SyncStatus.label(): String = when (this) {
    SyncStatus.NotConfigured -> AppCopy.Account.notConfigured
    SyncStatus.Offline -> AppCopy.Account.offline
    SyncStatus.Syncing -> AppCopy.Account.syncing
    SyncStatus.UpToDate -> AppCopy.Account.synced
    is SyncStatus.Pending -> AppCopy.Account.pending(count)
    is SyncStatus.Failed -> kind.presentation().title
}

private fun SyncStatus.shortLabel(): String = when (this) {
    SyncStatus.NotConfigured -> AppCopy.Account.shortNotConfigured
    SyncStatus.Offline -> AppCopy.Account.shortOffline
    SyncStatus.Syncing -> AppCopy.Account.shortSyncing
    SyncStatus.UpToDate -> AppCopy.Account.shortSynced
    is SyncStatus.Pending -> AppCopy.Account.shortPending(count)
    is SyncStatus.Failed -> AppCopy.Account.shortRetry
}

internal fun SyncStatus.color() = when (this) {
    SyncStatus.UpToDate -> DailyRecordSuccess
    SyncStatus.Syncing -> DailyRecordDefaultAccent
    SyncStatus.Offline, is SyncStatus.Pending -> DailyRecordWarning
    SyncStatus.NotConfigured, is SyncStatus.Failed -> DailyRecordDanger
}

