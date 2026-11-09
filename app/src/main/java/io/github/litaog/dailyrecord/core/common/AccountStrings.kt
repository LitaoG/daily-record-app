package io.github.litaog.dailyrecord.core.common

internal interface AccountStrings {
        val signInSync: String
        val reSignIn: String
        val signInSyncAccessibility: String
        val accountAndSync: String
        val signOutConfirm: String
        val cloudDataRetained: String
        val restoreOnAnotherDevice: String
        val signOutMessage: String
        val back: String
        val confirmSignOut: String
        val syncDescription: String
        val syncing: String
        val syncNow: String
        val close: String
        val signOut: String
        val deleteAccount: String
        val notConfigured: String
        val offline: String
        val synced: String
        val shortNotConfigured: String
        val shortOffline: String
        val shortSyncing: String
        val shortSynced: String
        val shortRetry: String
        val networkFailureTitle: String
        val networkFailureGuidance: String
        val authFailureTitle: String
        val authFailureGuidance: String
        val permissionFailureTitle: String
        val permissionFailureGuidance: String
        val quotaFailureTitle: String
        val quotaFailureGuidance: String
        val serviceFailureTitle: String
        val serviceFailureGuidance: String
        val dataFailureTitle: String
        val dataFailureGuidance: String
        val unknownFailureTitle: String
        val unknownFailureGuidance: String
        val syncDialogMessage: String
        val dataFormatFailure: String
        val networkFailure: String
        val authFailure: String
        val permissionFailure: String
        val quotaFailure: String
        val serviceFailure: String
        val dataFailure: String
        val unknownFailure: String

        fun timeoutFailure(timeoutMillis: Long): String

        fun pending(count: Int): String

        fun shortPending(count: Int): String

        fun syncChipDescription(status: String): String
    }

internal interface AuthStrings {
        val signIn: String
        val register: String
        val email: String
        val password: String
        val confirmPassword: String
        val show: String
        val hide: String
        val showPassword: String
        val hidePassword: String
        val forgotPassword: String
        val openPasswordReset: String
        val title: String
        val signInSubtitle: String
        val registerSubtitle: String
        val signInLocalSyncNotice: String
        val registerLocalSyncNotice: String
        val passwordPolicy: String
        val signInVpnNotice: String
        val registerVpnNotice: String
        val emulatorNotice: String
        val wait: String
        val signInAndRestore: String
        val createAccount: String
        val continueOffline: String
        val continueOfflineRegister: String
        val emailRequired: String
        val emailInvalid: String
        val passwordTooShort: String
        val passwordMismatch: String
        val network: String
        val emailAlreadyRegistered: String
        val weakPassword: String
        val tooManyRequests: String
        val invalidCredentials: String
        val signInUnavailable: String
        val registerUnavailable: String
        val resetSentTitle: String
        val resetTitle: String
        val resetSentSubtitle: String
        val resetSubtitle: String
        val resetSuccess: String
        val backToSignIn: String
        val sendResetEmail: String
        val sendingResetEmail: String
        val cancel: String
        val resetNetwork: String
        val resetTooManyRequests: String
        val resetQuotaExceeded: String
        val resetUnavailable: String
    }

internal interface DeletionStrings {
        val warningTitle: String
        val confirmationTitle: String
        val irreversible: String
        val verifyPassword: String
        val warningMessage: String
        val localChoice: String
        val keepLocalTitle: String
        val keepLocalDescription: String
        val deleteLocalTitle: String
        val deleteLocalDescription: String
        val continueVerification: String
        val currentPassword: String
        val deleting: String
        val deletePermanently: String
        val confirmationKeepLocal: String
        val confirmationDeleteLocal: String
        val networkError: String
        val networkAuthError: String
        val authError: String
        val permissionError: String
        val serviceError: String
        val unknownError: String
        val localCleanupPending: String
        val authDeletionPending: String
        val retryRecovery: String
        val recoveryConflict: String
        val recoveryRetryGuidance: String
        val replaceLocalAndRestore: String
        val replaceLocalAndRestoreTitle: String
        val replaceLocalAndRestoreMessage: String
        val cancelRecoveryReplacement: String
        val wrongPassword: String
        val tooManyAttempts: String
        val localRecoveryPending: String

        fun selectionDescription(title: String, isSelected: Boolean): String
    }
