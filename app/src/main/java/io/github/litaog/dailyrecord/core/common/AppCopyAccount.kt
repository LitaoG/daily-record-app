package io.github.litaog.dailyrecord.core.common

internal interface AppCopyAuth {
        val signIn: String get() = AppLanguageState.current.auth.signIn
        val register: String get() = AppLanguageState.current.auth.register
        val email: String get() = AppLanguageState.current.auth.email
        val password: String get() = AppLanguageState.current.auth.password
        val confirmPassword: String get() = AppLanguageState.current.auth.confirmPassword
        val show: String get() = AppLanguageState.current.auth.show
        val hide: String get() = AppLanguageState.current.auth.hide
        val showPassword: String get() = AppLanguageState.current.auth.showPassword
        val hidePassword: String get() = AppLanguageState.current.auth.hidePassword
        val forgotPassword: String get() = AppLanguageState.current.auth.forgotPassword
        val openPasswordReset: String get() = AppLanguageState.current.auth.openPasswordReset
        val title: String get() = AppLanguageState.current.auth.title
        val signInSubtitle: String get() = AppLanguageState.current.auth.signInSubtitle
        val registerSubtitle: String get() = AppLanguageState.current.auth.registerSubtitle
        val signInLocalSyncNotice: String get() = AppLanguageState.current.auth.signInLocalSyncNotice
        val registerLocalSyncNotice: String get() = AppLanguageState.current.auth.registerLocalSyncNotice
        val passwordPolicy: String get() = AppLanguageState.current.auth.passwordPolicy
        val signInVpnNotice: String get() = AppLanguageState.current.auth.signInVpnNotice
        val registerVpnNotice: String get() = AppLanguageState.current.auth.registerVpnNotice
        val emulatorNotice: String get() = AppLanguageState.current.auth.emulatorNotice
        val wait: String get() = AppLanguageState.current.auth.wait
        val signInAndRestore: String get() = AppLanguageState.current.auth.signInAndRestore
        val createAccount: String get() = AppLanguageState.current.auth.createAccount
        val continueOffline: String get() = AppLanguageState.current.auth.continueOffline
        val continueOfflineRegister: String get() = AppLanguageState.current.auth.continueOfflineRegister
        val emailRequired: String get() = AppLanguageState.current.auth.emailRequired
        val emailInvalid: String get() = AppLanguageState.current.auth.emailInvalid
        val passwordTooShort: String get() = AppLanguageState.current.auth.passwordTooShort
        val passwordMismatch: String get() = AppLanguageState.current.auth.passwordMismatch
        val network: String get() = AppLanguageState.current.auth.network
        val emailAlreadyRegistered: String get() = AppLanguageState.current.auth.emailAlreadyRegistered
        val weakPassword: String get() = AppLanguageState.current.auth.weakPassword
        val tooManyRequests: String get() = AppLanguageState.current.auth.tooManyRequests
        val invalidCredentials: String get() = AppLanguageState.current.auth.invalidCredentials
        val signInUnavailable: String get() = AppLanguageState.current.auth.signInUnavailable
        val registerUnavailable: String get() = AppLanguageState.current.auth.registerUnavailable
        val resetSentTitle: String get() = AppLanguageState.current.auth.resetSentTitle
        val resetTitle: String get() = AppLanguageState.current.auth.resetTitle
        val resetSentSubtitle: String get() = AppLanguageState.current.auth.resetSentSubtitle
        val resetSubtitle: String get() = AppLanguageState.current.auth.resetSubtitle
        val resetSuccess: String get() = AppLanguageState.current.auth.resetSuccess
        val backToSignIn: String get() = AppLanguageState.current.auth.backToSignIn
        val sendResetEmail: String get() = AppLanguageState.current.auth.sendResetEmail
        val sendingResetEmail: String get() = AppLanguageState.current.auth.sendingResetEmail
        val cancel: String get() = AppLanguageState.current.auth.cancel
        val resetNetwork: String get() = AppLanguageState.current.auth.resetNetwork
        val resetTooManyRequests: String get() = AppLanguageState.current.auth.resetTooManyRequests
        val resetQuotaExceeded: String get() = AppLanguageState.current.auth.resetQuotaExceeded
        val resetUnavailable: String get() = AppLanguageState.current.auth.resetUnavailable
    }

