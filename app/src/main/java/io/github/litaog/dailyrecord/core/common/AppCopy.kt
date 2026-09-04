package io.github.litaog.dailyrecord.core.common

import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * Single source for user-facing copy used by both the UI and sync layer.
 *
 * The facade delegates every member to the active [AppStrings] implementation
 * selected by [AppLanguageState.current]; call sites keep their historical
 * shape, so switching languages never touches individual Composables.
 */
internal object AppCopy {
    val privateRecordSubtitle: String get() = AppLanguageState.current.privateRecordSubtitle
    val offlineSubtitle: String get() = AppLanguageState.current.offlineSubtitle
    val vpnSyncFailure: String get() = AppLanguageState.current.vpnSyncFailure
    val readingLocalRecords: String get() = AppLanguageState.current.readingLocalRecords
    val selected: String get() = AppLanguageState.current.selected
    val unselected: String get() = AppLanguageState.current.unselected
    val today: String get() = AppLanguageState.current.today
    val historyDate: String get() = AppLanguageState.current.historyDate
    val futureDate: String get() = AppLanguageState.current.futureDate

    fun weekdayName(dayOfWeek: Int): String = AppLanguageState.current.weekdayName(dayOfWeek)

    fun selectedState(label: String, isSelected: Boolean): String =
        AppLanguageState.current.selectedState(label, isSelected)

    object Auth {
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

    object Account {
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

    object RecordModule {
        val handBrewLabel: String get() = AppLanguageState.current.recordModule.handBrewLabel
        val handBrewAccessibilityLabel: String get() =
            AppLanguageState.current.recordModule.handBrewAccessibilityLabel
        val handBrewQuestionToday: String get() = AppLanguageState.current.recordModule.handBrewQuestionToday
        val handBrewQuestionPast: String get() = AppLanguageState.current.recordModule.handBrewQuestionPast
        val handBrewZero: String get() = AppLanguageState.current.recordModule.handBrewZero
        val sexLabel: String get() = AppLanguageState.current.recordModule.sexLabel
        val sexAccessibilityLabel: String get() =
            AppLanguageState.current.recordModule.sexAccessibilityLabel
        val sexQuestionToday: String get() = AppLanguageState.current.recordModule.sexQuestionToday
        val sexQuestionPast: String get() = AppLanguageState.current.recordModule.sexQuestionPast
        val sexZero: String get() = AppLanguageState.current.recordModule.sexZero

        fun recordLabel(label: String): String =
            AppLanguageState.current.recordModule.recordLabel(label)
    }

    object Deletion {
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

    object Calendar {
        val weekdays: List<String> get() = AppLanguageState.current.calendar.weekdays
        val recordHint: String get() = AppLanguageState.current.calendar.recordHint
        val unset: String get() = AppLanguageState.current.calendar.unset
        val future: String get() = AppLanguageState.current.calendar.future
        val zero: String get() = AppLanguageState.current.calendar.zero
        val recorded: String get() = AppLanguageState.current.calendar.recorded
        val previousMonth: String get() = AppLanguageState.current.calendar.previousMonth
        val nextMonth: String get() = AppLanguageState.current.calendar.nextMonth
        val selectDate: String get() = AppLanguageState.current.calendar.selectDate
        val backToToday: String get() = AppLanguageState.current.calendar.backToToday
        val legendDescription: String get() = AppLanguageState.current.calendar.legendDescription
        val unavailable: String get() = AppLanguageState.current.calendar.unavailable
        val oneTime: String get() = AppLanguageState.current.calendar.oneTime
        val twoTimes: String get() = AppLanguageState.current.calendar.twoTimes
        val ninePlusTimes: String get() = AppLanguageState.current.calendar.ninePlusTimes
        val todayShort: String get() = AppLanguageState.current.calendar.todayShort
        val futureDescription: String get() = AppLanguageState.current.calendar.futureDescription
        val unsetDescription: String get() = AppLanguageState.current.calendar.unsetDescription
        val zeroDescription: String get() = AppLanguageState.current.calendar.zeroDescription
        val selectedSuffix: String get() = AppLanguageState.current.calendar.selectedSuffix
        val todaySuffix: String get() = AppLanguageState.current.calendar.todaySuffix

