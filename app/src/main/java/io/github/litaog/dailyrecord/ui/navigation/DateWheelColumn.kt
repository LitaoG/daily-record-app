package io.github.litaog.dailyrecord.ui.navigation

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.components.DailyRecordHaptics
import io.github.litaog.dailyrecord.ui.components.rememberWheelMotion
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens

private const val MINIMUM_FLING_VELOCITY_DP_PER_SECOND = 90f

@Composable
internal fun DateWheelColumn(
    values: List<Int>,
    selectedValue: Int,
    label: String,
    valueLabel: (Int) -> String,
    optionEnabled: (Int) -> Boolean,
    onValueSelected: (Int) -> Unit,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    val initialIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    var selectedIndex by remember(values) { mutableIntStateOf(initialIndex) }
    var dragOffsetPx by remember(values) { mutableFloatStateOf(0f) }
    var isGestureActive by remember { mutableStateOf(false) }
    val latestOnValueSelected = rememberUpdatedState(onValueSelected)
    val latestOptionEnabled = rememberUpdatedState(optionEnabled)
    val rowHeightPx = with(LocalDensity.current) { DailyRecordSizes.MinimumTouchTarget.toPx() }
    val minimumFlingVelocity = with(LocalDensity.current) {
        MINIMUM_FLING_VELOCITY_DP_PER_SECOND.dp.toPx()
    }
    val wheelMotion = rememberWheelMotion(minimumFlingVelocity)
    val shape = DailyRecordShapes.Medium
    // One light tick each time a new value reaches the center line, like the
    // time wheel (ADR-022). A value clamped by another wheel does not tick.
    val haptic = LocalHapticFeedback.current

    /**
     * Keep the local wheel position in sync with changes made by another wheel
     * (for example, changing the year can clamp the day). During a drag the
     * local position is authoritative so recomposition cannot snap the content
     * back under the user's finger.
     */
    fun cancelAnimation() {
        wheelMotion.cancel()
    }

    LaunchedEffect(selectedValue, values, isGestureActive) {
        // A value emitted by this wheel already has the matching local index.
        // Only rebase when another wheel changed the valid values window (for
        // example, changing the year can clamp the day wheel).
        if (!isGestureActive && values.getOrNull(selectedIndex) != selectedValue) {
            cancelAnimation()
            selectedIndex = values.indexOf(selectedValue).coerceAtLeast(0)
            dragOffsetPx = 0f
        }
    }

    fun commitIndex(index: Int): Boolean {
        val candidate = values.getOrNull(index) ?: return false
        if (!latestOptionEnabled.value(candidate)) return false
        selectedIndex = index
        latestOnValueSelected.value(candidate)
        return true
    }

    fun applyOffsetDelta(delta: Float) {
        val indexBefore = selectedIndex
        var nextOffset = dragOffsetPx + delta

        // Rebase the rendered three-row window whenever a full row passes the
        // center. This is used by both the finger drag and the inertial decay,
        // so a quick swipe can travel across multiple date values smoothly.
        while (nextOffset <= -rowHeightPx && selectedIndex < values.lastIndex) {
            if (!commitIndex(selectedIndex + 1)) break
            nextOffset += rowHeightPx
        }
        while (nextOffset >= rowHeightPx && selectedIndex > 0) {
            if (!commitIndex(selectedIndex - 1)) break
            nextOffset -= rowHeightPx
        }
        // At most one tick per call, so a fast fling ticks at most once per frame.
        if (selectedIndex != indexBefore) DailyRecordHaptics.tick(haptic)

        // Add a small edge resistance instead of exposing an empty row when
        // the wheel is already at its first or last value.
        dragOffsetPx = when {
            selectedIndex == 0 && nextOffset > 0f ->
                (nextOffset - delta + delta * 0.24f)
                    .coerceAtMost(rowHeightPx * 0.65f)
            selectedIndex == values.lastIndex && nextOffset < 0f ->
                (nextOffset - delta + delta * 0.24f)
                    .coerceAtLeast(-rowHeightPx * 0.65f)
            else -> nextOffset
        }
    }

    fun snapToNearestValue() {
        if (dragOffsetPx <= -rowHeightPx / 2f && selectedIndex < values.lastIndex) {
            if (commitIndex(selectedIndex + 1)) {
                dragOffsetPx += rowHeightPx
                DailyRecordHaptics.tick(haptic)
            }
        } else if (dragOffsetPx >= rowHeightPx / 2f && selectedIndex > 0) {
            if (commitIndex(selectedIndex - 1)) {
                dragOffsetPx -= rowHeightPx
                DailyRecordHaptics.tick(haptic)
            }
        }
    }

    fun settleWheel(velocity: Float) {
        wheelMotion.settle(
            velocity = velocity,
            currentOffset = { dragOffsetPx },
            applyOffsetDelta = ::applyOffsetDelta,
            snapToNearestValue = ::snapToNearestValue,
            setOffset = { dragOffsetPx = it },
            onSettled = { isGestureActive = false },
        )
    }

    val draggableState = rememberDraggableState { dragAmount ->
        applyOffsetDelta(dragAmount)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(shape)
                .background(DailyRecordSurfaceMuted)
                .border(1.dp, DailyRecordDivider, shape)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStarted = {
                        cancelAnimation()
                        isGestureActive = true
                    },
                    onDragStopped = { velocity -> settleWheel(velocity) },
                ),
        ) {
            Canvas(Modifier.fillMaxWidth().height(160.dp)) {
                val bandHeight = DailyRecordSizes.MinimumTouchTarget.toPx()
                val top = (size.height - bandHeight) / 2f
                drawRect(
                    color = colors.soft.copy(alpha = .78f),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, top),
                    size = androidx.compose.ui.geometry.Size(size.width, bandHeight),
                )
                drawLine(
                    color = colors.primary.copy(alpha = .48f),
                    start = androidx.compose.ui.geometry.Offset(0f, top),
                    end = androidx.compose.ui.geometry.Offset(size.width, top),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = colors.primary.copy(alpha = .48f),
                    start = androidx.compose.ui.geometry.Offset(0f, top + bandHeight),
                    end = androidx.compose.ui.geometry.Offset(size.width, top + bandHeight),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .graphicsLayer { translationY = dragOffsetPx },
            ) {
                WheelValueRow(
                    value = values.getOrNull(selectedIndex - 1),
                    selected = false,
                    valueLabel = valueLabel,
                    enabled = values.getOrNull(selectedIndex - 1)
                        ?.let(latestOptionEnabled.value)
                        ?: false,
                    onClick = {
                        cancelAnimation()
                        isGestureActive = false
                        if (commitIndex(selectedIndex - 1)) {
                            dragOffsetPx = 0f
                            DailyRecordHaptics.tick(haptic)
                        }
                    },
                )
                WheelValueRow(
                    value = values.getOrNull(selectedIndex),
                    selected = true,
                    valueLabel = valueLabel,
                    enabled = values.getOrNull(selectedIndex)
                        ?.let(latestOptionEnabled.value)
                        ?: false,
                    onClick = {
                        cancelAnimation()
                        isGestureActive = false
                        if (commitIndex(selectedIndex)) {
                            dragOffsetPx = 0f
                        }
                    },
                )
                WheelValueRow(
                    value = values.getOrNull(selectedIndex + 1),
                    selected = false,
                    valueLabel = valueLabel,
                    enabled = values.getOrNull(selectedIndex + 1)
                        ?.let(latestOptionEnabled.value)
                        ?: false,
                    onClick = {
                        cancelAnimation()
                        isGestureActive = false
                        if (commitIndex(selectedIndex + 1)) {
                            dragOffsetPx = 0f
                            DailyRecordHaptics.tick(haptic)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun WheelValueRow(
    value: Int?,
    selected: Boolean,
    valueLabel: (Int) -> String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(DailyRecordSizes.MinimumTouchTarget)
            .clickable(enabled = value != null && enabled, role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                this.selected = selected
                if (value != null) contentDescription = valueLabel(value)
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value?.let(valueLabel).orEmpty(),
            color = when {
                selected -> DailyRecordText
                !enabled -> DailyRecordDivider
                else -> DailyRecordTextSecondary.copy(alpha = .72f)
            },
            style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
