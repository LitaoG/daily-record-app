package io.github.litaog.dailyrecord.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
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
                shape = RoundedCornerShape(18.dp),
                moduleColors = colors,
                level = if (enabled) DailyRecordGlassLevel.Elevated else DailyRecordGlassLevel.Muted,
            ),
        color = Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        border = null,
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
