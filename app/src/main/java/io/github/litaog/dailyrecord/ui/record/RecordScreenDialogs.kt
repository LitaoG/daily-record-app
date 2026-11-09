package io.github.litaog.dailyrecord.ui.record

import androidx.compose.runtime.Composable
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.components.DailyRecordConfirmationDialog

@Composable
internal fun RecordClearConfirmDialog(
    detailsOnly: Boolean,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirmDetailsOnly: () -> Unit,
    onConfirmRecord: () -> Unit,
) {
    val clearDetailsOnly = detailsOnly
    DailyRecordConfirmationDialog(
        title = if (clearDetailsOnly) {
            AppCopy.Record.clearDetailsTitle
        } else {
            AppCopy.Record.clearTitle
        },
        subtitle = if (clearDetailsOnly) {
            AppCopy.Record.clearDetailsSubtitle
        } else {
            AppCopy.Record.clearSubtitle
        },
        message = if (clearDetailsOnly) {
            AppCopy.Record.clearDetailsMessage
        } else {
            AppCopy.Record.clearMessage
        },
        cancelLabel = AppCopy.Auth.cancel,
        confirmLabel = if (clearDetailsOnly) {
            AppCopy.Record.confirmClearDetails
        } else {
            AppCopy.Record.confirmClear
        },
        testTag = "clear_record_dialog",
        confirmEnabled = !saving,
        onDismiss = onDismiss,
        onConfirm = {
            if (!saving) {
                onDismiss()
                if (clearDetailsOnly) {
                    onConfirmDetailsOnly()
                } else {
                    onConfirmRecord()
                }
            }
        },
    )
}

@Composable
internal fun RecordRemoveDetailConfirmDialog(
    occurrenceCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    DailyRecordConfirmationDialog(
        title = AppCopy.Record.detailDiscardTitle,
        subtitle = AppCopy.Record.detailOccurrence(occurrenceCount),
        message = AppCopy.Record.detailDiscardMessage,
        cancelLabel = AppCopy.Auth.cancel,
        confirmLabel = AppCopy.Record.detailConfirmRemove,
        testTag = "remove_detail_dialog",
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
internal fun RecordDiscardConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    DailyRecordConfirmationDialog(
        title = AppCopy.Record.discardTitle,
        subtitle = AppCopy.Record.unsavedSubtitle,
        message = AppCopy.Record.discardMessage,
        cancelLabel = AppCopy.Record.continueEditing,
        confirmLabel = AppCopy.Record.discard,
        testTag = "discard_record_dialog",
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}