        fun monthSummary(count: Long, days: Int): String =
            AppLanguageState.current.calendar.monthSummary(count, days)

        fun monthTitle(month: YearMonth): String =
            AppLanguageState.current.calendar.monthTitle(month)

        fun monthTitleMultiline(month: YearMonth): String =
            AppLanguageState.current.calendar.monthTitleMultiline(month)

        fun monthSelectionDescription(month: YearMonth): String =
            AppLanguageState.current.calendar.monthSelectionDescription(month)

        fun monthDateDescription(date: LocalDate, state: String, focused: Boolean): String =
            AppLanguageState.current.calendar.monthDateDescription(date, state, focused)

        fun legendDescription(moduleLabel: String): String =
            AppLanguageState.current.calendar.legendDescription(moduleLabel)

        fun countDescription(count: Int): String =
            AppLanguageState.current.calendar.countDescription(count)

        fun statusDescription(
            date: LocalDate,
            today: LocalDate,
            unsupported: Boolean,
            future: Boolean,
            count: Int?,
            moduleLabel: String,
        ): String = AppLanguageState.current.calendar.statusDescription(
            date, today, unsupported, future, count, moduleLabel,
        )
    }

    object Navigation {
        val weekdays: List<String> get() = AppLanguageState.current.navigation.weekdays
        val title: String get() = AppLanguageState.current.navigation.title
        val subtitle: String get() = AppLanguageState.current.navigation.subtitle
        val dateWheelSubtitle: String get() = AppLanguageState.current.navigation.dateWheelSubtitle
        val monthSubtitle: String get() = AppLanguageState.current.navigation.monthSubtitle
        val jumpToDate: String get() = AppLanguageState.current.navigation.jumpToDate
        val jumpToMonth: String get() = AppLanguageState.current.navigation.jumpToMonth
        val jumpToYear: String get() = AppLanguageState.current.navigation.jumpToYear
        val selected: String get() = AppLanguageState.current.navigation.selected
        val switchYear: String get() = AppLanguageState.current.navigation.switchYear
        val returnToDatePicker: String get() = AppLanguageState.current.navigation.returnToDatePicker
        val selectYear: String get() = AppLanguageState.current.navigation.selectYear
        val selectMonth: String get() = AppLanguageState.current.navigation.selectMonth
        val dateWheelHint: String get() = AppLanguageState.current.navigation.dateWheelHint
        val yearUnit: String get() = AppLanguageState.current.navigation.yearUnit
        val monthUnit: String get() = AppLanguageState.current.navigation.monthUnit
        val dayUnit: String get() = AppLanguageState.current.navigation.dayUnit

        fun switchYearDescription(year: Int): String =
            AppLanguageState.current.navigation.switchYearDescription(year)

        fun nextYearDescription(forward: Boolean): String =
            AppLanguageState.current.navigation.nextYearDescription(forward)

        fun dateText(date: LocalDate): String =
            AppLanguageState.current.navigation.dateText(date)

        fun dateLabel(date: LocalDate, weekday: String): String =
            AppLanguageState.current.navigation.dateLabel(date, weekday)

        fun selectYearDescription(year: Int): String =
            AppLanguageState.current.navigation.selectYearDescription(year)

        fun yearTitle(year: Int): String = AppLanguageState.current.navigation.yearTitle(year)

        fun monthTitle(month: YearMonth): String =
            AppLanguageState.current.navigation.monthTitle(month)

        fun monthLabel(month: Int): String = AppLanguageState.current.navigation.monthLabel(month)

        fun dayLabel(day: Int): String = AppLanguageState.current.navigation.dayLabel(day)

        fun monthDescription(month: YearMonth): String =
            AppLanguageState.current.navigation.monthDescription(month)
    }

