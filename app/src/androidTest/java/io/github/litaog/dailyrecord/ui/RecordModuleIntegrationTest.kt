package io.github.litaog.dailyrecord.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import java.time.Instant
import java.time.LocalDate
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

        composeRule.onNodeWithText("本月手冲").assertIsDisplayed()
        composeRule.onNodeWithText("2 次 · 1 天").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("上个月").performClick()
        composeRule.onNodeWithContentDescription("选择年份和日期，当前2026年6月").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("做爱记录，未选择").performClick()

        composeRule.onNodeWithText("本月做爱").assertIsDisplayed()
        composeRule.onNodeWithText("0 次 · 0 天").assertIsDisplayed()
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
        composeRule.onNodeWithContentDescription("2026年7月17日，做爱 1 次，已选择").performClick()
        composeRule.onNodeWithText("今天做爱了几次？").assertIsDisplayed()
        composeRule.onNodeWithText("0 次＝明确没有，会保留记录。").assertIsDisplayed()
    }

    private fun setDualModuleContent() {
        composeRule.setContent {
            DailyRecordTheme {
                HandBrewApp(
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
        composeRule.waitForIdle()
    }
}
