package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens

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

@Composable
internal fun YearLineChartCard(
    year: YearStatistics,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    val scale = remember(year) { yearLineChartScale(year) }
    val points = remember(year, scale) { yearLineChartPoints(year, scale) }
    StatisticsSurface(
        modifier = modifier,
        title = AppCopy.Statistics.annualCount,
        subtitle = AppCopy.Statistics.annualAverage(year.monthlyAverage),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
                .semantics { contentDescription = yearLineChartDescription(year) },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            YearLineChartAxis(scale)
            YearLineChartPlot(
                points = points,
                scale = scale,
                colors = colors,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp),
        ) {
            points.forEach { point ->
                Text(
                    text = AppCopy.Statistics.monthLabel(point.monthNumber),
                    color = if (point.future) DailyRecordTextMuted else DailyRecordTextSecondary,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = AppCopy.Statistics.annualLineHint,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun YearLineChartAxis(scale: YearLineChartScale) {
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(156.dp),
    ) {
        val density = LocalDensity.current
        val plotTopPx = with(density) { 30.dp.toPx() }
        val plotBottomPx = with(density) { 140.dp.toPx() }
        scale.ticks.forEach { tick ->
            val fraction = tick.toFloat() / scale.maximum.toFloat()
            val y = plotBottomPx - (plotBottomPx - plotTopPx) * fraction
            Text(
                text = tick.toString(),
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .offset(y = (with(density) { y.toDp() } - 8.dp).coerceAtLeast(0.dp))
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun YearLineChartPlot(
    points: List<YearLineChartPoint>,
    scale: YearLineChartScale,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .height(156.dp)
            .testTag("year_line_chart"),
    ) {
        val density = LocalDensity.current
        val plotTop = 30.dp
        val plotBottom = 140.dp
        val plotTopPx = with(density) { plotTop.toPx() }
        val plotBottomPx = with(density) { plotBottom.toPx() }
        val chartWidthPx = constraints.maxWidth.toFloat()
        val monthWidth = maxWidth / 12
        val offsets = points.mapIndexed { index, point ->
            point.fraction?.let { fraction ->
                Offset(
                    x = chartWidthPx * (index + .5f) / 12f,
                    y = plotBottomPx - (plotBottomPx - plotTopPx) * fraction,
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridPathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(3.dp.toPx(), 4.dp.toPx()),
            )
            scale.ticks.forEach { tick ->
                val fraction = tick.toFloat() / scale.maximum.toFloat()
                val y = plotBottomPx - (plotBottomPx - plotTopPx) * fraction
                drawLine(
                    color = DailyRecordDivider.copy(alpha = if (tick == 0L) .62f else .38f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = gridPathEffect,
                )
            }

            offsets.contiguousSegments().forEach { segment ->
                if (segment.size > 1) {
                    val fillPath = smoothYearLinePath(segment).apply {
                        lineTo(segment.last().x, plotBottomPx)
                        lineTo(segment.first().x, plotBottomPx)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = .26f),
                                colors.primary.copy(alpha = .025f),
                            ),
                            startY = plotTopPx,
                            endY = plotBottomPx,
                        ),
                    )
                    drawPath(
                        path = smoothYearLinePath(segment),
                        color = colors.primary,
                        style = Stroke(width = 2.25.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }

            points.forEachIndexed { index, point ->
                val offset = offsets[index]
                if (offset == null) {
                    drawCircle(
                        color = DailyRecordDivider.copy(alpha = if (point.future) .35f else .72f),
                        radius = 3.dp.toPx(),
                        center = Offset(chartWidthPx * (index + .5f) / 12f, plotBottomPx),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                } else {
                    drawCircle(
                        color = colors.primary.copy(alpha = .14f),
                        radius = 7.dp.toPx(),
                        center = offset,
                    )
                    drawCircle(color = DailyRecordSurface, radius = 5.dp.toPx(), center = offset)
                    drawCircle(color = colors.primary, radius = 3.25.dp.toPx(), center = offset)
                }
            }
        }

        points.forEachIndexed { index, point ->
            val offset = offsets[index]
            if (point.count != null && offset != null) {
                val labelY = with(density) { offset.y.toDp() } - 27.dp
                Box(
                    modifier = Modifier
                        .offset(x = monthWidth * index, y = labelY.coerceAtLeast(0.dp))
                        .width(monthWidth)
                        .height(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = point.count.toString(),
                        color = colors.primary.copy(alpha = if (point.inProgress) .82f else 1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun List<Offset?>.contiguousSegments(): List<List<Offset>> = buildList {
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

private fun smoothYearLinePath(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, points.first().y)
    points.zipWithNext().forEach { (current, next) ->
        val middleX = (current.x + next.x) / 2f
        cubicTo(
            middleX,
            current.y,
            middleX,
            next.y,
            next.x,
            next.y,
        )
    }
}

private fun yearLineChartDescription(year: YearStatistics): String =
    AppCopy.Statistics.annualChartAccessibility(
        year.months.joinToString("，") { month ->
            AppCopy.Statistics.monthChartLabel(
                month = month.month.monthValue,
                isFuture = month.future,
                recorded = month.recorded,
                count = month.count,
            )
        },
    )