    object Record {
        val loading: String get() = AppLanguageState.current.record.loading
        val saving: String get() = AppLanguageState.current.record.saving
        val saved: String get() = AppLanguageState.current.record.saved
        val save: String get() = AppLanguageState.current.record.save
        val saveFailure: String get() = AppLanguageState.current.record.saveFailure
        val clear: String get() = AppLanguageState.current.record.clear
        val clearFailure: String get() = AppLanguageState.current.record.clearFailure
        val countOnly: String get() = AppLanguageState.current.record.countOnly
        val countFirst: String get() = AppLanguageState.current.record.countFirst
        val countAndDetails: String get() = AppLanguageState.current.record.countAndDetails
        val detailEntry: String get() = AppLanguageState.current.record.detailEntry
        val detailEntryHintFormat: String get() = AppLanguageState.current.record.detailEntryHintFormat
        val detailSectionTitle: String get() = AppLanguageState.current.record.detailSectionTitle
        val detailSectionHint: String get() = AppLanguageState.current.record.detailSectionHint
        val detailCollapse: String get() = AppLanguageState.current.record.detailCollapse
        val detailExpand: String get() = AppLanguageState.current.record.detailExpand
        val detailOccurrenceFormat: String get() = AppLanguageState.current.record.detailOccurrenceFormat
        val detailStartTime: String get() = AppLanguageState.current.record.detailStartTime
        val detailEndTime: String get() = AppLanguageState.current.record.detailEndTime
        val detailStartTimeUnset: String get() = AppLanguageState.current.record.detailStartTimeUnset
        val detailEndTimeUnset: String get() = AppLanguageState.current.record.detailEndTimeUnset
        val detailTimeUnset: String get() = AppLanguageState.current.record.detailTimeUnset
        val detailTimePickerTitle: String get() = AppLanguageState.current.record.detailTimePickerTitle
        val detailTimePickerSubtitle: String get() = AppLanguageState.current.record.detailTimePickerSubtitle
        val detailTimePickerHour: String get() = AppLanguageState.current.record.detailTimePickerHour
        val detailTimePickerMinute: String get() = AppLanguageState.current.record.detailTimePickerMinute
        val detailTimePickerHint: String get() = AppLanguageState.current.record.detailTimePickerHint
        val detailTimePickerConfirm: String get() = AppLanguageState.current.record.detailTimePickerConfirm
        val detailWriteFeeling: String get() = AppLanguageState.current.record.detailWriteFeeling
        val detailCollapseFeeling: String get() = AppLanguageState.current.record.detailCollapseFeeling
        val detailFeelingLabel: String get() = AppLanguageState.current.record.detailFeelingLabel
        val detailFeelingHint: String get() = AppLanguageState.current.record.detailFeelingHint
        val detailFeelingCounter: String get() = AppLanguageState.current.record.detailFeelingCounter
        val detailEndBeforeStart: String get() = AppLanguageState.current.record.detailEndBeforeStart
        val detailDiscardTitle: String get() = AppLanguageState.current.record.detailDiscardTitle
        val detailDiscardMessage: String get() = AppLanguageState.current.record.detailDiscardMessage
        val detailConfirmRemove: String get() = AppLanguageState.current.record.detailConfirmRemove
        val detailEntryUnavailable: String get() = AppLanguageState.current.record.detailEntryUnavailable
        val loadingRecords: String get() = AppLanguageState.current.record.loadingRecords
        val futureUnavailable: String get() = AppLanguageState.current.record.futureUnavailable
        val notSaved: String get() = AppLanguageState.current.record.notSaved
        val zeroRecorded: String get() = AppLanguageState.current.record.zeroRecorded
        val clearTitle: String get() = AppLanguageState.current.record.clearTitle
        val clearSubtitle: String get() = AppLanguageState.current.record.clearSubtitle
        val clearMessage: String get() = AppLanguageState.current.record.clearMessage
        val confirmClear: String get() = AppLanguageState.current.record.confirmClear
        val clearDetailsFailure: String get() = AppLanguageState.current.record.clearDetailsFailure
        val clearDetailsTitle: String get() = AppLanguageState.current.record.clearDetailsTitle
        val clearDetailsSubtitle: String get() = AppLanguageState.current.record.clearDetailsSubtitle
        val clearDetailsMessage: String get() = AppLanguageState.current.record.clearDetailsMessage
        val confirmClearDetails: String get() = AppLanguageState.current.record.confirmClearDetails
        val discardTitle: String get() = AppLanguageState.current.record.discardTitle
        val unsavedSubtitle: String get() = AppLanguageState.current.record.unsavedSubtitle
        val discardMessage: String get() = AppLanguageState.current.record.discardMessage
        val continueEditing: String get() = AppLanguageState.current.record.continueEditing
        val discard: String get() = AppLanguageState.current.record.discard
        val backToCalendar: String get() = AppLanguageState.current.record.backToCalendar

