package io.github.litaog.dailyrecord.ui.record

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.SexColorTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecordTimePickerDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun wheelsInitializeAtBoundaryAndWrapToMidnight() {
        var confirmedMinutes: Int? = null

        composeRule.setContent {
            DailyRecordTheme {
                RecordTimePickerDialog(
                    initialMinutes = 23 * 60 + 59,
                    colors = HandBrewColorTokens,
                    onDismiss = {},
                    onConfirm = { confirmedMinutes = it },
                )
            }
        }

        composeRule.onNodeWithTag("time_picker_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("time_picker_hour_wheel").assertIsDisplayed()
        composeRule.onNodeWithTag("time_picker_minute_wheel").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("小时，当前 23").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("分钟，当前 59").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("请输入时间").fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithContentDescription("选择小时 00").performClick()
        composeRule.onNodeWithContentDescription("选择分钟 00").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("确定").performClick()

        assertEquals(0, confirmedMinutes)
    }

    @Test
    fun cancelDoesNotConfirmAndLargeTextKeepsActionsOperable() {
        var dismissCalls = 0
        var confirmedMinutes: Int? = null

        composeRule.setContent {
            DailyRecordTheme {
                CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                    RecordTimePickerDialog(
                        initialMinutes = 17 * 60 + 30,
                        colors = HandBrewColorTokens,
                        onDismiss = { dismissCalls += 1 },
                        onConfirm = { confirmedMinutes = it },
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("小时，当前 17").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("分钟，当前 30").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("取消").assertIsDisplayed().performClick()

        assertEquals(1, dismissCalls)
        assertNull(confirmedMinutes)
    }

    @Test
    fun verticalDragAdvancesTheHourWithMomentum() {
        var confirmedMinutes: Int? = null

        composeRule.setContent {
            DailyRecordTheme {
                RecordTimePickerDialog(
                    initialMinutes = 17 * 60 + 30,
                    colors = HandBrewColorTokens,
                    onDismiss = {},
                    onConfirm = { confirmedMinutes = it },
                )
            }
        }

        composeRule.onNodeWithTag("time_picker_hour_wheel_surface").performTouchInput {
            swipe(
                start = center,
                end = Offset(center.x, center.y - height / 3f),
                durationMillis = 300,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("确定").performClick()

        assertTrue(requireNotNull(confirmedMinutes) > 17 * 60 + 30)
    }

    @Test
    fun verticalDragAdvancesTheMinuteWithMomentum() {
        var confirmedMinutes: Int? = null

        composeRule.setContent {
            DailyRecordTheme {
                RecordTimePickerDialog(
                    initialMinutes = 17 * 60 + 30,
                    colors = HandBrewColorTokens,
                    onDismiss = {},
                    onConfirm = { confirmedMinutes = it },
                )
            }
        }

        composeRule.onNodeWithTag("time_picker_minute_wheel_surface").performTouchInput {
            swipe(
                start = center,
                end = Offset(center.x, center.y - height / 3f),
                durationMillis = 300,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("确定").performClick()

        assertTrue(requireNotNull(confirmedMinutes) > 17 * 60 + 30)
    }

    @Test
    fun fastFlingContinuesAcrossMultipleHourValuesBeforeSettling() {
        var confirmedMinutes: Int? = null

        composeRule.setContent {
            DailyRecordTheme {
                RecordTimePickerDialog(
                    initialMinutes = 0,
                    colors = HandBrewColorTokens,
                    onDismiss = {},
                    onConfirm = { confirmedMinutes = it },
                )
            }
        }

        composeRule.onNodeWithTag("time_picker_hour_wheel_surface").performTouchInput {
            swipe(
                start = center,
                end = Offset(center.x, center.y - height * .9f),
                durationMillis = 80,
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("确定").performClick()

        assertTrue(requireNotNull(confirmedMinutes) / 60 >= 2)
    }

    @Test
    fun dialogContentStaysInsideTheAvailableWindow() {
        composeRule.setContent {
            DailyRecordTheme {
                RecordTimePickerDialog(
                    initialMinutes = 12 * 60,
                    colors = HandBrewColorTokens,
                    onDismiss = {},
                    onConfirm = {},
                )
            }
        }

        val rootBounds = composeRule.onAllNodes(isRoot()).fetchSemanticsNodes()
            .maxBy { it.boundsInRoot.width * it.boundsInRoot.height }
            .boundsInRoot
        val dialogBounds = composeRule.onNodeWithTag("time_picker_dialog").fetchSemanticsNode().boundsInRoot
        val hourBounds = composeRule.onNodeWithTag("time_picker_hour_wheel").fetchSemanticsNode().boundsInRoot
        val minuteBounds = composeRule.onNodeWithTag("time_picker_minute_wheel").fetchSemanticsNode().boundsInRoot

        assertTrue(dialogBounds.left >= rootBounds.left)
        assertTrue(dialogBounds.right <= rootBounds.right)
        assertTrue(hourBounds.right <= minuteBounds.left)
        composeRule.onNodeWithContentDescription("取消").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("确定").assertIsDisplayed()
    }

    @Test
    fun wheelUsesTheActiveRecordModulePalette() {
        val activeColors = mutableStateOf(HandBrewColorTokens)

        composeRule.setContent {
            DailyRecordTheme {
                RecordTimePickerDialog(
                    initialMinutes = 17 * 60 + 30,
                    colors = activeColors.value,
                    onDismiss = {},
                    onConfirm = {},
                )
            }
        }

        assertWheelSelectionBandColor(HandBrewColorTokens)
        composeRule.runOnIdle { activeColors.value = SexColorTokens }
        assertWheelSelectionBandColor(SexColorTokens)
    }

    private fun assertWheelSelectionBandColor(colors: RecordModuleColorTokens) {
        var bitmap: ImageBitmap? = null
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                bitmap = composeRule
                    .onNodeWithTag("time_picker_hour_wheel_surface")
                    .captureToImage()
            }.isSuccess
        }
        val pixels = requireNotNull(bitmap).toPixelMap()
        val sampleX = pixels.width / 4
        val sampleY = pixels.height / 2
        val expected = colors.soft.copy(alpha = .88f).compositeOver(
            colors.soft.copy(alpha = .34f).compositeOver(DailyRecordSurface),
        )

        assertEquals(expected.toArgb(), pixels[sampleX, sampleY].toArgb())
    }
}
