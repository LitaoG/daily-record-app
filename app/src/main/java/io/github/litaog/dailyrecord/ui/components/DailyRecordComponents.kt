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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.TopDestination
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.RecordModuleUiSpec
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.core.statistics.label
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import io.github.litaog.dailyrecord.ui.theme.DailyRecordBorders
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordPeriodInactiveText
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.SexColorTokens
import io.github.litaog.dailyrecord.ui.theme.MetricNumberMedium
import io.github.litaog.dailyrecord.ui.theme.DailyRecordOnAccent
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.periodGlassGlow
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.dailyRecordAccentBrush
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlass
import io.github.litaog.dailyrecord.ui.theme.dailyRecordPeriodSliderBrush
import io.github.litaog.dailyrecord.ui.theme.dailyRecordPeriodTrackBrush
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.getValue
import io.github.litaog.dailyrecord.ui.theme.appleLiquidGlass
import io.github.litaog.dailyrecord.ui.theme.LiquidGlassElevation
import io.github.litaog.dailyrecord.ui.theme.DailyRecordMotion

// Copy rule for the shared components: visible text arrives as a parameter or from AppCopy
// (ZhStrings / EnStrings). Never hardcode visible text or CJK punctuation here; see
// docs/ENGINEERING_STANDARDS.md section 7. scripts/copy-integrity.test.mjs rejects CJK leaks.



@Composable
internal fun CalendarGlyph(
    color: Color = DailyRecordDefaultAccent,
    modifier: Modifier = Modifier,
    theme: BrandIconTheme = BrandIconTheme.Solo,
) {
    BrandIcon(
        asset = BrandIconAsset.Calendar,
        theme = theme,
        modifier = modifier,
        tint = color,
    )
}

@Composable
internal fun StatisticsGlyph(
    color: Color = DailyRecordDefaultAccent,
    modifier: Modifier = Modifier,
    theme: BrandIconTheme = BrandIconTheme.Solo,
) {
    BrandIcon(
        asset = BrandIconAsset.Statistics,
        theme = theme,
        modifier = modifier,
        tint = color,
    )
}

/**
 * Navigation chevron. The artwork ships as two locked brand variants, so
 * callers pick [theme] instead of a tint; a disabled state is expressed with
 * [modifier] alpha by the caller.
 */
@Composable
internal fun ChevronIcon(
    forward: Boolean,
    modifier: Modifier = Modifier,
    color: Color = DailyRecordText,
    theme: BrandIconTheme = BrandIconTheme.Solo,
) {
    BrandIcon(
        asset = if (forward) BrandIconAsset.Next else BrandIconAsset.Previous,
        theme = theme,
        modifier = modifier,
    )
}

@Composable
internal fun BackChevronIcon(
    modifier: Modifier = Modifier,
    color: Color = DailyRecordText,
    theme: BrandIconTheme = BrandIconTheme.Solo,
) {
    ChevronIcon(forward = false, modifier = modifier, color = color, theme = theme)
}

/** Masturbation module symbol (paper plane), tinted with the theme accent. */
@Composable
fun HandBrewIcon(
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    Icon(
        imageVector = DailyRecordIcons.SoloModule,
        contentDescription = null,
        modifier = modifier,
        tint = tint ?: HandBrewColorTokens.primary,
    )
}

/** Sex module symbol (two interlocking rings), tinted with the theme accent. */
@Composable
fun SexIcon(
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    Icon(
        imageVector = DailyRecordIcons.DuoModule,
        contentDescription = null,
        modifier = modifier,
        tint = tint ?: SexColorTokens.primary,
    )
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
    val trackShape = DailyRecordShapes.Full
    val sliderShape = DailyRecordShapes.Full
    val sliderInset = DailyRecordSpacing.Compact
    val glassGlow = colors.periodGlassGlow
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
            .testTag("statistics_period_tabs"),
        color = Color.Transparent,
        shape = trackShape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = .78f)),
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
                .clip(trackShape)
                .background(
                    brush = dailyRecordPeriodTrackBrush(colors),
                )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    // Keep a finite inner canvas so the slider and labels are
                    // measured consistently inside lazy scrolling content.
                    .height(DailyRecordSizes.MinimumTouchTarget),
            ) {
                val segmentWidth = maxWidth / periods.size
                val sliderWidth = (segmentWidth - (sliderInset * 2f)).coerceAtLeast(1.dp)
                val animatedOffset = animateDpAsState(
                    targetValue = (segmentWidth * selectedIndex) + sliderInset,
                    // Same spatial spring as the module selector (ADR-022).
                    animationSpec = DailyRecordMotion.spatial(),
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
                            .shadow(1.dp, sliderShape, clip = false)
                            .clip(sliderShape)
                            .background(
                                brush = dailyRecordPeriodSliderBrush(colors),
                            )
                            .drawWithContent {
                                drawContent()
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = .46f),
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
                                            glassGlow.copy(alpha = .10f),
                                            Color.Transparent,
                                        ),
                                        center = Offset(size.width / 2f, size.height),
                                        radius = size.width * .78f,
                                    ),
                                    cornerRadius = CornerRadius(size.height / 2f),
                                )
                            }
                            .border(1.dp, Color.White.copy(alpha = .90f), sliderShape)
                            .testTag("statistics_period_slider"),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize(),
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
                                .pressScale(interactionSource)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = {
                                        if (!active) DailyRecordHaptics.tabSwitch(haptic)
                                        onSelected(period)
                                    },
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

/**
 * Extracts decimal digits of a non-negative counter value.
 * Used for multi-digit transition validation (ADR-022 / Feature 26).
 */
internal fun extractDigits(count: Int): List<Int> =
    if (count <= 0) listOf(0) else count.toString().map { it.digitToInt() }

