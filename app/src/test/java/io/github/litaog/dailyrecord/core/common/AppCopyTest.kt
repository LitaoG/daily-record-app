package io.github.litaog.dailyrecord.core.common

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppCopyTest {
    @Test
    fun historyStatusIsACompactDateRangeWithoutDuplicateStartLabel() {
        val copy = AppCopy.Statistics.historyStatus(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3))

        assertEquals("2026年8月—2026年8月", copy)
        assertFalse(copy.contains("记录起点"))
        assertFalse(copy.contains("首次记录"))
    }

    @Test
    fun periodActionsUseNaturalPeriodNames() {
        assertEquals("上一周", AppCopy.Statistics.periodAction(AppCopy.Statistics.weekTab, previous = true))
        assertEquals("下个月", AppCopy.Statistics.periodAction(AppCopy.Statistics.monthTab, previous = false))
        assertEquals("上一年", AppCopy.Statistics.periodAction(AppCopy.Statistics.yearTab, previous = true))
    }

    @Test
    fun zeroHintExplainsTheDifferenceFromAnUnsetDay() {
        assertEquals(
            "填 0 表示当天没有手冲，会保留记录。",
            AppCopy.Record.explicitZeroHint(AppCopy.RecordModule.handBrewZero),
        )
    }

    @Test
    fun copyUsesDirectTermsAndKeepsZeroStatusUnambiguous() {
        assertEquals("登录并同步", AppCopy.Account.signInSync)
        assertEquals("次数分布", AppCopy.Statistics.countComposition)
        assertEquals("发生天数", AppCopy.Statistics.recordedDaysLabel)
        assertEquals("3 天", AppCopy.Statistics.categoryDays(3))
        assertEquals("月均 0.6 次", AppCopy.Statistics.annualAverage(.6))
        assertEquals(
            "手冲，记录为 0 次，今天",
            AppCopy.Calendar.statusDescription(
                date = LocalDate.of(2026, 8, 2),
                today = LocalDate.of(2026, 8, 2),
                unsupported = false,
                future = false,
                count = 0,
                moduleLabel = AppCopy.RecordModule.handBrewLabel,
            ),
        )
        assertFalse(AppCopy.RecordModule.handBrewZero.contains("明确没"))
    }

    @Test
    fun calendarStatusUsesSameCountBucketsAsVisualLabels() {
        val moduleLabel = AppCopy.RecordModule.handBrewLabel
        val today = LocalDate.of(2026, 8, 2)
        val pastDate = LocalDate.of(2026, 8, 1)

        // TalkBack and the visible cell must share the exact same bucketing:
        // 1..8 stay exact, 9+ collapses to "9 次以上".
        assertEquals(
            "手冲，1 次",
            AppCopy.Calendar.statusDescription(
                pastDate, today, unsupported = false, future = false,
                count = 1, moduleLabel = moduleLabel,
            ),
        )
        assertEquals(
            "手冲，8 次",
            AppCopy.Calendar.statusDescription(
                pastDate, today, unsupported = false, future = false,
                count = 8, moduleLabel = moduleLabel,
            ),
        )
        assertEquals(
            "手冲，9 次以上",
            AppCopy.Calendar.statusDescription(
                pastDate, today, unsupported = false, future = false,
                count = 9, moduleLabel = moduleLabel,
            ),
        )
        assertEquals(
            "手冲，9 次以上",
            AppCopy.Calendar.statusDescription(
                pastDate, today, unsupported = false, future = false,
                count = 10, moduleLabel = moduleLabel,
            ),
        )
        // The visual label itself uses the same helper.
        assertEquals("9 次以上", AppCopy.Calendar.countDescription(9))
        assertEquals("9 次以上", AppCopy.Calendar.countDescription(10))
        assertEquals("8 次", AppCopy.Calendar.countDescription(8))
    }
}
