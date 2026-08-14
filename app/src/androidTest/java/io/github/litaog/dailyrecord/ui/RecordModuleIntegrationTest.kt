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
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.ui.components.DailyRecordSnackbarHost
import io.github.litaog.dailyrecord.ui.components.PeriodTabs
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.SexColorTokens
import java.time.Instant
import java.time.LocalDate
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RecordModuleIntegrationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 7, 17)
    private val instant = Instant.parse("2026-07-17T00:00:00Z")

    @Before
    fun selectHandBrewByDefault() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SelectedRecordModulePreference(context).setSelectedModule(RecordModule.HandBrew)
    }

    @Test
    fun switchingModuleKeepsMonthAndNeverMixesCounts() {
        setDualModuleContent()

        composeRule.onNodeWithText("本月 2 次 · 1 天有记录").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("上个月").performClick()
        composeRule.onNodeWithContentDescription("选择年份和日期，当前2026年6月").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("做爱记录，未选择").performClick()

        composeRule.onNodeWithText("本月 0 次 · 0 天有记录").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("选择年份和日期，当前2026年6月").assertIsDisplayed()
    }

    @Test
    fun settingsIconFollowsActiveModuleAccentAndKeepsTalkBackLabel() {
        setDualModuleContent()

        composeRule.onNodeWithContentDescription(AppCopy.Settings.open).assertIsDisplayed()
        assertMinimumSize("home_settings_button", 60f)
        assertSettingsIconContains(
            expected = HandBrewColorTokens.primary,
            unexpected = SexColorTokens.primary,
        )

        composeRule.onNodeWithContentDescription("做爱记录，未选择").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(AppCopy.Settings.open).assertIsDisplayed()
        assertSettingsIconContains(
            expected = SexColorTokens.primary,
            unexpected = HandBrewColorTokens.primary,
        )
    }

    @Test
    fun statisticsAndRecordEditorUseSelectedModuleLanguage() {
        setDualModuleContent()

        composeRule.onNodeWithContentDescription("做爱记录，未选择").performClick()
        composeRule.onNodeWithContentDescription("统计，未选择").performClick()
        composeRule.onNodeWithText("本周 · 做爱次数").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("周五 17日，1 次，1 天")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithContentDescription("日历，未选择").performClick()
        composeRule.onNodeWithContentDescription("2026年7月17日，做爱，1 次，今天，已选择").performClick()
        composeRule.onNodeWithText("今天做爱了几次？").assertIsDisplayed()
        composeRule.onNodeWithText("填 0 表示当天没有做爱，会保留记录。").assertIsDisplayed()
    }

    @Test
    fun handBrewDraftNeverLeaksIntoSexModuleForTheSameDate() {
        setDualModuleContent()

        composeRule.onNodeWithContentDescription("2026年7月17日，自慰，2 次，今天，已选择").performClick()
        composeRule.onNodeWithContentDescription("增加一次").performClick()
        composeRule.onNodeWithText("待保存 · 3 次").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回日历").performClick()
        composeRule.onNodeWithText("放弃修改").performClick()

        composeRule.onNodeWithContentDescription("做爱记录，未选择").performClick()
        composeRule.onNodeWithContentDescription("2026年7月17日，做爱，1 次，今天，已选择").performClick()
        composeRule.onAllNodesWithText("待保存 · 3 次").assertCountEquals(0)
        composeRule.onNodeWithText("已记录 · 1 次").assertIsDisplayed()
    }

    @Test
    fun moduleSelectorSplitsTheAvailableWidthAtTheExactCenter() {
        setDualModuleContent()

        val selectorBounds = composeRule
            .onNodeWithTag("record_module_selector")
            .fetchSemanticsNode()
            .boundsInRoot
        val handBrewBounds = composeRule
            .onNodeWithTag("record_module_HandBrew")
            .fetchSemanticsNode()
            .boundsInRoot
        val sexBounds = composeRule
            .onNodeWithTag("record_module_Sex")
            .fetchSemanticsNode()
            .boundsInRoot

        assertEquals(selectorBounds.width / 2f, handBrewBounds.width, 0.5f)
        assertEquals(selectorBounds.width / 2f, sexBounds.width, 0.5f)
        assertEquals(selectorBounds.left, handBrewBounds.left, 0.5f)
        assertEquals(selectorBounds.top, handBrewBounds.top, 0.5f)
        assertEquals(selectorBounds.bottom, handBrewBounds.bottom, 0.5f)
        assertEquals(handBrewBounds.right, sexBounds.left, 0.5f)
        assertEquals(selectorBounds.top, sexBounds.top, 0.5f)
        assertEquals(selectorBounds.right, sexBounds.right, 0.5f)
        assertEquals(selectorBounds.bottom, sexBounds.bottom, 0.5f)
    }

    @Test
    fun handBrewColorCoversTheCompleteSelectedHalfWithoutAnInnerPill() {
        setDualModuleContent()

        assertSelectorHalfColors(
            left = HandBrewColorTokens.primary,
            right = DailyRecordSurface,
        )
    }

    @Test
    fun sexColorCoversTheCompleteSelectedHalfWithoutAnInnerPill() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SelectedRecordModulePreference(context).setSelectedModule(RecordModule.Sex)
        setDualModuleContent()

        assertSelectorHalfColors(
            left = DailyRecordSurface,
            right = SexColorTokens.primary,
        )
    }

    @Test
    fun sharedShellRemainsReadableAndTouchableAt200PercentText() {
        setDualModuleContent(fontScale = 2f)

        assertMinimumHeight("record_module_selector", 52f)
        assertMinimumHeight("record_module_HandBrew", 52f)
        assertMinimumHeight("record_module_Sex", 52f)
        assertMinimumHeight("bottom_destination_日历", 48f)
        assertMinimumHeight("bottom_destination_统计", 48f)
        composeRule.onNodeWithText("自慰").assertIsDisplayed()
        composeRule.onNodeWithText("做爱").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("统计，未选择").performClick()

        assertMinimumHeight("statistics_period_Week", 48f)
        assertMinimumHeight("statistics_period_Month", 48f)
        assertMinimumHeight("statistics_period_Year", 48f)
        assertMinimumHeight("statistics_period_All", 48f)
        composeRule.onNodeWithText("全部").assertIsDisplayed()
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

    private fun assertSelectorHalfColors(left: Color, right: Color) {
        var bitmap: androidx.compose.ui.graphics.ImageBitmap? = null
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                bitmap = composeRule.onNodeWithTag("record_module_selector").captureToImage()
            }.isSuccess
        }
        val resolvedBitmap = requireNotNull(bitmap)
        val pixels = resolvedBitmap.toPixelMap()
        val sampleY = resolvedBitmap.height / 2
        val inset = 10.coerceAtMost(resolvedBitmap.width / 8)

        assertEquals(left.toArgb(), pixels[inset, sampleY].toArgb())
        assertEquals(right.toArgb(), pixels[resolvedBitmap.width - inset - 1, sampleY].toArgb())
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

    private fun assertSettingsIconContains(expected: Color, unexpected: Color) {
        var bitmap: androidx.compose.ui.graphics.ImageBitmap? = null
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                bitmap = composeRule.onNodeWithTag("home_settings_button").captureToImage()
            }.isSuccess
        }
        val pixels = requireNotNull(bitmap).toPixelMap()
        val expectedRed = expected.red
        val expectedGreen = expected.green
        val expectedBlue = expected.blue
        var exactMatches = 0
        for (x in 0 until pixels.width) {
            for (y in 0 until pixels.height) {
                val pixel = pixels[x, y]
                if (abs(pixel.red - expectedRed) <= .08f &&
                    abs(pixel.green - expectedGreen) <= .08f &&
                    abs(pixel.blue - expectedBlue) <= .08f
                ) {
                    exactMatches++
                }
            }
        }
        assertTrue("Expected settings icon to contain $expected, found $exactMatches pixels", exactMatches > 0)
        val unexpectedRed = unexpected.red
        val unexpectedGreen = unexpected.green
        val unexpectedBlue = unexpected.blue
        var unexpectedMatches = 0
        for (x in 0 until pixels.width) {
            for (y in 0 until pixels.height) {
                val pixel = pixels[x, y]
                if (abs(pixel.red - unexpectedRed) <= .08f &&
                    abs(pixel.green - unexpectedGreen) <= .08f &&
                    abs(pixel.blue - unexpectedBlue) <= .08f
                ) {
                    unexpectedMatches++
                }
            }
        }
        assertTrue(
            "Settings icon unexpectedly contained $unexpected in $unexpectedMatches pixels",
            unexpectedMatches == 0,
        )
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
