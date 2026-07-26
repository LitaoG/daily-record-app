package io.github.litaog.dailyrecord.ui.statistics

import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsPeriodCardsTest {
    @Test
    fun `recorded zero has no visible bar`() {
        val detail = StatisticsDetail(label = "周三 22日", count = 0L, days = 0, recorded = true)

        assertEquals(0f, distributionFraction(detail, maxCount = 4L, minNonZeroFraction = .16f))
        assertEquals(0f, distributionFraction(detail, maxCount = 4L, minNonZeroFraction = .08f))
    }

    @Test
    fun `unrecorded and future values have no visible bar`() {
        val unrecorded = StatisticsDetail(
            label = "周一 20日",
            count = null,
            days = null,
            recorded = false,
        )
        val future = StatisticsDetail(
            label = "周四 23日",
            count = null,
            days = null,
            future = true,
            recorded = false,
        )

        assertEquals(0f, distributionFraction(unrecorded, maxCount = 4L, minNonZeroFraction = .16f))
        assertEquals(0f, distributionFraction(future, maxCount = 4L, minNonZeroFraction = .16f))
    }

    @Test
    fun `positive values keep a minimum visible fraction and scale to the maximum`() {
        val one = StatisticsDetail(label = "周二 21日", count = 1L, days = 1)
        val maximum = StatisticsDetail(label = "周三 22日", count = 4L, days = 1)

        assertEquals(.25f, distributionFraction(one, maxCount = 4L, minNonZeroFraction = .16f))
        assertEquals(1f, distributionFraction(maximum, maxCount = 4L, minNonZeroFraction = .16f))
    }
}
