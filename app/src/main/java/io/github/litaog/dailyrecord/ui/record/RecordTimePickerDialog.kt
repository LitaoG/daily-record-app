package io.github.litaog.dailyrecord.ui.record

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
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
import io.github.litaog.dailyrecord.ui.components.DailyRecordDialog
import io.github.litaog.dailyrecord.ui.components.OutlineActionButton
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val HOURS_PER_DAY = 24
private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = HOURS_PER_DAY * MINUTES_PER_HOUR

@Composable
internal fun RecordTimePickerDialog(
    initialMinutes: Int,
    colors: RecordModuleColorTokens,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val boundedInitialMinutes = initialMinutes.coerceIn(0, MINUTES_PER_DAY - 1)
    var selectedHour by remember(boundedInitialMinutes) {
        mutableIntStateOf(boundedInitialMinutes / MINUTES_PER_HOUR)
    }
    var selectedMinute by remember(boundedInitialMinutes) {
        mutableIntStateOf(boundedInitialMinutes % MINUTES_PER_HOUR)
    }
    val fontScale = LocalDensity.current.fontScale
    val stackActions = fontScale >= 1.35f
    val wheelRowHeight = if (fontScale >= 1.6f) 72.dp else 56.dp

    DailyRecordDialog(
        title = AppCopy.Record.detailTimePickerTitle,
        subtitle = AppCopy.Record.detailTimePickerSubtitle,
        testTag = "time_picker_dialog",
        onDismissRequest = onDismiss,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeWheelColumn(
                value = selectedHour,
                valueCount = HOURS_PER_DAY,
                unit = AppCopy.Record.detailTimePickerHour,
                colors = colors,
                rowHeight = wheelRowHeight,
                surfaceTestTag = "time_picker_hour_wheel_surface",
                onValueChanged = { selectedHour = it },
                modifier = Modifier.weight(1f).testTag("time_picker_hour_wheel"),
            )
            Text(
                text = ":",
                color = colors.primary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 22.dp),
            )
            TimeWheelColumn(
                value = selectedMinute,
                valueCount = MINUTES_PER_HOUR,
                unit = AppCopy.Record.detailTimePickerMinute,
                colors = colors,
                rowHeight = wheelRowHeight,
                surfaceTestTag = "time_picker_minute_wheel_surface",
                onValueChanged = { selectedMinute = it },
                modifier = Modifier.weight(1f).testTag("time_picker_minute_wheel"),
            )
        }
        Text(
            text = AppCopy.Record.detailTimePickerHint,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
        Spacer(Modifier.height(18.dp))
        if (stackActions) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PrimaryActionButton(
                    label = AppCopy.Record.detailTimePickerConfirm,
                    onClick = {
                        onConfirm(selectedHour * MINUTES_PER_HOUR + selectedMinute)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    accent = colors.primary,
                )
                OutlineActionButton(
                    label = AppCopy.Auth.cancel,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    accent = colors.primary,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlineActionButton(
                    label = AppCopy.Auth.cancel,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    accent = colors.primary,
                )
                PrimaryActionButton(
                    label = AppCopy.Record.detailTimePickerConfirm,
                    onClick = {
                        onConfirm(selectedHour * MINUTES_PER_HOUR + selectedMinute)
                    },
                    modifier = Modifier.weight(1f),
                    accent = colors.primary,
                )
            }
        }
    }
}

