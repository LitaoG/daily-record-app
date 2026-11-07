package io.github.litaog.dailyrecord.core.common

import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStringsLocalizationTest {
    private val cjk = Regex("[\\u4e00-\\u9fff]")

    @After
    fun resetLanguage() {
        AppLanguageState.current = ZhStrings
    }

    @Test
    fun englishModuleLabelsAreShortAndAccessibilityUsesFullTerms() = inEnglish {
        assertEquals("Solo", AppCopy.RecordModule.handBrewLabel)
        assertEquals("Duo", AppCopy.RecordModule.sexLabel)
        assertEquals("Solo journal", AppCopy.RecordModule.handBrewAccessibilityLabel)
        assertEquals("Intimate journal", AppCopy.RecordModule.sexAccessibilityLabel)
    }

    @Test
    fun englishPeriodTabsUseCompactLabels() = inEnglish {
        assertEquals("Wk", AppCopy.Statistics.weekTab)
        assertEquals("Mo", AppCopy.Statistics.monthTab)
        assertEquals("Yr", AppCopy.Statistics.yearTab)
        assertEquals("All", AppCopy.Statistics.allTab)
    }

    @Test
    fun englishDateFormats() = inEnglish {
        assertEquals("Aug 2026", AppCopy.Calendar.monthTitle(YearMonth.of(2026, 8)))
        assertEquals("Aug\n2026", AppCopy.Calendar.monthTitleMultiline(YearMonth.of(2026, 8)))
        assertEquals("Aug 16, 2026", AppCopy.Navigation.dateText(LocalDate.of(2026, 8, 16)))
        assertEquals(
            "Aug 2026 – Aug 2026",
            AppCopy.Statistics.historyStatus(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3)),
        )
        assertEquals(
            "Aug 6, 2026–Aug 12, 2026",
            AppCopy.Statistics.dateRangeTitle(LocalDate.of(2026, 8, 6), LocalDate.of(2026, 8, 12)),
        )
        assertEquals(
            "Aug 6, 2026–Jan 5, 2027",
            AppCopy.Statistics.dateRangeTitle(LocalDate.of(2026, 8, 6), LocalDate.of(2027, 1, 5)),
        )
    }

    @Test
    fun englishNumbersAndUnits() = inEnglish {
        assertEquals("3 times", AppCopy.Statistics.countText(3))
        assertEquals("5 days", AppCopy.Statistics.daysText(5))
        assertEquals("1.7 /day", AppCopy.Statistics.average(1.7))
        assertEquals("Recorded · 3", AppCopy.Record.recordedStatus(3))
        assertEquals("Unsaved · 2", AppCopy.Record.savedStatus(2))
        assertEquals("3+", AppCopy.Statistics.threePlus)
        assertEquals("9+", AppCopy.Calendar.ninePlusTimes)
        assertEquals("0", AppCopy.Calendar.countDescription(0))
        assertEquals("3", AppCopy.Calendar.countDescription(3))
        assertEquals("9+", AppCopy.Calendar.countDescription(9))
    }

    @Test
    fun englishWeekdays() = inEnglish {
        assertEquals("Mon", AppCopy.weekdayName(1))
        assertEquals("Sun", AppCopy.weekdayName(7))
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), AppCopy.Calendar.weekdays)
    }

    @Test
    fun englishSeparatorsLocaleAndSemantics() = inEnglish {
        assertEquals(", ", AppCopy.SEMANTICS_SEPARATOR)
        assertEquals(Locale.US, AppCopy.DISPLAY_LOCALE)
        assertEquals("X, selected", AppCopy.selectedState("X", true))
        assertEquals("X, not selected", AppCopy.selectedState("X", false))
        assertEquals("X, Y", AppCopy.Components.joinSemantics("X", "Y"))
    }

    @Test
    fun englishShortCopyAnchors() = inEnglish {
        assertEquals("Calendar", AppCopy.NavigationBar.calendar)
        assertEquals("Stats", AppCopy.NavigationBar.statistics)
        assertEquals("Offline", AppCopy.Account.shortOffline)
        assertEquals("Syncing", AppCopy.Account.shortSyncing)
        assertEquals("Settings", AppCopy.Settings.title)
        assertEquals("Avg / day", AppCopy.Statistics.averageLabel)
        assertEquals("Days", AppCopy.Statistics.recordedDaysLabel)
        assertEquals("Language", AppCopy.Settings.languageTitle)
        assertEquals("Choose language", AppCopy.Settings.languageDialogTitle)
        assertEquals("中文", AppCopy.Settings.languageZh)
        assertEquals("English", AppCopy.Settings.languageEn)
        assertEquals("This month: 3 times · 2 days", AppCopy.Calendar.monthSummary(3, 2))
    }

    @Test
    fun englishStringsContainNoCjkCharacters() = inEnglish {
        // Language option names are self-named in their own language on purpose.
        val selfNamed = setOf(AppCopy.Settings.languageZh, AppCopy.Settings.languageEn)
        val values = collectStringValues(EnStrings).filterNot { it in selfNamed }
        assertTrue("Reflection should collect English copy values", values.isNotEmpty())
        values.forEach { value ->
            assertFalse("Unexpected CJK in English copy: $value", cjk.containsMatchIn(value))
        }
    }

    @Test
    fun zhAnchorsRemainUnchanged() {
        assertEquals("个人私密", AppCopy.RecordModule.handBrewLabel)
        assertEquals("伴侣亲密", AppCopy.RecordModule.sexLabel)
        assertEquals("周", AppCopy.Statistics.weekTab)
        assertEquals("2026年 8月", AppCopy.Calendar.monthTitle(YearMonth.of(2026, 8)))
        assertEquals("3 次", AppCopy.Statistics.countText(3))
        assertEquals("9 次以上", AppCopy.Calendar.countDescription(9))
    }

    @Test
    fun zhElegantDefaultsUseIntimacyTerms() {
        val previous = AppLanguageState.current
        AppLanguageState.current = ZhStrings(TerminologyStyle.ELEGANT)
        try {
            assertEquals("个人私密", AppCopy.RecordModule.handBrewLabel)
            assertEquals("伴侣亲密", AppCopy.RecordModule.sexLabel)
            assertEquals("个人私密", AppCopy.RecordModule.handBrewAccessibilityLabel)
            assertEquals("伴侣亲密", AppCopy.RecordModule.sexAccessibilityLabel)
            assertEquals("今天个人私密了几次？", AppCopy.RecordModule.handBrewQuestionToday)
            assertEquals("今天伴侣亲密了几次？", AppCopy.RecordModule.sexQuestionToday)
            assertEquals("当天个人私密了几次？", AppCopy.RecordModule.handBrewQuestionPast)
            assertEquals("当天伴侣亲密了几次？", AppCopy.RecordModule.sexQuestionPast)
            assertEquals("当天没有个人私密", AppCopy.RecordModule.handBrewZero)
            assertEquals("当天没有伴侣亲密", AppCopy.RecordModule.sexZero)
        } finally {
            AppLanguageState.current = previous
        }
    }

    @Test
    fun zhDirectStyleUsesDirectTerms() {
        val previous = AppLanguageState.current
        AppLanguageState.current = ZhStrings(TerminologyStyle.DIRECT)
        try {
            assertEquals("自慰", AppCopy.RecordModule.handBrewLabel)
            assertEquals("做爱", AppCopy.RecordModule.sexLabel)
            assertEquals("自慰", AppCopy.RecordModule.handBrewAccessibilityLabel)
            assertEquals("做爱", AppCopy.RecordModule.sexAccessibilityLabel)
            assertEquals("今天自慰了几次？", AppCopy.RecordModule.handBrewQuestionToday)
            assertEquals("今天做爱了几次？", AppCopy.RecordModule.sexQuestionToday)
            assertEquals("当天自慰了几次？", AppCopy.RecordModule.handBrewQuestionPast)
            assertEquals("当天做爱了几次？", AppCopy.RecordModule.sexQuestionPast)
            assertEquals("当天没有自慰", AppCopy.RecordModule.handBrewZero)
            assertEquals("当天没有做爱", AppCopy.RecordModule.sexZero)
        } finally {
            AppLanguageState.current = previous
        }
    }

    @Test
    fun noForbiddenSelfIntimacyTermInAnyStrings() {
        val forbidden = "自我亲密"
        val elegantValues = collectStringValues(ZhStrings(TerminologyStyle.ELEGANT))
        val directValues = collectStringValues(ZhStrings(TerminologyStyle.DIRECT))
        val enValues = collectStringValues(EnStrings)

        (elegantValues + directValues + enValues).forEach { value ->
            assertFalse("Strings must not contain '$forbidden', found in: $value", value.contains(forbidden))
        }
    }

    @Test
    fun englishStringsUnaffectedByTerminologyStyle() {
        val elegantEn = AppLanguage.EN.strings(TerminologyStyle.ELEGANT)
        val directEn = AppLanguage.EN.strings(TerminologyStyle.DIRECT)
        assertEquals("Solo", elegantEn.recordModule.handBrewLabel)
        assertEquals("Solo", directEn.recordModule.handBrewLabel)
        assertEquals("Duo", elegantEn.recordModule.sexLabel)
        assertEquals("Duo", directEn.recordModule.sexLabel)
    }

    @Test
    fun settingsAndOnboardingCopyArePresent() {
        assertEquals("用语风格", ZhStrings.settings.terminologyTitle)
        assertEquals("选择用语风格", ZhStrings.settings.terminologyDialogTitle)
        assertEquals("典雅", ZhStrings.settings.terminologyElegant)
        assertEquals("直白", ZhStrings.settings.terminologyDirect)
        assertEquals("用语说明", ZhStrings.onboarding.title)
        assertEquals("个人私密", ZhStrings.onboarding.soloLabel)
        assertEquals("伴侣亲密", ZhStrings.onboarding.duoLabel)
        assertEquals("我知道了", ZhStrings.onboarding.dismissAction)

        assertEquals("Terminology Style", EnStrings.settings.terminologyTitle)
        assertEquals("Choose terminology style", EnStrings.settings.terminologyDialogTitle)
        assertEquals("Elegant", EnStrings.settings.terminologyElegant)
        assertEquals("Direct", EnStrings.settings.terminologyDirect)
        assertEquals("Terminology Guide", EnStrings.onboarding.title)
        assertEquals("Solo Intimacy", EnStrings.onboarding.soloLabel)
        assertEquals("Partner Intimacy", EnStrings.onboarding.duoLabel)
        assertEquals("Got it", EnStrings.onboarding.dismissAction)
    }

    @Test
    fun weekdayListsStayStructurallyAlignedAcrossLanguages() {
        val zhWeekdays = ZhStrings.calendar.weekdays
        val enWeekdays = inEnglish { AppCopy.Calendar.weekdays }
        assertEquals(7, zhWeekdays.size)
        assertEquals(zhWeekdays.size, enWeekdays.size)
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), enWeekdays)
        assertEquals(enWeekdays, inEnglish { AppCopy.Statistics.weekdays })
        assertEquals(enWeekdays, inEnglish { AppCopy.Navigation.weekdays })
    }

    private fun <T> inEnglish(block: () -> T): T {
        val previous = AppLanguageState.current
        AppLanguageState.current = EnStrings
        return try {
            block()
        } finally {
            AppLanguageState.current = previous
        }
    }

    private fun collectStringValues(root: Any): List<String> {
        val values = mutableListOf<String>()

        fun walk(target: Any) {
            val methods = target.javaClass.methods.filter {
                it.name.startsWith("get") &&
                    it.name != "getClass" &&
                    it.parameterCount == 0 &&
                    it.returnType == String::class.java
            }
            methods.forEach { values += it.invoke(target) as String }
            val stringLists = target.javaClass.methods.filter {
                it.name.startsWith("get") &&
                    it.name != "getClass" &&
                    it.parameterCount == 0 &&
                    it.returnType == List::class.java
            }
            stringLists.forEach { method ->
                values += (method.invoke(target) as List<*>).filterIsInstance<String>()
            }
            val nested = target.javaClass.methods.filter {
                it.name.startsWith("get") &&
                    it.parameterCount == 0 &&
                    it.returnType.isInterface &&
                    it.returnType.name.startsWith("io.github.litaog.dailyrecord.core.common.AppStrings")
            }
            nested.forEach { method -> method.invoke(target)?.let { walk(it) } }
        }

        walk(root)
        return values
    }
}
