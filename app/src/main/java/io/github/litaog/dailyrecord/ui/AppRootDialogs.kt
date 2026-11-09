package io.github.litaog.dailyrecord.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.github.litaog.dailyrecord.core.account.LocalDataAfterAccountDeletion
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.statistics.EARLIEST_SUPPORTED_DATE
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.account.AccountDeletionDialog
import io.github.litaog.dailyrecord.ui.account.AccountDialog
import io.github.litaog.dailyrecord.ui.navigation.DateNavigationDialog
import io.github.litaog.dailyrecord.ui.navigation.DateNavigationSelection
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import java.time.LocalDate

@Composable
internal fun AppRootFeedbackEffects(
    syncStatus: SyncStatus,
    showAccountDialog: Boolean,
    showSettings: Boolean,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(syncStatus) {
        if (
            !showAccountDialog &&
            !showSettings &&
            syncStatus is SyncStatus.Failed &&
            syncStatus.networkRelated
        ) {
            snackbarHostState.showSnackbar(
                message = AppCopy.vpnSyncFailure,
                duration = SnackbarDuration.Short,
            )
        }
    }
    LaunchedEffect(showAccountDialog) {
        if (showAccountDialog) {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
}

@Composable
internal fun AppRootDialogs(
    showDatePicker: Boolean,
    browseDate: LocalDate,
    effectiveToday: LocalDate,
    colors: RecordModuleColorTokens,
    datePickerSelectionName: String,
    showAccountDialog: Boolean,
    accountEmail: String?,
    syncStatus: SyncStatus,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: suspend (String, LocalDataAfterAccountDeletion) -> Result<Unit>,
    onDismissAccountDialog: () -> Unit,
    onOpenAccountDeletion: () -> Unit,
    showAccountDeletion: Boolean,
    onDismissAccountDeletion: () -> Unit,
    onDatePickerDismiss: () -> Unit,
    onDatePickerDateSelected: (LocalDate) -> Unit,
) {
    val datePickerSelection = DateNavigationSelection.entries.firstOrNull {
        it.name == datePickerSelectionName
    } ?: DateNavigationSelection.Date
    if (showDatePicker) {
        DateNavigationDialog(
            initialDate = browseDate,
            earliestDate = EARLIEST_SUPPORTED_DATE,
            latestDate = effectiveToday,
            colors = colors,
            selection = datePickerSelection,
            onDismiss = onDatePickerDismiss,
            onDateSelected = onDatePickerDateSelected,
        )
    }

    if (showAccountDialog && accountEmail != null) {
        AccountDialog(
            email = accountEmail,
            status = syncStatus,
            onSyncNow = onSyncNow,
            onDeleteAccount = onOpenAccountDeletion,
            onSignOut = onSignOut,
            onDismiss = onDismissAccountDialog,
        )
    }
    if (showAccountDeletion && accountEmail != null) {
        AccountDeletionDialog(
            onDeleteAccount = onDeleteAccount,
            onDismiss = onDismissAccountDeletion,
        )
    }
}
