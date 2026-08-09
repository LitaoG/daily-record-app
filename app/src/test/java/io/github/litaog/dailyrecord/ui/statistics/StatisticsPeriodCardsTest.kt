package io.github.litaog.dailyrecord.ui.statistics

import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.SexColorTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
    fun `clipped week detail keeps its calendar segment index`() {
        val clippedThursday = StatisticsDetail(
            label = "周四 1日",
            count = 1L,
            days = 1,
            calendarIndex = 3,
        )

        assertEquals(3, weekRingSegmentIndex(clippedThursday, fallbackIndex = 0))
        assertEquals(
            0,
            weekRingSegmentIndex(
                clippedThursday.copy(calendarIndex = null),
                fallbackIndex = 0,
            ),
        )
    }

    @Test
    fun `non-positive states never receive an active ring intensity`() {
        val zero = StatisticsDetail("周一 20日", count = 0L, days = 0, recorded = true)
        val future = StatisticsDetail("周四 23日", count = null, days = null, future = true, recorded = false)

        assertEquals(0f, weekRingIntensity(zero, maxCount = 4L))
        assertEquals(0f, weekRingIntensity(future, maxCount = 4L))
    }

    @Test
    fun `legend colors match ring segment states for both modules`() {
        listOf(HandBrewColorTokens, SexColorTokens).forEach { colors ->
            val unrecorded = weekRingSegmentColor(WeekRingState.Unrecorded, 0f, colors)
            val zero = weekRingSegmentColor(WeekRingState.ExplicitZero, 0f, colors)
            val positive = weekRingSegmentColor(WeekRingState.Positive, 1f, colors)
            val future = weekRingSegmentColor(WeekRingState.Future, 0f, colors)

            // Explicit zero must not be shown with the unrecorded color.
            assertNotEquals(unrecorded, zero)
            // Zero keeps the module primary identity, distinct from positive fills.
            assertNotEquals(zero, positive)
            assertNotEquals(future, unrecorded)
            assertNotEquals(future, zero)
            // Unrecorded stays a neutral divider tone for both palettes.
            assertEquals(DailyRecordDivider.copy(alpha = .92f), unrecorded)
            // Zero resolves to the module primary, so both modules keep their identity.
            assertEquals(colors.primary.copy(alpha = .82f), zero)
        }
    }
}
