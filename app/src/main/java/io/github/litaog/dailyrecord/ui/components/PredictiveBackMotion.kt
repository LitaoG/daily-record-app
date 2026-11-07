package io.github.litaog.dailyrecord.ui.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Predictive back gesture animation state and handler for sub-screens (Settings, Record, Auth).
 *
 * During an edge swipe gesture on Android 14+ (API 34+), the preview scales down slightly
 * into a floating card with rounded corners and gentle dimming, revealing the background.
 * If the user completes the gesture, [onBack] is triggered and the scale resets; if cancelled,
 * it springs back to full size.
 */
@Composable
fun rememberPredictiveBackState(
    enabled: Boolean = true,
    onBack: () -> Unit,
): PredictiveBackState {
    val coroutineScope = rememberCoroutineScope()
    val progressAnimatable = remember { Animatable(0f) }

    PredictiveBackHandler(enabled = enabled) { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { backEvent ->
                progressAnimatable.snapTo(backEvent.progress)
            }
            onBack()
            progressAnimatable.snapTo(0f)
        } catch (_: CancellationException) {
            coroutineScope.launch {
                progressAnimatable.animateTo(0f, animationSpec = spring())
            }
        }
    }

    return remember(progressAnimatable) {
        PredictiveBackState(progressAnimatable)
    }
}

class PredictiveBackState internal constructor(
    private val animatable: Animatable<Float, AnimationVector1D>,
) {
    val progress: Float
        get() = animatable.value
}

/**
 * Calculates the scale factor for a given back gesture progress.
 * At 0f progress, scale is 1.0f (full size).
 * At 1f progress, scale is 0.92f (8% reduction).
 */
internal fun calculatePredictiveBackScale(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    return 1f - (clamped * 0.08f)
}

/**
 * Calculates the alpha factor for a given back gesture progress.
 * At 0f progress, alpha is 1.0f (fully opaque).
 * At 1f progress, alpha is 0.85f (15% reduction).
 */
internal fun calculatePredictiveBackAlpha(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    return 1f - (clamped * 0.15f)
}

/**
 * Calculates the corner radius for a given back gesture progress.
 * At 0f progress, corner radius is 0dp (straight edges).
 * At 1f progress, corner radius is 24dp (rounded card).
 */
internal fun calculatePredictiveBackCornerRadius(progress: Float): Dp {
    val clamped = progress.coerceIn(0f, 1f)
    return (clamped * 24f).dp
}

fun Modifier.predictiveBackPreview(state: PredictiveBackState): Modifier = this
    .graphicsLayer {
        val p = state.progress
        scaleX = calculatePredictiveBackScale(p)
        scaleY = calculatePredictiveBackScale(p)
        alpha = calculatePredictiveBackAlpha(p)
        transformOrigin = TransformOrigin(0.5f, 0.5f)
    }
    .clip(RoundedCornerShape(calculatePredictiveBackCornerRadius(state.progress)))