        fun savedStatus(count: Int): String = AppLanguageState.current.record.savedStatus(count)

        fun recordedStatus(count: Int): String = AppLanguageState.current.record.recordedStatus(count)

        fun explicitZeroHint(text: String): String =
            AppLanguageState.current.record.explicitZeroHint(text)

        fun monthSaved(month: Int): String = AppLanguageState.current.record.monthSaved(month)

        fun monthSummary(count: Long, days: Int): String =
            AppLanguageState.current.record.monthSummary(count, days)

        fun moduleRecordLabel(moduleLabel: String): String =
            AppLanguageState.current.record.moduleRecordLabel(moduleLabel)

        fun dateLabel(date: LocalDate, weekday: String): String =
            AppLanguageState.current.record.dateLabel(date, weekday)

        fun detailEntryHint(count: Int): String = AppLanguageState.current.record.detailEntryHint(count)

        fun detailOccurrence(index: Int): String = AppLanguageState.current.record.detailOccurrence(index)

        fun detailFeelingCounter(count: Int): String =
            AppLanguageState.current.record.detailFeelingCounter(count)

        fun detailTimeDescription(occurrence: Int, label: String, value: String): String =
            AppLanguageState.current.record.detailTimeDescription(occurrence, label, value)

        fun detailTimeWheelCurrent(unit: String, value: String): String =
            AppLanguageState.current.record.detailTimeWheelCurrent(unit, value)

        fun detailTimeWheelOption(unit: String, value: String): String =
            AppLanguageState.current.record.detailTimeWheelOption(unit, value)

        fun detailFeelingActionDescription(occurrence: Int, action: String): String =
            AppLanguageState.current.record.detailFeelingActionDescription(occurrence, action)

        fun detailFeelingEditorDescription(occurrence: Int): String =
            AppLanguageState.current.record.detailFeelingEditorDescription(occurrence)
    }

