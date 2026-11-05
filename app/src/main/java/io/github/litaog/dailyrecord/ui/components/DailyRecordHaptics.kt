package io.github.litaog.dailyrecord.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import io.github.litaog.dailyrecord.ui.theme.DailyRecordMotion

/**
 * System haptics for counting and switching (ADR-022). Uses the platform
 * feedback constants, so the user's system "touch feedback" setting still
 * decides whether anything vibrates; no vibration permission is needed.
 */
internal object DailyRecordHaptics {
    /** A light tick for each +1 / -1 step. */
    fun tick(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /** A firmer confirmation after a record is saved. */
    fun success(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /** A light tick when the module, period or month changes. */
    fun tabSwitch(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}

/**
 * Material 3 Expressive press feedback: the control shrinks slightly while
 * pressed and springs back on release. Layout size and hit area never change.
 */
internal fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) DailyRecordMotion.PressedScale else 1f,
        animationSpec = DailyRecordMotion.effects(),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
