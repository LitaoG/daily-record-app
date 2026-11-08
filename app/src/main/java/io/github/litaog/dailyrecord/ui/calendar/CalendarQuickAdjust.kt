package io.github.litaog.dailyrecord.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.RecordModuleUiSpec
import io.github.litaog.dailyrecord.ui.components.CountButton
import io.github.litaog.dailyrecord.ui.components.pressScale
import io.github.litaog.dailyrecord.ui.theme.DailyRecordBorders
import io.github.litaog.dailyrecord.ui.theme.DailyRecordMotion
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.LiquidGlassElevation
import io.github.litaog.dailyrecord.ui.theme.MetricNumberMedium
import io.github.litaog.dailyrecord.ui.theme.appleLiquidGlass
import java.time.LocalDate

private val StepperShape = DailyRecordShapes.Large
private val StepperPill = DailyRecordShapes.Full

/**
 * Placement rule for the long-press stepper: centered on the date cell,
 * kept inside the window, above the cell when there is room and below it
 * otherwise (for example on the first calendar row).
 */
internal fun quickAdjustPopupOffset(
    anchorBounds: IntRect,
    windowSize: IntSize,
    popupContentSize: IntSize,
    gapPx: Int,
): IntOffset {
    val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
    val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
    val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(0, maxX)
    val above = anchorBounds.top - gapPx - popupContentSize.height
    val y = if (above >= 0) above else (anchorBounds.bottom + gapPx).coerceAtMost(maxY)
    return IntOffset(x, y)
}

private class QuickAdjustPositionProvider(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = quickAdjustPopupOffset(anchorBounds, windowSize, popupContentSize, gapPx)
}

/**
 * Long-press stepper for one calendar date (ADR-022). Each -1 / +1 saves at
 * once through [onAction]. The popup stays open until Done, an outside tap or
 * back: there is no auto-dismiss timer, so TalkBack and switch users have
 * enough time to act (WCAG 2.2.1).
 */
@Composable
internal fun CalendarQuickAdjustPopup(
    date: LocalDate,
    count: Int?,
    moduleSpec: RecordModuleUiSpec,
    onAction: (QuickLogAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val gapPx = with(LocalDensity.current) { 4.dp.roundToPx() }
    val positionProvider = remember(gapPx) { QuickAdjustPositionProvider(gapPx) }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        QuickAdjustContent(
            date = date,
            count = count,
            moduleSpec = moduleSpec,
            onAction = onAction,
            onDismiss = onDismiss,
        )
    }
}

@Composable
internal fun QuickAdjustContent(
    date: LocalDate,
    count: Int?,
    moduleSpec: RecordModuleUiSpec,
    onAction: (QuickLogAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val label = moduleSpec.semanticCountLabel
    Column(
        modifier = Modifier
            .testTag("calendar_quick_adjust")
            .semantics { paneTitle = AppCopy.Calendar.quickAdjustTitle(date) }
            // Transparent margin inside the popup window keeps the glass
            // shadow from being cut off at the window edge.
            .padding(10.dp)
            .appleLiquidGlass(
                shape = StepperShape,
                glowColor = moduleSpec.colors.primary,
                elevation = LiquidGlassElevation.Emphasis,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = AppCopy.Calendar.quickAdjustTitle(date),
            color = DailyRecordTextSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = AppCopy.Calendar.quickAdjustStatusDescription(date, label, count)
                liveRegion = LiveRegionMode.Polite
            },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CountButton(
                description = AppCopy.Calendar.quickAdjustDecreaseDescription(date, label),
                enabled = QuickLogAction.Decrease.nextCount(count) != null,
                primary = false,
                colors = moduleSpec.colors,
                onClick = { onAction(QuickLogAction.Decrease) },
            )
            // The status line above already speaks the count.
            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    val target = targetState ?: 0
                    val initial = initialState ?: 0
                    if (target >= initial) {
                        (slideInVertically(DailyRecordMotion.spatial()) { height -> height } + fadeIn(tween(150)))
                            .togetherWith(slideOutVertically(DailyRecordMotion.spatial()) { height -> -height } + fadeOut(tween(120)))
                    } else {
                        (slideInVertically(DailyRecordMotion.spatial()) { height -> -height } + fadeIn(tween(150)))
                            .togetherWith(slideOutVertically(DailyRecordMotion.spatial()) { height -> height } + fadeOut(tween(120)))
                    }
                },
                label = "quickAdjustCounter",
                modifier = Modifier
                    .widthIn(min = 44.dp)
                    .clearAndSetSemantics { },
            ) { targetCount ->
                Text(
                    text = targetCount?.toString() ?: AppCopy.Calendar.unset,
                    color = if (targetCount == null) DailyRecordTextMuted else DailyRecordText,
                    style = if (targetCount == null) MaterialTheme.typography.labelLarge else MetricNumberMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            CountButton(
                description = AppCopy.Calendar.quickAdjustIncreaseDescription(date, label),
                enabled = QuickLogAction.Increase.nextCount(count) != null,
                primary = true,
                colors = moduleSpec.colors,
                onClick = { onAction(QuickLogAction.Increase) },
            )
            QuickAdjustDoneButton(moduleSpec = moduleSpec, onClick = onDismiss)
        }
    }
}

@Composable
private fun QuickAdjustDoneButton(
    moduleSpec: RecordModuleUiSpec,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .heightIn(min = DailyRecordSizes.MinimumTouchTarget)
            .widthIn(min = DailyRecordSizes.MinimumTouchTarget)
            .testTag("calendar_quick_adjust_done")
            .pressScale(interactionSource)
            .clip(StepperPill)
            .border(DailyRecordBorders.Standard, moduleSpec.colors.primary, StepperPill)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = AppCopy.Calendar.quickAdjustDone,
            color = moduleSpec.colors.strong,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
