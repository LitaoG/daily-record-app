package io.github.litaog.dailyrecord.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Adversarial and empirical stress tests for Daily Record shapes and typography tokens.
 *
 * Stress-tests boundary geometries, extreme densities, 200% accessibility font scaling,
 * numerical precision, and hierarchical monotonicity.
 */
class ThemeAdversarialStressTest {

    private val allShapes: List<Pair<String, CornerBasedShape>> = listOf(
        "ExtraSmall" to DailyRecordShapes.ExtraSmall,
        "Small" to DailyRecordShapes.Small,
        "Medium" to DailyRecordShapes.Medium,
        "Large" to DailyRecordShapes.Large,
        "Full" to DailyRecordShapes.Full,
        "ModuleSelector" to DailyRecordShapes.ModuleSelector,
        "ModuleSelectorSlider" to DailyRecordShapes.ModuleSelectorSlider,
        "Control" to DailyRecordShapes.Control,
        "Compact" to DailyRecordShapes.Compact,
        "Card" to DailyRecordShapes.Card,
        "BottomBar" to DailyRecordShapes.BottomBar,
        "M3.extraSmall" to DailyRecordShapes.Material3Shapes.extraSmall,
        "M3.small" to DailyRecordShapes.Material3Shapes.small,
        "M3.medium" to DailyRecordShapes.Material3Shapes.medium,
        "M3.large" to DailyRecordShapes.Material3Shapes.large,
        "M3.extraLarge" to DailyRecordShapes.Material3Shapes.extraLarge,
    )

    private val allTypographyRoles: List<Pair<String, TextStyle>> = listOf(
        "displayLarge" to Typography.displayLarge,
        "displayMedium" to Typography.displayMedium,
        "displaySmall" to Typography.displaySmall,
        "headlineLarge" to Typography.headlineLarge,
        "headlineMedium" to Typography.headlineMedium,
        "headlineSmall" to Typography.headlineSmall,
        "titleLarge" to Typography.titleLarge,
        "titleMedium" to Typography.titleMedium,
        "titleSmall" to Typography.titleSmall,
        "bodyLarge" to Typography.bodyLarge,
        "bodyMedium" to Typography.bodyMedium,
        "bodySmall" to Typography.bodySmall,
        "labelLarge" to Typography.labelLarge,
        "labelMedium" to Typography.labelMedium,
        "labelSmall" to Typography.labelSmall,
        "MetricNumberLarge" to MetricNumberLarge,
        "MetricNumberMedium" to MetricNumberMedium,
        "MetricNumberSmall" to MetricNumberSmall,
        "SyncFailureGuidance" to SyncFailureGuidance,
    )

    // =========================================================================
    // 1. Shape Adversarial Dimension & Density Stress Tests
    // =========================================================================

    @Test
    fun shapesSurviveZeroDimensionCleanlyWithoutCrashing() {
        val zeroSize = Size.Zero
        val density = Density(density = 2.5f, fontScale = 1.0f)

        allShapes.forEach { (name, shape) ->
            val outline = shape.createOutline(zeroSize, LayoutDirection.Ltr, density)
            assertNotNull("$name outline must not be null at Size.Zero", outline)
            val bounds = outline.bounds
            assertEquals("$name width must be 0 at Size.Zero", 0f, bounds.width, 0.001f)
            assertEquals("$name height must be 0 at Size.Zero", 0f, bounds.height, 0.001f)
            assertFalse("$name bounds must not have NaN", bounds.left.isNaN() || bounds.top.isNaN())
        }
    }

