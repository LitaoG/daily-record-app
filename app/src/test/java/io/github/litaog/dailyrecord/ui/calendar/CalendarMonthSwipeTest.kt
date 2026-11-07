package io.github.litaog.dailyrecord.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarMonthSwipeTest {
    private val threshold = 64f

    @Test
    fun rightSwipePastTheThresholdShowsThePreviousMonth() {
        assertEquals(MonthSwipe.Previous, monthSwipeFor(64f, threshold, canGoPrevious = true, canGoNext = true))
        assertEquals(MonthSwipe.Previous, monthSwipeFor(200f, threshold, canGoPrevious = true, canGoNext = false))
    }

    @Test
    fun leftSwipePastTheThresholdShowsTheNextMonth() {
        assertEquals(MonthSwipe.Next, monthSwipeFor(-64f, threshold, canGoPrevious = true, canGoNext = true))
        assertEquals(MonthSwipe.Next, monthSwipeFor(-200f, threshold, canGoPrevious = false, canGoNext = true))
    }

    @Test
    fun shortDragsDoNotChangeMonth() {
        assertNull(monthSwipeFor(0f, threshold, canGoPrevious = true, canGoNext = true))
        assertNull(monthSwipeFor(63.9f, threshold, canGoPrevious = true, canGoNext = true))
        assertNull(monthSwipeFor(-63.9f, threshold, canGoPrevious = true, canGoNext = true))
    }

    @Test
    fun swipesTowardAMonthOutOfRangeDoNothing() {
        // January 1970 has no previous month; the current month has no next one.
        assertNull(monthSwipeFor(200f, threshold, canGoPrevious = false, canGoNext = true))
        assertNull(monthSwipeFor(-200f, threshold, canGoPrevious = true, canGoNext = false))
    }

    @Test
    fun gridFollowsHalfTheDragTowardAnAvailableMonth() {
        assertEquals(20f, monthSwipeFollowPx(40f, maxFollowPx = 72f, towardAvailableMonth = true), 0.001f)
        assertEquals(-20f, monthSwipeFollowPx(-40f, maxFollowPx = 72f, towardAvailableMonth = true), 0.001f)
    }

    @Test
    fun gridOnlyRubberBandsTowardAnUnavailableMonthAndNeverPassesTheLimit() {
        assertEquals(6f, monthSwipeFollowPx(40f, maxFollowPx = 72f, towardAvailableMonth = false), 0.001f)
        assertEquals(72f, monthSwipeFollowPx(1000f, maxFollowPx = 72f, towardAvailableMonth = true), 0.001f)
        assertEquals(-72f, monthSwipeFollowPx(-1000f, maxFollowPx = 72f, towardAvailableMonth = true), 0.001f)
    }
}
