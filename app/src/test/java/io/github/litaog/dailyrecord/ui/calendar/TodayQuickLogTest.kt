package io.github.litaog.dailyrecord.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodayQuickLogTest {
    @Test
    fun increaseStartsAnUnfilledDayAtOneAndCountsUp() {
        assertEquals(1, QuickLogAction.Increase.nextCount(null))
        assertEquals(1, QuickLogAction.Increase.nextCount(0))
        assertEquals(4, QuickLogAction.Increase.nextCount(3))
    }

    @Test
    fun increaseStopsAtTheIntegerLimit() {
        assertNull(QuickLogAction.Increase.nextCount(Int.MAX_VALUE))
    }

    @Test
    fun decreaseFromOneSavesAnExplicitZeroAndNeverGoesBelowZero() {
        // ADR-004: 0 is an explicit record, not a cleared day.
        assertEquals(0, QuickLogAction.Decrease.nextCount(1))
        assertEquals(2, QuickLogAction.Decrease.nextCount(3))
        assertNull(QuickLogAction.Decrease.nextCount(0))
        assertNull(QuickLogAction.Decrease.nextCount(null))
    }

    @Test
    fun markZeroOnlyFillsAnUnfilledDay() {
        assertEquals(0, QuickLogAction.MarkZero.nextCount(null))
        assertNull(QuickLogAction.MarkZero.nextCount(0))
        assertNull(QuickLogAction.MarkZero.nextCount(2))
    }

    @Test
    fun quickTapsAppliedInOrderCountEveryTap() {
        // The caller re-reads the stored count inside one lock before each
        // action, so three fast taps on an unfilled day end at 3, not 1.
        var stored: Int? = null
        repeat(3) { stored = QuickLogAction.Increase.nextCount(stored) ?: stored }
        assertEquals(3, stored)

        stored = QuickLogAction.Decrease.nextCount(stored) ?: stored
        assertEquals(2, stored)
    }
}