internal interface AppCopyAccount {
        val signInSync: String get() = AppLanguageState.current.account.signInSync
        val reSignIn: String get() = AppLanguageState.current.account.reSignIn
        val signInSyncAccessibility: String get() = AppLanguageState.current.account.signInSyncAccessibility
        val accountAndSync: String get() = AppLanguageState.current.account.accountAndSync
        val signOutConfirm: String get() = AppLanguageState.current.account.signOutConfirm
        val cloudDataRetained: String get() = AppLanguageState.current.account.cloudDataRetained
        val restoreOnAnotherDevice: String get() = AppLanguageState.current.account.restoreOnAnotherDevice
        val signOutMessage: String get() = AppLanguageState.current.account.signOutMessage
        val back: String get() = AppLanguageState.current.account.back
        val confirmSignOut: String get() = AppLanguageState.current.account.confirmSignOut
        val syncDescription: String get() = AppLanguageState.current.account.syncDescription
        val syncing: String get() = AppLanguageState.current.account.syncing
        val syncNow: String get() = AppLanguageState.current.account.syncNow
        val close: String get() = AppLanguageState.current.account.close
        val signOut: String get() = AppLanguageState.current.account.signOut
        val deleteAccount: String get() = AppLanguageState.current.account.deleteAccount
        val notConfigured: String get() = AppLanguageState.current.account.notConfigured
        val offline: String get() = AppLanguageState.current.account.offline
        val synced: String get() = AppLanguageState.current.account.synced
        val shortNotConfigured: String get() = AppLanguageState.current.account.shortNotConfigured
        val shortOffline: String get() = AppLanguageState.current.account.shortOffline
        val shortSyncing: String get() = AppLanguageState.current.account.shortSyncing
        val shortSynced: String get() = AppLanguageState.current.account.shortSynced
        val shortRetry: String get() = AppLanguageState.current.account.shortRetry
        val networkFailureTitle: String get() = AppLanguageState.current.account.networkFailureTitle
        val networkFailureGuidance: String get() = AppLanguageState.current.account.networkFailureGuidance
        val authFailureTitle: String get() = AppLanguageState.current.account.authFailureTitle
        val authFailureGuidance: String get() = AppLanguageState.current.account.authFailureGuidance
        val permissionFailureTitle: String get() = AppLanguageState.current.account.permissionFailureTitle
        val permissionFailureGuidance: String get() = AppLanguageState.current.account.permissionFailureGuidance
        val quotaFailureTitle: String get() = AppLanguageState.current.account.quotaFailureTitle
        val quotaFailureGuidance: String get() = AppLanguageState.current.account.quotaFailureGuidance
        val serviceFailureTitle: String get() = AppLanguageState.current.account.serviceFailureTitle
        val serviceFailureGuidance: String get() = AppLanguageState.current.account.serviceFailureGuidance
        val dataFailureTitle: String get() = AppLanguageState.current.account.dataFailureTitle
        val dataFailureGuidance: String get() = AppLanguageState.current.account.dataFailureGuidance
        val unknownFailureTitle: String get() = AppLanguageState.current.account.unknownFailureTitle
        val unknownFailureGuidance: String get() = AppLanguageState.current.account.unknownFailureGuidance
        val syncDialogMessage: String get() = AppLanguageState.current.account.syncDialogMessage
        val dataFormatFailure: String get() = AppLanguageState.current.account.dataFormatFailure
        val networkFailure: String get() = AppLanguageState.current.account.networkFailure
        val authFailure: String get() = AppLanguageState.current.account.authFailure
        val permissionFailure: String get() = AppLanguageState.current.account.permissionFailure
        val quotaFailure: String get() = AppLanguageState.current.account.quotaFailure
        val serviceFailure: String get() = AppLanguageState.current.account.serviceFailure
        val dataFailure: String get() = AppLanguageState.current.account.dataFailure
        val unknownFailure: String get() = AppLanguageState.current.account.unknownFailure

