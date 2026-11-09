package io.github.litaog.dailyrecord.core.common

import java.util.Locale

/**
 * English copy set.
 *
 * Length policy: fixed-width containers (module halves, period capsules,
 * calendar cells, sync chips) use short labels; TalkBack reads full terms.
 */
internal object EnStrings : AppStrings {
    override val privateRecordSubtitle = "Track daily counts"
    override val offlineSubtitle = "Offline, on this device"
    override val vpnSyncFailure = "Check your VPN and retry. Records stay on this device."
    override val readingLocalRecords = "Loading local records"
    override val selected = "selected"
    override val unselected = "not selected"
    override val today = "today"
    override val historyDate = "past date"
    override val futureDate = "future date"
    override val displayLocale: Locale = Locale.US
    override val semanticsSeparator = ", "

    override val auth: AuthStrings = EnAuthStrings
    override val account: AccountStrings = EnAccountStrings
    override val recordModule: RecordModuleStrings = EnRecordModule
    override val deletion: DeletionStrings = EnDeletionStrings
    override val calendar: CalendarStrings = EnCalendarStrings
    override val navigation: NavigationStrings = EnNavigationStrings
    override val record: RecordStrings = EnRecordStrings
    override val statistics: StatisticsStrings = EnStatisticsStrings
    override val settings: SettingsStrings = EnSettingsStrings
    override val navigationBar: NavigationBarStrings = EnNavigationBarStrings
    override val components: ComponentStrings = EnComponentStrings
    override val appLock: AppLockStrings = EnAppLockStrings
    override val disguise: DisguiseStrings = EnDisguiseStrings
    override val onboarding: OnboardingStrings = EnOnboardingStrings

    fun withStyle(style: TerminologyStyle): AppStrings = this

    operator fun invoke(style: TerminologyStyle = TerminologyStyle.ELEGANT): AppStrings = this

    private val weekdayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    override fun weekdayName(dayOfWeek: Int): String {
        require(dayOfWeek in 1..7) { "dayOfWeek must be in 1..7, was $dayOfWeek" }
        return weekdayNames[dayOfWeek - 1]
    }

    override fun selectedState(label: String, isSelected: Boolean): String =
        "$label, ${if (isSelected) selected else unselected}"

    override fun joinSemantics(vararg parts: String): String =
        parts.joinToString(semanticsSeparator)

    override fun joinSemantics(parts: Iterable<String>): String =
        parts.joinToString(semanticsSeparator)

}
