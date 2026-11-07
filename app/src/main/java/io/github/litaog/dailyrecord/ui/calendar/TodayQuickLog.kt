package io.github.litaog.dailyrecord.ui.calendar

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.RecordModuleUiSpec
import io.github.litaog.dailyrecord.ui.components.CountButton
import io.github.litaog.dailyrecord.ui.components.DailyRecordHaptics
import io.github.litaog.dailyrecord.ui.components.pressScale
import io.github.litaog.dailyrecord.ui.theme.DailyRecordBorders
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.LiquidGlassElevation
import io.github.litaog.dailyrecord.ui.theme.MetricNumberMedium
import io.github.litaog.dailyrecord.ui.theme.appleLiquidGlass

/** One quick-log intent. The caller applies it to the stored count inside one lock. */
internal enum class QuickLogAction {
    Increase,
    Decrease,
    MarkZero,
}

/**
 * The count to save for this action given today's stored count, or null when
 * the action no longer applies (for example "mark 0" after another tap already
 * filled the day). [current] null means today is not filled yet.
 *
 * Decreasing from 1 saves an explicit 0 (ADR-004). Clearing a day back to
 * "not filled" stays a confirmed action on the record page.
 */
internal fun QuickLogAction.nextCount(current: Int?): Int? = when (this) {
    QuickLogAction.Increase -> when (current) {
        null -> 1
        Int.MAX_VALUE -> null
        else -> current + 1
    }
    QuickLogAction.Decrease -> if (current != null && current > 0) current - 1 else null
    QuickLogAction.MarkZero -> if (current == null) 0 else null
}

private val QuickLogPill = RoundedCornerShape(percent = 50)

/**
 * Today quick-log island (ADR-022): adjusts today's count of the current module
 * without opening the record page. [count] is what Room currently stores for
 * today (null = not filled); the island never keeps its own copy, so it can
 * not drift from the calendar or the statistics.
 */
@Composable
internal fun TodayQuickLogIsland(
    count: Int?,
    moduleSpec: RecordModuleUiSpec,
    onAction: (QuickLogAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val largeText = LocalDensity.current.fontScale >= 1.4f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("today_quick_log")
            .appleLiquidGlass(
                shape = DailyRecordShapes.Card,
                glowColor = moduleSpec.colors.primary,
                elevation = LiquidGlassElevation.Elevated,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (largeText) {
            // 200% font: the status takes its own lines so nothing truncates.
            QuickLogStatus(count = count, moduleLabel = moduleSpec.semanticCountLabel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuickLogControls(count = count, moduleSpec = moduleSpec, onAction = onAction)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuickLogStatus(
                    count = count,
                    moduleLabel = moduleSpec.semanticCountLabel,
                    modifier = Modifier.weight(1f),
                )
                QuickLogControls(count = count, moduleSpec = moduleSpec, onAction = onAction)
            }
        }
        if (count == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                QuickLogMarkZeroButton(
                    moduleSpec = moduleSpec,
                    onClick = { onAction(QuickLogAction.MarkZero) },
                )
            }
        }
    }
}

@Composable
private fun QuickLogStatus(
    count: Int?,
    moduleLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .testTag("today_quick_log_status")
            .clearAndSetSemantics {
                contentDescription = AppCopy.Calendar.quickLogStatusDescription(moduleLabel, count)
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Text(
            text = AppCopy.Calendar.quickLogTitle,
            color = DailyRecordText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = AppCopy.Calendar.quickLogStatus(count),
            color = DailyRecordTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun QuickLogControls(
    count: Int?,
    moduleSpec: RecordModuleUiSpec,
    onAction: (QuickLogAction) -> Unit,
) {
    val label = moduleSpec.semanticCountLabel
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CountButton(
            description = AppCopy.Calendar.quickLogDecreaseDescription(label),
            enabled = QuickLogAction.Decrease.nextCount(count) != null,
            primary = false,
            colors = moduleSpec.colors,
            onClick = { onAction(QuickLogAction.Decrease) },
        )
        // The status line already speaks the count; the number is visual only.
        Text(
            text = count?.toString() ?: AppCopy.Calendar.unset,
            color = if (count == null) DailyRecordTextMuted else DailyRecordText,
            style = if (count == null) MaterialTheme.typography.labelLarge else MetricNumberMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .widthIn(min = 44.dp)
                .clearAndSetSemantics { },
        )
        CountButton(
            description = AppCopy.Calendar.quickLogIncreaseDescription(label),
            enabled = QuickLogAction.Increase.nextCount(count) != null,
            primary = true,
            colors = moduleSpec.colors,
            onClick = { onAction(QuickLogAction.Increase) },
        )
    }
}

@Composable
private fun QuickLogMarkZeroButton(
    moduleSpec: RecordModuleUiSpec,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
            .testTag("today_quick_log_mark_zero")
            .pressScale(interactionSource)
            .clip(QuickLogPill)
            .border(DailyRecordBorders.Standard, moduleSpec.colors.primary, QuickLogPill)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
            ) {
                DailyRecordHaptics.tick(haptic)
                onClick()
            }
            .semantics {
                contentDescription = AppCopy.Calendar.quickLogMarkZeroDescription(
                    moduleSpec.semanticCountLabel,
                )
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = AppCopy.Calendar.quickLogMarkZero,
            color = moduleSpec.colors.strong,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}
