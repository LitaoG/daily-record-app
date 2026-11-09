package io.github.litaog.dailyrecord.ui.account

import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.sync.SyncFailureKind

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
        guidance = AppCopy.Account.syncDialogMessage,
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
