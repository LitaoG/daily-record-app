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
    /** Every module token set of every selectable theme (ADR-022). */
    private val allModuleTokens: List<Pair<String, RecordModuleColorTokens>> =
        DailyRecordColorTheme.entries.flatMap { theme ->
            listOf(
                "${theme.id}.solo" to theme.palette.solo,
                "${theme.id}.duo" to theme.palette.duo,
            )
        }

    @Test
    fun approvedFoundationColorsRemainExact() {
        assertArgb(0xFFF8FAFC, DailyRecordCanvas)
        assertArgb(0xFFFFFFFF, DailyRecordSurface)
        assertArgb(0xFF1E293B, DailyRecordText)
        assertArgb(0xFF475569, DailyRecordTextSecondary)
        assertArgb(0xFF586880, DailyRecordTextMuted)
        assertArgb(0xFFE2E8F0, DailyRecordDivider)
        assertArgb(0xFFE6EAF0, DailyRecordSurfaceDisabled)
        assertArgb(0xFF536078, DailyRecordPeriodInactiveText)
    }

    @Test
    fun defaultThemeIsAuroraWithApprovedTokens() {
        assertEquals(DailyRecordColorTheme.Aurora, DailyRecordColorTheme.Default)
        val aurora = DailyRecordColorTheme.Aurora.palette
        assertArgb(0xFF7C3AED, aurora.solo.primary)
        assertArgb(0xFF6D28D9, aurora.solo.strong)
        assertArgb(0xFFFAF7FE, aurora.solo.unset)
        assertArgb(0xFFF5EEFD, aurora.solo.soft)
        assertArgb(0xFFE9D5FF, aurora.solo.medium)
        assertArgb(0xFFD8B4FE, aurora.solo.intense)
        assertArgb(0xFFC084FC, aurora.solo.peak)
        assertArgb(0xFFDE1B46, aurora.duo.primary)
        assertArgb(0xFFBE123C, aurora.duo.strong)
        assertArgb(0xFFFFF8F9, aurora.duo.unset)
        assertArgb(0xFFFFF1F2, aurora.duo.soft)
        assertArgb(0xFFFFE4E6, aurora.duo.medium)
        assertArgb(0xFFFECDD3, aurora.duo.intense)
        assertArgb(0xFFFB7185, aurora.duo.peak)
    }

    @Test
    fun selectedThemeDrivesModuleTokens() {
        val previous = DailyRecordThemeState.current
        try {
            DailyRecordColorTheme.entries.forEach { theme ->
                DailyRecordThemeState.current = theme
                assertEquals(theme.palette.solo, HandBrewColorTokens)
                assertEquals(theme.palette.duo, SexColorTokens)
                assertEquals(theme.palette.solo.primary, DailyRecordDefaultAccent)
                assertEquals(theme.palette.solo.strong, DailyRecordDefaultAccentStrong)
                assertEquals(theme.palette.solo.soft, DailyRecordDefaultAccentSoft)
            }
        } finally {
            DailyRecordThemeState.current = previous
        }
    }

    @Test
    fun themeIdsAreUniqueAndUnknownIdsFallBackToDefault() {
        val ids = DailyRecordColorTheme.entries.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        DailyRecordColorTheme.entries.forEach { theme ->
            assertEquals(theme, DailyRecordColorTheme.fromId(theme.id))
        }
        assertEquals(DailyRecordColorTheme.Default, DailyRecordColorTheme.fromId(null))
        assertEquals(DailyRecordColorTheme.Default, DailyRecordColorTheme.fromId("removed_theme"))
    }

    @Test
    fun periodGlassFollowsEachModuleInEveryTheme() {
        DailyRecordColorTheme.entries.forEach { theme ->
            val solo = theme.palette.solo
            val duo = theme.palette.duo
            assertEquals(solo.soft, solo.periodGlassTint)
            assertEquals(solo.primary, solo.periodGlassGlow)
            assertEquals(duo.soft, duo.periodGlassTint)
            assertEquals(duo.primary, duo.periodGlassGlow)
            assertNotEquals(theme.id, solo.periodGlassGlow, duo.periodGlassGlow)
        }
    }

    @Test
    fun neutralTextPairsMeetWcagAa() {
        assertContrastAtLeast(4.5, DailyRecordText, DailyRecordCanvas)
        assertContrastAtLeast(4.5, DailyRecordText, DailyRecordCanvasEnd)
        assertContrastAtLeast(4.5, DailyRecordTextSecondary, DailyRecordCanvas)
        assertContrastAtLeast(4.5, DailyRecordTextMuted, DailyRecordSurfaceMuted)
        assertContrastAtLeast(4.5, DailyRecordTextMuted, DailyRecordCanvas)
        assertContrastAtLeast(4.5, DailyRecordTextSecondary, DailyRecordSurfaceDisabled)
        assertContrastAtLeast(4.5, DailyRecordPeriodInactiveText, Color.White)
    }

    @Test
    fun everyThemeModuleTextPairMeetsWcagAa() {
        allModuleTokens.forEach { (name, module) ->
            assertContrastAtLeast(4.5, module.onPrimary, module.primary, name)
            assertContrastAtLeast(4.5, module.onPrimary, module.strong, name)
            assertContrastAtLeast(4.5, DailyRecordText, module.soft, name)
            assertContrastAtLeast(4.5, DailyRecordText, module.medium, name)
            assertContrastAtLeast(4.5, DailyRecordText, module.intense, name)
            assertContrastAtLeast(4.5, module.onPeak, module.peak, name)
            assertContrastAtLeast(4.5, DailyRecordTextSecondary, module.unset, name)
            // Accent text and outlines on the canvas, and selected labels on tinted sliders.
            assertContrastAtLeast(4.5, module.primary, DailyRecordCanvas, name)
            assertContrastAtLeast(4.5, module.strong, module.soft, name)
        }
    }

    @Test
    fun dailyCountColorsAreDistinctAndProgressFromLightToDark() {
        allModuleTokens.forEach { (name, module) ->
            val countColors = listOf(module.soft, module.medium, module.intense, module.peak)

            assertEquals(name, 4, countColors.distinct().size)
            countColors.zipWithNext().forEach { (lighter, darker) ->
                assertTrue(
                    "$name: expected $lighter to be lighter than $darker",
                    relativeLuminance(lighter) > relativeLuminance(darker),
                )
            }
            assertTrue(
                "$name: expected strong to stay darker than primary",
                relativeLuminance(module.strong) < relativeLuminance(module.primary),
            )
        }
    }

    @Test
    fun fourPlusUsesThePeakStepWithReadableContent() {
        allModuleTokens.forEach { (name, module) ->
            val fourPlus = module.colorsFor(RecordVisualState.FourPlus)
            assertEquals(name, module.peak, fourPlus.background)
            assertContrastAtLeast(4.5, fourPlus.content, fourPlus.background, name)
        }
    }

    @Test
    fun unsetStaysLightestFillOfItsPalette() {
        allModuleTokens.forEach { (name, module) ->
            assertTrue(
                "$name: expected unset to stay lighter than the one-count fill",
                relativeLuminance(module.unset) > relativeLuminance(module.soft),
            )
            assertTrue(
                "$name: expected unset to stay lighter than the future fill",
                relativeLuminance(module.unset) > relativeLuminance(DailyRecordSurfaceDisabled),
            )
        }
    }

    @Test
    fun unsetColorsStayModuleSpecific() {
        DailyRecordColorTheme.entries.forEach { theme ->
            val soloUnset = theme.palette.solo.colorsFor(RecordVisualState.Unset)
            val duoUnset = theme.palette.duo.colorsFor(RecordVisualState.Unset)

            assertEquals(theme.palette.solo.unset, soloUnset.background)
            assertEquals(theme.palette.duo.unset, duoUnset.background)
            assertNotEquals(theme.id, soloUnset.background, duoUnset.background)
        }
    }

    @Test
    fun explicitZeroIsNotVisuallyCollapsedIntoUnset() {
        allModuleTokens.forEach { (name, module) ->
            val unset = module.colorsFor(RecordVisualState.Unset)
            val zero = module.colorsFor(RecordVisualState.ExplicitZero)

            assertNotEquals(name, unset.background, zero.background)
            assertNotEquals(name, unset.outline, zero.outline)
            assertEquals(name, module.primary, zero.outline)
        }
    }

    @Test
    fun futureDatesAreNotVisuallyCollapsedIntoPastUnsetDates() {
        allModuleTokens.forEach { (name, module) ->
            val unset = module.colorsFor(RecordVisualState.Unset)
            val disabled = module.colorsFor(RecordVisualState.Disabled)

            assertNotEquals(name, unset.background, disabled.background)
            assertContrastAtLeast(4.5, disabled.content, disabled.background, name)
        }
    }

    @Test
    fun bothModulesDefineEverySharedRecordState() {
        allModuleTokens.forEach { (_, module) ->
            RecordVisualState.entries.forEach { state ->
                val colors = module.colorsFor(state)
                assertTrue(colors.background != Color.Unspecified)
                assertTrue(colors.content != Color.Unspecified)
                assertTrue(colors.outline != Color.Unspecified)
            }
        }
    }

    @Test
    fun identityGradientsAreDecorativeMultiStop() {
        allModuleTokens.forEach { (name, module) ->
            assertTrue("$name: gradient needs at least two stops", module.gradient.size >= 2)
            assertTrue("$name: glow must stay translucent", module.glow.alpha < .2f)
        }
    }

    private fun assertArgb(expected: Long, actual: Color) {
        assertEquals(expected, actual.toArgb().toLong() and 0xFFFF_FFFFL)
    }

    private fun assertContrastAtLeast(
        minimum: Double,
        foreground: Color,
        background: Color,
        label: String = "",
    ) {
        val lighter = max(relativeLuminance(foreground), relativeLuminance(background))
        val darker = min(relativeLuminance(foreground), relativeLuminance(background))
        val contrast = (lighter + 0.05) / (darker + 0.05)
        assertTrue("$label: expected contrast >= $minimum, was $contrast", contrast >= minimum)
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