@Composable
private fun TimeWheelColumn(
    value: Int,
    valueCount: Int,
    unit: String,
    colors: RecordModuleColorTokens,
    rowHeight: androidx.compose.ui.unit.Dp,
    surfaceTestTag: String,
    onValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedValue by remember(value, valueCount) {
        mutableIntStateOf(wrapTimeWheelValue(value, valueCount))
    }
    var dragOffsetPx by remember(valueCount) { mutableFloatStateOf(0f) }
    val latestOnValueChanged = rememberUpdatedState(onValueChanged)
    val settleAnimation = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }
    val wheelHeight = rowHeight * 3
    val shape = RoundedCornerShape(16.dp)

    fun commitDelta(delta: Int) {
        selectedValue = wrapTimeWheelValue(selectedValue + delta, valueCount)
        latestOnValueChanged.value(selectedValue)
    }

    fun cancelSettle() {
        settleJob?.cancel()
        settleJob = null
        coroutineScope.launch { settleAnimation.stop() }
    }

    fun settleWheel() {
        if (dragOffsetPx <= -rowHeightPx / 2f) {
            commitDelta(1)
            dragOffsetPx += rowHeightPx
        } else if (dragOffsetPx >= rowHeightPx / 2f) {
            commitDelta(-1)
            dragOffsetPx -= rowHeightPx
        }

        val startOffset = dragOffsetPx
        settleJob?.cancel()
        settleJob = coroutineScope.launch {
            try {
                settleAnimation.snapTo(startOffset)
                settleAnimation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.92f, stiffness = 500f),
                ) {
                    dragOffsetPx = this.value
                }
                dragOffsetPx = 0f
            } catch (_: CancellationException) {
                // A new gesture or option tap takes ownership of the wheel.
            }
        }
    }

    val draggableState = rememberDraggableState { dragAmount ->
        var nextOffset = dragOffsetPx + dragAmount
        while (nextOffset <= -rowHeightPx) {
            commitDelta(1)
            nextOffset += rowHeightPx
        }
        while (nextOffset >= rowHeightPx) {
            commitDelta(-1)
            nextOffset -= rowHeightPx
        }
        dragOffsetPx = nextOffset
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = unit,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(wheelHeight)
                .clip(shape)
                .background(colors.soft.copy(alpha = .34f))
                .border(1.dp, colors.primary.copy(alpha = .28f), shape)
                .testTag(surfaceTestTag)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStarted = {
                        cancelSettle()
                    },
                    onDragStopped = { settleWheel() },
                ),
        ) {
            Canvas(Modifier.fillMaxWidth().height(wheelHeight)) {
                val bandHeight = rowHeight.toPx()
                val top = (size.height - bandHeight) / 2f
                drawRect(
                    color = colors.soft.copy(alpha = .88f),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, top),
                    size = androidx.compose.ui.geometry.Size(size.width, bandHeight),
                )
                drawLine(
                    color = colors.primary.copy(alpha = .52f),
                    start = androidx.compose.ui.geometry.Offset(0f, top),
                    end = androidx.compose.ui.geometry.Offset(size.width, top),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = colors.primary.copy(alpha = .52f),
                    start = androidx.compose.ui.geometry.Offset(0f, top + bandHeight),
                    end = androidx.compose.ui.geometry.Offset(size.width, top + bandHeight),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            (-2..2).forEach { distance ->
                TimeWheelValueRow(
                    value = wrapTimeWheelValue(selectedValue + distance, valueCount),
                    unit = unit,
                    selected = distance == 0,
                    interactive = distance in -1..1,
                    rowHeight = rowHeight,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            translationY = distance * rowHeightPx + dragOffsetPx
                        },
                    onClick = {
                        cancelSettle()
                        if (distance != 0) commitDelta(distance)
                        dragOffsetPx = 0f
                    },
                )
            }
        }
    }
}

@Composable
private fun TimeWheelValueRow(
    value: Int,
    unit: String,
    selected: Boolean,
    interactive: Boolean,
    rowHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val formattedValue = formatTimeWheelValue(value)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            .then(
                if (interactive) {
                    Modifier
                        .clickable(role = Role.Button, onClick = onClick)
                        .semantics {
                            role = Role.Button
                            this.selected = selected
                            contentDescription = if (selected) {
                                AppCopy.Record.detailTimeWheelCurrent(unit, formattedValue)
                            } else {
                                AppCopy.Record.detailTimeWheelOption(unit, formattedValue)
                            }
                        }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = formattedValue,
            color = if (selected) DailyRecordText else DailyRecordTextSecondary.copy(alpha = .48f),
            style = if (selected) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

internal fun wrapTimeWheelValue(value: Int, valueCount: Int): Int {
    require(valueCount > 0) { "Time wheel must contain at least one value." }
    return Math.floorMod(value, valueCount)
}

internal fun formatTimeWheelValue(value: Int): String =
    String.format(Locale.ROOT, "%02d", value)
