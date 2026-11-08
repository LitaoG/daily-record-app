package io.github.litaog.dailyrecord.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.RecordModuleUiSpec
import io.github.litaog.dailyrecord.ui.theme.DailyRecordMotion
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.LiquidGlassElevation
import io.github.litaog.dailyrecord.ui.theme.appleLiquidGlass

@Composable
internal fun RecordModuleSelector(
    selected: RecordModule,
    specs: List<RecordModuleUiSpec>,
    onSelected: (RecordModule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val selectedIndex = specs.indexOfFirst { it.module == selected }.coerceAtLeast(0)
    val targetColors = specs.getOrNull(selectedIndex)?.colors
    val targetPrimary = targetColors?.primary ?: Color.Transparent

    // Smoothly animate the module accent color to create a liquid color-morphing transition
    val animatedAccentColor by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = DailyRecordMotion.spatial(),
        label = "moduleSelectorAccentColor",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DailyRecordSizes.ModuleSelectorMinHeight)
            .testTag("record_module_selector")
            .appleLiquidGlass(
                shape = DailyRecordShapes.ModuleSelector,
                glowColor = animatedAccentColor,
                elevation = LiquidGlassElevation.Subtle,
            )
            .padding(DailyRecordSpacing.Compact),
    ) {
        val segmentWidth = if (specs.isEmpty()) maxWidth else maxWidth / specs.size
        val sliderOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = DailyRecordMotion.spatial(),
            label = "moduleSlider",
        )

        if (targetColors != null) {
            Box(Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(sliderOffset.roundToPx(), 0) }
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .shadow(
                            elevation = 6.dp,
                            shape = DailyRecordShapes.ModuleSelectorSlider,
                            ambientColor = Color.Transparent,
                            spotColor = animatedAccentColor.copy(alpha = .35f),
                        )
                        .clip(DailyRecordShapes.ModuleSelectorSlider)
                        .background(animatedAccentColor)
                        // Specular highlight meniscus brush on top edge
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    0.0f to Color.White.copy(alpha = 0.40f),
                                    1.0f to Color.White.copy(alpha = 0.08f),
                                ),
                            ),
                            shape = DailyRecordShapes.ModuleSelectorSlider,
                        ),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            specs.forEachIndexed { index, spec ->
                val active = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }
                val labelColor by animateColorAsState(
                    targetValue = if (active) spec.colors.onPrimary else DailyRecordTextSecondary,
                    label = "moduleLabel",
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
                        .testTag("record_module_${spec.module.name}")
                        .pressScale(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                        ) {
                            if (!active) DailyRecordHaptics.tabSwitch(haptic)
                            onSelected(spec.module)
                        }
                        .padding(
                            horizontal = DailyRecordSpacing.Inline,
                            vertical = DailyRecordSpacing.Inline,
                        )
                        .semantics {
                            this.selected = active
                            role = Role.Tab
                            contentDescription = AppCopy.selectedState(
                                AppCopy.RecordModule.recordLabel(spec.semanticCountLabel),
                                active,
                            )
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
                        color = labelColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
