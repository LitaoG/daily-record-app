package io.github.litaog.dailyrecord.core.common

import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * Language-agnostic surface of every user-facing string and display format.
 *
 * Compile-time completeness: any member missing from an implementation fails
 * the build, so a new copy key can never be silently untranslated.
 */
internal interface AppStrings {
    val privateRecordSubtitle: String
    val offlineSubtitle: String
    val vpnSyncFailure: String
    val readingLocalRecords: String
    val selected: String
    val unselected: String
    val today: String
    val historyDate: String
    val futureDate: String
    val displayLocale: Locale
    val semanticsSeparator: String
    val auth: AuthStrings
    val account: AccountStrings
    val recordModule: RecordModuleStrings
    val deletion: DeletionStrings
    val calendar: CalendarStrings
    val navigation: NavigationStrings
    val record: RecordStrings
    val statistics: StatisticsStrings
    val settings: SettingsStrings
    val navigationBar: NavigationBarStrings
    val components: ComponentStrings

    fun weekdayName(dayOfWeek: Int): String

    fun selectedState(label: String, isSelected: Boolean): String

    fun joinSemantics(vararg parts: String): String

    fun joinSemantics(parts: Iterable<String>): String

    interface AuthStrings {
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

    interface AccountStrings {
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

    interface RecordModuleStrings {
        val handBrewLabel: String
        val handBrewAccessibilityLabel: String
        val handBrewQuestionToday: String
        val handBrewQuestionPast: String
        val handBrewZero: String
        val sexLabel: String
        val sexAccessibilityLabel: String
        val sexQuestionToday: String
        val sexQuestionPast: String
        val sexZero: String

        fun recordLabel(label: String): String
    }

    interface DeletionStrings {
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

    interface CalendarStrings {
        val weekdays: List<String>
        val recordHint: String
        val unset: String
        val future: String
        val zero: String
        val recorded: String
        val previousMonth: String
        val nextMonth: String
        val selectDate: String
        val backToToday: String
        val legendDescription: String
        val unavailable: String
        val oneTime: String
        val twoTimes: String
        val ninePlusTimes: String
        val todayShort: String
        val futureDescription: String
        val unsetDescription: String
        val zeroDescription: String
        val selectedSuffix: String
        val todaySuffix: String

        fun monthSummary(count: Long, days: Int): String

        fun monthTitle(month: YearMonth): String

        fun monthTitleMultiline(month: YearMonth): String

        fun monthSelectionDescription(month: YearMonth): String

        fun monthDateDescription(date: LocalDate, state: String, focused: Boolean): String

        fun legendDescription(moduleLabel: String): String

        fun countDescription(count: Int): String

        fun statusDescription(
            date: LocalDate,
            today: LocalDate,
            unsupported: Boolean,
            future: Boolean,
            count: Int?,
            moduleLabel: String,
        ): String
    }

    interface NavigationStrings {
        val weekdays: List<String>
        val title: String
        val subtitle: String
        val dateWheelSubtitle: String
        val monthSubtitle: String
        val jumpToDate: String
        val jumpToMonth: String
        val jumpToYear: String
        val selected: String
        val switchYear: String
        val returnToDatePicker: String
        val selectYear: String
        val selectMonth: String
        val dateWheelHint: String
        val yearUnit: String
        val monthUnit: String
        val dayUnit: String

        fun switchYearDescription(year: Int): String

        fun nextYearDescription(forward: Boolean): String

        fun dateText(date: LocalDate): String

        fun dateLabel(date: LocalDate, weekday: String): String

        fun selectYearDescription(year: Int): String

        fun yearTitle(year: Int): String

        fun monthTitle(month: YearMonth): String

        fun monthLabel(month: Int): String

        fun dayLabel(day: Int): String

        fun monthDescription(month: YearMonth): String
    }