    object Statistics {
        val weekdays: List<String> get() = AppLanguageState.current.statistics.weekdays
        val title: String get() = AppLanguageState.current.statistics.title
        val countAndDays: String get() = AppLanguageState.current.statistics.countAndDays
        val countUnit: String get() = AppLanguageState.current.statistics.countUnit
        val dayUnit: String get() = AppLanguageState.current.statistics.dayUnit
        val perDayUnit: String get() = AppLanguageState.current.statistics.perDayUnit
        val recordedDaysLabel: String get() = AppLanguageState.current.statistics.recordedDaysLabel
        val averageLabel: String get() = AppLanguageState.current.statistics.averageLabel
        val dailyDetails: String get() = AppLanguageState.current.statistics.dailyDetails
        val monthlyDetails: String get() = AppLanguageState.current.statistics.monthlyDetails
        val yearlyDetails: String get() = AppLanguageState.current.statistics.yearlyDetails
        val allHistory: String get() = AppLanguageState.current.statistics.allHistory
        val ended: String get() = AppLanguageState.current.statistics.ended
        val inProgress: String get() = AppLanguageState.current.statistics.inProgress
        val noRecords: String get() = AppLanguageState.current.statistics.noRecords
        val weekTab: String get() = AppLanguageState.current.statistics.weekTab
        val monthTab: String get() = AppLanguageState.current.statistics.monthTab
        val yearTab: String get() = AppLanguageState.current.statistics.yearTab
        val allTab: String get() = AppLanguageState.current.statistics.allTab
        val currentWeek: String get() = AppLanguageState.current.statistics.currentWeek
        val currentMonth: String get() = AppLanguageState.current.statistics.currentMonth
        val monthTotalCount: String get() = AppLanguageState.current.statistics.monthTotalCount
        val currentYear: String get() = AppLanguageState.current.statistics.currentYear
        val historyPeriod: String get() = AppLanguageState.current.statistics.historyPeriod
        val selectRange: String get() = AppLanguageState.current.statistics.selectRange
        val emptyTitle: String get() = AppLanguageState.current.statistics.emptyTitle
        val emptyMessage: String get() = AppLanguageState.current.statistics.emptyMessage
        val calendarAction: String get() = AppLanguageState.current.statistics.calendarAction
        val dailyDistribution: String get() = AppLanguageState.current.statistics.dailyDistribution
        val times: String get() = AppLanguageState.current.statistics.times
        val weeklySummaryTitle: String get() = AppLanguageState.current.statistics.weeklySummaryTitle
        val weeklyRecordedLabel: String get() = AppLanguageState.current.statistics.weeklyRecordedLabel
        val weeklyLegendFourPlus: String get() = AppLanguageState.current.statistics.weeklyLegendFourPlus
        val weeklyLegendThree: String get() = AppLanguageState.current.statistics.weeklyLegendThree
        val weeklyLegendTwo: String get() = AppLanguageState.current.statistics.weeklyLegendTwo
        val weeklyLegendOne: String get() = AppLanguageState.current.statistics.weeklyLegendOne
        val weeklyLegendZero: String get() = AppLanguageState.current.statistics.weeklyLegendZero
        val weeklyLegendUnrecorded: String get() = AppLanguageState.current.statistics.weeklyLegendUnrecorded
        val weeklyLegendFuture: String get() = AppLanguageState.current.statistics.weeklyLegendFuture
        val dailyCount: String get() = AppLanguageState.current.statistics.dailyCount
        val byDate: String get() = AppLanguageState.current.statistics.byDate
        val countComposition: String get() = AppLanguageState.current.statistics.countComposition
        val explicitZero: String get() = AppLanguageState.current.statistics.explicitZero
        val once: String get() = AppLanguageState.current.statistics.once
        val twice: String get() = AppLanguageState.current.statistics.twice
        val threePlus: String get() = AppLanguageState.current.statistics.threePlus
        val unfilledDays: String get() = AppLanguageState.current.statistics.unfilledDays
        val futureDays: String get() = AppLanguageState.current.statistics.futureDays
        val noSavedDays: String get() = AppLanguageState.current.statistics.noSavedDays
        val singleDayExtremes: String get() = AppLanguageState.current.statistics.singleDayExtremes
        val byPositiveCount: String get() = AppLanguageState.current.statistics.byPositiveCount
        val maximumDay: String get() = AppLanguageState.current.statistics.maximumDay
        val minimumPositiveDay: String get() = AppLanguageState.current.statistics.minimumPositiveDay
        val noPositiveDay: String get() = AppLanguageState.current.statistics.noPositiveDay
        val future: String get() = AppLanguageState.current.statistics.future
        val unset: String get() = AppLanguageState.current.statistics.unset
        val unsetShort: String get() = AppLanguageState.current.statistics.unsetShort
        val dash: String get() = AppLanguageState.current.statistics.dash
        val annualCount: String get() = AppLanguageState.current.statistics.annualCount
        val quarterShare: String get() = AppLanguageState.current.statistics.quarterShare
        val noPositiveCount: String get() = AppLanguageState.current.statistics.noPositiveCount
        val byCount: String get() = AppLanguageState.current.statistics.byCount
        val quarterShareHint: String get() = AppLanguageState.current.statistics.quarterShareHint
        val monthSummary: String get() = AppLanguageState.current.statistics.monthSummary
        val fullMonths: String get() = AppLanguageState.current.statistics.fullMonths
        val monthExtremesHint: String get() = AppLanguageState.current.statistics.monthExtremesHint
        val maximumMonth: String get() = AppLanguageState.current.statistics.maximumMonth
        val minimumMonth: String get() = AppLanguageState.current.statistics.minimumMonth
        val monthAverageFormat: String get() = AppLanguageState.current.statistics.monthAverageFormat

