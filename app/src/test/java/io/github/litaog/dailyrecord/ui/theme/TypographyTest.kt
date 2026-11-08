package io.github.litaog.dailyrecord.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the completeness, visual hierarchy, letter tracking,
 * and tabular metric configurations of the Daily Record typography scale.
 */
class TypographyTest {

    @Test
    fun allFifteenM3RolesAreExplicitlyDefined() {
        val styles = listOf(
            Typography.displayLarge,
            Typography.displayMedium,
            Typography.displaySmall,
            Typography.headlineLarge,
            Typography.headlineMedium,
            Typography.headlineSmall,
            Typography.titleLarge,
            Typography.titleMedium,
            Typography.titleSmall,
            Typography.bodyLarge,
            Typography.bodyMedium,
            Typography.bodySmall,
            Typography.labelLarge,
            Typography.labelMedium,
            Typography.labelSmall,
        )

        // All 15 styles must use the cohesive app Sans font family
        styles.forEach { style ->
            assertEquals("All M3 styles must use SansSerif", FontFamily.SansSerif, style.fontFamily)
            assertTrue("Line height must equal or exceed font size to prevent clipping", style.lineHeight.value >= style.fontSize.value)
        }
    }

    @Test
    fun fontSizesStrictlyFollowHierarchicalProgression() {
        // Display scale
        assertTrue(Typography.displayLarge.fontSize > Typography.displayMedium.fontSize)
        assertTrue(Typography.displayMedium.fontSize > Typography.displaySmall.fontSize)
        assertTrue(Typography.displaySmall.fontSize > Typography.headlineLarge.fontSize)

        // Headline scale
        assertTrue(Typography.headlineLarge.fontSize > Typography.headlineMedium.fontSize)
        assertTrue(Typography.headlineMedium.fontSize > Typography.headlineSmall.fontSize)
        assertTrue(Typography.headlineSmall.fontSize >= Typography.titleLarge.fontSize)

        // Title scale
        assertTrue(Typography.titleLarge.fontSize > Typography.titleMedium.fontSize)
        assertTrue(Typography.titleMedium.fontSize > Typography.titleSmall.fontSize)

        // Body scale
        assertTrue(Typography.bodyLarge.fontSize > Typography.bodyMedium.fontSize)
        assertTrue(Typography.bodyMedium.fontSize > Typography.bodySmall.fontSize)

        // Label scale
        assertTrue(Typography.labelLarge.fontSize > Typography.labelMedium.fontSize)
        assertTrue(Typography.labelMedium.fontSize > Typography.labelSmall.fontSize)
    }

    @Test
    fun metricNumeralsUseTabularFiguresAndSansFont() {
        val metrics = listOf(MetricNumberLarge, MetricNumberMedium, MetricNumberSmall)
        metrics.forEach { metric ->
            assertEquals("Metric numerals must use SansSerif, not Monospace", FontFamily.SansSerif, metric.fontFamily)
            assertNotEquals("Metric numerals must not use generic typewriter Monospace", FontFamily.Monospace, metric.fontFamily)
            assertEquals("Metric numerals must enable OpenType tabular numbers", "tnum", metric.fontFeatureSettings)
            assertEquals("Metric numerals must be bold for high readability", FontWeight.Bold, metric.fontWeight)
            assertTrue("Line height must be greater than or equal to font size", metric.lineHeight.value >= metric.fontSize.value)
        }

        assertTrue(MetricNumberLarge.fontSize > MetricNumberMedium.fontSize)
        assertTrue(MetricNumberMedium.fontSize > MetricNumberSmall.fontSize)
    }

    @Test
    fun syncFailureGuidancePreservesExactSpecification() {
        assertEquals(FontFamily.SansSerif, SyncFailureGuidance.fontFamily)
        assertEquals(FontWeight.SemiBold, SyncFailureGuidance.fontWeight)
        assertEquals(9.33.sp, SyncFailureGuidance.fontSize)
        assertEquals(13.33.sp, SyncFailureGuidance.lineHeight)
    }
}
