package io.github.litaog.dailyrecord.ui.record

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.ui.FakeHandBrewRecordRepository
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
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
        setRecordContent(repository)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("清除记录").performClick()
        composeRule.onNodeWithText("清除这天的记录？").assertIsDisplayed()
        composeRule.onNodeWithText("取消").performClick()
        assertEquals(0, repository.clearCalls)

        composeRule.onNodeWithContentDescription("清除记录").performClick()
        composeRule.onNodeWithText("确认清除").performClick()
        composeRule.waitUntil(5_000) { repository.clearCalls == 1 }
        assertEquals(1, repository.clearCalls)
    }

    @Test
    fun backWithDraftAsksBeforeDiscarding() {
        val repository = FakeHandBrewRecordRepository()
        var backCalls = 0
        setRecordContent(repository, onBack = { backCalls += 1 })
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("增加一次").performClick()
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
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
    fun failedSaveRestoresControlsAndShowsMessage() {
        val repository = FakeHandBrewRecordRepository().apply { failSave = true }
        setRecordContent(repository)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("保存记录").performClick()
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
        composeRule.onNodeWithText("清除失败，请重试").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("清除记录").assertIsEnabled()
        composeRule.onNodeWithText("已记录 · 2 次").assertIsDisplayed()
    }

    private fun setRecordContent(
        repository: FakeHandBrewRecordRepository,
        onBack: () -> Unit = {},
    ) {
        composeRule.setContent {
            DailyRecordTheme {
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