        fun detailCount(count: Long?): String =
            AppLanguageState.current.statistics.detailCount(count)

        fun detailDays(days: Int?): String =
            AppLanguageState.current.statistics.detailDays(days)

        fun historyStatus(first: LocalDate?, today: LocalDate): String =
            AppLanguageState.current.statistics.historyStatus(first, today)

        fun yearTitle(year: Int): String = AppLanguageState.current.statistics.yearTitle(year)

        fun monthTitle(year: Int, month: Int): String =
            AppLanguageState.current.statistics.monthTitle(year, month)

        fun monthLabel(month: Int): String = AppLanguageState.current.statistics.monthLabel(month)

        fun dateDescription(date: LocalDate, status: String): String =
            AppLanguageState.current.statistics.dateDescription(date, status)

        fun weekdayDateLabel(weekday: String, date: LocalDate): String =
            AppLanguageState.current.statistics.weekdayDateLabel(weekday, date)

        fun dayLabel(day: Int): String = AppLanguageState.current.statistics.dayLabel(day)

        fun dateRangeTitle(start: LocalDate, end: LocalDate): String =
            AppLanguageState.current.statistics.dateRangeTitle(start, end)

        fun yearStatus(end: LocalDate, today: LocalDate): String =
            AppLanguageState.current.statistics.yearStatus(end, today)

        fun periodStatus(end: LocalDate, today: LocalDate): String =
            AppLanguageState.current.statistics.periodStatus(end, today)

        fun periodAction(period: String, previous: Boolean): String =
            AppLanguageState.current.statistics.periodAction(period, previous)

        fun datePickerDescription(title: String): String =
            AppLanguageState.current.statistics.datePickerDescription(title)

        fun emptyTitle(moduleLabel: String): String =
            AppLanguageState.current.statistics.emptyTitle(moduleLabel)

        fun periodCountLabel(period: String, moduleLabel: String): String =
            AppLanguageState.current.statistics.periodCountLabel(period, moduleLabel)

        fun statisticsLabel(period: String): String =
            AppLanguageState.current.statistics.statisticsLabel(period)

        fun average(value: Double): String = AppLanguageState.current.statistics.average(value)

        fun averageNumber(value: Double): String =
            AppLanguageState.current.statistics.averageNumber(value)

        fun annualAverage(value: Double): String =
            AppLanguageState.current.statistics.annualAverage(value)

        fun countText(count: Long): String = AppLanguageState.current.statistics.countText(count)

        fun weeklyRecordedDays(recorded: Int, total: Int): String =
            AppLanguageState.current.statistics.weeklyRecordedDays(recorded, total)

        fun weeklyCountSuffix(count: Long): String =
            AppLanguageState.current.statistics.weeklyCountSuffix(count)

        fun daysText(days: Int): String = AppLanguageState.current.statistics.daysText(days)

        fun savedDaysSubtitle(days: Int): String =
            AppLanguageState.current.statistics.savedDaysSubtitle(days)

        fun categoryDays(days: Int): String = AppLanguageState.current.statistics.categoryDays(days)

        fun dayChartValue(day: Int, count: Long?, future: Boolean, recorded: Boolean): String =
            AppLanguageState.current.statistics.dayChartValue(day, count, future, recorded)

        fun monthDailyChartAccessibility(days: String): String =
            AppLanguageState.current.statistics.monthDailyChartAccessibility(days)

        fun monthSummaryAccessibility(totalCount: Long, recordedDays: Int, average: Double): String =
            AppLanguageState.current.statistics.monthSummaryAccessibility(totalCount, recordedDays, average)

