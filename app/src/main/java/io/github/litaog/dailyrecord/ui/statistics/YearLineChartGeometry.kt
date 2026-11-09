package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.statistics.YearStatistics

internal data class YearLineChartPoint(
    val monthNumber: Int,
    val count: Long?,
    val fraction: Float?,
    val future: Boolean,
    val inProgress: Boolean,
)

internal data class YearLineChartScale(
    val maximum: Long,
    val ticks: List<Long>,
)

internal const val YEAR_LINE_REVEAL_DURATION_MILLIS = 1500
internal const val YEAR_LINE_REVEAL_DELAY_MILLIS = 70
private const val YEAR_LINE_LABEL_FADE_SPAN = .04f

internal fun yearLineChartScale(year: YearStatistics): YearLineChartScale {
    val maximumCount = year.months
        .asSequence()
        .filter { it.recorded && !it.future }
        .mapNotNull { it.count }
        .maxOrNull()
        ?.coerceAtLeast(1L) ?: 1L
    var step = 1L
    var phase = 0
    while (step * 4L < maximumCount) {
        step = when (phase) {
            0 -> step * 2L
            1 -> step * 5L / 2L
            else -> step * 2L
        }
        phase = (phase + 1) % 3
    }
    val maximum = ((maximumCount + step - 1L) / step) * step
    return YearLineChartScale(
        maximum = maximum,
        ticks = (0L..maximum step step).toList(),
    )
}

internal fun yearLineChartPoints(
    year: YearStatistics,
    scale: YearLineChartScale = yearLineChartScale(year),
): List<YearLineChartPoint> = year.months.map { month ->
    val count = month.count.takeIf { month.recorded && !month.future }
    YearLineChartPoint(
        monthNumber = month.month.monthValue,
        count = count,
        fraction = count?.let { (it.toFloat() / scale.maximum.toFloat()).coerceIn(0f, 1f) },
        future = month.future,
        inProgress = month.inProgress,
    )
}

internal fun yearLineChartPointRevealAlpha(
    monthIndex: Int,
    revealProgress: Float,
): Float {
    require(monthIndex in 0 until 12)
    val pointPosition = (monthIndex + .5f) / 12f
    return ((revealProgress.coerceIn(0f, 1f) - pointPosition) / YEAR_LINE_LABEL_FADE_SPAN)
        .coerceIn(0f, 1f)
}

internal fun List<Offset?>.contiguousSegments(): List<List<Offset>> = buildList {
    var current = mutableListOf<Offset>()
    this@contiguousSegments.forEach { point ->
        if (point == null) {
            if (current.isNotEmpty()) {
                add(current)
                current = mutableListOf()
            }
        } else {
            current += point
        }
    }
    if (current.isNotEmpty()) add(current)
}

internal fun straightYearLinePath(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, points.first().y)
    points.drop(1).forEach { point ->
        lineTo(point.x, point.y)
    }
}

internal fun yearLineChartDescription(year: YearStatistics): String =
    AppCopy.Statistics.annualChartAccessibility(
        AppCopy.Components.joinSemantics(
            year.months.map { month ->
                AppCopy.Statistics.monthChartLabel(
                    month = month.month.monthValue,
                    isFuture = month.future,
                    recorded = month.recorded,
                    count = month.count,
                )
            },
        ),
    )
