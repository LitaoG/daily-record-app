package io.github.litaog.dailyrecord.core.common

import java.util.Locale

/**
 * Single source for user-facing copy used by both the UI and sync layer.
 *
 * The facade delegates every member to the active [AppStrings] implementation
 * selected by [AppLanguageState.current]; call sites keep their historical
 * shape, so switching languages never touches individual Composables.
 *
 * Visible text lives only in ZhStrings and EnStrings and reaches the UI through
 * this facade (docs/ENGINEERING_STANDARDS.md section 7); no other source file may
 * hardcode visible text or CJK punctuation.
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

    object Auth : AppCopyAuth
    object Account : AppCopyAccount
    object RecordModule : AppCopyRecordModule
    object Deletion : AppCopyDeletion
    object Calendar : AppCopyCalendar
    object Navigation : AppCopyNavigation
    object Record : AppCopyRecord
    object Statistics : AppCopyStatistics
    object Settings : AppCopySettings
    object NavigationBar : AppCopyNavigationBar
    object AppLock : AppCopyAppLock
    object Disguise : AppCopyDisguise
    object Components : AppCopyComponent
    object Onboarding : AppCopyOnboarding

    /** Locale used for user-visible date/weekday formatting. */
    val DISPLAY_LOCALE: Locale get() = AppLanguageState.current.displayLocale

    /** TalkBack-visible list separator used across app semantic descriptions. */
    val SEMANTICS_SEPARATOR: String get() = AppLanguageState.current.semanticsSeparator

}
