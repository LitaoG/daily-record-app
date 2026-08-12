package io.github.litaog.dailyrecord.ui.statistics

import io.github.litaog.dailyrecord.core.statistics.StatisticsDetail
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.SexColorTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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
    fun `positive ring counts use fixed one two three and four plus bands`() {
        assertEquals(
            WeekRingCountBand.One,
            weekRingCountBand(StatisticsDetail("周一 20日", count = 1L, days = 1)),
        )
        assertEquals(
            WeekRingCountBand.Two,
            weekRingCountBand(StatisticsDetail("周二 21日", count = 2L, days = 1)),
        )
        assertEquals(
            WeekRingCountBand.Three,
            weekRingCountBand(StatisticsDetail("周三 22日", count = 3L, days = 1)),
        )
        assertEquals(
            WeekRingCountBand.FourPlus,
            weekRingCountBand(StatisticsDetail("周四 23日", count = 4L, days = 1)),
        )
        assertEquals(
            WeekRingCountBand.FourPlus,
            weekRingCountBand(StatisticsDetail("周五 24日", count = 12L, days = 1)),
        )
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
    fun `non-positive states do not receive a count color band`() {
        val zero = StatisticsDetail("周一 20日", count = 0L, days = 0, recorded = true)
        val future = StatisticsDetail("周四 23日", count = null, days = null, future = true, recorded = false)

        assertEquals(null, weekRingCountBand(zero))
        assertEquals(null, weekRingCountBand(future))
    }

    @Test
    fun `zero and unrecorded legend markers are hollow while future stays filled`() {
        assertEquals(
            WeekRingLegendMarkerStyle.HollowPrimary,
            weekRingLegendMarkerStyle(WeekRingState.ExplicitZero),
        )
        assertEquals(
            WeekRingLegendMarkerStyle.HollowNeutral,
            weekRingLegendMarkerStyle(WeekRingState.Unrecorded),
        )
        assertEquals(
            WeekRingLegendMarkerStyle.Filled,
            weekRingLegendMarkerStyle(WeekRingState.Future),
        )
        assertEquals(
            WeekRingLegendMarkerStyle.Filled,
            weekRingLegendMarkerStyle(WeekRingState.Positive),
        )
    }

    @Test
    fun `weekly recorded day count ignores explicit zero and unfilled days`() {
        val details = listOf(
            StatisticsDetail("周一 3日", count = 0L, days = 0, recorded = true),
            StatisticsDetail("周二 4日", count = 1L, days = 1, recorded = true),
            StatisticsDetail("周三 5日", count = null, days = null, recorded = false),
            StatisticsDetail("周四 6日", count = null, days = null, future = true, recorded = false),
        )

        assertEquals(1, weekRingPositiveDayCount(details))
    }

    @Test
    fun `weekday labels and their arcs share the same radial midpoint`() {
        repeat(7) { index ->
            assertEquals(index * 360f / 7f, weekRingLabelAngleDegrees(index), 0.001f)
            assertEquals(
                weekRingLabelAngleDegrees(index) - 90f,
                weekRingCanvasMidpointDegrees(index),
                0.001f,
            )
        }
    }

    @Test
    fun `all weekday labels reuse the side constrained clearance`() {
        val sharedGap = weekRingSharedLabelGapDp(
            ringOuterRadius = 92f,
            labelHalfWidth = 34f,
            labelHalfHeight = 22f,
            availableHalfWidth = 128f,
            availableHalfHeight = 130f,
            preferredGap = 12f,
        )
        val wednesdayAngle = weekRingLabelAngleDegrees(2)
        val angle = Math.toRadians(wednesdayAngle.toDouble())
        val wednesdayHorizontalLimit = 128f / abs(sin(angle).toFloat())
        val wednesdayZeroGapRadius = weekRingLabelRadialDistance(
            angleDegrees = wednesdayAngle,
            ringOuterRadius = 92f,
            labelHalfWidth = 34f,
            labelHalfHeight = 22f,
            gap = 0f,
        )

        assertEquals(
            (wednesdayHorizontalLimit - wednesdayZeroGapRadius).coerceAtLeast(0f),
            sharedGap,
            0.001f,
        )
        assertTrue(sharedGap < 12f)

        repeat(7) { index ->
            val angleDegrees = weekRingLabelAngleDegrees(index)
            val labelAngle = Math.toRadians(angleDegrees.toDouble())
            val angleSin = abs(sin(labelAngle).toFloat())
            val angleCos = abs(cos(labelAngle).toFloat())
            val horizontalLimit = if (angleSin > .001f) 128f / angleSin else Float.POSITIVE_INFINITY
            val verticalLimit = if (angleCos > .001f) 130f / angleCos else Float.POSITIVE_INFINITY
            val zeroGapRadius = weekRingLabelRadialDistance(
                angleDegrees = angleDegrees,
                ringOuterRadius = 92f,
                labelHalfWidth = 34f,
                labelHalfHeight = 22f,
                gap = 0f,
            )
            val resolvedRadius = minOf(
                weekRingLabelRadialDistance(
                    angleDegrees = angleDegrees,
                    ringOuterRadius = 92f,
                    labelHalfWidth = 34f,
                    labelHalfHeight = 22f,
                    gap = sharedGap,
                ),
                horizontalLimit,
                verticalLimit,
            )

            assertEquals("weekday index $index", sharedGap, resolvedRadius - zeroGapRadius, 0.001f)
        }

        assertEquals(
            12f,
            weekRingSharedLabelGapDp(
                ringOuterRadius = 92f,
                labelHalfWidth = 34f,
                labelHalfHeight = 22f,
                availableHalfWidth = 200f,
                availableHalfHeight = 200f,
                preferredGap = 12f,
            ),
            0.001f,
        )
    }

    @Test
    fun `count bands keep four visible colors independent`() {
        val colors = WeekRingCountBand.entries.map { weekRingColorForBand(it, HandBrewColorTokens) }

        assertEquals(4, colors.distinct().size)
        assertNotEquals(colors[0], colors[1])
        assertNotEquals(colors[1], colors[2])
        assertNotEquals(colors[2], colors[3])
    }

    @Test
    fun `label radial distance compensates for top and side text boxes`() {
        val top = weekRingLabelRadialDistance(
            angleDegrees = 0f,
            ringOuterRadius = 100f,
            labelHalfWidth = 34f,
            labelHalfHeight = 22f,
            gap = 0f,
        )
        val side = weekRingLabelRadialDistance(
            angleDegrees = 90f,
            ringOuterRadius = 100f,
            labelHalfWidth = 34f,
            labelHalfHeight = 22f,
            gap = 0f,
        )

        assertEquals(122f, top, 0.001f)
        assertEquals(134f, side, 0.001f)
        assertTrue(side > top)
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
            assertEquals(colors.primary, zero)
        }
    }
}