    @Test
    fun shapesSurviveSubPixelAndFractionalDimensionsWithoutOverlapOrCrash() {
        val testSizes = listOf(
            Size(0.001f, 0.001f),
            Size(0.5f, 0.5f),
            Size(1f, 1f),
            Size(2.333f, 4.777f),
            Size(0.1f, 50f),
            Size(50f, 0.1f),
            Size(15.5f, 15.5f), // Smaller than Large (22.dp) and Medium (16.dp) corner radii
        )
        val densities = listOf(
            Density(1f, 1f),
            Density(2.625f, 1f),
            Density(3.5f, 1f),
            Density(4f, 1f),
        )

        allShapes.forEach { (name, shape) ->
            for (density in densities) {
                for (size in testSizes) {
                    val outline = shape.createOutline(size, LayoutDirection.Ltr, density)
                    assertNotNull("$name at size $size density ${density.density} must produce outline", outline)
                    val bounds = outline.bounds
                    assertFalse("$name width at $size must not be NaN", bounds.width.isNaN())
                    assertFalse("$name height at $size must not be NaN", bounds.height.isNaN())
                    assertTrue("$name width at $size must be >= 0", bounds.width >= 0f)
                    assertTrue("$name height at $size must be >= 0", bounds.height >= 0f)
                }
            }
        }
    }

    @Test
    fun shapesSurviveExtremeLargeDimensionsWithoutNumericOverflow() {
        val hugeSizes = listOf(
            Size(10_000f, 10_000f),
            Size(100_000f, 500f),
            Size(500f, 100_000f),
        )
        val density = Density(density = 4f, fontScale = 1f)

        allShapes.forEach { (name, shape) ->
            for (size in hugeSizes) {
                val outline = shape.createOutline(size, LayoutDirection.Ltr, density)
                val bounds = outline.bounds
                assertFalse("$name bounds must not overflow to infinite", bounds.width.isInfinite() || bounds.height.isInfinite())
                assertEquals("$name width must match huge size", size.width, bounds.width, 0.1f)
                assertEquals("$name height must match huge size", size.height, bounds.height, 0.1f)
            }
        }
    }

    @Test
    fun shapesUnderLtrAndRtlLayoutDirectionsBehaveSymmetrically() {
        val size = Size(120f, 48f)
        val density = Density(density = 2.625f, fontScale = 1f)

        allShapes.forEach { (name, shape) ->
            val ltrOutline = shape.createOutline(size, LayoutDirection.Ltr, density)
            val rtlOutline = shape.createOutline(size, LayoutDirection.Rtl, density)
            assertEquals("$name LTR and RTL bounds width must match", ltrOutline.bounds.width, rtlOutline.bounds.width, 0.001f)
            assertEquals("$name LTR and RTL bounds height must match", ltrOutline.bounds.height, rtlOutline.bounds.height, 0.001f)
        }
    }

    @Test
    fun fullPillShapeClampsToCapsuleUnderExtremeAspectRatios() {
        val density = Density(density = 2.75f, fontScale = 1f)

        // Ultra wide capsule: corner radius should cap at height / 2
        val wideSize = Size(400f, 48f)
        val wideOutline = DailyRecordShapes.Full.createOutline(wideSize, LayoutDirection.Ltr, density)
        assertTrue("Wide full shape must produce Outline.Rounded", wideOutline is Outline.Rounded)
        val wideRound = (wideOutline as Outline.Rounded).roundRect
        // In Compose, percent = 50 on height = 48 gives 24 corner radius
        assertEquals(24f, wideRound.topLeftCornerRadius.y, 0.01f)

        // Ultra narrow capsule: corner radius should cap at width / 2
        val tallSize = Size(48f, 400f)
        val tallOutline = DailyRecordShapes.Full.createOutline(tallSize, LayoutDirection.Ltr, density)
        assertTrue("Tall full shape must produce Outline.Rounded", tallOutline is Outline.Rounded)
        val tallRound = (tallOutline as Outline.Rounded).roundRect
        // 50% on width = 48 gives 24 corner radius
        assertEquals(24f, tallRound.topLeftCornerRadius.x, 0.01f)
    }

    // =========================================================================
    // 2. Typography 200% Accessibility Font Scale Stress Tests
    // =========================================================================

