package io.github.litaog.dailyrecord.core.common

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
    val appLock: AppLockStrings
    val disguise: DisguiseStrings
    val onboarding: OnboardingStrings

    fun weekdayName(dayOfWeek: Int): String

    fun selectedState(label: String, isSelected: Boolean): String

    fun joinSemantics(vararg parts: String): String

    fun joinSemantics(parts: Iterable<String>): String
}

/** Single mutable source of the active copy set, readable from non-UI layers. */
internal object AppLanguageState {
    @Volatile
    var current: AppStrings = ZhStrings
}

internal fun AppLanguage.strings(style: TerminologyStyle = TerminologyStyle.ELEGANT): AppStrings = when (this) {
    AppLanguage.ZH -> ZhStrings(style)
    AppLanguage.EN -> EnStrings(style)
}
