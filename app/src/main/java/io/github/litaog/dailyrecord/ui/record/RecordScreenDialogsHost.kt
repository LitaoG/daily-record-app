package io.github.litaog.dailyrecord.ui.record

import androidx.compose.runtime.Composable

/**
 * Mounts the record screen's confirmation dialogs. The confirm/dismiss
 * closures stay at the call site because they mutate caller-owned draft
 * state; the wiring here mirrors the original inline blocks verbatim.
 */
@Composable
internal fun RecordScreenDialogsHost(
    showClearDialog: Boolean,
    clearDetailsOnly: Boolean,
    saving: Boolean,
    onClearDismiss: () -> Unit,
    onConfirmClearDetails: () -> Unit,
    onConfirmClearRecord: () -> Unit,
    showRemoveDetailDialog: Boolean,
    occurrenceCount: Int,
    onRemoveDetailDismiss: () -> Unit,
    onRemoveDetailConfirm: () -> Unit,
    showDiscardDialog: Boolean,
    onDiscardDismiss: () -> Unit,
    onDiscardConfirm: () -> Unit,
) {
    if (showClearDialog) {
        RecordClearConfirmDialog(
            detailsOnly = clearDetailsOnly,
            saving = saving,
            onDismiss = onClearDismiss,
            onConfirmDetailsOnly = onConfirmClearDetails,
            onConfirmRecord = onConfirmClearRecord,
        )
    }

    if (showRemoveDetailDialog) {
        RecordRemoveDetailConfirmDialog(
            occurrenceCount = occurrenceCount,
            onDismiss = onRemoveDetailDismiss,
            onConfirm = onRemoveDetailConfirm,
        )
    }

    if (showDiscardDialog) {
        RecordDiscardConfirmDialog(
            onDismiss = onDiscardDismiss,
            onConfirm = onDiscardConfirm,
        )
    }
}