        fun timeoutFailure(timeoutMillis: Long): String =
            AppLanguageState.current.account.timeoutFailure(timeoutMillis)

        fun pending(count: Int): String = AppLanguageState.current.account.pending(count)

        fun shortPending(count: Int): String = AppLanguageState.current.account.shortPending(count)

        fun syncChipDescription(status: String): String =
            AppLanguageState.current.account.syncChipDescription(status)
    }

internal interface AppCopyDeletion {
        val warningTitle: String get() = AppLanguageState.current.deletion.warningTitle
        val confirmationTitle: String get() = AppLanguageState.current.deletion.confirmationTitle
        val irreversible: String get() = AppLanguageState.current.deletion.irreversible
        val verifyPassword: String get() = AppLanguageState.current.deletion.verifyPassword
        val warningMessage: String get() = AppLanguageState.current.deletion.warningMessage
        val localChoice: String get() = AppLanguageState.current.deletion.localChoice
        val keepLocalTitle: String get() = AppLanguageState.current.deletion.keepLocalTitle
        val keepLocalDescription: String get() = AppLanguageState.current.deletion.keepLocalDescription
        val deleteLocalTitle: String get() = AppLanguageState.current.deletion.deleteLocalTitle
        val deleteLocalDescription: String get() = AppLanguageState.current.deletion.deleteLocalDescription
        val continueVerification: String get() = AppLanguageState.current.deletion.continueVerification
        val currentPassword: String get() = AppLanguageState.current.deletion.currentPassword
        val deleting: String get() = AppLanguageState.current.deletion.deleting
        val deletePermanently: String get() = AppLanguageState.current.deletion.deletePermanently
        val confirmationKeepLocal: String get() = AppLanguageState.current.deletion.confirmationKeepLocal
        val confirmationDeleteLocal: String get() = AppLanguageState.current.deletion.confirmationDeleteLocal
        val networkError: String get() = AppLanguageState.current.deletion.networkError
        val networkAuthError: String get() = AppLanguageState.current.deletion.networkAuthError
        val authError: String get() = AppLanguageState.current.deletion.authError
        val permissionError: String get() = AppLanguageState.current.deletion.permissionError
        val serviceError: String get() = AppLanguageState.current.deletion.serviceError
        val unknownError: String get() = AppLanguageState.current.deletion.unknownError
        val localCleanupPending: String get() = AppLanguageState.current.deletion.localCleanupPending
        val authDeletionPending: String get() = AppLanguageState.current.deletion.authDeletionPending
        val retryRecovery: String get() = AppLanguageState.current.deletion.retryRecovery
        val recoveryConflict: String get() = AppLanguageState.current.deletion.recoveryConflict
        val recoveryRetryGuidance: String get() = AppLanguageState.current.deletion.recoveryRetryGuidance
        val replaceLocalAndRestore: String get() = AppLanguageState.current.deletion.replaceLocalAndRestore
        val replaceLocalAndRestoreTitle: String get() = AppLanguageState.current.deletion.replaceLocalAndRestoreTitle
        val replaceLocalAndRestoreMessage: String get() = AppLanguageState.current.deletion.replaceLocalAndRestoreMessage
        val cancelRecoveryReplacement: String get() = AppLanguageState.current.deletion.cancelRecoveryReplacement
        val wrongPassword: String get() = AppLanguageState.current.deletion.wrongPassword
        val tooManyAttempts: String get() = AppLanguageState.current.deletion.tooManyAttempts
        val localRecoveryPending: String get() = AppLanguageState.current.deletion.localRecoveryPending

        fun selectionDescription(title: String, isSelected: Boolean): String =
            AppLanguageState.current.deletion.selectionDescription(title, isSelected)
    }
