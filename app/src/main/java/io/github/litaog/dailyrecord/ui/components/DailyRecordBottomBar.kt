package io.github.litaog.dailyrecord.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.TopDestination
import io.github.litaog.dailyrecord.ui.theme.DailyRecordMotion
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.LiquidGlassElevation
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.appleLiquidGlass

@Composable
internal fun DailyRecordBottomBar(
    selected: TopDestination,
    colors: RecordModuleColorTokens,
    onSelected: (TopDestination) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = DailyRecordSpacing.ScreenHorizontal,
                end = DailyRecordSpacing.ScreenHorizontal,
                bottom = DailyRecordSpacing.Inline,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DailyRecordSizes.BottomBarMinHeight)
                .appleLiquidGlass(
                    shape = DailyRecordShapes.BottomBar,
                    glowColor = colors.primary,
                    elevation = LiquidGlassElevation.Elevated,
                )
                .padding(
                    horizontal = DailyRecordSpacing.Content,
                    vertical = DailyRecordSpacing.Compact,
                ),
            horizontalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Inline),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomDestination(
                label = AppCopy.NavigationBar.calendar,
                selected = selected == TopDestination.Calendar,
                accent = colors.primary,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(TopDestination.Calendar) },
                icon = { color ->
                    CalendarGlyph(
                        color = color,
                        modifier = Modifier.size(24.dp),
                        theme = colors.brandIconTheme,
                    )
                },
            )
            BottomDestination(
                label = AppCopy.NavigationBar.statistics,
                selected = selected == TopDestination.Statistics,
                accent = colors.primary,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(TopDestination.Statistics) },
                icon = { color ->
                    StatisticsGlyph(
                        color = color,
                        modifier = Modifier.size(24.dp),
                        theme = colors.brandIconTheme,
                    )
                },
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
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val contentColor by animateColorAsState(
        targetValue = if (selected) accent else DailyRecordTextSecondary,
        label = "bottomDestinationColor",
    )

    // Micro-scaling pop for active destination icon
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1.0f,
        animationSpec = DailyRecordMotion.effects(),
        label = "bottomDestinationIconScale",
    )

    // Polished jewel indicator underline that springs open from center
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 24.dp else 0.dp,
        animationSpec = DailyRecordMotion.spatial(),
        label = "bottomDestinationIndicator",
    )

    Column(
        modifier = modifier
            .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
            .testTag("bottom_destination_$label")
            .pressScale(interactionSource)
            .clip(DailyRecordShapes.Control)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
            ) {
                if (!selected) DailyRecordHaptics.tabSwitch(haptic)
                onClick()
            }
            .semantics {
                this.selected = selected
                role = Role.Tab
                contentDescription = AppCopy.selectedState(label, selected)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
            },
            contentAlignment = Alignment.Center,
        ) {
            icon(contentColor)
        }
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
        Spacer(Modifier.height(DailyRecordSpacing.Compact))
        Box(
            Modifier
                .width(indicatorWidth)
                .height(3.dp)
                .clip(CircleShape)
                .background(accent),
        )
    }
}
