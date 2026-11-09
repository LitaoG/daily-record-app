package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import io.github.litaog.dailyrecord.core.statistics.MonthStatistics
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens

@Composable
internal fun MonthDailyCountCard(
    month: MonthStatistics,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    val scale = remember(month) { monthDailyChartScale(month) }
    // dailyCount is localized: keying on it re-resolves the TalkBack string on
    // a language switch even where no activity recreation rebuilds this card.
    val description = remember(month, AppCopy.Statistics.dailyCount) {
        AppCopy.Statistics.monthDailyChartAccessibility(
            AppCopy.Components.joinSemantics(month.days.map { day ->
                AppCopy.Statistics.dayChartValue(
                    day = day.date.dayOfMonth,
                    count = day.count,
                    future = day.future,
                    recorded = day.recorded,
                )
            }),
        )
    }
    StatisticsSurface(
        modifier = modifier.testTag("month_daily_count_card"),
        title = AppCopy.Statistics.dailyCount,
        subtitle = AppCopy.Statistics.byDate,
        colors = colors,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(monthDailyChartHeight)
                .semantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MonthDailyChartAxis(scale)
            MonthDailyChartPlot(
                month = month,
                scale = scale,
                colors = colors,
                modifier = Modifier.weight(1f),
            )
        }
        MonthDailyXAxis(dayCount = month.days.size)
    }
}

