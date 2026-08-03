package io.github.litaog.dailyrecord.core.common

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppCopyTest {
    @Test
    fun historyStartIsShownAsOneLineWithoutDuplicateHeading() {
        val copy = AppCopy.Statistics.firstRecord(LocalDate.of(2026, 8, 1))

        assertEquals("首次记录：2026-08-01", copy)
        assertFalse(copy.contains("记录起点"))
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
            "填 0 表示明确没有，会保留记录。",
            AppCopy.Record.explicitZeroHint("明确没有"),
        )
    }
}
