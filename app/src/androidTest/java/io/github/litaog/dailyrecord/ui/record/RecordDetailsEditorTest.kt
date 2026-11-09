package io.github.litaog.dailyrecord.ui.record

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.click
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.HandBrewRecordDetail
import io.github.litaog.dailyrecord.ui.FakeHandBrewRecordRepository
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.CompletableDeferred
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test


class RecordDetailsEditorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 7, 17)


    @Test
    fun detailsStayHiddenUntilOpenedAndExposeOccurrenceContextToTalkBack() {
        val repository = FakeHandBrewRecordRepository(
            initialRecords = listOf(record(today, 1)),
            initialDetails = listOf(
                HandBrewRecordDetail(
                    id = "detail-${today}-1",
                    localDate = today,
                    occurrenceIndex = 1,
                    startTime = LocalTime.of(9, 15),
                    endTime = LocalTime.of(9, 45),
                    feeling = "清醒",
                ),
            ),
        )
        setRecordContent(repository)
        composeRule.waitForIdle()

        assertTrue(
            composeRule.onAllNodesWithTag("record_detail_1").fetchSemanticsNodes().isEmpty(),
        )
        composeRule.onNodeWithContentDescription("记录时间和感受").performClick()

        composeRule.onNodeWithTag("record_detail_1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("第 1 次，开始，09:15").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("第 1 次，结束，09:45").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("第 1 次，写感受").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("开始").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("结束").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun detailActionsKeepAllThreeControlsOnOneRowAtStandardPhoneWidth() {
        val repository = FakeHandBrewRecordRepository(initialRecords = listOf(record(today, 2)))
        setRecordContent(repository, width = 360.dp)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("记录时间和感受").performClick()

        val section = composeRule.onNodeWithTag("record_details_section").fetchSemanticsNode().boundsInRoot
        val firstCard = composeRule.onNodeWithTag("record_detail_1").fetchSemanticsNode().boundsInRoot
        val firstStart = composeRule.onNodeWithTag("record_detail_1_start_time").fetchSemanticsNode().boundsInRoot
        val firstEnd = composeRule.onNodeWithTag("record_detail_1_end_time").fetchSemanticsNode().boundsInRoot
        val firstFeeling = composeRule.onNodeWithTag("record_detail_1_feeling").fetchSemanticsNode().boundsInRoot
        val secondStart = composeRule.onNodeWithTag("record_detail_2_start_time").fetchSemanticsNode().boundsInRoot

        // Each occurrence is a full-width glass card; the three controls fill
        // its padded content row with the same inset on both sides.
        assertEquals(section.left, firstCard.left, 0.5f)
        assertEquals(section.right, firstCard.right, 0.5f)
        val inset = firstStart.left - firstCard.left
        assertTrue(inset > 0f)
        assertEquals(inset, firstCard.right - firstFeeling.right, 0.5f)
        assertEquals(firstStart.top, firstEnd.top, 0.5f)
        assertEquals(firstStart.top, firstFeeling.top, 0.5f)
        assertEquals(firstStart.height, firstEnd.height, 0.5f)
        assertEquals(firstStart.height, firstFeeling.height, 0.5f)
        assertTrue(firstStart.right <= firstEnd.left)
        assertTrue(firstEnd.right <= firstFeeling.left)
        assertEquals(firstStart.left, secondStart.left, 0.5f)
    }

    @Test
    fun savingFeelingStaysOnExpandedRecordPageAndKeepsFeelingVisible() {
        val repository = FakeHandBrewRecordRepository()
        setRecordContent(repository)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("增加一次").performClick()
        composeRule.onNodeWithContentDescription("记录时间和感受").performClick()
        composeRule.onNodeWithContentDescription("第 1 次，写感受").performClick()
        composeRule.onNodeWithTag("record_detail_1_feeling_editor").performTextInput("今天感觉很好")
        composeRule.onNodeWithTag("save_record_button").performClick()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("已记录 · 1 次").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("record_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("record_details_section").assertIsDisplayed()
        composeRule.onNodeWithText("今天感觉很好").assertIsDisplayed()
        composeRule.onAllNodesWithTag("record_detail_1_feeling_editor").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("第 1 次，写感受").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回日历").assertIsDisplayed()
    }

    @Test
    fun detailCardsKeepFullWidthWhenPreviousFeelingHasText() {
        val repository = FakeHandBrewRecordRepository(
            initialRecords = listOf(record(today, 2)),
            initialDetails = listOf(
                HandBrewRecordDetail(
                    id = "detail-${today}-1",
                    localDate = today,
                    occurrenceIndex = 1,
                    feeling = "今天感觉很好",
                ),
                HandBrewRecordDetail(
                    id = "detail-${today}-2",
                    localDate = today,
                    occurrenceIndex = 2,
                ),
            ),
        )
        setRecordContent(repository)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("记录时间和感受").performClick()

        val section = composeRule.onNodeWithTag("record_details_section")
            .fetchSemanticsNode()
            .boundsInRoot
        val firstCard = composeRule.onNodeWithTag("record_detail_1")
            .fetchSemanticsNode()
            .boundsInRoot
        val secondCard = composeRule.onNodeWithTag("record_detail_2")
            .fetchSemanticsNode()
            .boundsInRoot

        assertEquals(section.left, firstCard.left, 0.5f)
        assertEquals(section.right, firstCard.right, 0.5f)
        assertEquals(section.left, secondCard.left, 0.5f)
        assertEquals(section.right, secondCard.right, 0.5f)
        // Spacing between the cards replaces the old divider lines.
        assertTrue(secondCard.top > firstCard.bottom)
        assertTrue(
            composeRule.onAllNodesWithTag("record_detail_divider_1").fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun unsetTimeLabelsAreSpecificAndCenteredInsideTheirFields() {
        val repository = FakeHandBrewRecordRepository(initialRecords = listOf(record(today, 1)))
        setRecordContent(repository, width = 390.dp)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("记录时间和感受").performClick()

        val startField = composeRule.onNodeWithTag("record_detail_1_start_time").fetchSemanticsNode().boundsInRoot
        val endField = composeRule.onNodeWithTag("record_detail_1_end_time").fetchSemanticsNode().boundsInRoot
        val startText = composeRule.onNodeWithText("开始时间", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val endText = composeRule.onNodeWithText("结束时间", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

        composeRule.onNodeWithText("开始时间", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("结束时间", useUnmergedTree = true).assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("选择时间", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        assertEquals(startField.center.x, startText.center.x, 0.5f)
        assertEquals(startField.center.y, startText.center.y, 0.5f)
        assertEquals(endField.center.x, endText.center.x, 0.5f)
        assertEquals(endField.center.y, endText.center.y, 0.5f)
    }

    @Test
    fun selectedTimeLabelsRemainCenteredInsideTheirFields() {
        val repository = FakeHandBrewRecordRepository(
            initialRecords = listOf(record(today, 1)),
            initialDetails = listOf(
                HandBrewRecordDetail(
                    id = "detail-${today}-1",
                    localDate = today,
                    occurrenceIndex = 1,
                    startTime = LocalTime.of(9, 15),
                    endTime = LocalTime.of(9, 45),
                    feeling = "",
                ),
            ),
        )
        setRecordContent(repository, width = 390.dp)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("记录时间和感受").performClick()

        val startField = composeRule.onNodeWithTag("record_detail_1_start_time").fetchSemanticsNode().boundsInRoot
        val endField = composeRule.onNodeWithTag("record_detail_1_end_time").fetchSemanticsNode().boundsInRoot
        val startText = composeRule.onNodeWithText("09:15", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val endText = composeRule.onNodeWithText("09:45", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

        assertEquals(startField.center.x, startText.center.x, 0.5f)
        assertEquals(startField.center.y, startText.center.y, 0.5f)
        assertEquals(endField.center.x, endText.center.x, 0.5f)
        assertEquals(endField.center.y, endText.center.y, 0.5f)
    }

    @Test
    fun feelingActionOpensTheRequestedOccurrenceEditor() {
        val repository = FakeHandBrewRecordRepository(initialRecords = listOf(record(today, 2)))
        setRecordContent(repository)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("记录时间和感受").performClick()
        composeRule.onNodeWithContentDescription("第 2 次，写感受").performClick()

        composeRule.onNodeWithTag("record_detail_2_feeling_editor").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("record_detail_1_feeling_editor").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun timeFieldUsesWheelDialogAndCancelKeepsOriginalValue() {
        val repository = FakeHandBrewRecordRepository(
            initialRecords = listOf(record(today, 1)),
            initialDetails = listOf(
                HandBrewRecordDetail(
                    id = "detail-${today}-1",
                    localDate = today,
                    occurrenceIndex = 1,
                    startTime = LocalTime.of(23, 59),
                    endTime = null,
                    feeling = "",
                ),
            ),
        )
        setRecordContent(repository)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("记录时间和感受").performClick()
        composeRule.onNodeWithContentDescription("第 1 次，开始，23:59").performClick()

        composeRule.onNodeWithTag("time_picker_dialog").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("小时，当前 23").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("分钟，当前 59").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("取消").performClick()

        assertTrue(composeRule.onAllNodesWithTag("time_picker_dialog").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithContentDescription("第 1 次，开始，23:59").assertIsDisplayed()
    }

    @Test
    fun confirmingWheelSelectionUpdatesOnlyTheRequestedTime() {
        val repository = FakeHandBrewRecordRepository(
            initialRecords = listOf(record(today, 1)),
            initialDetails = listOf(
                HandBrewRecordDetail(
                    id = "detail-${today}-1",
                    localDate = today,
                    occurrenceIndex = 1,
                    startTime = LocalTime.of(9, 15),
                    endTime = LocalTime.of(12, 45),
                    feeling = "",
                ),
            ),
        )
        setRecordContent(repository)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("记录时间和感受").performClick()
        composeRule.onNodeWithContentDescription("第 1 次，开始，09:15").performClick()
        composeRule.onNodeWithContentDescription("选择小时 10").performClick()
        composeRule.onNodeWithContentDescription("选择分钟 16").performClick()
        composeRule.onNodeWithContentDescription("确定").performClick()

        composeRule.onNodeWithContentDescription("第 1 次，开始，10:16").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("第 1 次，结束，12:45").assertIsDisplayed()
    }

    @Test
    fun confirmingEndWheelSelectionLeavesStartTimeUntouched() {
        val repository = FakeHandBrewRecordRepository(
            initialRecords = listOf(record(today, 1)),
            initialDetails = listOf(
                HandBrewRecordDetail(
                    id = "detail-${today}-1",
                    localDate = today,
                    occurrenceIndex = 1,
                    startTime = LocalTime.of(9, 15),
                    endTime = LocalTime.of(12, 45),
                    feeling = "",
                ),
            ),
        )
        setRecordContent(repository)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("记录时间和感受").performClick()
        composeRule.onNodeWithContentDescription("第 1 次，结束，12:45").performClick()
        composeRule.onNodeWithContentDescription("选择小时 13").performClick()
        composeRule.onNodeWithContentDescription("选择分钟 46").performClick()
        composeRule.onNodeWithContentDescription("确定").performClick()

        composeRule.onNodeWithContentDescription("第 1 次，开始，09:15").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("第 1 次，结束，13:46").assertIsDisplayed()
    }

    @Test
    fun detailsRemainReadableAt200PercentFontScale() {
        val repository = FakeHandBrewRecordRepository(initialRecords = listOf(record(today, 1)))
        setRecordContent(repository, fontScale = 2f)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("记录时间和感受").performClick()
        val details = composeRule.onNodeWithTag("record_detail_1").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val start = composeRule.onNodeWithTag("record_detail_1_start_time").fetchSemanticsNode().boundsInRoot
        val end = composeRule.onNodeWithTag("record_detail_1_end_time").fetchSemanticsNode().boundsInRoot
        val feeling = composeRule.onNodeWithTag("record_detail_1_feeling").fetchSemanticsNode().boundsInRoot
        composeRule.onNodeWithContentDescription("第 1 次，写感受").assertIsDisplayed()
        assertTrue(start.left >= details.left)
        assertTrue(end.right <= details.right)
        assertTrue(start.right <= end.left)
        assertTrue(feeling.top >= maxOf(start.bottom, end.bottom))
    }

    private fun setRecordContent(
        repository: FakeHandBrewRecordRepository,
        onBack: () -> Unit = {},
        width: Dp? = null,
        viewportHeight: Dp? = null,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            DailyRecordTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(density = 1f, fontScale = fontScale),
                ) {
                    Box(
                        modifier = (width?.let(Modifier::width) ?: Modifier.fillMaxWidth())
                            .then(viewportHeight?.let(Modifier::height) ?: Modifier.fillMaxHeight()),
                    ) {
                        RecordScreen(
                            date = today,
                            today = today,
                            repository = repository,
                            monthRecords = emptyList(),
                            onBack = onBack,
                            onSaved = {},
                        )
                    }
                }
            }
        }
    }

    private fun record(
        date: LocalDate,
        count: Int,
        updatedAt: Instant = Instant.EPOCH,
    ) = HandBrewRecord(
        id = date.toString(),
        localDate = date,
        brewCount = count,
        createdAt = Instant.EPOCH,
        updatedAt = updatedAt,
    )
}
