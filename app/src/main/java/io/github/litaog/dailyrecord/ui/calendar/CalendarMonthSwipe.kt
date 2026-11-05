package io.github.litaog.dailyrecord.ui.calendar

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.components.DailyRecordHaptics
import io.github.litaog.dailyrecord.ui.theme.DailyRecordMotion
import java.time.YearMonth
import kotlin.math.abs
import kotlinx.coroutines.launch

/** A completed horizontal swipe on the month grid. */
internal enum class MonthSwipe {
    Previous,
    Next,
}

/**
 * Pure swipe rule (ADR-022): dragging at least [thresholdPx] to the right
 * shows the previous month and to the left the next month. Shorter drags and
 * drags toward a month outside 1970..today do nothing.
 */
internal fun monthSwipeFor(
    dragPx: Float,
    thresholdPx: Float,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
): MonthSwipe? = when {
    dragPx >= thresholdPx && canGoPrevious -> MonthSwipe.Previous
    dragPx <= -thresholdPx && canGoNext -> MonthSwipe.Next
    else -> null
}

/**
 * How far the grid follows the finger: half the drag toward an available
 * month, a small rubber-band pull toward an unavailable one, never more than
 * [maxFollowPx] either way.
 */
internal fun monthSwipeFollowPx(
    dragPx: Float,
    maxFollowPx: Float,
    towardAvailableMonth: Boolean,
): Float = (dragPx * if (towardAvailableMonth) 0.5f else 0.15f).coerceIn(-maxFollowPx, maxFollowPx)

private val MonthSwipeThreshold = 64.dp
private val MonthSwipeMaxFollow = 72.dp

/**
 * Swipe left / right on the month grid to change month, with the grid
 * following the finger and the new month sliding in on a spring. The arrows
 * and the title picker stay, so TalkBack and switch users keep a non-gesture
 * path. Vertical scrolling is untouched: the detector only claims a drag
 * after horizontal touch slop. Month changes from the arrows or the picker
 * slide in from the matching side too.
 */
internal fun Modifier.monthSwipe(
    month: YearMonth,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { MonthSwipeThreshold.toPx() }
    val maxFollowPx = with(density) { MonthSwipeMaxFollow.toPx() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var lastMonth by remember { mutableStateOf(month) }
    val currentCanGoPrevious by rememberUpdatedState(canGoPrevious)
    val currentCanGoNext by rememberUpdatedState(canGoNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)
    val currentOnNext by rememberUpdatedState(onNext)

    LaunchedEffect(month) {
        val previous = lastMonth
        lastMonth = month
        if (month != previous) {
            // A later month enters from the end side, an earlier one from the start side.
            val forward = month > previous
            val enter = if (forward != rtl) maxFollowPx else -maxFollowPx
            offset.snapTo(enter)
            offset.animateTo(0f, DailyRecordMotion.spatial())
        }
    }

    pointerInput(thresholdPx, maxFollowPx, rtl) {
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragCancel = {
                total = 0f
                scope.launch { offset.animateTo(0f, DailyRecordMotion.spatial()) }
            },
            onDragEnd = {
                val drag = if (rtl) -total else total
                total = 0f
                when (monthSwipeFor(drag, thresholdPx, currentCanGoPrevious, currentCanGoNext)) {
                    MonthSwipe.Previous -> {
                        DailyRecordHaptics.tabSwitch(haptic)
                        currentOnPrevious()
                    }
                    MonthSwipe.Next -> {
                        DailyRecordHaptics.tabSwitch(haptic)
                        currentOnNext()
                    }
                    null -> {
                        scope.launch { offset.animateTo(0f, DailyRecordMotion.spatial()) }
                    }
                }
            },
        ) { change, dragAmount ->
            change.consume()
            total += dragAmount
            val drag = if (rtl) -total else total
            val towardAvailable = (drag > 0f && currentCanGoPrevious) || (drag < 0f && currentCanGoNext)
            val follow = monthSwipeFollowPx(total, maxFollowPx, towardAvailable)
            scope.launch { offset.snapTo(follow) }
        }
    }.graphicsLayer {
        translationX = offset.value
        alpha = 1f - (abs(offset.value) / maxFollowPx).coerceAtMost(1f) * 0.35f
    }
}
