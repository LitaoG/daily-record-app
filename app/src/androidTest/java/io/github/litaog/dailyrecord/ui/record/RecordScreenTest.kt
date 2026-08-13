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

class RecordScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 7, 17)

    @Test
    fun saveStaysDisabledUntilLoadedRecordChanges() {
        val delayedRecord = MutableSharedFlow<HandBrewRecord?>(extraBufferCapacity = 1)
        val repository = FakeHandBrewRecordRepository(recordFlowOverride = delayedRecord)
        setRecordContent(repository)

        composeRule.onNodeWithContentDescription("正在读取…").assertIsNotEnabled()

        composeRule.runOnIdle { delayedRecord.tryEmit(record(today, 3)) }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithContentDescription("已保存").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("已保存").assertIsNotEnabled()
        composeRule.onNodeWithText("3").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("增加一次").performClick()
        composeRule.onNodeWithContentDescription("保存记录").assertIsEnabled()
    }

    @Test
    fun remoteUpdateRefreshesAnUntouchedDraftButPreservesLocalEdits() {
        val recordUpdates = MutableSharedFlow<HandBrewRecord?>(extraBufferCapacity = 1)
        val repository = FakeHandBrewRecordRepository(recordFlowOverride = recordUpdates)
        setRecordContent(repository)

        composeRule.runOnIdle { recordUpdates.tryEmit(record(today, 2, Instant.EPOCH)) }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithContentDescription("已保存").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("2").assertIsDisplayed()

        composeRule.runOnIdle { recordUpdates.tryEmit(record(today, 4, Instant.EPOCH.plusSeconds(1))) }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("4").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("已保存").assertIsNotEnabled()

        composeRule.onNodeWithContentDescription("增加一次").performClick()
        composeRule.runOnIdle { recordUpdates.tryEmit(record(today, 7, Instant.EPOCH.plusSeconds(2))) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("5").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("保存记录").assertIsEnabled()
    }

    @Test
    fun rapidDoubleTapSavesOnlyOnce() {
        val saveGate = CompletableDeferred<Unit>()
        val repository = FakeHandBrewRecordRepository().apply { this.saveGate = saveGate }
        setRecordContent(repository)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("增加一次").performClick()
        composeRule.onNodeWithTag("save_record_button").performClick()
        composeRule.waitUntil(5_000) { repository.saveCalls == 1 }
        composeRule.onNodeWithTag("save_record_button").assertIsNotEnabled().performTouchInput { click() }

        assertEquals(1, repository.saveCalls)
        composeRule.runOnIdle { saveGate.complete(Unit) }
    }

    @Test
    fun clearRequiresExplicitConfirmation() {
        val repository = FakeHandBrewRecordRepository(listOf(record(today, 2)))
        var backCalls = 0
        setRecordContent(repository, onBack = { backCalls += 1 })
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("清除记录").performClick()
        composeRule.onNodeWithTag("clear_record_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("清除这天的记录？").assertIsDisplayed()
        composeRule.onNodeWithText("只影响当前模块").assertIsDisplayed()
        composeRule.onNodeWithText("取消").performClick()
        assertEquals(0, repository.clearCalls)

        composeRule.onNodeWithContentDescription("清除记录").performClick()
        composeRule.onNodeWithText("确认清除").performClick()
        composeRule.waitUntil(5_000) { repository.clearCalls == 1 }
        assertEquals(1, repository.clearCalls)
        assertEquals(1, backCalls)
    }

    @Test
    fun backWithDraftAsksBeforeDiscarding() {
        val repository = FakeHandBrewRecordRepository()
        var backCalls = 0
        setRecordContent(repository, onBack = { backCalls += 1 })
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("增加一次").performClick()
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithTag("discard_record_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("放弃未保存的修改？").assertIsDisplayed()
        composeRule.onNodeWithText("继续编辑").performClick()
        assertEquals(0, backCalls)

        composeRule.onNodeWithContentDescription("返回日历").performClick()
        composeRule.onNodeWithText("放弃修改").performClick()
        assertEquals(1, backCalls)
    }

    @Test
    fun countControlsStopAtZeroAndRecoverAfterIncrement() {
        val repository = FakeHandBrewRecordRepository()
        setRecordContent(repository)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("减少一次").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("增加一次").performClick()
        composeRule.onNodeWithContentDescription("减少一次").assertIsEnabled().performClick()
        composeRule.onNodeWithContentDescription("减少一次").assertIsNotEnabled()
    }

    @Test
    fun unsavedCountIsDistinctFromAnEmptyRecord() {
        val repository = FakeHandBrewRecordRepository()
        setRecordContent(repository)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("尚未填写").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("增加一次").performClick()
        composeRule.onNodeWithText("待保存 · 1 次").assertIsDisplayed()

        composeRule.onNodeWithTag("save_record_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("已记录 · 1 次").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("已记录 · 1 次").assertIsDisplayed()
    }

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
        composeRule.onNodeWithContentDescription("第 1 次，编辑感受").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("开始").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("结束").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun detailActionsMatchReferenceRowsAndDoNotReserveATimelineColumn() {
        val repository = FakeHandBrewRecordRepository(initialRecords = listOf(record(today, 2)))
        setRecordContent(repository, width = 390.dp)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("记录时间和感受").performClick()

        val section = composeRule.onNodeWithTag("record_details_section").fetchSemanticsNode().boundsInRoot
        val firstStart = composeRule.onNodeWithTag("record_detail_1_start_time").fetchSemanticsNode().boundsInRoot
        val firstEnd = composeRule.onNodeWithTag("record_detail_1_end_time").fetchSemanticsNode().boundsInRoot
        val firstFeeling = composeRule.onNodeWithTag("record_detail_1_feeling").fetchSemanticsNode().boundsInRoot
        val secondStart = composeRule.onNodeWithTag("record_detail_2_start_time").fetchSemanticsNode().boundsInRoot

        assertEquals(section.left, firstStart.left, 0.5f)
        assertEquals(firstStart.top, firstEnd.top, 0.5f)
        assertTrue(firstFeeling.top >= maxOf(firstStart.bottom, firstEnd.bottom))
        assertEquals(firstStart.height, firstEnd.height, 0.5f)
        assertEquals(section.right, firstFeeling.right, 0.5f)
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
        composeRule.onNodeWithContentDescription("返回日历").assertIsDisplayed()
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
    fun detailsReflowInsideANarrowViewport() {
        val repository = FakeHandBrewRecordRepository(initialRecords = listOf(record(today, 1)))
        setRecordContent(repository, width = 260.dp)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("记录时间和感受").performClick()
        val details = composeRule.onNodeWithTag("record_detail_1").fetchSemanticsNode().boundsInRoot
        val screen = composeRule.onNodeWithTag("record_screen").fetchSemanticsNode().boundsInRoot
        assertTrue(details.left >= screen.left)
        assertTrue(details.right <= screen.right)
        val start = composeRule.onNodeWithTag("record_detail_1_start_time").fetchSemanticsNode().boundsInRoot
        val end = composeRule.onNodeWithTag("record_detail_1_end_time").fetchSemanticsNode().boundsInRoot
        val feeling = composeRule.onNodeWithTag("record_detail_1_feeling").fetchSemanticsNode().boundsInRoot
        composeRule.onNodeWithContentDescription("第 1 次，写感受").assertIsDisplayed()
        assertTrue(start.right <= end.left)
        assertTrue(feeling.top >= maxOf(start.bottom, end.bottom))
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

    @Test
    fun recordActionsFollowMonthSummaryAndScrollWithContent() {
        val repository = FakeHandBrewRecordRepository(
            initialRecords = listOf(record(today, 12)),
        )
        setRecordContent(repository, viewportHeight = 720.dp)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("记录时间和感受").performClick()
        composeRule.waitForIdle()

        repeat(24) {
            composeRule.onNodeWithTag("record_scroll_content")
                .performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
        composeRule.waitForIdle()

        val scrollBounds = composeRule.onNodeWithTag("record_scroll_content")
            .fetchSemanticsNode()
            .boundsInRoot
        val summaryBounds = composeRule.onNodeWithTag("record_month_summary")
            .fetchSemanticsNode()
            .boundsInRoot
        val actionBounds = composeRule.onNodeWithTag("record_actions")
            .fetchSemanticsNode()
            .boundsInRoot
        val lastDetailBounds = composeRule.onNodeWithTag("record_detail_12")
            .fetchSemanticsNode()
            .boundsInRoot

        composeRule.onNodeWithTag("record_month_summary").assertIsDisplayed()
        composeRule.onNodeWithTag("record_actions").assertIsDisplayed()
        assertTrue(actionBounds.top >= summaryBounds.bottom)
        assertTrue(actionBounds.bottom <= scrollBounds.bottom)
        assertTrue(lastDetailBounds.bottom <= actionBounds.top)
        composeRule.onNodeWithTag("record_detail_12").assertIsDisplayed()
        composeRule.onNodeWithTag("save_record_button").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("清除记录").assertIsDisplayed()
    }

    @Test
    fun recordActionsFollowContentFlowOnNarrowLargeTextViewport() {
        val repository = FakeHandBrewRecordRepository(
            initialRecords = listOf(record(today, 12)),
        )
        setRecordContent(repository, width = 260.dp, viewportHeight = 720.dp, fontScale = 2f)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("记录时间和感受").performClick()
        composeRule.waitForIdle()
        repeat(30) {
            composeRule.onNodeWithTag("record_scroll_content")
                .performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
        composeRule.waitForIdle()

        val scrollBounds = composeRule.onNodeWithTag("record_scroll_content")
            .fetchSemanticsNode()
            .boundsInRoot
        val summaryBounds = composeRule.onNodeWithTag("record_month_summary")
            .fetchSemanticsNode()
            .boundsInRoot
        val actionBounds = composeRule.onNodeWithTag("record_actions")
            .fetchSemanticsNode()
            .boundsInRoot
        val lastDetailBounds = composeRule.onNodeWithTag("record_detail_12")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(actionBounds.top >= summaryBounds.bottom)
        assertTrue(actionBounds.bottom <= scrollBounds.bottom)
        assertTrue(lastDetailBounds.bottom <= actionBounds.top)
        composeRule.onNodeWithTag("record_detail_12").assertIsDisplayed()
        composeRule.onNodeWithTag("save_record_button").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("清除记录").assertIsDisplayed()
    }

    @Test
    fun expandedDetailsFollowCountAndDisappearWhenCountReturnsToZero() {
        val repository = FakeHandBrewRecordRepository()
        setRecordContent(repository)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("增加一次").performClick()
        composeRule.onNodeWithContentDescription("记录时间和感受").performClick()
        composeRule.onNodeWithTag("record_detail_1").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("增加一次").performClick()
        composeRule.onNodeWithTag("record_detail_2").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("减少一次").performClick()
        assertTrue(composeRule.onAllNodesWithTag("record_detail_2").fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithContentDescription("减少一次").performClick()
        assertTrue(
            composeRule.onAllNodesWithContentDescription("记录时间和感受")
                .fetchSemanticsNodes()
                .isEmpty(),
        )
        assertTrue(composeRule.onAllNodesWithTag("record_detail_1").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun failedSaveRestoresControlsAndShowsMessage() {
        val repository = FakeHandBrewRecordRepository().apply { failSave = true }
        setRecordContent(repository)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("保存记录").performClick()
        composeRule.onNodeWithTag("daily_record_snackbar").assertIsDisplayed()
        composeRule.onNodeWithText("保存失败，请重试").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("保存记录").assertIsEnabled()
    }

    @Test
    fun failedClearKeepsRecordAndRestoresControls() {
        val repository = FakeHandBrewRecordRepository(listOf(record(today, 2))).apply { failClear = true }
        setRecordContent(repository)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("清除记录").performClick()
        composeRule.onNodeWithText("确认清除").performClick()
        composeRule.onNodeWithTag("daily_record_snackbar").assertIsDisplayed()
        composeRule.onNodeWithText("清除失败，请重试").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("清除记录").assertIsEnabled()
        composeRule.onNodeWithText("已记录 · 2 次").assertIsDisplayed()
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
