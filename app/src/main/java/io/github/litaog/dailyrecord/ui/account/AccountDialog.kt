package io.github.litaog.dailyrecord.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.components.DangerActionButton
import io.github.litaog.dailyrecord.ui.components.DailyRecordDialog
import io.github.litaog.dailyrecord.ui.components.DailyRecordTextAction
import io.github.litaog.dailyrecord.ui.components.OutlineActionButton
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDanger
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSuccess
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordWarning
import io.github.litaog.dailyrecord.ui.theme.SyncFailureGuidance

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
                        style = SyncFailureGuidance,
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

internal fun SyncStatus.shortLabel(): String = when (this) {
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
