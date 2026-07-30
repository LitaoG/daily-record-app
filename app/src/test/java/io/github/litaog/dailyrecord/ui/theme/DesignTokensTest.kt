package io.github.litaog.dailyrecord.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignTokensTest {
    @Test
    fun approvedFoundationColorsRemainExact() {
        assertArgb(0xFFFAF8F3, DailyRecordCanvas)
        assertArgb(0xFFFFFEFB, DailyRecordSurface)
        assertArgb(0xFF2D2926, DailyRecordText)
        assertArgb(0xFFD8D0C6, DailyRecordDivider)
        assertArgb(0xFF72517C, HandBrewColorTokens.primary)
        assertArgb(0xFF4B3354, HandBrewColorTokens.strong)
        assertArgb(0xFF8D3E45, SexColorTokens.primary)
        assertArgb(0xFF5F272C, SexColorTokens.strong)
    }

    @Test
    fun moduleAndSemanticTextPairsMeetWcagAa() {
        assertContrastAtLeast(4.5, DailyRecordText, DailyRecordCanvas)
        assertContrastAtLeast(4.5, DailyRecordTextMuted, DailyRecordSurfaceMuted)
        assertContrastAtLeast(4.5, HandBrewColorTokens.onPrimary, HandBrewColorTokens.primary)
        assertContrastAtLeast(4.5, SexColorTokens.onPrimary, SexColorTokens.primary)
        assertContrastAtLeast(4.5, DailyRecordText, HandBrewColorTokens.soft)
        assertContrastAtLeast(4.5, DailyRecordText, HandBrewColorTokens.medium)
        assertContrastAtLeast(4.5, DailyRecordText, SexColorTokens.soft)
        assertContrastAtLeast(4.5, DailyRecordText, SexColorTokens.medium)
    }

    @Test
    fun explicitZeroIsNotVisuallyCollapsedIntoUnset() {
        listOf(HandBrewColorTokens, SexColorTokens).forEach { module ->
            val unset = module.colorsFor(RecordVisualState.Unset)
            val zero = module.colorsFor(RecordVisualState.ExplicitZero)

            assertNotEquals(unset.background, zero.background)
            assertNotEquals(unset.outline, zero.outline)
            assertEquals(module.primary, zero.outline)
        }
    }

    @Test
    fun futureDatesAreNotVisuallyCollapsedIntoPastUnsetDates() {
        listOf(HandBrewColorTokens, SexColorTokens).forEach { module ->
            val unset = module.colorsFor(RecordVisualState.Unset)
            val disabled = module.colorsFor(RecordVisualState.Disabled)

            assertNotEquals(unset.background, disabled.background)
        }
    }

    @Test
    fun bothModulesDefineEverySharedRecordState() {
        listOf(HandBrewColorTokens, SexColorTokens).forEach { module ->
            RecordVisualState.entries.forEach { state ->
                val colors = module.colorsFor(state)
                assertTrue(colors.background != Color.Unspecified)
                assertTrue(colors.content != Color.Unspecified)
                assertTrue(colors.outline != Color.Unspecified)
            }
        }
    }

    private fun assertArgb(expected: Long, actual: Color) {
        assertEquals(expected, actual.toArgb().toLong() and 0xFFFF_FFFFL)
    }

    private fun assertContrastAtLeast(minimum: Double, foreground: Color, background: Color) {
        val lighter = max(relativeLuminance(foreground), relativeLuminance(background))
        val darker = min(relativeLuminance(foreground), relativeLuminance(background))
        val contrast = (lighter + 0.05) / (darker + 0.05)
        assertTrue("Expected contrast >= $minimum, was $contrast", contrast >= minimum)
    }

    private fun relativeLuminance(color: Color): Double {
        val argb = color.toArgb()
        fun channel(shift: Int): Double {
            val value = ((argb shr shift) and 0xFF) / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