    interface RecordStrings {
        val loading: String
        val saving: String
        val saved: String
        val save: String
        val saveFailure: String
        val clear: String
        val clearFailure: String
        val countOnly: String
        val countFirst: String
        val countAndDetails: String
        val detailEntry: String
        val detailEntryHintFormat: String
        val detailSectionTitle: String
        val detailSectionHint: String
        val detailCollapse: String
        val detailExpand: String
        val detailOccurrenceFormat: String
        val detailStartTime: String
        val detailEndTime: String
        val detailStartTimeUnset: String
        val detailEndTimeUnset: String
        val detailTimeUnset: String
        val detailTimePickerTitle: String
        val detailTimePickerSubtitle: String
        val detailTimePickerHour: String
        val detailTimePickerMinute: String
        val detailTimePickerHint: String
        val detailTimePickerConfirm: String
        val detailWriteFeeling: String
        val detailCollapseFeeling: String
        val detailFeelingLabel: String
        val detailFeelingHint: String
        val detailFeelingCounter: String
        val detailEndBeforeStart: String
        val detailDiscardTitle: String
        val detailDiscardMessage: String
        val detailConfirmRemove: String
        val detailEntryUnavailable: String
        val loadingRecords: String
        val futureUnavailable: String
        val notSaved: String
        val zeroRecorded: String
        val clearTitle: String
        val clearSubtitle: String
        val clearMessage: String
        val confirmClear: String
        val clearDetailsFailure: String
        val clearDetailsTitle: String
        val clearDetailsSubtitle: String
        val clearDetailsMessage: String
        val confirmClearDetails: String
        val discardTitle: String
        val unsavedSubtitle: String
        val discardMessage: String
        val continueEditing: String
        val discard: String
        val backToCalendar: String

        fun savedStatus(count: Int): String

        fun recordedStatus(count: Int): String

        fun explicitZeroHint(text: String): String

        fun monthSaved(month: Int): String

        fun monthSummary(count: Long, days: Int): String

        fun moduleRecordLabel(moduleLabel: String): String

        fun dateLabel(date: LocalDate, weekday: String): String

        fun detailEntryHint(count: Int): String

        fun detailOccurrence(index: Int): String

        fun detailFeelingCounter(count: Int): String

        fun detailTimeDescription(occurrence: Int, label: String, value: String): String

        fun detailTimeWheelCurrent(unit: String, value: String): String

        fun detailTimeWheelOption(unit: String, value: String): String

        fun detailFeelingActionDescription(occurrence: Int, action: String): String

        fun detailFeelingEditorDescription(occurrence: Int): String
    }

    interface StatisticsStrings {
        val weekdays: List<String>
        val title: String
        val countAndDays: String
        val countUnit: String
        val dayUnit: String
        val perDayUnit: String
        val recordedDaysLabel: String
        val averageLabel: String
        val dailyDetails: String
        val monthlyDetails: String
        val yearlyDetails: String
        val allHistory: String
        val ended: String
        val inProgress: String
        val noRecords: String
        val weekTab: String
        val monthTab: String
        val yearTab: String
        val allTab: String
        val currentWeek: String
        val currentMonth: String
        val monthTotalCount: String
        val currentYear: String
        val historyPeriod: String
        val selectRange: String
        val emptyTitle: String
        val emptyMessage: String
        val calendarAction: String
        val dailyDistribution: String
        val times: String
        val weeklySummaryTitle: String
        val weeklyRecordedLabel: String
        val weeklyLegendFourPlus: String
        val weeklyLegendThree: String
        val weeklyLegendTwo: String
        val weeklyLegendOne: String
        val weeklyLegendZero: String
        val weeklyLegendUnrecorded: String
        val weeklyLegendFuture: String
        val dailyCount: String
        val byDate: String
        val countComposition: String
        val explicitZero: String
        val once: String
        val twice: String
        val threePlus: String
        val unfilledDays: String
        val futureDays: String
        val noSavedDays: String
        val singleDayExtremes: String
        val byPositiveCount: String
        val maximumDay: String
        val minimumPositiveDay: String
        val noPositiveDay: String
        val future: String
        val unset: String
        val unsetShort: String
        val dash: String
        val annualCount: String
        val quarterShare: String
        val noPositiveCount: String
        val byCount: String
        val quarterShareHint: String
        val monthSummary: String
        val fullMonths: String
        val monthExtremesHint: String
        val maximumMonth: String
        val minimumMonth: String
        val monthAverageFormat: String

