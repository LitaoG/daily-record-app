package io.github.litaog.dailyrecord.ui.calendar

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CalendarScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactDayCellsHideEmptyLabelsButKeepAccessibleSemantics() {
        val today = LocalDate.of(2026, 7, 17)
        composeRule.setContent {
            DailyRecordTheme {
                CalendarScreen(
                    month = YearMonth.of(2026, 7),
                    focusedDate = today,
                    today = today,
                    records = listOf(
                        record(LocalDate.of(2026, 7, 12), 10),
                        record(LocalDate.of(2026, 7, 13), 3),
                        record(LocalDate.of(2026, 7, 14), 2),
                        record(LocalDate.of(2026, 7, 15), 1),
                        record(LocalDate.of(2026, 7, 16), 0),
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onToday = {},
                    onOpenDatePicker = {},
                    onDateSelected = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("2026年7月16日，手冲，记录为 0 次")
            .assertExists()
        composeRule
            .onNodeWithContentDescription("2026年7月18日，未来日期，不可记录")
            .assertExists()
            .assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("2026年7月13日，手冲，3 次").assertExists()
        composeRule.onNodeWithContentDescription("2026年7月14日，手冲，2 次").assertExists()
        composeRule.onNodeWithContentDescription("2026年7月15日，手冲，1 次").assertExists()
        // The 9+ cell must read with the same bucketing as its visual label;
        // an exact "10 次" description would break the consistency contract.
        composeRule.onNodeWithContentDescription("2026年7月12日，手冲，9 次以上").assertExists()
        composeRule.onAllNodes(
            hasText("未填") and hasAnyAncestor(hasTestTag("calendar_day_2026-07-01")),
            useUnmergedTree = true,
        ).assertCountEquals(0)
        composeRule.onAllNodes(
            hasText("未来") and hasAnyAncestor(hasTestTag("calendar_day_2026-07-18")),
            useUnmergedTree = true,
        ).assertCountEquals(0)
        composeRule.onAllNodesWithText("0次").assertCountEquals(0)
        composeRule.onNodeWithText("9 次以上").assertExists()
        composeRule.onNodeWithText("今").assertExists()
    }

    @Test
    fun monthlySummaryShowsOnlyTotalAndRecordedDays() {
        val today = LocalDate.of(2026, 7, 17)
        composeRule.setContent {
            DailyRecordTheme {
                CalendarScreen(
                    month = YearMonth.of(2026, 7),
                    focusedDate = today,
                    today = today,
                    records = listOf(
                        record(LocalDate.of(2026, 7, 14), 0),
                        record(LocalDate.of(2026, 7, 15), 1),
                        record(LocalDate.of(2026, 7, 16), 2),
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onToday = {},
                    onOpenDatePicker = {},
                    onDateSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("本月 3 次 · 2 天有记录").assertExists()
        composeRule.onAllNodesWithText("1.5次/天").assertCountEquals(0)
        composeRule
            .onNodeWithContentDescription("本月 3 次 · 2 天有记录")
            .assertExists()
        composeRule
            .onNodeWithContentDescription(
                "点击日期填写手冲次数。状态包括：未填写、未来不可填写、0 次和已记录",
            )
            .assertExists()
    }

    @Test
    fun dayTouchTargetsRemainAtLeast48DpAtNormalText() {
        assertDayTargetHeight(fontScale = 1f, minimumDp = 48f)
    }

    @Test
    fun dayCellsExpandAt200PercentText() {
        assertDayTargetHeight(fontScale = 2f, minimumDp = 76f)
    }

    @Test
    fun earliestSupportedMonthDisablesPreviousNavigation() {
        val today = LocalDate.of(2026, 7, 17)
        composeRule.setContent {
            DailyRecordTheme {
                CalendarScreen(
                    month = YearMonth.of(1970, 1),
                    focusedDate = LocalDate.of(1970, 1, 1),
                    today = today,
                    records = emptyList(),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onToday = {},
                    onOpenDatePicker = {},
                    onDateSelected = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("上个月")
            .assertExists()
            .assertIsNotEnabled()
        composeRule
            .onAllNodesWithContentDescription("1969年12月31日，超出支持范围，不可记录")
            .assertCountEquals(0)
    }

    @Test
    fun monthGridHidesAdjacentMonthDatesAndVisibleJumpHint() {
        val today = LocalDate.of(2026, 7, 17)
        composeRule.setContent {
            DailyRecordTheme {
                CalendarScreen(
                    month = YearMonth.of(2026, 7),
                    focusedDate = today,
                    today = today,
                    records = emptyList(),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onToday = {},
                    onOpenDatePicker = {},
                    onDateSelected = {},
                )
            }
        }

        composeRule.onAllNodesWithText("点此快速跳转").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("2026年6月29日，未填写").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("2026年8月1日，未来日期，不可记录").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("2026年7月1日，未填写").assertExists()
    }

    private fun record(date: LocalDate, count: Int) = HandBrewRecord(
        id = date.toString(),
        localDate = date,
        brewCount = count,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun assertDayTargetHeight(fontScale: Float, minimumDp: Float) {
        val today = LocalDate.of(2026, 7, 17)
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                DailyRecordTheme {
                    CalendarScreen(
                        month = YearMonth.of(2026, 7),
                        focusedDate = today,
                        today = today,
                        records = emptyList(),
                        onPreviousMonth = {},
                        onNextMonth = {},
                        onToday = {},
                        onOpenDatePicker = {},
                        onDateSelected = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val heightPx = composeRule
            .onNodeWithTag("calendar_day_2026-07-01")
            .fetchSemanticsNode()
            .boundsInRoot
            .height
        val heightDp = heightPx / context.resources.displayMetrics.density
        assertTrue("Calendar day height was $heightDp dp", heightDp >= minimumDp)
    }
}
