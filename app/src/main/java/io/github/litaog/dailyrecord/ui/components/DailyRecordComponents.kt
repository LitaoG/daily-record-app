package io.github.litaog.dailyrecord.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.TopDestination
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.RecordModuleUiSpec
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import io.github.litaog.dailyrecord.ui.theme.DailyRecordBorders
import io.github.litaog.dailyrecord.ui.theme.DailyRecordElevations
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordPeriodInactiveText
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.MetricNumberMedium
import io.github.litaog.dailyrecord.ui.theme.DailyRecordOnAccent
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.periodGlassGlow
import io.github.litaog.dailyrecord.ui.theme.periodGlassTint

enum class StatisticsPeriod(val label: String) {
    Week(AppCopy.Statistics.weekTab),
    Month(AppCopy.Statistics.monthTab),
    Year(AppCopy.Statistics.yearTab),
    All(AppCopy.Statistics.allTab),
}

@Composable
internal fun DailyRecordBottomBar(
    selected: TopDestination,
    colors: RecordModuleColorTokens,
    onSelected: (TopDestination) -> Unit,
) {
    Surface(
        modifier = Modifier.navigationBarsPadding(),
        color = DailyRecordSurface,
        shadowElevation = DailyRecordElevations.Flat,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DailyRecordSizes.BottomBarMinHeight)
                .drawWithContent {
                    drawContent()
                    drawLine(
                        color = DailyRecordDivider,
                        start = Offset.Zero,
                        end = Offset(size.width, 0f),
                        strokeWidth = DailyRecordBorders.Standard.toPx(),
                    )
                }
                .padding(
                    horizontal = DailyRecordSpacing.ScreenHorizontal,
                    vertical = DailyRecordSpacing.Inline,
                ),
            horizontalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Inline),
        ) {
            BottomDestination(
                label = AppCopy.NavigationBar.calendar,
                selected = selected == TopDestination.Calendar,
                accent = colors.primary,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(TopDestination.Calendar) },
                icon = { color -> CalendarGlyph(color) },
            )
            BottomDestination(
                label = AppCopy.NavigationBar.statistics,
                selected = selected == TopDestination.Statistics,
                accent = colors.primary,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(TopDestination.Statistics) },
                icon = { color -> StatisticsGlyph(color) },
            )
        }
    }
}

@Composable
private fun BottomDestination(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    val contentColor = if (selected) accent else DailyRecordTextSecondary
    Column(
        modifier = modifier
            .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
            .testTag("bottom_destination_$label")
            .clip(DailyRecordShapes.Control)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                this.selected = selected
                role = Role.Tab
                contentDescription = AppCopy.selectedState(label, selected)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon(contentColor)
        Text(text = label, color = contentColor, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(DailyRecordSpacing.Compact))
        Box(
            Modifier
                .width(24.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(if (selected) accent else Color.Transparent),
        )
    }
}

@Composable
fun CalendarGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val stroke = 2.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * .18f, size.height * .22f),
            size = Size(size.width * .64f, size.height * .62f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(stroke),
        )
        drawLine(color, Offset(size.width * .18f, size.height * .40f), Offset(size.width * .82f, size.height * .40f), stroke)
        drawLine(color, Offset(size.width * .34f, size.height * .12f), Offset(size.width * .34f, size.height * .30f), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width * .66f, size.height * .12f), Offset(size.width * .66f, size.height * .30f), stroke, StrokeCap.Round)
    }
}

@Composable
fun StatisticsGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val barWidth = size.width * .16f
        drawRoundRect(color, Offset(size.width * .18f, size.height * .48f), Size(barWidth, size.height * .34f))
        drawRoundRect(color, Offset(size.width * .42f, size.height * .25f), Size(barWidth, size.height * .57f))
        drawRoundRect(color, Offset(size.width * .66f, size.height * .58f), Size(barWidth, size.height * .24f))
    }
}

