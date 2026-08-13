package io.github.litaog.dailyrecord.ui.record

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.rememberSplineBasedDecay
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
private const val MINIMUM_FLING_VELOCITY_DP_PER_SECOND = 90f

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
    var hourSettling by remember(boundedInitialMinutes) { mutableStateOf(false) }
    var minuteSettling by remember(boundedInitialMinutes) { mutableStateOf(false) }
    val wheelSettling = hourSettling || minuteSettling
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
                initialValue = selectedHour,
                resetKey = boundedInitialMinutes,
                valueCount = HOURS_PER_DAY,
                unit = AppCopy.Record.detailTimePickerHour,
                colors = colors,
                rowHeight = wheelRowHeight,
                surfaceTestTag = "time_picker_hour_wheel_surface",
                onValueChanged = { selectedHour = it },
                onSettlingChanged = { hourSettling = it },
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
                initialValue = selectedMinute,
                resetKey = boundedInitialMinutes,
                valueCount = MINUTES_PER_HOUR,
                unit = AppCopy.Record.detailTimePickerMinute,
                colors = colors,
                rowHeight = wheelRowHeight,
                surfaceTestTag = "time_picker_minute_wheel_surface",
                onValueChanged = { selectedMinute = it },
                onSettlingChanged = { minuteSettling = it },
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
                        if (!wheelSettling) {
                            onConfirm(selectedHour * MINUTES_PER_HOUR + selectedMinute)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !wheelSettling,
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
                        if (!wheelSettling) {
                            onConfirm(selectedHour * MINUTES_PER_HOUR + selectedMinute)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !wheelSettling,
                    accent = colors.primary,
                )
            }
        }
    }
}

@Composable
private fun TimeWheelColumn(
    initialValue: Int,
    resetKey: Int,
    valueCount: Int,
    unit: String,
    colors: RecordModuleColorTokens,
    rowHeight: androidx.compose.ui.unit.Dp,
    surfaceTestTag: String,
    onValueChanged: (Int) -> Unit,
    onSettlingChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedValue by remember(resetKey, valueCount) {
        mutableIntStateOf(wrapTimeWheelValue(initialValue, valueCount))
    }
    var dragOffsetPx by remember(resetKey, valueCount) { mutableFloatStateOf(0f) }
    val latestOnValueChanged = rememberUpdatedState(onValueChanged)
    val flingAnimation = remember { Animatable(0f) }
    val settleAnimation = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var animationJob by remember { mutableStateOf<Job?>(null) }
    var animationGeneration by remember { mutableIntStateOf(0) }
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }
    val minimumFlingVelocity = with(LocalDensity.current) {
        MINIMUM_FLING_VELOCITY_DP_PER_SECOND.dp.toPx()
    }
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val wheelHeight = rowHeight * 3
    val shape = RoundedCornerShape(16.dp)

    fun commitDelta(delta: Int) {
        selectedValue = wrapTimeWheelValue(selectedValue + delta, valueCount)
        latestOnValueChanged.value(selectedValue)
    }

    fun applyOffsetDelta(delta: Float) {
        val nextState = advanceTimeWheelOffset(
            state = TimeWheelOffsetState(selectedValue, dragOffsetPx),
            deltaPx = delta,
            rowHeightPx = rowHeightPx,
            valueCount = valueCount,
        )
        if (nextState.value != selectedValue) {
            selectedValue = nextState.value
            latestOnValueChanged.value(nextState.value)
        }
        dragOffsetPx = nextState.offsetPx
    }

    fun cancelAnimation() {
        animationGeneration += 1
        animationJob?.cancel()
        animationJob = null
        onSettlingChanged(false)
    }

    fun snapToNearestValue() {
        if (dragOffsetPx <= -rowHeightPx / 2f) {
            commitDelta(1)
            dragOffsetPx += rowHeightPx
        } else if (dragOffsetPx >= rowHeightPx / 2f) {
            commitDelta(-1)
            dragOffsetPx -= rowHeightPx
        }
    }

    fun settleWheel(velocity: Float) {
        cancelAnimation()
        val generation = animationGeneration + 1
        animationGeneration = generation
        onSettlingChanged(true)
        animationJob = coroutineScope.launch {
            try {
                if (kotlin.math.abs(velocity) >= minimumFlingVelocity) {
                    var previous = 0f
                    flingAnimation.snapTo(0f)
                    flingAnimation.animateDecay(
                        initialVelocity = velocity,
                        animationSpec = decayAnimationSpec,
                    ) {
                        val current = this.value
                        val delta = current - previous
                        previous = current
                        applyOffsetDelta(delta)
                    }
                }

                snapToNearestValue()
                settleAnimation.snapTo(dragOffsetPx)
                settleAnimation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 1f, stiffness = 700f),
                ) {
                    dragOffsetPx = this.value
                }
                dragOffsetPx = 0f
            } catch (_: CancellationException) {
                // A new gesture or option tap takes ownership of the wheel.
            } finally {
                if (generation == animationGeneration) {
                    animationJob = null
                    onSettlingChanged(false)
                }
            }
        }
    }

    val draggableState = rememberDraggableState { dragAmount ->
        applyOffsetDelta(dragAmount)
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
                    onDragStarted = { cancelAnimation() },
                    onDragStopped = { velocity -> settleWheel(velocity) },
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
                        cancelAnimation()
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

internal data class TimeWheelOffsetState(
    val value: Int,
    val offsetPx: Float,
)

internal fun advanceTimeWheelOffset(
    state: TimeWheelOffsetState,
    deltaPx: Float,
    rowHeightPx: Float,
    valueCount: Int,
): TimeWheelOffsetState {
    require(rowHeightPx > 0f) { "Time wheel row height must be positive." }

    var nextValue = wrapTimeWheelValue(state.value, valueCount)
    var nextOffset = state.offsetPx + deltaPx
    while (nextOffset <= -rowHeightPx) {
        nextValue = wrapTimeWheelValue(nextValue + 1, valueCount)
        nextOffset += rowHeightPx
    }
    while (nextOffset >= rowHeightPx) {
        nextValue = wrapTimeWheelValue(nextValue - 1, valueCount)
        nextOffset -= rowHeightPx
    }
    return TimeWheelOffsetState(nextValue, nextOffset)
}
