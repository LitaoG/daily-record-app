package io.github.litaog.dailyrecord.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.core.statistics.label
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordPeriodInactiveText
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.periodGlassGlow
import io.github.litaog.dailyrecord.ui.theme.dailyRecordPeriodSliderBrush
import io.github.litaog.dailyrecord.ui.theme.dailyRecordPeriodTrackBrush
import androidx.compose.ui.platform.LocalHapticFeedback
import io.github.litaog.dailyrecord.ui.theme.DailyRecordMotion

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
