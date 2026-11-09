package io.github.litaog.dailyrecord.ui.calendar

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class IntimacyOnboardingCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dismissActionKeepsThe48DpTouchTarget() {
        composeRule.setContent {
            DailyRecordTheme {
                IntimacyOnboardingCard(
                    colors = HandBrewColorTokens,
                    onDismiss = {},
                )
            }
        }

        // The dismiss label is visually small; its touch and TalkBack target
        // must still meet the 48dp contract so the card cannot regress to a
        // bare clickable again (issue #287).
        val density = ApplicationProvider.getApplicationContext<Context>()
            .resources.displayMetrics.density
        val bounds = composeRule
            .onNodeWithTag("intimacy_onboarding_dismiss")
            .fetchSemanticsNode()
            .boundsInRoot
        val widthDp = bounds.width / density
        val heightDp = bounds.height / density
        assertTrue("Dismiss touch target width was $widthDp dp", widthDp >= 48f)
        assertTrue("Dismiss touch target height was $heightDp dp", heightDp >= 48f)
    }
}