@Composable
fun ChevronIcon(
    forward: Boolean,
    modifier: Modifier = Modifier,
    color: Color = DailyRecordText,
) {
    Canvas(modifier.size(20.dp)) {
        val stroke = 2.4.dp.toPx()
        val startX = if (forward) size.width * .38f else size.width * .62f
        val middleX = if (forward) size.width * .64f else size.width * .36f
        drawLine(
            color,
            Offset(startX, size.height * .18f),
            Offset(middleX, size.height * .50f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(middleX, size.height * .50f),
            Offset(startX, size.height * .82f),
            stroke,
            StrokeCap.Round,
        )
    }
}

@Composable
fun BackChevronIcon(modifier: Modifier = Modifier, color: Color = DailyRecordText) {
    ChevronIcon(forward = false, modifier = modifier, color = color)
}

@Composable
fun PlaneIcon(modifier: Modifier = Modifier, color: Color = DailyRecordDefaultAccent) {
    Canvas(modifier.size(36.dp)) {
        val path = Path().apply {
            moveTo(size.width * .50f, size.height * .06f)
            lineTo(size.width * .58f, size.height * .42f)
            lineTo(size.width * .90f, size.height * .62f)
            lineTo(size.width * .90f, size.height * .72f)
            lineTo(size.width * .58f, size.height * .62f)
            lineTo(size.width * .58f, size.height * .86f)
            lineTo(size.width * .70f, size.height * .94f)
            lineTo(size.width * .70f, size.height)
            lineTo(size.width * .50f, size.height * .94f)
            lineTo(size.width * .30f, size.height)
            lineTo(size.width * .30f, size.height * .94f)
            lineTo(size.width * .42f, size.height * .86f)
            lineTo(size.width * .42f, size.height * .62f)
            lineTo(size.width * .10f, size.height * .72f)
            lineTo(size.width * .10f, size.height * .62f)
            lineTo(size.width * .42f, size.height * .42f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

/**
 * A neutral, non-explicit intimacy mark: two interlocking rings.
 * Text always accompanies this icon, so module identity never depends on shape or color alone.
 */
@Composable
fun IntimacyIcon(modifier: Modifier = Modifier, color: Color = DailyRecordDefaultAccent) {
    Canvas(modifier.size(36.dp)) {
        val stroke = 2.6.dp.toPx()
        val radius = size.minDimension * .23f
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(size.width * .39f, size.height * .50f),
            style = Stroke(stroke),
        )
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(size.width * .61f, size.height * .50f),
            style = Stroke(stroke),
        )
    }
}

@Composable
internal fun RecordModuleSelector(
    selected: RecordModule,
    specs: List<RecordModuleUiSpec>,
    onSelected: (RecordModule) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = DailyRecordSizes.ModuleSelectorMinHeight)
            .testTag("record_module_selector")
            .clip(DailyRecordShapes.ModuleSelector)
            .background(DailyRecordSurface)
            .border(
                DailyRecordBorders.Standard,
                DailyRecordDivider,
                DailyRecordShapes.ModuleSelector,
            )
            .drawWithContent {
                drawContent()
                if (specs.size > 1) {
                    drawLine(
                        color = DailyRecordDivider,
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = DailyRecordBorders.Standard.toPx(),
                    )
                }
            },
    ) {
        specs.forEach { spec ->
            val active = spec.module == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = DailyRecordSizes.ModuleSelectorMinHeight)
                    .testTag("record_module_${spec.module.name}")
                    .background(if (active) spec.colors.primary else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelected(spec.module) }
                    .padding(
                        horizontal = DailyRecordSpacing.Inline,
                        vertical = DailyRecordSpacing.Content,
                    )
                    .semantics {
                        this.selected = active
                        role = Role.Tab
                        contentDescription = AppCopy.selectedState(AppCopy.RecordModule.recordLabel(spec.label), active)
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                spec.icon(
                    Modifier.size(DailyRecordSizes.ModuleIcon),
                    if (active) spec.colors.onPrimary else DailyRecordTextSecondary,
                )
                Spacer(Modifier.width(DailyRecordSpacing.Inline))
                Text(
                    text = spec.label,
                    color = if (active) spec.colors.onPrimary else DailyRecordTextSecondary,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun PeriodTabs(
    selected: StatisticsPeriod,
    onSelected: (StatisticsPeriod) -> Unit,
    modifier: Modifier = Modifier,
    colors: RecordModuleColorTokens = HandBrewColorTokens,
) {
    val periods = StatisticsPeriod.entries
    val selectedIndex = periods.indexOf(selected).coerceAtLeast(0)
    val trackShape = RoundedCornerShape(percent = 50)
    val sliderShape = RoundedCornerShape(percent = 50)
    val sliderInset = DailyRecordSpacing.Compact
    val glassTint = colors.periodGlassTint
    val glassGlow = colors.periodGlassGlow

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
            .testTag("statistics_period_tabs"),
        color = Color.Transparent,
        shape = trackShape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = .58f)),
        shadowElevation = 1.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
                .clip(trackShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = .68f),
                            glassTint.copy(alpha = .72f),
                            Color.White.copy(alpha = .46f),
                        ),
                    ),
                )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = DailyRecordSizes.MinimumTouchTarget),
            ) {
                val segmentWidth = maxWidth / periods.size
                val sliderWidth = (segmentWidth - (sliderInset * 2f)).coerceAtLeast(1.dp)
                val animatedOffset = animateDpAsState(
                    targetValue = (segmentWidth * selectedIndex) + sliderInset,
                    animationSpec = spring(
                        dampingRatio = .88f,
                        stiffness = 480f,
                    ),
                    label = "period_slider_offset",
                ).value

                Box(
                    modifier = Modifier
                        .offset(x = animatedOffset)
                        .width(sliderWidth)
                        .fillMaxHeight()
                        .padding(vertical = sliderInset)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(2.dp, sliderShape, clip = false)
                            .clip(sliderShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = .94f),
                                        glassTint.copy(alpha = .30f),
                                    ),
                                ),
                            )
                            .drawWithContent {
                                drawContent()
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = .34f),
                                            Color.Transparent,
                                        ),
                                        startY = 0f,
                                        endY = size.height * .55f,
                                    ),
                                    cornerRadius = CornerRadius(size.height / 2f),
                                )
                                drawRoundRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            glassGlow.copy(alpha = .16f),
                                            Color.Transparent,
                                        ),
                                        center = Offset(size.width / 2f, size.height),
                                        radius = size.width * .78f,
                                    ),
                                    cornerRadius = CornerRadius(size.height / 2f),
                                )
                            }
                            .border(1.dp, Color.White.copy(alpha = .82f), sliderShape)
                            .testTag("statistics_period_slider"),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = DailyRecordSizes.MinimumTouchTarget),
                ) {
                    periods.forEach { period ->
                        val active = period == selected
                        val textColor = animateColorAsState(
                            targetValue = if (active) colors.strong else DailyRecordPeriodInactiveText,
                            animationSpec = tween(durationMillis = 180),
                            label = "period_text_color_${period.name}",
                        ).value
                        val interactionSource = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("statistics_period_${period.name}")
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = { onSelected(period) },
                                )
                                .semantics {
                                    this.selected = active
                                    role = Role.Tab
                                    contentDescription = AppCopy.selectedState(
                                        AppCopy.Statistics.statisticsLabel(period.label),
                                        active,
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = period.label,
                                color = textColor,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyCountControl(
    count: Int,
    enabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    colors: RecordModuleColorTokens = HandBrewColorTokens,
) {
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 132.dp),
        color = if (enabled) DailyRecordSurface else DailyRecordSurfaceMuted,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (enabled) colors.primary else DailyRecordDivider),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CountButton(AppCopy.Components.decrease, enabled && count > 0, false, colors, onDecrease)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (enabled) count.toString() else "—",
                    color = if (enabled) DailyRecordText else DailyRecordTextMuted,
                    style = MetricNumberMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            CountButton(AppCopy.Components.increase, enabled && count < Int.MAX_VALUE, true, colors, onIncrease)
        }
    }
}

