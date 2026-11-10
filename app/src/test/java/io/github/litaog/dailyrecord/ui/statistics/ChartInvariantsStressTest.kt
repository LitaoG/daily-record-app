package io.github.litaog.dailyrecord.ui.statistics

import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.core.common.AppLanguageState
import io.github.litaog.dailyrecord.core.common.TerminologyStyle
import io.github.litaog.dailyrecord.core.common.strings
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.statistics.EARLIEST_SUPPORTED_DATE
import io.github.litaog.dailyrecord.core.statistics.StatisticsDetail
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.core.statistics.QuarterStatistics
import io.github.litaog.dailyrecord.core.statistics.YearStatistics
import io.github.litaog.dailyrecord.core.statistics.buildDailyCountStatistics
import io.github.litaog.dailyrecord.core.statistics.nextPeriodAnchor
import io.github.litaog.dailyrecord.core.statistics.previousPeriodAnchor
import java.time.LocalDate
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Invariant stress tests for the statistics engine's period switching, anchor
 * navigation, scale boundaries and the bilingual accessibility copy matrix
 * (JVM unit tests that call production functions directly; no instrumentation).
 */
class ChartInvariantsStressTest {

    @Test
    fun rapidPeriodSwitchingMaintainsDeterministicInvariantsUnderStress() {
        val random = Random(42)
        val today = LocalDate.of(2026, 11, 8)
        val earliest = EARLIEST_SUPPORTED_DATE

        // Generate diverse synthetic record sets:
        // empty, single record, sparse, dense, leap year, extreme values
        val testRecordSets = listOf(
            emptyList(),
            listOf(DailyCountEntry(today, 1)),
            listOf(DailyCountEntry(today, 0)),
            listOf(DailyCountEntry(today.minusDays(1), 5), DailyCountEntry(today, 3)),
            (0..30).map { DailyCountEntry(today.minusDays(it.toLong()), (it % 4).toInt()) },
            // Leap year 2024 records
            listOf(
                DailyCountEntry(LocalDate.of(2024, 2, 28), 2),
                DailyCountEntry(LocalDate.of(2024, 2, 29), 4),
                DailyCountEntry(LocalDate.of(2024, 3, 1), 1),
            ),
            // High count records
            listOf(
                DailyCountEntry(today.minusDays(10), 999),
                DailyCountEntry(today.minusDays(5), 10_000),
            ),
        )

        val anchorDates = listOf(
            today,
            today.minusWeeks(2),
            today.minusMonths(6),
            LocalDate.of(2024, 2, 29), // Leap day
            LocalDate.of(2026, 1, 1),   // Year boundary
            LocalDate.of(2026, 12, 31), // Year end
            earliest,
        )

        val periods = StatisticsPeriod.entries

        // Perform 2,000 rapid randomized transitions across all 4 periods
        for (i in 0 until 2_000) {
            val period = periods[random.nextInt(periods.size)]
            val anchor = anchorDates[random.nextInt(anchorDates.size)]
            val records = testRecordSets[random.nextInt(testRecordSets.size)]

            val model = buildDailyCountStatistics(
                period = period,
                anchorDate = anchor,
                today = today,
                records = records,
                earliestDate = earliest,
            )

            // Assert fundamental mathematical invariants
            assertTrue("Iteration $i: totalCount must be non-negative", model.summary.totalCount >= 0L)
            assertTrue("Iteration $i: recordedDays must be non-negative", model.summary.recordedDays >= 0)
            assertTrue("Iteration $i: average must be non-negative", model.summary.average >= 0.0)
            assertFalse("Iteration $i: average must not be NaN", model.summary.average.isNaN())
            assertFalse("Iteration $i: average must not be infinite", model.summary.average.isInfinite())
            assertFalse("Iteration $i: title must not be blank", model.title.isBlank())
            assertFalse("Iteration $i: status must not be blank", model.status.isBlank())

            // Period-specific structural invariants
            when (period) {
                StatisticsPeriod.Week -> {
                    assertTrue("Week details must not exceed 7 days", model.details.size <= 7)
                    assertNull("Week period must not populate MonthStatistics", model.month)
                    assertNull("Week period must not populate YearStatistics", model.year)
                }
                StatisticsPeriod.Month -> {
                    assertNotNull("Month period must populate MonthStatistics", model.month)
                    val monthDays = model.month!!.days
                    assertTrue(
                        "Month days count (${monthDays.size}) must be between 28 and 31",
                        monthDays.size in 28..31,
                    )
                    assertNull("Month period must not populate YearStatistics", model.year)
                }
                StatisticsPeriod.Year -> {
                    assertNotNull("Year period must populate YearStatistics", model.year)
                    assertEquals("Year period must contain exactly 12 months", 12, model.year!!.months.size)
                    assertEquals("Year period must contain exactly 4 quarters", 4, model.year!!.quarters.size)
                    assertNull("Year period must not populate MonthStatistics", model.month)
                }
                StatisticsPeriod.All -> {
                    assertNull("All period must not populate MonthStatistics", model.month)
                    assertNull("All period must not populate YearStatistics", model.year)
                }
            }
        }
    }