        fun monthCompositionAccessibility(
            savedDays: Int,
            explicitZeroDays: Int,
            oneCountDays: Int,
            twoCountDays: Int,
            threePlusCountDays: Int,
            unfilledDays: Int,
            futureDays: Int,
        ): String = AppLanguageState.current.statistics.monthCompositionAccessibility(
            savedDays, explicitZeroDays, oneCountDays, twoCountDays,
            threePlusCountDays, unfilledDays, futureDays,
        )

        fun monthExtremeAccessibility(label: String, count: Long): String =
            AppLanguageState.current.statistics.monthExtremeAccessibility(label, count)

        fun percentage(value: Double): String = AppLanguageState.current.statistics.percentage(value)

        fun quarterLabel(quarter: Int): String =
            AppLanguageState.current.statistics.quarterLabel(quarter)

        fun monthChartLabel(month: Int, isFuture: Boolean, recorded: Boolean, count: Long?): String =
            AppLanguageState.current.statistics.monthChartLabel(month, isFuture, recorded, count)

        fun totalCountAccessibility(total: Long, quarters: String): String =
            AppLanguageState.current.statistics.totalCountAccessibility(total, quarters)

        fun annualChartAccessibility(months: String): String =
            AppLanguageState.current.statistics.annualChartAccessibility(months)

        fun chartMonthValue(isFuture: Boolean, recorded: Boolean, count: Long?): String =
            AppLanguageState.current.statistics.chartMonthValue(isFuture, recorded, count)
    }

    object Settings {
        val title: String get() = AppLanguageState.current.settings.title
        val open: String get() = AppLanguageState.current.settings.open
        val back: String get() = AppLanguageState.current.settings.back
        val accountSection: String get() = AppLanguageState.current.settings.accountSection
        val localAccountTitle: String get() = AppLanguageState.current.settings.localAccountTitle
        val localAccountSummary: String get() = AppLanguageState.current.settings.localAccountSummary
        val signedInAccountSummary: String get() = AppLanguageState.current.settings.signedInAccountSummary
        val generalSection: String get() = AppLanguageState.current.settings.generalSection
        val languageTitle: String get() = AppLanguageState.current.settings.languageTitle
        val languageZh: String get() = AppLanguageState.current.settings.languageZh
        val languageEn: String get() = AppLanguageState.current.settings.languageEn
        val languageDialogTitle: String get() = AppLanguageState.current.settings.languageDialogTitle
        val dataSection: String get() = AppLanguageState.current.settings.dataSection
        val localFirstTitle: String get() = AppLanguageState.current.settings.localFirstTitle
        val localFirstSummary: String get() = AppLanguageState.current.settings.localFirstSummary
        val privacyTitle: String get() = AppLanguageState.current.settings.privacyTitle
        val privacySummary: String get() = AppLanguageState.current.settings.privacySummary
        val aboutSection: String get() = AppLanguageState.current.settings.aboutSection
        val version: String get() = AppLanguageState.current.settings.version
        val license: String get() = AppLanguageState.current.settings.license
        val licenseValue: String get() = AppLanguageState.current.settings.licenseValue

        fun accountDescription(title: String, status: String): String =
            AppLanguageState.current.settings.accountDescription(title, status)
    }

    object NavigationBar {
        val calendar: String get() = AppLanguageState.current.navigationBar.calendar
        val statistics: String get() = AppLanguageState.current.navigationBar.statistics
    }

    object Components {
        val decrease: String get() = AppLanguageState.current.components.decrease
        val increase: String get() = AppLanguageState.current.components.increase

        fun joinSemantics(vararg parts: String): String =
            AppLanguageState.current.joinSemantics(*parts)

        fun joinSemantics(parts: Iterable<String>): String =
            AppLanguageState.current.joinSemantics(parts)
    }

    /** Locale used for user-visible date/weekday formatting. */
    val DISPLAY_LOCALE: Locale get() = AppLanguageState.current.displayLocale

    /** TalkBack-visible list separator used across app semantic descriptions. */
    val SEMANTICS_SEPARATOR: String get() = AppLanguageState.current.semanticsSeparator
}
