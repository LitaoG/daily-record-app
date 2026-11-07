package io.github.litaog.dailyrecord.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictiveBackMotionTest {

    @Test
    fun scaleInterpolatesSmoothlyBetweenOneAndNinetyTwoPercent() {
        assertEquals(1.0f, calculatePredictiveBackScale(0f), 0.001f)
        assertEquals(0.96f, calculatePredictiveBackScale(0.5f), 0.001f)
        assertEquals(0.92f, calculatePredictiveBackScale(1.0f), 0.001f)
    }

    @Test
    fun scaleClampsOutOfBoundsValues() {
        assertEquals(1.0f, calculatePredictiveBackScale(-0.5f), 0.001f)
        assertEquals(0.92f, calculatePredictiveBackScale(1.5f), 0.001f)
    }

    @Test
    fun alphaInterpolatesSmoothlyBetweenOneAndEightyFivePercent() {
        assertEquals(1.0f, calculatePredictiveBackAlpha(0f), 0.001f)
        assertEquals(0.925f, calculatePredictiveBackAlpha(0.5f), 0.001f)
        assertEquals(0.85f, calculatePredictiveBackAlpha(1.0f), 0.001f)
    }

    @Test
    fun alphaClampsOutOfBoundsValues() {
        assertEquals(1.0f, calculatePredictiveBackAlpha(-0.5f), 0.001f)
        assertEquals(0.85f, calculatePredictiveBackAlpha(2.0f), 0.001f)
    }

    @Test
    fun cornerRadiusInterpolatesFromZeroToTwentyFourDp() {
        assertEquals(0.dp, calculatePredictiveBackCornerRadius(0f))
        assertEquals(12.dp, calculatePredictiveBackCornerRadius(0.5f))
        assertEquals(24.dp, calculatePredictiveBackCornerRadius(1.0f))
    }

    @Test
    fun cornerRadiusClampsOutOfBoundsValues() {
        assertEquals(0.dp, calculatePredictiveBackCornerRadius(-0.2f))
        assertEquals(24.dp, calculatePredictiveBackCornerRadius(1.2f))
    }
}