    @Test
    fun periodAnchorsNavigationMaintainsStrictMonotonicity() {
        val today = LocalDate.of(2026, 11, 8)
        val earliest = EARLIEST_SUPPORTED_DATE

        StatisticsPeriod.entries.filter { it != StatisticsPeriod.All }.forEach { period ->
            var current = today
            // Navigate backward 20 steps
            repeat(20) {
                val previous = previousPeriodAnchor(period, current, earliest)
                if (previous != null) {
                    assertTrue(
                        "$period: previous anchor ($previous) must be before current ($current)",
                        previous.isBefore(current),
                    )
                    assertTrue(
                        "$period: previous anchor ($previous) must not precede earliest ($earliest)",
                        !previous.isBefore(earliest),
                    )
                    current = previous
                }
            }

            // Navigate forward back toward today
            repeat(20) {
                val next = nextPeriodAnchor(period, current, today)
                if (next != null) {
                    assertTrue(
                        "$period: next anchor ($next) must be after current ($current)",
                        next.isAfter(current),
                    )
                    assertTrue(
                        "$period: next anchor ($next) must not exceed today ($today)",
                        !next.isAfter(today),
                    )
                    current = next
                }
            }
        }
    }

    @Test
    fun scalesHandleZeroAndExtremeCountsWithoutDivisionByZeroOrOverflow() {
        val today = LocalDate.of(2026, 7, 15)

        // 1. Month chart scale with zero records
        val emptyMonth = buildDailyCountStatistics(
            period = StatisticsPeriod.Month,
            anchorDate = today,
            today = today,
            records = emptyList(),
        ).month!!
        val emptyMonthScale = monthDailyChartScale(emptyMonth)
        assertEquals(1L, emptyMonthScale.maximum)
        assertEquals(listOf(0L, 1L), emptyMonthScale.ticks)

        // 2. Month chart scale with huge extreme count (100,000)
        val extremeMonth = buildDailyCountStatistics(
            period = StatisticsPeriod.Month,
            anchorDate = today,
            today = today,
            records = listOf(DailyCountEntry(today, 100_000)),
        ).month!!
        val extremeMonthScale = monthDailyChartScale(extremeMonth)
        assertTrue("Scale maximum (${extremeMonthScale.maximum}) must >= 100000", extremeMonthScale.maximum >= 100_000L)
        assertTrue("Ticks must start with 0", extremeMonthScale.ticks.first() == 0L)
        assertTrue("Ticks must end with maximum", extremeMonthScale.ticks.last() == extremeMonthScale.maximum)
        assertTrue("Ticks must be strictly monotonically increasing", extremeMonthScale.ticks.zipWithNext().all { it.first < it.second })

        // 3. Year chart scale with zero records
        val emptyYear = buildDailyCountStatistics(
            period = StatisticsPeriod.Year,
            anchorDate = today,
            today = today,
            records = emptyList(),
        ).year!!
        val emptyYearScale = yearLineChartScale(emptyYear)
        assertEquals(1L, emptyYearScale.maximum)
        assertEquals(listOf(0L, 1L), emptyYearScale.ticks)

        // 4. Year chart scale with single count = 1L
        val singleYear = buildDailyCountStatistics(
            period = StatisticsPeriod.Year,
            anchorDate = today,
            today = today,
            records = listOf(DailyCountEntry(today, 1)),
        ).year!!
        val singleYearScale = yearLineChartScale(singleYear)
        assertEquals(1L, singleYearScale.maximum)
        assertEquals(listOf(0L, 1L), singleYearScale.ticks)

        // 5. Year chart scale with extreme count = 50,000L
        val extremeYear = buildDailyCountStatistics(
            period = StatisticsPeriod.Year,
            anchorDate = today,
            today = today,
            records = listOf(DailyCountEntry(today, 50_000)),
        ).year!!
        val extremeYearScale = yearLineChartScale(extremeYear)
        assertTrue(extremeYearScale.maximum >= 50_000L)
        assertTrue(extremeYearScale.ticks.size in 3..6)
    }

    @Test
    fun quarterShareGapDegreesHandlesZeroSingleAndMultiplePositiveQuarters() {
        // Zero positive quarters: 0f
        assertEquals(0f, quarterShareGapDegrees(0), 0.001f)
        // Single positive quarter: 0f (closes the circle without a false gap)
        assertEquals(0f, quarterShareGapDegrees(1), 0.001f)
        // Multiple positive quarters: 3f separator gap
        assertEquals(3f, quarterShareGapDegrees(2), 0.001f)
        assertEquals(3f, quarterShareGapDegrees(3), 0.001f)
        assertEquals(3f, quarterShareGapDegrees(4), 0.001f)
    }

