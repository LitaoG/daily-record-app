package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.statistics.YearStatistics
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens

@Composable
internal fun YearLineChartCard(
    year: YearStatistics,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    // The reveal animation must play once per year, not once per data change:
    // records (and therefore the YearStatistics instance) refresh on every
    // save or sync, but the year only changes when the user navigates. Scale
    // and points still follow the latest data.
    val yearNumber = remember(year) { year.months.firstOrNull()?.month?.year ?: 0 }
    val scale = remember(year) { yearLineChartScale(year) }
    val points = remember(year, scale) { yearLineChartPoints(year, scale) }
    // Scrub bubble text per month; annualCount is localized, so keying on it
    // re-resolves the labels after a language switch.
    val scrubLabels = remember(year, AppCopy.Statistics.annualCount) {
        year.months.map { month ->
            AppCopy.Statistics.monthChartLabel(
                month = month.month.monthValue,
                isFuture = month.future,
                recorded = month.recorded,
                count = month.count,
            )
        }
    }
    val revealProgress = remember(yearNumber) { Animatable(0f) }
    LaunchedEffect(yearNumber) {
        revealProgress.snapTo(0f)
        revealProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = YEAR_LINE_REVEAL_DURATION_MILLIS,
                delayMillis = YEAR_LINE_REVEAL_DELAY_MILLIS,
                easing = LinearOutSlowInEasing,
            ),
        )
    }
    StatisticsSurface(
        modifier = modifier,
        title = AppCopy.Statistics.annualCount,
        subtitle = AppCopy.Statistics.annualAverage(year.monthlyAverage),
        colors = colors,
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
                revealProgress = revealProgress,
                scrubLabels = scrubLabels,
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

/** Separate restart scope: moving between months recomposes only the bubble. */
@Composable
internal fun YearScrubBubble(
    scrub: ChartScrubState,
    labels: List<String>,
    chartWidthPx: Float,
    colors: RecordModuleColorTokens,
    alpha: () -> Float,
) {
    val shown = scrub.shownIndex ?: return
    val label = labels.getOrNull(shown) ?: return
    ChartScrubBubble(
        text = label,
        anchorX = chartWidthPx * (shown + .5f) / 12f,
        colors = colors,
        alpha = alpha,
    )
}
