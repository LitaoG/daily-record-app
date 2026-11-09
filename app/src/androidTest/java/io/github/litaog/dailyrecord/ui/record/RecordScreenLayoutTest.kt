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
import androidx.compose.ui.test.performScrollTo
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


class RecordScreenLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 7, 17)


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
        // Since the pinned save bar (docs/UI_UX.md: keep the bottom save bar
        // fixed), the bar sits below the scrolling viewport instead of inside it.
        val screenBounds = composeRule.onNodeWithTag("record_screen").fetchSemanticsNode().boundsInRoot
        assertTrue(actionBounds.bottom <= screenBounds.bottom)
        assertTrue(actionBounds.bottom >= scrollBounds.bottom - 1)
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

        composeRule.onNodeWithContentDescription("记录时间和感受")
            .performScrollTo()
            .performClick()
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
        // Since the pinned save bar (docs/UI_UX.md: keep the bottom save bar
        // fixed), the bar sits below the scrolling viewport instead of inside it.
        val screenBounds = composeRule.onNodeWithTag("record_screen").fetchSemanticsNode().boundsInRoot
        assertTrue(actionBounds.bottom <= screenBounds.bottom)
        assertTrue(actionBounds.bottom >= scrollBounds.bottom - 1)
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
