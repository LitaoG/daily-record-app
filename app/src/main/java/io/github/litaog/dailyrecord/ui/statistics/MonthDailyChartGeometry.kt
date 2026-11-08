package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.statistics.MonthDayStatistics
import io.github.litaog.dailyrecord.core.statistics.MonthStatistics

internal data class MonthDailyChartScale(
    val maximum: Long,
    val ticks: List<Long>,
)

internal val monthDailyChartHeight = 164.dp
internal val monthDailyPlotTop = 24.dp
internal val monthDailyPlotBottom = 154.dp
internal val monthDailyHorizontalInset = 5.dp

/**
 * Stable date anchors used on the compact month chart's x-axis.
 *
 * The same anchors drive both the labels and the highlighted baseline markers so a
 * user can trace a pulse back to an exact day without adding a label for every date.
 */
internal fun monthDailyChartTickDays(dayCount: Int): List<Int> {
    if (dayCount <= 0) return emptyList()
    return listOf(1, 5, 10, 15, 20, 25, dayCount)
        .filter { it in 1..dayCount }
        .distinct()
}

internal fun monthDailyChartNeedsNonAnchorBaselineDot(
    day: MonthDayStatistics,
    dayOfMonth: Int,
    tickDays: List<Int>,
): Boolean = day.recorded && !day.future && (day.count ?: 0L) > 0L && dayOfMonth !in tickDays

/**
 * Returns the x-coordinate for a day using the same edge inset as the plot.
 * Keeping this calculation shared by the plot and labels keeps the first
 * tick aligned with its baseline marker.
 */
internal fun monthDailyChartXPosition(
    day: Int,
    dayCount: Int,
    width: Float,
    inset: Float,
): Float {
    if (dayCount <= 1) return inset
    val fraction = (day - 1).toFloat() / (dayCount - 1).toFloat()
    return inset + (width - inset * 2f).coerceAtLeast(0f) * fraction
}

internal fun monthDailyChartScale(month: MonthStatistics): MonthDailyChartScale {
    val maximumCount = month.days
        .asSequence()
        .filter { it.recorded && !it.future }
        .mapNotNull(MonthDayStatistics::count)
        .maxOrNull()
        ?.coerceAtLeast(1L) ?: 1L
    var step = 1L
    var phase = 0
    while ((maximumCount + step - 1L) / step > 4L) {
        step = when (phase) {
            0 -> step * 2L
            1 -> step * 5L / 2L
            else -> step * 2L
        }
        phase = (phase + 1) % 3
    }
    val maximum = ((maximumCount + step - 1L) / step) * step
    return MonthDailyChartScale(
        maximum = maximum,
        ticks = (0L..maximum step step).toList(),
    )
}