@Composable
private fun CountButton(
    description: String,
    enabled: Boolean,
    primary: Boolean,
    colors: RecordModuleColorTokens,
    onClick: () -> Unit,
) {
    val background = when {
        !enabled -> DailyRecordSurfaceMuted
        primary -> colors.primary
        else -> DailyRecordSurface
    }
    val content = when {
        !enabled -> DailyRecordTextMuted
        primary -> DailyRecordOnAccent
        else -> colors.primary
    }
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(background)
            .border(
                width = if (!primary && enabled) DailyRecordBorders.Standard else 0.dp,
                color = if (enabled) colors.primary else DailyRecordDivider,
                shape = CircleShape,
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val stroke = 2.5.dp.toPx()
            drawLine(
                color = content,
                start = Offset(2.dp.toPx(), size.height / 2f),
                end = Offset(size.width - 2.dp.toPx(), size.height / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            if (primary) {
                drawLine(
                    color = content,
                    start = Offset(size.width / 2f, 2.dp.toPx()),
                    end = Offset(size.width / 2f, size.height - 2.dp.toPx()),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
fun PrimaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = DailyRecordDefaultAccent,
) {
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(DailyRecordShapes.Control)
            .background(if (enabled) accent else DailyRecordSurfaceMuted)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button; contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = if (enabled) DailyRecordOnAccent else DailyRecordTextMuted, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun OutlineActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = DailyRecordDefaultAccent,
) {
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(DailyRecordShapes.Control)
            .background(DailyRecordSurface)
            .border(
                DailyRecordBorders.Standard,
                if (enabled) accent else DailyRecordDivider,
                DailyRecordShapes.Control,
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button; contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = if (enabled) accent else DailyRecordTextMuted, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StatisticRow(
    label: String,
    countText: String,
    daysText: String,
    future: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (future) DailyRecordSurfaceMuted else DailyRecordSurface)
            .border(1.dp, DailyRecordDivider, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label，$countText，$daysText"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = if (future) DailyRecordTextMuted else DailyRecordText,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = countText,
            color = if (future) DailyRecordTextMuted else DailyRecordTextSecondary,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(.8f),
        )
        Text(
            text = daysText,
            color = if (future) DailyRecordTextMuted else DailyRecordTextSecondary,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(.8f),
        )
    }
}