/**
 * Vertical slide animated numeric ticker for counter values (Feature 26).
 *
 * Increments slide in from bottom and exit to top.
 * Decrements slide in from top and exit to bottom.
 * Uses tabular figures ("tnum") to prevent horizontal glyph jitter.
 */
@Composable
fun RollingCounter(
    count: Int,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MetricNumberMedium,
    color: Color = DailyRecordText,
) {
    Box(
        modifier = modifier
            .clipToBounds()
            .semantics(mergeDescendants = true) {
                contentDescription = if (enabled) count.toString() else AppCopy.Statistics.dash
            }
            .testTag("rolling_counter"),
        contentAlignment = Alignment.Center,
    ) {
        if (!enabled) {
            Text(
                text = AppCopy.Statistics.dash,
                color = DailyRecordTextMuted,
                style = textStyle,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("rolling_counter_disabled"),
            )
        } else {
            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    val isIncrease = targetState >= initialState
                    if (isIncrease) {
                        (slideInVertically(DailyRecordMotion.spatial()) { height -> height } + fadeIn(tween(150)))
                            .togetherWith(slideOutVertically(DailyRecordMotion.spatial()) { height -> -height } + fadeOut(tween(120)))
                    } else {
                        (slideInVertically(DailyRecordMotion.spatial()) { height -> -height } + fadeIn(tween(150)))
                            .togetherWith(slideOutVertically(DailyRecordMotion.spatial()) { height -> height } + fadeOut(tween(120)))
                    }
                },
                label = "rolling_counter_transition",
            ) { targetCount ->
                Text(
                    text = targetCount.toString(),
                    color = color,
                    style = textStyle,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("rolling_counter_text"),
                )
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
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 108.dp else 132.dp)
            .dailyRecordGlass(
                shape = DailyRecordShapes.Large,
                moduleColors = colors,
                level = if (enabled) DailyRecordGlassLevel.Elevated else DailyRecordGlassLevel.Muted,
            ),
        color = Color.Transparent,
        shape = DailyRecordShapes.Large,
        border = null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CountButton(AppCopy.Components.decrease, enabled && count > 0, false, colors, onDecrease)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RollingCounter(
                    count = count,
                    enabled = enabled,
                    textStyle = MetricNumberMedium,
                    color = if (enabled) DailyRecordText else DailyRecordTextMuted,
                )
            }
            CountButton(AppCopy.Components.increase, enabled && count < Int.MAX_VALUE, true, colors, onIncrease)
        }
    }
}

/** Round -1 / +1 step button shared by the record page and the today island. */
@Composable
internal fun CountButton(
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
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .sizeIn(minWidth = DailyRecordSizes.MinimumTouchTarget, minHeight = DailyRecordSizes.MinimumTouchTarget)
            .size(DailyRecordSizes.MinimumTouchTarget)
            .pressScale(interactionSource)
            .clip(CircleShape)
            .background(background)
            .border(
                width = if (!primary && enabled) DailyRecordBorders.Standard else 0.dp,
                color = if (enabled) colors.primary else DailyRecordDivider,
                shape = CircleShape,
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
            ) {
                // One light tick per step; the count change itself stays with
                // the caller so drafts and saving keep their existing rules.
                DailyRecordHaptics.tick(haptic)
                onClick()
            }
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
            .then(
                if (enabled) {
                    Modifier.background(dailyRecordAccentBrush(accent))
                } else {
                    Modifier.background(DailyRecordSurfaceMuted)
                },
            )
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
            .background(if (enabled) DailyRecordSurface else DailyRecordSurfaceMuted)
            .border(
                width = 1.dp,
                color = if (enabled) accent.copy(alpha = .66f) else DailyRecordDivider,
                shape = DailyRecordShapes.Control,
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
            .dailyRecordGlass(
                shape = RoundedCornerShape(12.dp),
                level = if (future) DailyRecordGlassLevel.Muted else DailyRecordGlassLevel.Base,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = AppCopy.Components.joinSemantics(label, countText, daysText)
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

@Composable
internal fun dailyRecordFieldColors(): TextFieldColors {
    // Material3 exposes this factory as @Composable, so keep the first result
    // per composition instead of calling it for every recomposition.
    val cached = remember { arrayOfNulls<TextFieldColors>(1) }
    return cached[0] ?: OutlinedTextFieldDefaults.colors(
        focusedTextColor = DailyRecordText,
        unfocusedTextColor = DailyRecordText,
        disabledTextColor = DailyRecordTextMuted,
        focusedBorderColor = DailyRecordDefaultAccent,
        unfocusedBorderColor = DailyRecordDivider,
        disabledBorderColor = DailyRecordDivider,
        focusedLabelColor = DailyRecordDefaultAccent,
        unfocusedLabelColor = DailyRecordTextMuted,
        disabledLabelColor = DailyRecordTextMuted,
        cursorColor = DailyRecordDefaultAccent,
        focusedContainerColor = DailyRecordSurface,
        unfocusedContainerColor = DailyRecordSurface,
        disabledContainerColor = DailyRecordSurface,
    ).also { cached[0] = it }
}

/**
 * Shared chevron navigation button for calendar months and statistics
 * periods. The 48dp touch target, disabled alpha and 20dp chevron are the
 * single source of truth for both surfaces.
 */
@Composable
internal fun IconArrowButton(
    forward: Boolean,
    description: String,
    enabled: Boolean,
    theme: BrandIconTheme,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .sizeIn(
                minWidth = DailyRecordSizes.MinimumTouchTarget,
                minHeight = DailyRecordSizes.MinimumTouchTarget,
            )
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else .3f)
            .semantics { role = Role.Button; contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        ChevronIcon(
            forward = forward,
            modifier = Modifier.size(20.dp),
            theme = theme,
        )
    }
}