    @Test
    fun leapYearFebruaryGeneratesCorrectDayCountAndDateAnchors() {
        val leapDate = LocalDate.of(2024, 2, 15)
        val nonLeapDate = LocalDate.of(2023, 2, 15)

        val leapMonth = buildDailyCountStatistics(
            period = StatisticsPeriod.Month,
            anchorDate = leapDate,
            today = LocalDate.of(2026, 11, 8),
            records = emptyList(),
        ).month!!
        assertEquals("Feb 2024 must have 29 days", 29, leapMonth.days.size)
        assertEquals(
            listOf(1, 5, 10, 15, 20, 25, 29),
            monthDailyChartTickDays(leapMonth.days.size),
        )

        val nonLeapMonth = buildDailyCountStatistics(
            period = StatisticsPeriod.Month,
            anchorDate = nonLeapDate,
            today = LocalDate.of(2026, 11, 8),
            records = emptyList(),
        ).month!!
        assertEquals("Feb 2023 must have 28 days", 28, nonLeapMonth.days.size)
        assertEquals(
            listOf(1, 5, 10, 15, 20, 25, 28),
            monthDailyChartTickDays(nonLeapMonth.days.size),
        )
    }

    @Test
    fun accessibilityDescriptionsCoverAllStatisticsComponentsInChineseAndEnglish() {
        val previousStrings = AppLanguageState.current
        try {
            val configurations = listOf(
                AppLanguage.ZH to TerminologyStyle.ELEGANT,
                AppLanguage.ZH to TerminologyStyle.DIRECT,
                AppLanguage.EN to TerminologyStyle.ELEGANT,
            )

            for ((lang, style) in configurations) {
                AppLanguageState.current = lang.strings(style)

                // 1. StatisticsSummaryCard merged semantics
                val summaryDescription = AppCopy.Components.joinSemantics(
                    AppCopy.Statistics.periodCountLabel("本周", "记录"),
                    AppCopy.Statistics.countText(15L),
                    AppCopy.Statistics.recordedDaysLabel,
                    AppCopy.Statistics.daysText(5),
                    AppCopy.Statistics.averageLabel,
                    AppCopy.Statistics.average(3.0),
                )
                assertFalse(summaryDescription.isBlank())
                assertTrue(summaryDescription.contains("15"))
                assertTrue(summaryDescription.contains("5"))

                // 2. MonthSummaryCard semantics
                val monthAccessibility = AppCopy.Statistics.monthSummaryAccessibility(
                    totalCount = 20L,
                    recordedDays = 12,
                    average = 1.67,
                )
                assertFalse(monthAccessibility.isBlank())
                assertTrue(monthAccessibility.contains("20"))

                // 3. Week distribution detail semantics across states
                val positiveDetail = StatisticsDetail("周一 1日", count = 3L, days = 1, recorded = true)
                val zeroDetail = StatisticsDetail("周二 2日", count = 0L, days = 0, recorded = true)
                val unfilledDetail = StatisticsDetail("周三 3日", count = null, days = null, recorded = false)
                val futureDetail = StatisticsDetail("周四 4日", count = null, days = null, future = true, recorded = false)

                listOf(positiveDetail, zeroDetail, unfilledDetail, futureDetail).forEach { detail ->
                    val desc = when {
                        detail.future -> AppCopy.Components.joinSemantics(detail.label, AppCopy.Statistics.future, AppCopy.Statistics.dash)
                        !detail.recorded -> AppCopy.Components.joinSemantics(detail.label, AppCopy.Statistics.unset, AppCopy.Statistics.dash)
                        else -> AppCopy.Components.joinSemantics(detail.label, AppCopy.Statistics.countText(detail.count ?: 0L), AppCopy.Statistics.daysText(detail.days ?: 0))
                    }
                    assertFalse("Description for $detail must not be blank", desc.isBlank())
                    assertTrue("Description must contain label ${detail.label}", desc.contains(detail.label))
                }

                // 4. Quarter share summary description
                val testYear = YearStatistics(
                    months = emptyList(),
                    quarters = listOf(
                        QuarterStatistics(1, 10L),
                        QuarterStatistics(2, 20L),
                        QuarterStatistics(3, 0L),
                        QuarterStatistics(4, 30L),
                    ),
                    monthlyAverage = 5.0,
                    maximumMonths = emptyList(),
                    minimumMonths = emptyList(),
                )
                val quarterDesc = AppCopy.Statistics.totalCountAccessibility(
                    60L,
                    AppCopy.Components.joinSemantics(testYear.quarters.map {
                        "${AppCopy.Statistics.quarterLabel(it.quarter)} ${AppCopy.Statistics.percentage(it.totalCount * 100.0 / 60.0)}"
                    }),
                )
                assertFalse(quarterDesc.isBlank())
                assertTrue(quarterDesc.contains("60"))

                // 5. Period action navigation semantics
                val prevAction = AppCopy.Statistics.periodAction("周", previous = true)
                val nextAction = AppCopy.Statistics.periodAction("周", previous = false)
                assertFalse(prevAction.isBlank())
                assertFalse(nextAction.isBlank())
                assertNotEquals(prevAction, nextAction)
            }
        } finally {
            AppLanguageState.current = previousStrings
        }
    }
}
