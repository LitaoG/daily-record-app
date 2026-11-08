package io.github.litaog.dailyrecord.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassMaterialTest {

    @Test
    fun elevationTiersPreserveDistinctMonotonicShadows() {
        assertEquals(0.dp, LiquidGlassElevation.Flat.shadow)
        assertEquals(3.dp, LiquidGlassElevation.Subtle.shadow)
        assertEquals(8.dp, LiquidGlassElevation.Elevated.shadow)
        assertEquals(16.dp, LiquidGlassElevation.Emphasis.shadow)

        assertTrue(LiquidGlassElevation.Flat.shadow < LiquidGlassElevation.Subtle.shadow)
        assertTrue(LiquidGlassElevation.Subtle.shadow < LiquidGlassElevation.Elevated.shadow)
        assertTrue(LiquidGlassElevation.Elevated.shadow < LiquidGlassElevation.Emphasis.shadow)
    }

    @Test
    fun elevationTiersPreserveGlowAlphaOrdering() {
        assertEquals(0.0f, LiquidGlassElevation.Flat.spotGlowAlpha, 0.001f)
        assertEquals(0.12f, LiquidGlassElevation.Subtle.spotGlowAlpha, 0.001f)
        assertEquals(0.16f, LiquidGlassElevation.Elevated.spotGlowAlpha, 0.001f)
        assertEquals(0.22f, LiquidGlassElevation.Emphasis.spotGlowAlpha, 0.001f)

        assertTrue(LiquidGlassElevation.Flat.spotGlowAlpha < LiquidGlassElevation.Subtle.spotGlowAlpha)
        assertTrue(LiquidGlassElevation.Subtle.spotGlowAlpha < LiquidGlassElevation.Elevated.spotGlowAlpha)
        assertTrue(LiquidGlassElevation.Elevated.spotGlowAlpha < LiquidGlassElevation.Emphasis.spotGlowAlpha)
    }

    @Test
    fun legacyLiquidRimBrushMaintainsCompatibility() {
        assertNotNull(LiquidRimBrush)
        assertNotNull(liquidRimBrush(LiquidGlassElevation.Flat))
        assertNotNull(liquidRimBrush(LiquidGlassElevation.Subtle))
        assertNotNull(liquidRimBrush(LiquidGlassElevation.Elevated))
        assertNotNull(liquidRimBrush(LiquidGlassElevation.Emphasis))
    }

    @Test
    fun allThemesMaintainNineOptionsWithFullColorPalettes() {
        assertEquals(9, DailyRecordColorTheme.entries.size)
        DailyRecordColorTheme.entries.forEach { theme ->
            assertNotNull(theme.palette.solo)
            assertNotNull(theme.palette.duo)
            assertTrue(theme.palette.solo.gradient.size >= 2)
            assertTrue(theme.palette.duo.gradient.size >= 2)
        }
    }
}