    @Test
    fun allTypographyTokensHaveSpUnitsAndPositiveValues() {
        allTypographyRoles.forEach { (name, style) ->
            assertEquals("$name fontSize must be in Sp units", TextUnitType.Sp, style.fontSize.type)
            assertEquals("$name lineHeight must be in Sp units", TextUnitType.Sp, style.lineHeight.type)
            assertTrue("$name fontSize must be strictly positive", style.fontSize.value > 0f)
            assertTrue("$name lineHeight must be strictly positive", style.lineHeight.value > 0f)
            assertTrue(
                "$name lineHeight (${style.lineHeight.value}sp) must be >= fontSize (${style.fontSize.value}sp) to prevent line overlap",
                style.lineHeight.value >= style.fontSize.value,
            )
        }
    }

    @Test
    fun typographyScalesMonotonicallyUnder200PercentAccessibilityFontScale() {
        val baseDensity = Density(density = 2.75f, fontScale = 1.0f)
        val a11yDensity = Density(density = 2.75f, fontScale = 2.0f)

        allTypographyRoles.forEach { (name, style) ->
            val baseFontPx = with(baseDensity) { style.fontSize.toPx() }
            val a11yFontPx = with(a11yDensity) { style.fontSize.toPx() }
            val baseLinePx = with(baseDensity) { style.lineHeight.toPx() }
            val a11yLinePx = with(a11yDensity) { style.lineHeight.toPx() }

            // Verify non-NaN and non-infinite
            assertFalse("$name fontPx at 200% must not be NaN", a11yFontPx.isNaN())
            assertFalse("$name linePx at 200% must not be NaN", a11yLinePx.isNaN())
            assertFalse("$name fontPx at 200% must not be Infinite", a11yFontPx.isInfinite())
            assertFalse("$name linePx at 200% must not be Infinite", a11yLinePx.isInfinite())

            // In Android 14+ / Compose (API 34+), typography enforces non-linear font scaling:
            // small body text scales up to 200%, while large display/headline roles use a compressed
            // curve to prevent screen overflow. Assert non-linear accessibility bounds:
            // 1. Scaled font and line height must strictly exceed base (positive scaling)
            assertTrue("$name fontPx at 200% ($a11yFontPx) must exceed base ($baseFontPx)", a11yFontPx > baseFontPx)
            assertTrue("$name linePx at 200% ($a11yLinePx) must exceed base ($baseLinePx)", a11yLinePx > baseLinePx)

            // 2. Scaled font and line height must not exceed the linear 2.0x theoretical ceiling
            assertTrue("$name fontPx at 200% ($a11yFontPx) must not exceed 2x linear limit (${baseFontPx * 2.0f})", a11yFontPx <= baseFontPx * 2.0f + 0.01f)
            assertTrue("$name linePx at 200% ($a11yLinePx) must not exceed 2x linear limit (${baseLinePx * 2.0f})", a11yLinePx <= baseLinePx * 2.0f + 0.01f)

            // 3. Line height must maintain vertical headroom >= font size to prevent overlapping lines
            assertTrue("$name lineHeight ($a11yLinePx) must maintain >= fontSize ($a11yFontPx) at 200% scale", a11yLinePx >= a11yFontPx)
        }
    }

