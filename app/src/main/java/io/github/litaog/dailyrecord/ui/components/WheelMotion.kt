package io.github.litaog.dailyrecord.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Shared fling/settle lifecycle for the date and time wheels.
 *
 * The two wheels have different value-boundary rules and different row
 * rendering, but their animation ownership, cancellation generation and
 * decay-to-snap sequence are identical. Keeping that lifecycle here prevents
 * a fix in one wheel from silently diverging from the other.
 */
internal class WheelMotionController internal constructor(
    private val coroutineScope: CoroutineScope,
    private val flingAnimation: Animatable<Float, AnimationVector1D>,
    private val settleAnimation: Animatable<Float, AnimationVector1D>,
    private val decayAnimationSpec: androidx.compose.animation.core.DecayAnimationSpec<Float>,
    private val minimumFlingVelocity: Float,
) {
    private var animationJob: Job? = null
    private var animationGeneration = 0

    fun cancel(onCancelled: () -> Unit = {}) {
        animationGeneration += 1
        animationJob?.cancel()
        animationJob = null
        onCancelled()
    }

    fun settle(
        velocity: Float,
        currentOffset: () -> Float,
        applyOffsetDelta: (Float) -> Unit,
        snapToNearestValue: () -> Unit,
        setOffset: (Float) -> Unit,
        onSettlingChanged: (Boolean) -> Unit = {},
        onSettled: () -> Unit = {},
    ) {
        cancel()
        val generation = animationGeneration + 1
        animationGeneration = generation
        onSettlingChanged(true)
        animationJob = coroutineScope.launch {
            try {
                if (abs(velocity) >= minimumFlingVelocity) {
                    var previous = 0f
                    flingAnimation.snapTo(0f)
                    flingAnimation.animateDecay(
                        initialVelocity = velocity,
                        animationSpec = decayAnimationSpec,
                    ) {
                        val current = value
                        applyOffsetDelta(current - previous)
                        previous = current
                    }
                }

                snapToNearestValue()
                settleAnimation.snapTo(currentOffset())
                settleAnimation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 1f, stiffness = 700f),
                ) {
                    setOffset(value)
                }
                setOffset(0f)
            } catch (_: CancellationException) {
                // A new gesture or option tap takes ownership of the wheel.
            } finally {
                if (generation == animationGeneration) {
                    animationJob = null
                    onSettlingChanged(false)
                    onSettled()
                }
            }
        }
    }
}

@Composable
internal fun rememberWheelMotion(minimumFlingVelocity: Float): WheelMotionController {
    val coroutineScope = rememberCoroutineScope()
    val flingAnimation = remember { Animatable(0f) }
    val settleAnimation = remember { Animatable(0f) }
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    return remember(minimumFlingVelocity, decayAnimationSpec) {
        WheelMotionController(
            coroutineScope = coroutineScope,
            flingAnimation = flingAnimation,
            settleAnimation = settleAnimation,
            decayAnimationSpec = decayAnimationSpec,
            minimumFlingVelocity = minimumFlingVelocity,
        )
    }
}
