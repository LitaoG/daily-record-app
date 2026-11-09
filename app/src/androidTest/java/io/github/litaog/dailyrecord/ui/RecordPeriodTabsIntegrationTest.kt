package io.github.litaog.dailyrecord.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.AppLanguageState
import io.github.litaog.dailyrecord.core.common.TerminologyStyle
import io.github.litaog.dailyrecord.core.common.ZhStrings
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.ui.components.DailyRecordSnackbarHost
import io.github.litaog.dailyrecord.ui.components.PeriodTabs
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.SexColorTokens
import java.time.Instant
import java.time.LocalDate
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RecordPeriodTabsIntegrationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 7, 17)
    private val instant = Instant.parse("2026-07-17T00:00:00Z")

    @Before
    fun selectHandBrewByDefault() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SelectedRecordModulePreference(context).setSelectedModule(RecordModule.HandBrew)
        // ADR-025: these tests assert the direct wording. Module specs and all
        // copy read the process-global AppLanguageState, which DailyRecordApp
        // only refreshes from the persisted style inside event callbacks, so
        // both the preference and the global copy set must be pinned here.
        TerminologyStylePreference(context).setStyle(TerminologyStyle.DIRECT)
        AppLanguageState.current = ZhStrings(TerminologyStyle.DIRECT)
    }

    @After
    fun resetGlobalCopyState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(TerminologyStylePreference.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(TerminologyStylePreference.KEY_TERMINOLOGY_STYLE)
            .commit()
        // DailyRecordApp compositions overwrite AppLanguageState; restore the
        // default so screen-level tests that read the global stay order-proof.
        AppLanguageState.current = ZhStrings
    }

    @Test
    fun periodTabsKeepEqualTouchTargetsAndInsetGlassSlider() {
        setDualModuleContent()

        composeRule.onNodeWithContentDescription("统计，未选择").performClick()
        composeRule.waitForIdle()

        val container = composeRule.onNodeWithTag("statistics_period_tabs").fetchSemanticsNode().boundsInRoot
        val slider = composeRule.onNodeWithTag("statistics_period_slider").fetchSemanticsNode().boundsInRoot
        val segments = StatisticsPeriod.entries.map { period ->
            composeRule.onNodeWithTag("statistics_period_${period.name}").fetchSemanticsNode().boundsInRoot
        }
        val weekLabel = composeRule.onNodeWithText("周").fetchSemanticsNode().boundsInRoot
        val minWidth = segments.minOf { it.width }
        val maxWidth = segments.maxOf { it.width }

        // Segment widths are integer pixels on a device; one pixel of rounding
        // is expected when the full width is not divisible by four.
        assertEquals(minWidth, maxWidth, 1f)
        assertEquals(segments.first().center.y, weekLabel.center.y, 1.5f)
        assertTrue(slider.width < segments.first().width)
        assertTrue(slider.left > segments.first().left)
        assertTrue(slider.right < segments.first().right)
        assertTrue(slider.top > container.top)
        assertTrue(slider.bottom < container.bottom)

        composeRule.onNodeWithTag("statistics_period_Month").performClick()
        composeRule.waitForIdle()
        val movedSlider = composeRule.onNodeWithTag("statistics_period_slider").fetchSemanticsNode().boundsInRoot
        assertEquals(segments[1].left + (segments[1].width - movedSlider.width) / 2f, movedSlider.left, 1.5f)
        assertEquals(segments[1].right - (segments[1].width - movedSlider.width) / 2f, movedSlider.right, 1.5f)
    }

    @Test
    fun periodTabsKeepRatiosAcrossParentWidths() {
        val parentWidth = androidx.compose.runtime.mutableStateOf(240.dp)
        composeRule.setContent {
            DailyRecordTheme {
                Box(modifier = Modifier.width(parentWidth.value)) {
                    PeriodTabs(
                        selected = StatisticsPeriod.Week,
                        onSelected = {},
                        colors = HandBrewColorTokens,
                    )
                }
            }
        }

        listOf(240.dp, 360.dp, 600.dp).forEach { width ->
            composeRule.runOnIdle { parentWidth.value = width }
            composeRule.waitForIdle()

            val container = composeRule.onNodeWithTag("statistics_period_tabs").fetchSemanticsNode().boundsInRoot
            val segments = StatisticsPeriod.entries.map { period ->
                composeRule.onNodeWithTag("statistics_period_${period.name}").fetchSemanticsNode().boundsInRoot
            }
            val slider = composeRule.onNodeWithTag("statistics_period_slider").fetchSemanticsNode().boundsInRoot

            assertTrue(container.width > 0f)
            assertEquals(segments.first().width, segments[1].width, 1f)
            assertEquals(segments.first().width, segments[2].width, 1f)
            assertEquals(segments.first().width, segments[3].width, 1f)
            assertTrue(slider.width < segments.first().width)
            assertTrue(slider.left > segments.first().left)
            assertTrue(slider.right < segments.first().right)
            assertTrue(slider.top > container.top)
            assertTrue(slider.bottom < container.bottom)
        }
    }

    @Test
    fun snackbarUsesTheActiveModuleColorTokens() {
        val hostState = SnackbarHostState()
        val activeColors = androidx.compose.runtime.mutableStateOf(HandBrewColorTokens)
        composeRule.setContent {
            DailyRecordTheme {
                DailyRecordSnackbarHost(
                    hostState = hostState,
                    colors = activeColors.value,
                )
                LaunchedEffect(Unit) {
                    hostState.showSnackbar("网络提示", duration = SnackbarDuration.Indefinite)
                }
            }
        }

        assertSnackbarColor(HandBrewColorTokens.strong)
        composeRule.runOnIdle { activeColors.value = SexColorTokens }
        assertSnackbarColor(SexColorTokens.strong)
    }

    private fun assertSnackbarColor(expected: Color) {
        var bitmap: androidx.compose.ui.graphics.ImageBitmap? = null
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                bitmap = composeRule.onNodeWithTag("daily_record_snackbar").captureToImage()
            }.isSuccess
        }
        val pixels = requireNotNull(bitmap).toPixelMap()
        val sampleX = (pixels.width - 8).coerceAtLeast(0)
        val sampleY = pixels.height / 2
        assertEquals(expected.toArgb(), pixels[sampleX, sampleY].toArgb())
    }

    private fun assertMinimumHeight(tag: String, minimumDp: Float) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val heightPx = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.height
        val heightDp = heightPx / context.resources.displayMetrics.density
        assertTrue("$tag height was $heightDp dp", heightDp >= minimumDp)
    }

    private fun assertMinimumSize(tag: String, minimumDp: Float) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val density = context.resources.displayMetrics.density
        val widthDp = bounds.width / density
        val heightDp = bounds.height / density
        assertTrue("$tag width was $widthDp dp", widthDp >= minimumDp)
        assertTrue("$tag height was $heightDp dp", heightDp >= minimumDp)
    }

    private fun setDualModuleContent(fontScale: Float = 1f) {
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                DailyRecordTheme {
                    DailyRecordApp(
                        repository = FakeHandBrewRecordRepository(
                            initialRecords = listOf(
                                HandBrewRecord(
                                    id = "brew",
                                    localDate = today,
                                    brewCount = 2,
                                    createdAt = instant,
                                    updatedAt = instant,
                                ),
                            ),
                        ),
                        sexRepository = FakeSexRecordRepository(
                            initialRecords = listOf(
                                SexRecord(
                                    id = "sex",
                                    localDate = today,
                                    sexCount = 1,
                                    createdAt = instant,
                                    updatedAt = instant,
                                ),
                            ),
                        ),
                        today = today,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }
}
