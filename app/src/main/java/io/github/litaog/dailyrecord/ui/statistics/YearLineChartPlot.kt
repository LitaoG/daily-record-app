package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens

@Composable
internal fun YearLineChartPlot(
    points: List<YearLineChartPoint>,
    scale: YearLineChartScale,
    colors: RecordModuleColorTokens,
    revealProgress: Animatable<Float, AnimationVector1D>,
    scrubLabels: List<String>,
    modifier: Modifier = Modifier,
) {
    // Touch scrubbing (ADR-022): only press and release recompose the plot;
    // the probe itself is drawn from state read in the draw phase.
    val scrub = rememberChartScrubState()
    val scrubbing by remember(scrub) { derivedStateOf { scrub.activeIndex != null } }
    val probeAlpha by animateFloatAsState(
        targetValue = if (scrubbing) 1f else 0f,
        animationSpec = tween(durationMillis = if (scrubbing) 120 else 300),
        label = "yearProbeAlpha",
    )
    val monthCount = points.size
    BoxWithConstraints(
        modifier = modifier
            .height(156.dp)
            .testTag("year_line_chart")
            .chartScrub(
                key = monthCount,
                indexAt = { x, width -> yearLineChartIndexAt(x, width, monthCount) },
                onScrub = scrub::update,
            ),
    ) {
        val density = LocalDensity.current
        val plotTop = 30.dp
        val plotBottom = 140.dp
        val plotTopPx = with(density) { plotTop.toPx() }
        val plotBottomPx = with(density) { plotBottom.toPx() }
        val chartWidthPx = constraints.maxWidth.toFloat()
        val monthWidth = maxWidth / 12
        val offsets = remember(points, plotTopPx, plotBottomPx, chartWidthPx) {
            points.mapIndexed { index, point ->
                point.fraction?.let { fraction ->
                    Offset(
                        x = chartWidthPx * (index + .5f) / 12f,
                        y = plotBottomPx - (plotBottomPx - plotTopPx) * fraction,
                    )
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val reveal = revealProgress.value.coerceIn(0f, 1f)
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

            points.forEachIndexed { index, point ->
                val offset = offsets[index]
                if (offset == null) {
                    drawCircle(
                        color = DailyRecordDivider.copy(alpha = if (point.future) .35f else .72f),
                        radius = 3.dp.toPx(),
                        center = Offset(chartWidthPx * (index + .5f) / 12f, plotBottomPx),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }

            val revealRight = size.width * reveal
            if (revealRight > 0f) {
                clipRect(right = revealRight) {
                    offsets.contiguousSegments().forEach { segment ->
                        if (segment.size > 1) {
                            val fillPath = straightYearLinePath(segment).apply {
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
                                path = straightYearLinePath(segment),
                                color = colors.primary,
                                style = Stroke(width = 2.25.dp.toPx(), cap = StrokeCap.Round),
                            )
                        }
                    }

                    points.forEachIndexed { index, _ ->
                        val offset = offsets[index] ?: return@forEachIndexed
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

            // Scrub probe above the data; months without a value sit on the baseline.
            val shown = scrub.shownIndex
            if (shown != null && shown in points.indices) {
                drawChartProbe(
                    x = chartWidthPx * (shown + .5f) / 12f,
                    pointY = offsets[shown]?.y ?: plotBottomPx,
                    topY = plotTopPx,
                    bottomY = plotBottomPx,
                    color = colors.primary,
                    alpha = probeAlpha,
                )
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
                        .height(20.dp)
                        .graphicsLayer {
                            alpha = yearLineChartPointRevealAlpha(index, revealProgress.value)
                        },
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

        YearScrubBubble(
            scrub = scrub,
            labels = scrubLabels,
            chartWidthPx = chartWidthPx,
            colors = colors,
            alpha = { probeAlpha },
        )
    }
}