@Composable
private fun MonthDailyChartAxis(scale: MonthDailyChartScale) {
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(monthDailyChartHeight),
    ) {
        val density = LocalDensity.current
        val plotTopPx = with(density) { monthDailyPlotTop.toPx() }
        val plotBottomPx = with(density) { monthDailyPlotBottom.toPx() }
        scale.ticks.forEach { tick ->
            val fraction = tick.toFloat() / scale.maximum.toFloat()
            val y = plotBottomPx - (plotBottomPx - plotTopPx) * fraction
            Text(
                text = tick.toString(),
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .offset(y = (with(density) { y.toDp() } - 6.dp).coerceAtLeast(0.dp))
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MonthDailyChartPlot(
    month: MonthStatistics,
    scale: MonthDailyChartScale,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    // Touch scrubbing (ADR-022): only press and release recompose the plot;
    // the probe itself is drawn from state read in the draw phase.
    val scrub = rememberChartScrubState()
    val scrubbing by remember(scrub) { derivedStateOf { scrub.activeIndex != null } }
    val probeAlpha by animateFloatAsState(
        targetValue = if (scrubbing) 1f else 0f,
        animationSpec = tween(durationMillis = if (scrubbing) 120 else 300),
        label = "monthProbeAlpha",
    )
    val dayCount = month.days.size
    val scrubInsetPx = with(LocalDensity.current) { monthDailyHorizontalInset.toPx() }
    BoxWithConstraints(
        modifier = modifier
            .height(monthDailyChartHeight)
            .testTag("month_daily_count_chart")
            .chartScrub(
                key = dayCount,
                indexAt = { x, width -> monthDailyChartIndexAt(x, dayCount, width, scrubInsetPx) },
                onScrub = scrub::update,
            ),
    ) {
        val density = LocalDensity.current
        val plotTop = monthDailyPlotTop
        val plotBottom = monthDailyPlotBottom
        val horizontalInset = monthDailyHorizontalInset
        val plotTopPx = with(density) { plotTop.toPx() }
        val plotBottomPx = with(density) { plotBottom.toPx() }
        val horizontalInsetPx = with(density) { horizontalInset.toPx() }
        val slotWidth = maxWidth / month.days.size
        val tickDays = monthDailyChartTickDays(month.days.size)
        val offsets = remember(month, scale, plotTopPx, plotBottomPx, horizontalInsetPx, constraints.maxWidth) {
            month.days.mapIndexed { index, day ->
                val count = day.count?.takeIf { day.recorded && !day.future }
                Offset(
                    x = monthDailyChartXPosition(
                        day = index + 1,
                        dayCount = month.days.size,
                        width = constraints.maxWidth.toFloat(),
                        inset = horizontalInsetPx,
                    ),
                    y = count?.let { value ->
                        val fraction = (value.toFloat() / scale.maximum.toFloat()).coerceIn(0f, 1f)
                        plotBottomPx - (plotBottomPx - plotTopPx) * fraction
                    } ?: plotBottomPx,
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
            scale.ticks.forEach { tick ->
                val fraction = tick.toFloat() / scale.maximum.toFloat()
                val y = plotBottomPx - (plotBottomPx - plotTopPx) * fraction
                drawLine(
                    color = DailyRecordDivider.copy(alpha = if (tick == 0L) .62f else .38f),
                    start = Offset(horizontalInsetPx, y),
                    end = Offset(size.width - horizontalInsetPx, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash,
                )
            }
            month.days.forEachIndexed { index, day ->
                val offset = offsets[index]
                when {
                    day.future -> drawCircle(
                        color = DailyRecordDivider.copy(alpha = .32f),
                        radius = 2.35.dp.toPx(),
                        center = Offset(offset.x, plotBottomPx),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                    !day.recorded -> drawCircle(
                        color = DailyRecordDivider.copy(alpha = .82f),
                        radius = 2.35.dp.toPx(),
                        center = Offset(offset.x, plotBottomPx),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                    day.count == 0L -> {
                        drawCircle(
                            color = colors.primary.copy(alpha = .16f),
                            radius = 5.dp.toPx(),
                            center = Offset(offset.x, plotBottomPx),
                        )
                        drawCircle(
                            color = colors.primary,
                            radius = 2.6.dp.toPx(),
                            center = Offset(offset.x, plotBottomPx),
                        )
                    }
                    else -> {
                        drawLine(
                            color = colors.primary,
                            start = Offset(offset.x, plotBottomPx),
                            end = offset,
                            strokeWidth = 1.6.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                        drawCircle(
                            color = colors.primary.copy(alpha = .15f),
                            radius = 6.dp.toPx(),
                            center = offset,
                        )
                        drawCircle(color = DailyRecordSurface, radius = 4.4.dp.toPx(), center = offset)
                        drawCircle(color = colors.primary, radius = 3.dp.toPx(), center = offset)
                        if (monthDailyChartNeedsNonAnchorBaselineDot(day, index + 1, tickDays)) {
                            drawCircle(
                                color = colors.primary,
                                radius = 1.7.dp.toPx(),
                                center = Offset(offset.x, plotBottomPx),
                            )
                        }
                    }
                }
            }

            // Key x-axis dates get a small accent ring and dot. This keeps the
            // baseline readable on a sparse chart and makes the date anchors
            // visually traceable without coloring every day.
            tickDays.forEach { dayOfMonth ->
                val index = dayOfMonth - 1
                val day = month.days[index]
                val alpha = when {
                    day.future -> .34f
                    !day.recorded -> .64f
                    else -> .92f
                }
                val baseline = Offset(offsets[index].x, plotBottomPx)
                drawCircle(
                    color = DailyRecordSurface,
                    radius = 5.5.dp.toPx(),
                    center = baseline,
                )
                drawCircle(
                    color = colors.primary.copy(alpha = alpha),
                    radius = 4.2.dp.toPx(),
                    center = baseline,
                    style = Stroke(width = 1.25.dp.toPx()),
                )
                drawCircle(
                    color = colors.primary.copy(alpha = alpha),
                    radius = 1.7.dp.toPx(),
                    center = baseline,
                )
            }

            // Scrub probe above the data. Unfilled, 0 and future days sit on
            // the baseline, which is where their offsets already point.
            val shown = scrub.shownIndex
            if (shown != null && shown in offsets.indices) {
                drawChartProbe(
                    x = offsets[shown].x,
                    pointY = offsets[shown].y,
                    topY = plotTopPx,
                    bottomY = plotBottomPx,
                    color = colors.primary,
                    alpha = probeAlpha,
                )
            }
        }

        month.days.forEachIndexed { index, day ->
            val count = day.count?.takeIf { day.recorded && !day.future && it > 0L } ?: return@forEachIndexed
            val labelY = with(density) { offsets[index].y.toDp() } - 16.dp
            Box(
                modifier = Modifier
                    .offset(x = slotWidth * index, y = labelY.coerceAtLeast(0.dp))
                    .width(slotWidth)
                    .height(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = count.toString(),
                    color = colors.primary,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }

        MonthScrubBubble(
            scrub = scrub,
            month = month,
            offsets = offsets,
            colors = colors,
            alpha = { probeAlpha },
        )
    }
}

/** Separate restart scope: moving between days recomposes only the bubble. */
@Composable
private fun MonthScrubBubble(
    scrub: ChartScrubState,
    month: MonthStatistics,
    offsets: List<Offset>,
    colors: RecordModuleColorTokens,
    alpha: () -> Float,
) {
    val shown = scrub.shownIndex ?: return
    val day = month.days.getOrNull(shown) ?: return
    val anchor = offsets.getOrNull(shown) ?: return
    ChartScrubBubble(
        text = AppCopy.Statistics.dayChartValue(
            day = day.date.dayOfMonth,
            count = day.count,
            future = day.future,
            recorded = day.recorded,
        ),
        anchorX = anchor.x,
        colors = colors,
        alpha = alpha,
    )
}

@Composable
private fun MonthDailyXAxis(dayCount: Int) {
    val tickDays = monthDailyChartTickDays(dayCount)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp)
            .height(18.dp),
    ) {
        val labelWidth = 24.dp
        val plotInset = 5.dp
        tickDays.forEach { day ->
            val center = monthDailyChartXPosition(
                day = day,
                dayCount = dayCount,
                width = maxWidth.value,
                inset = plotInset.value,
            ).dp
            // Do not clamp the first/last label into the box: clamping moves
            // the label's visual center away from its baseline anchor.
            val x = center - labelWidth / 2
            Text(
                text = day.toString(),
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.offset(x = x).width(labelWidth),
            )
        }
    }
}