        fun detailCount(count: Long?): String

        fun detailDays(days: Int?): String

        fun historyStatus(first: LocalDate?, today: LocalDate): String

        fun yearTitle(year: Int): String

        fun monthTitle(year: Int, month: Int): String

        fun monthLabel(month: Int): String

        fun dateDescription(date: LocalDate, status: String): String

        fun weekdayDateLabel(weekday: String, date: LocalDate): String

        fun dayLabel(day: Int): String

        fun dateRangeTitle(start: LocalDate, end: LocalDate): String

        fun yearStatus(end: LocalDate, today: LocalDate): String

        fun periodStatus(end: LocalDate, today: LocalDate): String

        fun periodAction(period: String, previous: Boolean): String

        fun datePickerDescription(title: String): String

        fun emptyTitle(moduleLabel: String): String

        fun periodCountLabel(period: String, moduleLabel: String): String

        fun statisticsLabel(period: String): String

        fun average(value: Double): String

        fun averageNumber(value: Double): String

        fun annualAverage(value: Double): String

        fun countText(count: Long): String

        fun weeklyRecordedDays(recorded: Int, total: Int): String

        fun weeklyCountSuffix(count: Long): String

        fun daysText(days: Int): String

        fun savedDaysSubtitle(days: Int): String

        fun categoryDays(days: Int): String

        fun dayChartValue(day: Int, count: Long?, future: Boolean, recorded: Boolean): String

        fun monthDailyChartAccessibility(days: String): String

        fun monthSummaryAccessibility(totalCount: Long, recordedDays: Int, average: Double): String

        fun monthCompositionAccessibility(
            savedDays: Int,
            explicitZeroDays: Int,
            oneCountDays: Int,
            twoCountDays: Int,
            threePlusCountDays: Int,
            unfilledDays: Int,
            futureDays: Int,
        ): String

        fun monthExtremeAccessibility(label: String, count: Long): String

        fun percentage(value: Double): String

        fun quarterLabel(quarter: Int): String

        fun monthChartLabel(month: Int, isFuture: Boolean, recorded: Boolean, count: Long?): String

        fun totalCountAccessibility(total: Long, quarters: String): String

        fun annualChartAccessibility(months: String): String

        fun chartMonthValue(isFuture: Boolean, recorded: Boolean, count: Long?): String
    }

    interface SettingsStrings {
        val title: String
        val open: String
        val back: String
        val accountSection: String
        val localAccountTitle: String
        val localAccountSummary: String
        val signedInAccountSummary: String
        val generalSection: String
        val languageTitle: String
        val languageZh: String
        val languageEn: String
        val languageDialogTitle: String
        val dataSection: String
        val localFirstTitle: String
        val localFirstSummary: String
        val privacyTitle: String
        val privacySummary: String
        val aboutSection: String
        val version: String
        val license: String
        val licenseValue: String

        fun accountDescription(title: String, status: String): String
    }

    interface NavigationBarStrings {
        val calendar: String
        val statistics: String
    }

    interface ComponentStrings {
        val decrease: String
        val increase: String
    }
}

/** Single mutable source of the active copy set, readable from non-UI layers. */
internal object AppLanguageState {
    @Volatile
    var current: AppStrings = ZhStrings
}

internal fun AppLanguage.strings(): AppStrings = when (this) {
    AppLanguage.ZH -> ZhStrings
    AppLanguage.EN -> EnStrings
}
