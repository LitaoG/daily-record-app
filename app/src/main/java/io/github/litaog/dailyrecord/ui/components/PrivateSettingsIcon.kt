package io.github.litaog.dailyrecord.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.SexColorTokens

/**
 * Settings entry icon backed by the locked Figma brand artwork.
 *
 * Keep the old parameter shape for callers that already supply the active
 * module tint; the Figma asset itself is selected as Purple or Wine rather
 * than being recoloured and losing its 3D shading.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun PrivateSettingsIcon(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    calendarTint: Color = DailyRecordTextSecondary,
    moduleTint: Color = DailyRecordDefaultAccent,
) {
    BrandIcon(
        asset = BrandIconAsset.Settings,
        theme = if (moduleTint == SexColorTokens.primary) {
            BrandIconTheme.Wine
        } else {
            BrandIconTheme.Purple
        },
        modifier = modifier.size(DailyRecordSizes.SettingsIcon),
        contentDescription = contentDescription,
    )
}
