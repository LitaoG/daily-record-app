package io.github.litaog.dailyrecord.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.theme.DailyRecordMotion
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordBorders
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.MetricNumberMedium
import io.github.litaog.dailyrecord.ui.theme.DailyRecordOnAccent
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlass

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
