package io.github.litaog.dailyrecord.core.common

import java.util.Locale

/** Chinese copy set. Wording is identical to the historical AppCopy source. */
internal object ZhStrings : AppStrings {
    override val privateRecordSubtitle = "记录每天的次数"
    override val offlineSubtitle = "本机记录可离线使用"
    override val vpnSyncFailure = "请打开 VPN（梯子）后重试；记录仍在本机。"
    override val readingLocalRecords = "正在读取本机记录"
    override val selected = "已选择"
    override val unselected = "未选择"
    override val today = "今天"
    override val historyDate = "历史日期"
    override val futureDate = "未来日期"
    override val displayLocale: Locale = Locale.SIMPLIFIED_CHINESE
    override val semanticsSeparator = "，"

    override val auth: AuthStrings = ZhAuthStrings
    override val account: AccountStrings = ZhAccountStrings
    override val recordModule: RecordModuleStrings = ZhElegantRecordModule
    override val deletion: DeletionStrings = ZhDeletionStrings
    override val calendar: CalendarStrings = ZhCalendarStrings
    override val navigation: NavigationStrings = ZhNavigationStrings
    override val record: RecordStrings = ZhRecordStrings
    override val statistics: StatisticsStrings = ZhStatisticsStrings
    override val settings: SettingsStrings = ZhSettingsStrings
    override val navigationBar: NavigationBarStrings = ZhNavigationBarStrings
    override val components: ComponentStrings = ZhComponentStrings
    override val appLock: AppLockStrings = ZhAppLockStrings
    override val disguise: DisguiseStrings = ZhDisguiseStrings
    override val onboarding: OnboardingStrings = ZhOnboardingStrings

    private val weekdayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    override fun weekdayName(dayOfWeek: Int): String {
        require(dayOfWeek in 1..7) { "dayOfWeek must be in 1..7, was $dayOfWeek" }
        return weekdayNames[dayOfWeek - 1]
    }

    override fun selectedState(label: String, isSelected: Boolean): String =
        "$label，${if (isSelected) selected else unselected}"

    override fun joinSemantics(vararg parts: String): String =
        parts.joinToString(semanticsSeparator)

    override fun joinSemantics(parts: Iterable<String>): String =
        parts.joinToString(semanticsSeparator)

    fun withStyle(style: TerminologyStyle): AppStrings = when (style) {
        TerminologyStyle.ELEGANT -> this
        TerminologyStyle.DIRECT -> ZhStringsWithStyle(style)
    }

    operator fun invoke(style: TerminologyStyle = TerminologyStyle.ELEGANT): AppStrings =
        withStyle(style)

}

private class ZhStringsWithStyle(
    private val style: TerminologyStyle,
) : AppStrings by ZhStrings {
    override val recordModule: RecordModuleStrings = when (style) {
        TerminologyStyle.ELEGANT -> ZhStrings.recordModule
        TerminologyStyle.DIRECT -> ZhDirectRecordModule
    }
}

private object ZhDirectRecordModule : RecordModuleStrings {
    override val handBrewLabel = "自慰"
    override val handBrewAccessibilityLabel = "自慰"
    override val handBrewQuestionToday = "今天自慰了几次？"
    override val handBrewQuestionPast = "当天自慰了几次？"
    override val handBrewZero = "当天没有自慰"
    override val sexLabel = "做爱"
    override val sexAccessibilityLabel = "做爱"
    override val sexQuestionToday = "今天做爱了几次？"
    override val sexQuestionPast = "当天做爱了几次？"
    override val sexZero = "当天没有做爱"

    override fun recordLabel(label: String): String = "${label}记录"
}
