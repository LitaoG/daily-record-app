package io.github.litaog.dailyrecord.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes

/**
 * Settings entry icon: the vector gear tinted with the active module accent
 * of the selected theme.
 *
 * Keeps the old parameter shape so callers that already pass the module tint
 * stay unchanged; [calendarTint] is no longer drawn.
 */
@Composable
fun PrivateSettingsIcon(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    moduleTint: Color = DailyRecordDefaultAccent,
) {
    BrandIcon(
        asset = BrandIconAsset.Settings,
        tint = moduleTint,
        modifier = modifier.size(DailyRecordSizes.SettingsIcon),
        contentDescription = contentDescription,
    )
}

