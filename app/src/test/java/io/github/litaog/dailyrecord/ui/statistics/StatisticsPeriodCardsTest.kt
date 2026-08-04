package io.github.litaog.dailyrecord.ui.statistics

import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsPeriodCardsTest {
    @Test
    fun `ring state keeps zero, unfilled and future distinct`() {
        assertEquals(
            WeekRingState.ExplicitZero,
            weekRingState(StatisticsDetail("周三 22日", count = 0L, days = 0, recorded = true)),
        )
        assertEquals(
            WeekRingState.Unrecorded,
            weekRingState(StatisticsDetail("周一 20日", count = null, days = null, recorded = false)),
        )
        assertEquals(
            WeekRingState.Future,
            weekRingState(
                StatisticsDetail(
                    "周四 23日",
                    count = null,
                    days = null,
                    future = true,
                    recorded = false,
                ),
            ),
        )
    }

    @Test
    fun `positive ring intensity scales to maximum`() {
        val one = StatisticsDetail("周二 21日", count = 1L, days = 1)
        val maximum = StatisticsDetail("周三 22日", count = 4L, days = 1)

        assertEquals(.25f, weekRingIntensity(one, maxCount = 4L))
        assertEquals(1f, weekRingIntensity(maximum, maxCount = 4L))
    }

    @Test
    fun `non-positive states never receive an active ring intensity`() {
        val zero = StatisticsDetail("周一 20日", count = 0L, days = 0, recorded = true)
        val future = StatisticsDetail("周四 23日", count = null, days = null, future = true, recorded = false)

        assertEquals(0f, weekRingIntensity(zero, maxCount = 4L))
        assertEquals(0f, weekRingIntensity(future, maxCount = 4L))
    }
}
