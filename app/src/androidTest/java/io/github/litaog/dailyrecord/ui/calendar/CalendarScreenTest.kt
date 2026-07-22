package io.github.litaog.dailyrecord.ui.calendar

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test

class CalendarScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun explicitZeroAndFutureDateHaveDistinctAccessibleSemantics() {
        val today = LocalDate.of(2026, 7, 17)
        composeRule.setContent {
            DailyRecordTheme {
                CalendarScreen(
                    month = YearMonth.of(2026, 7),
                    focusedDate = today,
                    today = today,
                    records = listOf(record(LocalDate.of(2026, 7, 16), 0)),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onToday = {},
                    onOpenDatePicker = {},
                    onDateSelected = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("2026年7月16日，明确记录 0 次手冲")
            .assertExists()
        composeRule
            .onNodeWithContentDescription("2026年7月18日，未来日期，不可记录")
            .assertExists()
            .assertIsNotEnabled()
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
}
