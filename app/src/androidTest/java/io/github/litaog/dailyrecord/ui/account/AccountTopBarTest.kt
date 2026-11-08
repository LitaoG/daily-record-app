package io.github.litaog.dailyrecord.ui.account

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccountTopBarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun pendingChipStaysCompactAt200PercentText() {
        assertCompactChipAt200PercentText(SyncStatus.Pending(1))
    }

    @Test
    fun failedChipStaysFlatAt200PercentText() {
        assertCompactChipAt200PercentText(
            SyncStatus.Failed(message = "network unavailable"),
        )
    }

    private fun assertCompactChipAt200PercentText(status: SyncStatus) {
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                DailyRecordTheme {
                    Box(modifier = androidx.compose.ui.Modifier.width(360.dp)) {
                        AccountTopBar(
                            status = status,
                            colors = HandBrewColorTokens,
                            onClick = {},
                            onSettings = {},
                        )
                    }
                }
            }
        }

        val chip = composeRule.onNodeWithContentDescription(
            AppCopy.Account.syncChipDescription(status.label()),
        )
        chip.assertIsDisplayed()

        val density = composeRule.activity.resources.displayMetrics.density
        val bounds = chip.fetchSemanticsNode().boundsInRoot
        val widthDp = bounds.width / density
        val heightDp = bounds.height / density
        assertTrue("${status.label()} chip was stretched to $widthDp dp", widthDp < 240f)
        // The interactive node carries the 48dp touch contract (#261, #287);
        // the visual pill inside it stays 38dp (#278).
        assertEquals("${status.label()} chip touch target", 48f, heightDp, 0.5f)
        val pillHeightDp = composeRule
            .onNodeWithTag("sync_status_chip_pill", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
            .height / density
        assertEquals("${status.label()} chip pill height", 38f, pillHeightDp, 0.5f)
    }
}
