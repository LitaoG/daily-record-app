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
        assertEquals("Sex", AppCopy.RecordModule.sexLabel)
        assertEquals("Masturbation", AppCopy.RecordModule.handBrewAccessibilityLabel)
        assertEquals("Sex", AppCopy.RecordModule.sexAccessibilityLabel)
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
        assertEquals("自慰", AppCopy.RecordModule.handBrewLabel)
        assertEquals("做爱", AppCopy.RecordModule.sexLabel)
        assertEquals("周", AppCopy.Statistics.weekTab)
        assertEquals("2026年 8月", AppCopy.Calendar.monthTitle(YearMonth.of(2026, 8)))
        assertEquals("3 次", AppCopy.Statistics.countText(3))
        assertEquals("9 次以上", AppCopy.Calendar.countDescription(9))
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
            val nested = target.javaClass.methods.filter {
                it.name.startsWith("get") &&
                    it.parameterCount == 0 &&
                    it.returnType.isInterface &&
                    it.returnType.name.startsWith("io.github.litaog.dailyrecord.core.common.AppStrings")
            }
            nested.forEach { walk(it.invoke(target)) }
        }

        walk(root)
        return values
    }
}
