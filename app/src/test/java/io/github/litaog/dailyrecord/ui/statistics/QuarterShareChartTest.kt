package io.github.litaog.dailyrecord.ui.statistics

import org.junit.Assert.assertEquals
import org.junit.Test

class QuarterShareChartTest {
    @Test
    fun singlePositiveQuarterClosesTheRing() {
        assertEquals(0f, quarterShareGapDegrees(positiveQuarterCount = 1), 0f)
    }

    @Test
    fun multiplePositiveQuartersKeepAThinSeparator() {
        assertEquals(3f, quarterShareGapDegrees(positiveQuarterCount = 2), 0f)
        assertEquals(3f, quarterShareGapDegrees(positiveQuarterCount = 4), 0f)
    }
}
