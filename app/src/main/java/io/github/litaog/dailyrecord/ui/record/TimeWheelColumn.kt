package io.github.litaog.dailyrecord.ui.record

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
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
import io.github.litaog.dailyrecord.ui.components.DailyRecordHaptics
import io.github.litaog.dailyrecord.ui.components.rememberWheelMotion
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens

@Composable
internal fun TimeWheelColumn(
    initialValue: Int,
    resetKey: Int,
    valueCount: Int,
    unit: String,
    colors: RecordModuleColorTokens,
    rowHeight: androidx.compose.ui.unit.Dp,
    surfaceTestTag: String,
    onValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSettlingChanged: (Boolean) -> Unit = {},
) {
    var selectedValue by remember(resetKey, valueCount) {
        mutableIntStateOf(wrapTimeWheelValue(initialValue, valueCount))
    }
    var dragOffsetPx by remember(resetKey, valueCount) { mutableFloatStateOf(0f) }
    val latestOnValueChanged = rememberUpdatedState(onValueChanged)
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }
    val minimumFlingVelocity = with(LocalDensity.current) {
        MINIMUM_FLING_VELOCITY_DP_PER_SECOND.dp.toPx()
    }
    val wheelMotion = rememberWheelMotion(minimumFlingVelocity)
    val wheelHeight = rowHeight * 3
    val shape = RoundedCornerShape(16.dp)
    // One light tick each time a new value reaches the center line (b1 4.6).
    // A fast fling ticks at most once per frame, because each frame applies
    // its whole offset in one call.
    val haptic = LocalHapticFeedback.current

    fun commitDelta(delta: Int) {
        selectedValue = wrapTimeWheelValue(selectedValue + delta, valueCount)
        latestOnValueChanged.value(selectedValue)
        DailyRecordHaptics.tick(haptic)
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
            DailyRecordHaptics.tick(haptic)
        }
        dragOffsetPx = nextState.offsetPx
    }

    fun cancelAnimation() {
        wheelMotion.cancel { onSettlingChanged(false) }
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
        wheelMotion.settle(
            velocity = velocity,
            currentOffset = { dragOffsetPx },
            applyOffsetDelta = ::applyOffsetDelta,
            snapToNearestValue = ::snapToNearestValue,
            setOffset = { dragOffsetPx = it },
            onSettlingChanged = onSettlingChanged,
        )
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
