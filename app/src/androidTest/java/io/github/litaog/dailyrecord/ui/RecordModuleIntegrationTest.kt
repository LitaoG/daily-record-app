package io.github.litaog.dailyrecord.ui

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.unit.Density
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.SexColorTokens
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RecordModuleIntegrationTest {
    @get:Rule
    val composeRule = createComposeRule()

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

        composeRule.onNodeWithText("本月 2 次 · 1 天").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("上个月").performClick()
        composeRule.onNodeWithContentDescription("选择年份和日期，当前2026年6月").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("做爱记录，未选择").performClick()

        composeRule.onNodeWithText("本月 0 次 · 0 天").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("选择年份和日期，当前2026年6月").assertIsDisplayed()
    }

    @Test
    fun statisticsAndRecordEditorUseSelectedModuleLanguage() {
        setDualModuleContent()

        composeRule.onNodeWithContentDescription("做爱记录，未选择").performClick()
        composeRule.onNodeWithContentDescription("统计，未选择").performClick()
        composeRule.onNodeWithText("本周 · 做爱次数").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("周五 17日，1 次，1 天").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("日历，未选择").performClick()
        composeRule.onNodeWithContentDescription("2026年7月17日，做爱 1 次，今天，已选择").performClick()
        composeRule.onNodeWithText("今天做爱了几次？").assertIsDisplayed()
        composeRule.onNodeWithText("0 次＝明确没有，会保留记录。").assertIsDisplayed()
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
        composeRule.onNodeWithText("手冲").assertIsDisplayed()
        composeRule.onNodeWithText("做爱").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("统计，未选择").performClick()

        assertMinimumHeight("statistics_period_Week", 48f)
        assertMinimumHeight("statistics_period_Month", 48f)
        assertMinimumHeight("statistics_period_Year", 48f)
        assertMinimumHeight("statistics_period_All", 48f)
        composeRule.onNodeWithText("全部").assertIsDisplayed()
    }

    private fun assertSelectorHalfColors(left: Color, right: Color) {
        val bitmap = composeRule.onNodeWithTag("record_module_selector").captureToImage()
        val pixels = bitmap.toPixelMap()
        val sampleY = bitmap.height / 2
        val inset = 10.coerceAtMost(bitmap.width / 8)

        assertEquals(left.toArgb(), pixels[inset, sampleY].toArgb())
        assertEquals(right.toArgb(), pixels[bitmap.width - inset - 1, sampleY].toArgb())
    }

    private fun assertMinimumHeight(tag: String, minimumDp: Float) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val heightPx = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.height
        val heightDp = heightPx / context.resources.displayMetrics.density
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