    @Test
    fun metricNumeralsPreserveTabularSettingsAndScaleHarmoniously() {
        val metrics = listOf(
            "MetricNumberLarge" to MetricNumberLarge,
            "MetricNumberMedium" to MetricNumberMedium,
            "MetricNumberSmall" to MetricNumberSmall,
        )

        metrics.forEach { (name, metric) ->
            assertEquals("$name must enforce OpenType tnum feature", "tnum", metric.fontFeatureSettings)
            assertEquals("$name must use SansSerif font family", FontFamily.SansSerif, metric.fontFamily)
            assertEquals("$name must be bold", FontWeight.Bold, metric.fontWeight)
            assertTrue("$name line height must exceed font size", metric.lineHeight.value > metric.fontSize.value)
        }

        // Progression check
        assertTrue(MetricNumberLarge.fontSize > MetricNumberMedium.fontSize)
        assertTrue(MetricNumberMedium.fontSize > MetricNumberSmall.fontSize)
        assertTrue(MetricNumberLarge.lineHeight > MetricNumberMedium.lineHeight)
        assertTrue(MetricNumberMedium.lineHeight > MetricNumberSmall.lineHeight)

        // At 200% scale, line height delta must remain positive
        val density = Density(density = 3f, fontScale = 2.0f)
        val largeH = with(density) { MetricNumberLarge.lineHeight.toPx() }
        val medH = with(density) { MetricNumberMedium.lineHeight.toPx() }
        val smallH = with(density) { MetricNumberSmall.lineHeight.toPx() }
        assertTrue(largeH > medH)
        assertTrue(medH > smallH)
    }

    // =========================================================================
    // 3. Hierarchy Inversion & Ratio Stress Tests
    // =========================================================================

    @Test
    fun headlineProgressionIsStrictlyMonotonicAndNonInverting() {
        // Explicitly check the historical inversion bug: headlineSmall was previously 24sp > headlineMedium 22sp
        assertTrue(
            "headlineLarge (${Typography.headlineLarge.fontSize}) must be > headlineMedium (${Typography.headlineMedium.fontSize})",
            Typography.headlineLarge.fontSize > Typography.headlineMedium.fontSize,
        )
        assertTrue(
            "headlineMedium (${Typography.headlineMedium.fontSize}) must be > headlineSmall (${Typography.headlineSmall.fontSize})",
            Typography.headlineMedium.fontSize > Typography.headlineSmall.fontSize,
        )
        assertEquals("headlineLarge must be 28sp", 28.sp, Typography.headlineLarge.fontSize)
        assertEquals("headlineMedium must be 24sp", 24.sp, Typography.headlineMedium.fontSize)
        assertEquals("headlineSmall must be 20sp", 20.sp, Typography.headlineSmall.fontSize)

        // Line heights must also follow strict progression
        assertTrue(Typography.headlineLarge.lineHeight > Typography.headlineMedium.lineHeight)
        assertTrue(Typography.headlineMedium.lineHeight > Typography.headlineSmall.lineHeight)
        assertEquals(36.sp, Typography.headlineLarge.lineHeight)
        assertEquals(32.sp, Typography.headlineMedium.lineHeight)
        assertEquals(28.sp, Typography.headlineSmall.lineHeight)
    }

    @Test
    fun lineSpacingRatiosRemainComfortableAcrossAllRoles() {
        allTypographyRoles.forEach { (name, style) ->
            val ratio = style.lineHeight.value / style.fontSize.value
            assertTrue(
                "$name line-height to font-size ratio ($ratio) must be >= 1.05 to avoid visual collision",
                ratio >= 1.05f,
            )
            assertTrue(
                "$name line-height to font-size ratio ($ratio) must be <= 1.6 to avoid excessively detached lines",
                ratio <= 1.6f,
            )
        }
    }

    @Test
    fun letterSpacingStaysWithinReasonableBoundsToPreventKerningClipping() {
        allTypographyRoles.forEach { (name, style) ->
            if (style.letterSpacing != androidx.compose.ui.unit.TextUnit.Unspecified) {
                assertEquals("$name letterSpacing must be in Sp", TextUnitType.Sp, style.letterSpacing.type)
                assertTrue(
                    "$name letterSpacing (${style.letterSpacing.value}sp) must not be tighter than -1.0sp",
                    style.letterSpacing.value >= -1.0f,
                )
                assertTrue(
                    "$name letterSpacing (${style.letterSpacing.value}sp) must not exceed 1.0sp",
                    style.letterSpacing.value <= 1.0f,
                )
            }
        }
    }
}
